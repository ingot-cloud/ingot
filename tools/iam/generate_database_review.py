"""可重现的逐表主键评估；结构取自manifest，审查结论在本文件维护。"""
import argparse
import json
from pathlib import Path
import re
from database_sources import ROOT
from schema_model import tables, comment, closing, parts
from schema_comments import SNAPSHOT

TARGET = ROOT / 'docs/modules/iam/DATABASE-REVIEW.md'

# 评估依据为当前生产入口；不使用行数、负载或碎片的虚构测量。
NOTES = {
'iam_account': '全局账号按ID及登录名定位；手机号/邮箱索引服务匹配检索。账号、成员和外部用户标识不可混用。',
'iam_platform_member': '按账号唯一定位平台资格；状态列表使用status/id。account_id唯一索引同时防止重复资格。',
'iam_tenant': '组织ID是组织事务的锁定对象；(id,owner_member_id)支持所属组织拥有者复合引用，不因id已有主键就删除。',
'iam_tenant_member': '按组织/账号唯一定位资格，状态列表及跨域账号反查有独立索引；(tenant_id,id)用于同租户复合外键。',
'iam_department': '按ID定位，部门树按tenant_id/parent_id访问；(tenant_id,id)承担租户归属约束，属于有意的复合引用键。',
'iam_member_department': '按tenant_id/member_id聚集，反向tenant_id/department_id/member_id支持部门成员；主部门辅助列唯一约束独立保留。本轮两入口共用差量写入。',
'iam_department_leader': '按tenant_id/department_id聚集，成员反查有反向索引；负责人关系与任职关系独立。本轮保留未变主标记，增删仍按差量。',
'iam_platform_group': '用户组主行按ID加锁及检查版本，成员关系独立维护；删除时清子关系是必要操作。',
'iam_platform_group_member': 'group_id/member_id自然唯一，支持整组读取；member_id/group_id支持成员反查。本轮保存按集合差量，保留原行。',
'iam_tenant_group': '用户组主行按ID锁定；(tenant_id,id)支持关系同租户约束，不改为只校验group_id。',
'iam_tenant_group_member': 'tenant_id/group_id/member_id支持读取整组，反向成员索引支撑授权组求值；本轮差量增删。',
'iam_tenant_group_department': '按组读取部门来源，反向部门索引支撑展开；include_descendants是关系属性，本轮仅属性变化时UPDATE。',
'iam_application': '应用按ID或全局code查找，治理目录含预留ID；应用域及baseline有业务约束，ID顺序不等于创建时间。',
'iam_resource': '按应用/code查询资源；(application_id,id)用于同应用操作外键，不能按逻辑冗余直接删除。',
'iam_action': '操作码全局唯一且精确；按应用/资源列举操作，复合应用引用键保护菜单与操作归属。',
'iam_menu': '按应用/父节点/排序构建菜单树；菜单操作绑定本轮按差量维护；路由键不承担数据库行标识。',
'iam_menu_action': 'application_id/menu_id/action_id支持菜单操作读取；反向索引服务操作引用检查。本轮仅绑定差异增删。',
'iam_tenant_app_entitlement': '按tenant_id/application_id唯一配置，时效是行属性。整体开通替换会删除并重建人群，属于独立业务语义，本轮保留并列为后续评审。',
'iam_app_audience': 'tenant_id/application_id就是配置身份；开通引用使用同一组合。独立保存仍清空重写子选择，且并发保护需与开通重置共同设计，本轮不改。',
'iam_audience_member': '按租户/应用读取所选成员，成员复合外键所需的反向索引由MySQL补建；本轮不将人群重建机械替换为差量。',
'iam_audience_department': '按租户/应用读取部门及include_descendants，反向部门归属由外键索引支持；需连同人群并发及模式切换评估。',
'iam_audience_group': '按租户/应用读取用户组来源，反向组索引由外键补建；需和人群保存事务一起评估。',
'iam_plan': '按ID锁定套餐版本；修改套餐应用目录不回写已有租户的开通事实。',
'iam_plan_application': 'plan_id/application_id支持整套餐读取并自然去重；应用外键的反向索引由MySQL补建。本轮保存按集合差量。',
'iam_role_definition': '角色身份与快照分开；current_revision_id用于共享/租户当前权限，平台保留固定版本。domain/tenant_key/id/kind约束角色域归属。',
'iam_role_revision': '按角色/发布序号唯一保存不可变权限；id/role_id、id/kind服务复合引用。发布创建新快照，不是同一关系的无效删插。',
'iam_role_parameter': '按revision_id加载参数定义；parameter_key是版本内自然键。128字符utf8mb4参数名放大聚簇键，但当前缺少实际参数数和缓存命中证据。',
'iam_role_grant': '按版本一次读取操作/范围JSON，不可变发布创建新版本；action_id外键隐式索引支持引用校验。不得跨版本原地差量覆盖。',
'iam_role_delta': '历史固定基础的角色差异按版本/操作唯一；租户当前快照模型另有归属。本轮保留历史记录及快照生命周期。',
'iam_delegation_grant': '按管理员/status/有效期寻找委派；domain/tenant_key/id及tenant_id/id用于所属域复合外键。撤销与重建子关系不能拆掉派生授权校验。',
'iam_delegation_role_revision': '平台委派按委派ID读取固定版本白名单；复合版本种类外键校验有效引用。委派替换重建子行本轮只审查。',
'iam_delegation_recipient_member': '已补显式id主键，应用复用IAM Leaf发号并使用IdType.INPUT；原delegation_id/member_id业务唯一键、生成列及全部外键/CHECK保留。两域入口显式赋值，历史回填从iam高水位预留号段，不用ID解释业务顺序。',
'iam_delegation_recipient_department': '按delegation_id读取部门白名单；tenant_id主要用于复合所属租户外键。include_descendants是属性，整体替换涉及委派上限，本轮保留。',
'iam_delegation_action_ceiling': '平台委派按委派ID/操作读取范围及绑定；与角色白名单、接收者、派生分配一致性共事务，本轮保留子集重建。',
'iam_role_assignment': '独立分配实体含状态/有效期/版本和历史引用，适合稳定ID；成员/组/角色/委派/版本批处理索引分别服务不同入口。多个索引的真实成本需负载数据验证。',
'iam_delegation_role': '租户委派按委派ID/角色身份读取，角色归属复合外键不能因role_id全局唯一而省略；清子行与派生授权重验有关，本轮保留。',
'iam_delegation_resource_ceiling': '租户当前模型按委派ID/资源读取范围上限，绑定JSON不构成主键；不把此路径改成平台固定版本语义。',
'iam_default_policy_revision': '策略kind/revision唯一，引用固定默认版本；发布为新快照，和关系原地替换不同。',
'iam_policy_selector': '选择器是租户内独立对象，成员及部门来源通过(tenant_id,id)引用；策略保存构造新选择器，不作为低风险差量目标。',
'iam_policy_selector_member': 'tenant_id/selector_id/member_id支持选择器展开；成员反向索引由外键补建。新选择器快照的首次插入应保留。',
'iam_policy_selector_department': '按选择器展开部门范围及下级，部门反向索引由外键补建；必须保留选择器与租户一致性。',
'iam_directory_policy': '一租户一配置，以tenant_id直接作为主键；默认版本/选择器引用与本地版本锁分别承担不同语义。',
'iam_directory_rule': '规则有独立ID，tenant_id/effect/id用于租户规则匹配；允许和拒绝不是简单双向多对多边。',
'iam_field_policy': '一租户一字段策略，tenant_id主键即业务身份；operation_rules为数组，本轮不改变默认及合并规则。',
'iam_field_rule': '按租户/场景/字段读取可见性规则；viewer_selector及target_scope是不同维度，不并入主键。',
'iam_member_export': '任务按ID定位，租户/status/到期时间索引支持状态及清理；JSON成员快照可能较大，需要实际任务量及保留周期。',
'iam_authorization_audit': '审计按租户时间、操作者、分配创建及待投递分别检索；event_id唯一承担幂等。需实际保留量、写入速率和执行计划才能判断索引增删。',
'iam_migration_batch': '迁移工具显式指定字符串批次标识，不是在线高频写入表；状态与verified_at共同限制验收过程。',
'iam_migration_mapping': '五个utf8mb4字符串构成宽自然主键，最大键值约1792字节（不含变长/页开销）；历史迁移已退出常规运行范围。若恢复大批导入，比较短代理键+原唯一键的总写入/空间，先采样再决定。',
'iam_migration_issue': '独立问题ID，batch_id/disposition/id定位未解决问题；处置说明保留，不能将删除问题记录视作优化。',
'iam_role_application_task': '按角色/准备键幂等；running_role_id唯一限制同角色一个RUNNING；状态/租约/id服务多实例领取。状态变化触发辅助索引维护是必要成本。',
'iam_role_application_item': 'task_id/assignment_id唯一去重，task_id/status/id支持批执行；独立ID承载明细状态和配置，不能按纯关联表处理。',
'iam_live_authorization_batch': '受控迁移工具显式批次标识与摘要，低频；正反迁移状态不等价于日常业务版本。',
'iam_live_authorization_mapping': '批次/实体类型/实体ID自然唯一，保存回退原行；键宽约104字节，工作量取决于实际迁移规模，未恢复在线写路径。',
'biz_leaf_alloc': '按业务标签锁定并领取号段，高水位增长不代表新增记录。biz_tag短字符串业务主键合适；禁止为改善顺序插入而重置高水位。',
'platform_dict': '树父节点、编码、类型状态和作用域各有读取索引；code当前不是数据库唯一键，不能无业务确认直接加唯一约束。',
'security_event': '数据库自增聚簇键，事件幂等、时间、用户、追踪索引分别服务入口；事件量和保留策略决定成本。event_id允许NULL，因此缺失幂等ID不互斥。',
'sys_social_details': '按ID及渠道配置定位，第三方app_id为字符串客户端标识；旧列名不意味着当前IAM数字应用ID。',
'sys_user_social': '独立绑定ID，unique_id/type/user_id反查渠道身份；是否需要其他方向查询和业务唯一约束需实际入口与历史重复数据共同验证。',
'sys_tenant_plan_record': '历史购买记录是事实实体，保留稳定ID；INT plan_id与当前BIGINT套餐并非可直接迁移的外键，本轮仅说明。',
'account_lock_state': '数据库自增ID，user_id/user_type业务唯一；locked/locked_until服务解锁扫描。框架持久化归属保留，不迁入IAM自有Mapper。',
'password_history': '保留原自增主键及user_id/sequence_number业务唯一键；经隔离MySQL三态对照后删除重叠idx_user_id。实际PasswordHistoryServiceImpl按用户查询/覆盖槽位/删除；最近历史仍按created_at排序，本轮不改变环形历史行为。',
'password_expiration': '自增ID，user_id唯一，expires_at和next_warning_at服务到期和提醒扫描；宽时效扫描成本需实际用户量及调度统计。',
}
RUNTIME = {'iam_role_parameter','iam_role_assignment','iam_member_export','iam_authorization_audit','security_event','password_expiration','sys_user_social'}
CANDIDATES = {'iam_migration_mapping'}
REWRITES = {'iam_member_department':'已改为共享差量写入', 'iam_department_leader':'已为差量；本轮跳过不变主标记',
 'iam_platform_group_member':'已改为差量写入','iam_tenant_group_member':'已改为差量写入','iam_tenant_group_department':'已改为差量写入及属性UPDATE',
 'iam_menu_action':'已改为差量写入','iam_plan_application':'已改为差量写入'}


