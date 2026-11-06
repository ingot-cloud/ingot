# 现有 IAM 数据迁移与回滚

> 已交付结构增量、只读导出/预检、正反转换与验证工具，并在隔离 MySQL 演练。没有连接或切换实际业务数据库。运行步骤见第 8 节。

> 未上线、租户尚未测试且021已执行的环境，按用户补充决定直接使用第9节的一键准备脚本；不需要逐步执行第8节的数据保留迁移。

## 1. 适用范围与交付物

适用于现有 IAM 目标库，不读取旧 PMS 运行表。新库更新权威 001–006、manifest 和生成 bundle；已有库使用独立增量，禁止执行包含 DROP 的完整初始化文件。实现应交付只读预检、来源清单、显式冲突处置输入、结构增量、正向转换、验证、反向转换和运行手册；文件落点为 databases/iam/migrations 与 tools/iam 下本能力目录，编号在实施时按实际清单分配，不能覆盖已有补丁。

DDL 在 MySQL 自动提交；增列/表/外键不宣称被业务事务整体回滚。增加结构、数据转换、程序切换是独立检查点。每一步保存结构指纹、批次状态和结果；中断后在写入冻结状态下检查已执行步骤，不用 `--force` 跳过错误。

## 2. 预检清单

预检只读，包含以下 JSON/CSV 报告；敏感原值、密码、JWT 不进入报告。部门/对象 ID 只在授权运维处置文件中保存，展示版按相同权限脱敏。

| 清单 | 内容 |
|---|---|
| source | 数据库/结构版本、规则版本、行数、ID 范围、校验摘要及批次标识 |
| owners | ownerMemberId 与成员/账号/组织状态；固定治理分配持有者、非拥有者 SYSTEM 来源 |
| roles | 每个 SHARED/TENANT_CUSTOM 的最新已发布快照、基础引用、实际合成结果及参数归属 |
| assignments | 旧版本 → 角色 ID → 目标当前快照；主体/绑定/期限/来源/状态保持信息及新旧有效权限差异 |
| delegations | 旧版本白名单去重后的角色；各资源原逐操作上限、绑定及可转换性 |
| conflicts | 缺版本/非法基础/悬空引用/跨组织/参数不兼容/范围不一致/缺资源上限等阻断项 |
| catalog | publish 改码的原 ACTION ID、upgrade 退休引用、菜单/分组/审计关联及反向改码映射 |
| rollback | 原始行备份位置、归档与映射完整性、目标变更计数和逆转换所需信息 |

每个角色以最新已发布序号选择当前定义，不按名称或最大 ID 猜测。无可用完整快照的角色不生成默认空角色，列为冲突。停用角色保留停用状态；其历史分配也须可解释。

## 3. 转换规则

### M01 角色

SHARED 的目标定义是其最新已发布完整权限。独立 TENANT_CUSTOM 同理。带基础的旧私有角色必须按它的最新已发布快照、当时固定共享基础和本地差异合成完整权限，再写新的无基础内部快照；不得用共享最新版本替换其基础再合成。原角色 ID、code、name、归属、状态保留，旧快照/差异/备注/修改记录保留为历史。

将 current_revision_id 指向目标完整快照。参数补充 resourceId，通过实际 ACTION 关联确定；同键跨资源或跨类型的异常不能猜测归属。迁移快照序号由服务器分配、记录来源 ID，不把修改时间伪造成历史发布时间。

### M02 分配

对 TENANT 普通分配从旧 revision_id 找到 role_id，校验是共享或本租户私有，再补 role_id/kind/tenant-key，当前权限统一使用该角色目标快照。保留分配 ID、主体、validFrom/validUntil、source、status、delegationGrantId、原绑定与审计；不合并同成员的多个分配，以免丢失不同来源/范围/期限。

ACTIVE 且当前或未来可生效记录的缺失/不兼容必填参数阻断。已撤销/到期记录仍保留原内容，引用不能悬空，历史参数异常作为历史说明而不复活授权。旧绑定中不再使用的键记录忽略映射，不静默改成 ALL。权限增减是已确认模型的预期变化，必须输出明细并纳入批次处置清单。

### M03 委派

租户旧允许版本映射为角色身份并去重；平台白名单不转换。按 resourceId 归并旧逐操作上限：规范化 scopes 和 bindings 后完全一致才自动转换；缺上限、不同部门/对象、下级规则差异、相对 SELF 语义不一致、未知资源等均阻断。不自动取并集，也不暗中取交集丢失业务能力。

新白名单的目标当前定义增加资源或参数而旧上限未覆盖时，必须由有资格治理者显式补齐或收缩白名单。派生分配、人群、部门和组引用重新验证；期限和原 ID 保留。迁移后的当前动作上限来自资源，新操作自动跟随是已确认目标语义，清单须标识这一变化。

### M04 拥有者与旧 SYSTEM

校验真实 ownerMemberId 为有效本租户成员，依它建立动态身份。当前拥有者原 INITIALIZATION/SYSTEM 分配作退休记录，保留原 ID、旧引用和原始备份，目标普通角色列表不再展示这条特权分配；迁移审计记录原因，不抹除历史。

非拥有者的历史 SYSTEM 分配不自动转为拥有者，也不默认撤销全部治理能力。预检必须要求治理者明确映射到已选共享/本地私有治理角色或明确撤销，记录角色及绑定、被移除的拥有者专属操作和原因；无处置则阻断。不新增共享继承/复制产品入口，不允许用迁移映射给非拥有者保留 owner-transfer 特权。

### M05 ACTION、菜单和历史

tenant role:publish、platform shared-role:publish 按同一原 ACTION ID 分别改为 update，保持原资源、字段能力及权限引用。保存旧 code → 新 code → ID 映射供回滚；历史审计中的原字符串不重写。平台自有角色固定快照 ID、grants、字段快照和运行任务不改写。

租户 upgrade 从当前目录及菜单关联退出，原行/历史引用保留，不能把它的授权映射成 update。旧租户/共享版本接口退出；前端对应入口同步移除。版本、基础和差异旧表暂保留供审计/逆转换，清理只能在后续独立、可回滚变更中进行。

## 4. 冲突处置与可重复执行

处置输入必须含 conflictId、来源摘要、target role/bindings/resource-ceiling 或 revoke 决定、操作者、理由和时间。只接受明确字段，不提供任意 SQL/通用策略脚本。不接受“自动保留最大范围”兜底。预检重跑校验来源摘要；数据或处置依据变化后旧报告作废，重新预检。

应用转换前冻结治理及业务写入，最后重新导出并核对指纹。批次在成功提交后才记完成；同批次重跑匹配来源/目标映射时跳过已完成记录，不能再次创建快照、角色或审计。分批中断保持服务停止，从经过校验的检查点恢复。未决阻断项数必须为零。

## 5. 切换步骤与门禁

| 顺序 | 动作 | 退出条件 |
|---|---|---|
| P01 | 在隔离副本运行预检、显式处置和正反转换 | B17–B20/B24 通过，源目标清单可追踪 |
| P02 | 记录所有服务/SDK/前端版本；冻结写入并做完整可恢复备份 | 验证备份可还原；无在运行授权写入或并发角色应用任务 |
| P03 | 增加结构、核对指纹，再执行转换 | 检查点明确，无未解决冲突，无跨组织引用 |
| P04 | 验证数据与有效权限 | 与目标当前定义一致，变化及安全关闭已由清单解释 |
| P05 | 停止旧实例并部署所有 IAM/消费 SDK/管理台 | 新旧租户 API 不并存放行；平台功能回归可用 |
| P06 | 清理旧规则缓存，验证新身份及 fresh 授权 | 无总线/旧允许缓存场景仍正确；已有会话无须重新登录使用新权限 |
| P07 | 在授权写入仍冻结的观察窗口验收并演练窗口内回滚 | 操作/字段/菜单/委派/性能证据齐全；观察窗口由发布负责人确定并记录 |
| P08 | 放开写入，登记新的业务写入起点 | 反向转换已演练可用；之后回滚必须使用 P10 流程 |

完整源/目标行数、ID、外键及内容摘要必须逐表核对。角色 DML 不按用户规模重写分配，迁移本身允许一次性回填分配引用。不能因快照缓存前缀变化强制用户登录或回退到旧角色判断。

## 6. 回滚

### P09 观察窗口内回滚

此时没有新模型业务写入。停止新实例，保持冻结，恢复迁移前完整备份（在隔离演练证明可还原），恢复原服务/SDK/前端及原 ACTION code。原角色、分配、字段策略、拥有者和审计恢复一致，清除授权缓存后验证旧基线。DDL 自动提交的部分不能靠 transaction rollback 假装撤销。

### P10 放开新模型写入后的回滚

禁止只切旧程序或直接恢复旧备份丢失新写入。先停止写入、备份当前新库并导出新增审计。逆转换至少完成：

1. 将每个角色当前完整内部快照作为旧模型最新完整版本；新私有角色保留 ID 和定义，不生成共享基础。
2. 把当前所有有效租户分配固定到该角色当前快照，保留新分配、主体、绑定、期限和撤销状态；新记录补旧必填 revision 引用。
3. 将租户资源上限按当前白名单快照内的实际操作展开为旧逐操作上限，并恢复版本白名单。未知资源/不可表示范围阻断，不能扩大范围；已失效来源保持失效。
4. 按当前 ownerMemberId 重建/复用旧模型必要的固定组织治理分配，并退休旧拥有者对应的初始化来源；保持新发生的所有权转交及其他普通分配。
5. 恢复 publish ACTION 原码与目录/菜单及旧路由契约；新增权限/资源/审计保留。平台任务、固定版本未转换部分核对不变。
6. 保留原始迁移批次、新审计和逆转换映射，验证外键、行数和有效权限后部署旧服务与前端，清空缓存再恢复写入。