def primary(table):
    for definition in table.constraints:
        match = re.match(r'PRIMARY KEY\s*\(([^)]+)\)',definition,re.I)
        if match:return [x.strip(' `') for x in match.group(1).split(',')]
    return [name for name, definition in table.columns.items() if re.search(r'\bPRIMARY KEY\b', definition,re.I)]


def width(table, keys):
    total = 0
    for key in keys:
        definition = table.columns[key]
        if re.search(r'\bbigint\b',definition,re.I):total+=8
        elif re.search(r'\bint\b',definition,re.I):total+=4
        else:
            match=re.search(r'(?:var)?char\((\d+)\)',definition,re.I)
            if not match:return '需按实际类型计算'
            total+=int(match.group(1))*(1 if 'CHARACTER SET ascii' in definition else 3 if 'utf8mb3' in definition else 4)
    return str(total)+' 字节（最大键值，不含记录头及变长开销）'


def render():
    model=tables()
    assert set(NOTES)=={t.name for t in model},'逐表审查未覆盖全部manifest'
    snapshot=json.loads(SNAPSHOT.read_text())
    inbound = {table.name: [] for table in model}
    for origin in model:
        for constraint in origin.constraints:
            reference = re.search(r'FOREIGN KEY\s*\(([^)]+)\)\s*REFERENCES\s+`?([a-z_]+)`?\s*\(([^)]+)\)', constraint, re.I)
            if reference and reference.group(2) in inbound:
                inbound[reference.group(2)].append(origin.name + '(' + reference.group(1) + ') → (' + reference.group(3) + ')')

    java=[(p,p.read_text()) for base in ('ingot-service','ingot-framework','tools/iam')
          for p in sorted((ROOT/base).rglob('*')) if p.is_file() and ('src/main/' in str(p) and p.suffix in ('.java','.xml') or base=='tools/iam' and p.suffix=='.py' and not p.name.startswith(('test','generate','schema')))]
    lines=['# IAM 主键、索引与关系写入评估','', '<!-- 生成：python3 tools/iam/generate_database_review.py；审查结论在生成源维护。 -->','',
           '覆盖 manifest 中全部63张表。结构来自权威DDL及隔离MySQL 8.4的有效索引快照；没有连接实际业务数据库，没有测量业务容量、页分裂率、缓存命中率或延迟。', '',
           '## 评估口径','',
           '- 结论分为保留、候选优化、需要运行数据验证。P1为模型明确性候选，P2为成本验证候选，P3为保留现状。',
           '- 联合主键按最左列依次排序；为已有成员/组添加关系可能写入旧页。局部性收益与页分裂成本应一并比较，不把非递增视为错误。',
           '- 加递增ID仍须保留业务唯一索引，原关系键仍可能中间插入，并增加一棵索引；反向索引已经包含主键列时不得重复计算全部主键附加成本。',
           '- BIGINT键值按8字节估算；字符串按定义字符集最大字节估算，不是磁盘测量。业务非自增ID由服务端发号或目录预留，不承诺严格提交时序。',
           '- 有效索引包括MySQL为外键隐式补建的索引；复合唯一键常用于验证所属租户/应用，不能仅因id全局唯一就删除。',
           '- 纯键投影可能覆盖查询；读取JSON、状态等其他属性通常仍需聚簇行。是否发生回表以具体SQL和EXPLAIN为准。', '',
           '依据：[InnoDB聚簇与二级索引](https://dev.mysql.com/doc/refman/8.0/en/innodb-index-types.html)、[索引页结构](https://dev.mysql.com/doc/refman/8.0/en/innodb-physical-structure.html)。', '',
           '## 本轮处理与后续候选','',
           '成员部门、两域用户组关系、菜单操作、套餐应用已实施差量；负责人未变主标记不再清空重设。公共API、版本、审计及授权失效规则保留；后续独立结构变更见下文。', '',
           '接收成员表已新增Leaf显式主键；密码历史经隔离对照删除重叠索引，原主键保留。后续候选仍为宽字符串主键iam_migration_mapping，尚未调整；详见[验证记录](../../../specs/changes/archive/2026/20261020-iam-recipient-primary-key-password-index/VALIDATION.md)。', '',
           '人群独立保存仍全量重写，整体开通替换会重建人群；两者需要统一并发和重置语义后另行设计。委派重建涉及派生授权重验，角色/策略新快照是首次写入，业务删除需要清关联；这些路径保留。', '',
           '## 逐表结论','']
    for t in model:
        keys=primary(t)
        decision='候选优化' if t.name in CANDIDATES else '需要运行数据验证' if t.name in RUNTIME else '保留'
        priority='P2' if decision!='保留' else 'P3'
        definition=snapshot['tables'][t.name];start=definition.index('(');end=closing(definition,start)
        effective=[x for x in parts(definition[start+1:end]) if re.match(r'(?:UNIQUE )?KEY ',x)]
        direct=[(p,text) for p,text in java if re.search(r'\b'+re.escape(t.name)+r'\b',text)]
        entities=[p.stem for p,text in direct if '@TableName' in text]
        related=[p for p,text in java if p.name.endswith('Repository.java') and any(re.search(r'\b'+e+r'\b',text) for e in entities)]
        evidence=list(dict.fromkeys(related+[p for p,_ in direct]))[:3]
        if t.name == 'password_history':
            evidence = [ROOT / 'ingot-framework/ingot-security/ingot-security-credential-data/src/main/java/com/ingot/framework/security/credential/data/service/PasswordHistoryServiceImpl.java']
        links=', '.join(f'[{p.name}](../../../{p.relative_to(ROOT)})' for p in evidence) or '受控迁移/建库工具，未发现在线业务写入入口'
        pk=' / '.join(keys) if keys else '未声明；唯一键含可空生成列，使用InnoDB隐藏行标识'
        gen='数据库AUTO_INCREMENT' if any('AUTO_INCREMENT' in x.upper() for x in t.columns.values()) else '组合关系/业务配置键，由绑定对象或受控工具提供' if len(keys)!=1 or keys[0]!='id' else '服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号'
        gen = {'iam_migration_batch': '历史迁移工具显式指定字符串批次标识',
               'iam_live_authorization_batch': '即时授权迁移工具显式指定字符串批次标识',
               'iam_delegation_recipient_member': 'IAM Leaf号段发号，应用显式填入BigInteger/IdType.INPUT',
               'biz_leaf_alloc': '配置的业务标签字符串，原行更新号段高水位'}.get(t.name,gen)
        foreign=[x for x in parts(definition[start+1:end]) if 'FOREIGN KEY' in x.upper()]
        lines += [f'### {t.name} — {decision}（{priority}）','',comment(t.options),'',
                  f'- 主键：`{pk}`；宽度：{width(t,keys) if keys else "隐藏行标识6字节，另维护业务唯一索引"}。',
                  f'- 标识来源：{gen}。', f'- 主要访问/写入：{links}。',
                  '- 有效二级索引/业务唯一键：'+('；'.join('``'+x+'``' for x in effective) if effective else '无')+'。',
                  '- 本表引用外键：'+('；'.join('``'+x.replace('\n',' ')+'``' for x in foreign) if foreign else 'DDL未声明；业务归属仍须由消费方校验')+'。',
                  '- 被其他表引用：'+('；'.join('``'+item+'``' for item in inbound[t.name]) or '未声明入向外键')+'。',
                  f'- 关系重写：{REWRITES.get(t.name,"参照下述入口语义；本轮保留该表写入生命周期")}。',
                  '- 判断：'+NOTES[t.name],
                  f'- DDL证据：[{t.source.name}](../../../{t.source.relative_to(ROOT)})；列定义见[数据字典](DATA-DICTIONARY.md#{t.name})。','']
        if decision!='保留':
            lines+=['后续验证：采集该表行数、主键/二级索引体积、写入频率、代表SQL的EXPLAIN及缓冲池命中；使用匿名等量数据比较原结构与候选结构的总写入、查询和空间。结构变更必须单独批准并提供外键、数据校验、发布及回滚方案。','']
    return '\n'.join(lines)


def build(check=False):
    expected=render()
    if check:
        if not TARGET.exists() or TARGET.read_text()!=expected:raise ValueError('DATABASE-REVIEW.md differs')
    else:TARGET.write_text(expected)


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check',action='store_true')
    build(parser.parse_args().check)