回滚恢复旧授权语义：拥有者不再动态获得未来全部操作，角色变更重新按旧固定版本行为处理；这是功能回滚的结果，发布说明必须明确，不能宣称同时保持新功能。业务写入和审计必须保留，差异通过逆转换报告解释。

逆转换不可验证或存在未解决冲突时禁止放行旧实例。保持冻结，使用当前新库备份恢复新版本并解决问题；任何备份恢复后人工重放不得作为无证据的默认方案。

## 7. 演练与记录

至少覆盖两组织、共享多个历史版本、独立/差异私有角色、直接/组/委派分配、同资源异范围、缺参数、SYSTEM 非拥有者、到期与未来分配、停用角色以及平台在途/历史任务。按阶段注入失败，验证没有部分放行。迁移后新增/修改/撤销角色、分配、委派及转交拥有者，再执行 P10 逆转换验证无业务记录丢失。

保存环境、代码/结构版本、报告摘要、处置输入、正反转换日志、校验结果、负责人确认和回滚耗时。仅运行结构单测或 Mock 不等于可切换；实库执行仍由获授权运维在发布窗口执行，本轮不操作任何实际数据库。

## 8. 已交付工具与执行手册

实现文件：`databases/iam/migrations/021_tenant_live_authorization.sql`（结构扩展）、`022_tenant_live_authorization_constraints.sql`（转换后门禁），以及 `tools/iam/tenant_live/export.py`、`migrate.py`、`test_migrate.py`、`test_sql.py`。历史 020 补丁按已冻结摘要校验；完整初始化 bundle 仅用于新库或隔离夹具。

下面是由发布负责人在获授权的维护窗口执行的步骤。本次仅对一次性测试库进行演练，不代表实际业务库已经迁移。使用专用配置文件提供 mysql 凭证；导出/报告权限为 0600，不把密码放命令行或报告。设置维护窗口、批次名与报告目录后，所有步骤都要保留退出码和输出。

```sh
umask 077
mkdir -p "$migration_dir"
python3 tools/iam/tenant_live/export.py --database "$iam_database" --defaults-extra-file "$mysql_config" --output "$migration_dir/source.json"
python3 tools/iam/tenant_live/migrate.py preflight --source "$migration_dir/source.json" --batch "$migration_batch" --output "$migration_dir/preflight.json"
```

预检退出码 2 表示存在阻断，停止后续步骤；0 才表示可生成转换。报告含原始来源、摘要、角色物化定义、逐分配增删权限/范围变化、委派转换及冲突。处置文件只接受已支持的明确决定，不能关闭非法拥有者/跨租户等硬冲突。来源变化后旧报告与处置作废。

```json
{
  "sourceDigest": "使用本次报告的sourceDigest",
  "decisions": [
    {
      "conflictId": "SYSTEM_ASSIGNMENT:历史分配ID",
      "actor": "获授权的发布负责人",
      "reason": "确认取消非拥有者历史系统特权",
      "at": "2026-10-14T01:00:00Z",
      "value": {"revoke": true}
    }
  ]
}
```

支持处置：SYSTEM_ASSIGNMENT 的 revoke 或 roleId/scopeBindings；ASSIGNMENT_BINDINGS 的 scopeBindings；RESOURCE_CEILING/MISSING_CEILING 的 resourceScopeCeiling。必须逐项审核资源、人员、期限和范围，禁止取并集。原始预检、处置文件及重跑报告一起归档。

```sh
python3 tools/iam/tenant_live/migrate.py preflight --source "$migration_dir/source.json" --batch "$migration_batch" --resolutions "$migration_dir/resolutions.json" --output "$migration_dir/preflight.json"
python3 tools/iam/tenant_live/migrate.py forward --source "$migration_dir/source.json" --report "$migration_dir/preflight.json" --batch "$migration_batch" --output "$migration_dir/forward.sql"
```

P02：停止所有 IAM/消费实例及授权写任务，冻结业务写入；记录 IAM、SDK、管理台制品版本。用完整 mysqldump/平台备份保留所有 IAM、账户、业务和审计表，并在隔离库还原验证。只读 export 是转换来源，不能代替完整备份。

P03：执行 021 后按 information_schema 核对新增列、索引、外键和表。DDL 自动提交；中断时只执行尚未完成的语句，禁止整段盲重跑或 `--force`。重新只读导出并预检，生成与扩展后来源一致的 forward.sql，再执行转换。工具在事务中验证来源行数/字段，失败整体回滚；固定过程名在重跑前安全重建，同批次已成功时不重复产生快照或映射。

```sh
mysql --defaults-extra-file="$mysql_config" --database="$iam_database" < databases/iam/migrations/021_tenant_live_authorization.sql
# 此处重新export/preflight/forward，替换扩展前的来源与报告。
mysql --defaults-extra-file="$mysql_config" --database="$iam_database" < "$migration_dir/forward.sql"
python3 tools/iam/tenant_live/export.py --database "$iam_database" --defaults-extra-file "$mysql_config" --output "$migration_dir/target.json"
python3 tools/iam/tenant_live/migrate.py verify --source "$migration_dir/target.json" --batch "$migration_batch" --output "$migration_dir/verify.json"
mysql --defaults-extra-file="$mysql_config" --database="$iam_database" < databases/iam/migrations/022_tenant_live_authorization_constraints.sql
```

P04–P07：verify 为 0、外键/门禁成功、原有 ID 与审计行数一致、转换变化清单获确认后，停尽旧实例，再部署新 IAM、全部授权 SDK/旧快照消费服务、两个管理台。清授权热缓存，执行无消息撤权/字段/委派/平台回归；继续冻结写入直到观察窗口验收通过。出现任何阻断、指纹变化、未知授权结果、旧实例仍可访问或字段上限错误时不放行。

P09：观察窗口没有新写入时，停止新实例，恢复完整旧备份和原制品，核对所有表、拥有者、原 ACTION code、审计及旧权限，再清缓存恢复服务。隔离演练已用 mysqldump 还原并比较全部授权来源摘要。

P10：开放新写入后，重新冻结并备份当前库，导出当前状态生成逆转换；严禁直接恢复旧备份。

```sh
python3 tools/iam/tenant_live/export.py --database "$iam_database" --defaults-extra-file "$mysql_config" --output "$migration_dir/reverse-source.json"
python3 tools/iam/tenant_live/migrate.py reverse --source "$migration_dir/reverse-source.json" --batch "$migration_batch" --output "$migration_dir/reverse.sql"
mysql --defaults-extra-file="$mysql_config" --database="$iam_database" < "$migration_dir/reverse.sql"
```

逆转换先核对批次方向，随后拆除当前分配门禁这一 DDL 检查点，再在事务内验证当前导出的全部来源。来源变化时保持停止，重新导出生成；旧程序禁止放行。重建固定版本引用、逐操作上限和当前拥有者旧治理分配，保留新角色/分配/委派/转交及所有审计。原 publish 码与退休 upgrade 菜单关系按映射恢复。完成后按逆转换清单验证旧引用、业务 ID/行数和新审计，再恢复旧制品与缓存；功能语义回到旧固定版本模型。

隔离复验命令：

```sh
python3 -m unittest discover -s tools/iam/tenant_live -p test_migrate.py
python3 -m unittest discover -s tools/iam/tenant_live -p test_sql.py
python3 databases/iam/test_identity_schema.py
```

测试需要本地已有 mysql:8.4 镜像，使用 `--pull=never` 和隔离测试库；纯迁移 SQL 测试容器不连外网。具体结果和限制记录在 VALIDATION.md，不把隔离测试当作生产切换批准。

## 9. 未上线环境直接准备

2026-10-14用户补充确认：系统未上线，租户端尚未测试，平台端已测试；已经执行021、022失败。此场景直接运行 `databases/iam/dev/prepare_tenant_live_test.sql`，不用重跑021、不用导出/处置/正转换，也不用再单独执行022。

在数据库客户端选中IAM库，停止IAM和授权写任务后，整份执行脚本；或从后端仓库根目录执行：

```sh
mysql -h 127.0.0.1 -P 3306 -u root -p --default-character-set=utf8mb4 你的IAM库名 < databases/iam/dev/prepare_tenant_live_test.sql
```

脚本保留平台自有角色/版本/分配/委派/任务、组织/成员/部门/应用开通及共享角色的最新操作范围。旧租户分配/委派停用；无当前头的旧私有角色建立空权限，由新界面重新配置。共享参数按资源重建；权限码按原ID改码并停用租户升级。已存在的新模型测试数据重跑不重置。数据准备事务失败回滚；成功后自动安装缺失的022约束。

看到 `TENANT_LIVE_TEST_READY` 后重新编译、启动新版IAM、相关SDK/消费服务和两个管理台，刷新浏览器开始产品测试。完整说明和增量验收见 `DEV-TEST-PREPARATION.md`。该脚本不生成生产迁移批次；若未来需要保留已有授权数据，仍使用原第8节及P09/P10。
