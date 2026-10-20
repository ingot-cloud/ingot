-- 自动生成：python3 tools/iam/generate_database.py；仅 COMMENT 升级，不是结构迁移。
-- 不要直接导入；先使用 tools/iam/schema_comments.py --database ... --defaults-file ... [--apply]。
-- 工具在任何 ALTER 前核对全部63表；停写并完成备份后在维护窗口执行，禁止 --force。
-- 可重跑；完整保留类型、默认值、生成表达式、索引和外键；无 USE/DROP/业务DML。
SET NAMES utf8mb4;

ALTER TABLE `iam_account`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '全局登录账号记录标识',
    MODIFY COLUMN username VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '全局唯一登录名，大小写按二进制排序规则区分',
    MODIFY COLUMN password_hash VARCHAR(300) NOT NULL COMMENT '不可逆密码哈希，禁止作为响应明文返回',
    MODIFY COLUMN phone VARCHAR(32) NULL COMMENT '账号登录手机号，可空；成员联系手机号独立保存',
    MODIFY COLUMN email VARCHAR(128) NULL COMMENT '账号邮箱，可空；成员联系邮箱独立保存',
    MODIFY COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    MODIFY COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否要求下次登录修改密码：1是、0否',
    MODIFY COLUMN password_changed_at DATETIME(6) NULL COMMENT '最近密码变更时间点，UTC；NULL表示尚无记录',
    MODIFY COLUMN last_login_at DATETIME(6) NULL COMMENT '最近登录时间点，UTC；NULL表示尚无记录',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    MODIFY COLUMN deleted_at DATETIME(6) NULL COMMENT '软删除时间点，UTC；NULL表示未删除',
    COMMENT = '全局登录账号';

ALTER TABLE `iam_platform_member`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '平台成员资格与资料记录标识',
    MODIFY COLUMN account_id BIGINT UNSIGNED NOT NULL COMMENT '关联全局登录账号ID，成员资格及联系资料与账号独立',
    MODIFY COLUMN display_name VARCHAR(128) NOT NULL COMMENT '当前管理域内的成员显示姓名',
    MODIFY COLUMN avatar VARCHAR(512) NULL COMMENT '头像对象路径，仅存bucket/objectName；响应按权限签发临时链接',
    MODIFY COLUMN phone VARCHAR(32) NULL COMMENT '平台联系资料，不修改全局登录手机号',
    MODIFY COLUMN email VARCHAR(128) NULL COMMENT '平台联系资料，不修改全局账号邮箱',
    MODIFY COLUMN status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE' COMMENT '平台成员资格：ACTIVE有效、SUSPENDED暂停、REMOVED移出；与账号启用状态独立',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    COMMENT = '平台成员资格与资料';

ALTER TABLE `iam_tenant`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '租户组织记录标识',
    MODIFY COLUMN name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    MODIFY COLUMN avatar VARCHAR(512) NULL COMMENT '头像对象路径，仅存bucket/objectName；响应按权限签发临时链接',
    MODIFY COLUMN owner_member_id BIGINT UNSIGNED NULL COMMENT '创建事务内允许暂空；提交前必须是本租户有效成员',
    MODIFY COLUMN plan_id BIGINT UNSIGNED NULL COMMENT '最近一次明确提交的套餐ID；套餐目录变化不自动回写组织开通',
    MODIFY COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    MODIFY COLUMN deleted_at DATETIME(6) NULL COMMENT '软删除时间点，UTC；NULL表示未删除',
    COMMENT = '租户组织';

ALTER TABLE `iam_tenant_member`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '租户成员资格与通讯录资料记录标识',
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN account_id BIGINT UNSIGNED NOT NULL COMMENT '关联全局登录账号ID，成员资格及联系资料与账号独立',
    MODIFY COLUMN display_name VARCHAR(128) NOT NULL COMMENT '当前管理域内的成员显示姓名',
    MODIFY COLUMN avatar VARCHAR(512) NULL COMMENT '头像对象路径，仅存bucket/objectName；响应按权限签发临时链接',
    MODIFY COLUMN phone VARCHAR(32) NULL COMMENT '组织通讯录资料，不修改全局登录手机号',
    MODIFY COLUMN email VARCHAR(128) NULL COMMENT '组织通讯录资料，不修改全局登录邮箱',
    MODIFY COLUMN status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE' COMMENT '当前租户成员资格：ACTIVE有效、SUSPENDED暂停、REMOVED移出；移出保留历史标识',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    COMMENT = '租户成员资格与通讯录资料';

ALTER TABLE `iam_department`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '租户部门树记录标识',
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN parent_id BIGINT UNSIGNED NULL COMMENT '父节点ID；NULL表示根节点，必须属于相同归属域',
    MODIFY COLUMN name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    MODIFY COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '同级显示排序权重，由查询排序规则使用',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    COMMENT = '租户部门树';

ALTER TABLE `iam_member_department`
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN member_id BIGINT UNSIGNED NOT NULL COMMENT '成员标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN department_id BIGINT UNSIGNED NOT NULL COMMENT '部门标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN is_primary BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为该成员的主部门：1是、0否；生成列唯一索引保证每成员至多一个',
    MODIFY COLUMN primary_member_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN is_primary = 1 THEN member_id ELSE NULL END) STORED COMMENT '自动约束键：主关系取member_id，否则NULL；与tenant_id唯一，保证成员至多一个主部门，非空集合业务规则由应用校验',
    COMMENT = '成员任职部门关系';

ALTER TABLE `iam_department_leader`
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN department_id BIGINT UNSIGNED NOT NULL COMMENT '部门标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN member_id BIGINT UNSIGNED NOT NULL COMMENT '成员标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN is_primary BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为该部门的主负责人：1是、0否；非空集合恰有一个由业务事务校验',
    MODIFY COLUMN primary_department_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN is_primary = 1 THEN department_id ELSE NULL END) STORED COMMENT '自动约束键：主关系取department_id，否则NULL；与tenant_id唯一，保证部门至多一个主负责人，非空集合须恰有一个由应用校验',
    COMMENT = '部门负责人关系';

ALTER TABLE `iam_platform_group`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '平台静态用户组记录标识',
    MODIFY COLUMN name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    MODIFY COLUMN description VARCHAR(512) NULL COMMENT '业务说明，可空',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    COMMENT = '平台静态用户组';

ALTER TABLE `iam_platform_group_member`
    MODIFY COLUMN group_id BIGINT UNSIGNED NOT NULL COMMENT '用户组标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN member_id BIGINT UNSIGNED NOT NULL COMMENT '成员标识；引用归属由本表外键及业务事务共同校验',
    COMMENT = '平台用户组显式成员';

ALTER TABLE `iam_tenant_group`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '租户静态用户组记录标识',
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    MODIFY COLUMN description VARCHAR(512) NULL COMMENT '业务说明，可空',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    COMMENT = '租户静态用户组';

ALTER TABLE `iam_tenant_group_member`
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN group_id BIGINT UNSIGNED NOT NULL COMMENT '用户组标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN member_id BIGINT UNSIGNED NOT NULL COMMENT '成员标识；引用归属由本表外键及业务事务共同校验',
    COMMENT = '租户用户组显式成员';

ALTER TABLE `iam_tenant_group_department`
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN group_id BIGINT UNSIGNED NOT NULL COMMENT '用户组标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN department_id BIGINT UNSIGNED NOT NULL COMMENT '部门标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN include_descendants BOOLEAN NOT NULL DEFAULT FALSE COMMENT '选择该部门时是否包含下级部门成员：1包含、0仅直属',
    COMMENT = '租户用户组部门来源';

ALTER TABLE `iam_application`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '应用目录记录标识',
    MODIFY COLUMN code VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '业务稳定编码，唯一性由本表索引限定',
    MODIFY COLUMN domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    MODIFY COLUMN name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    MODIFY COLUMN description VARCHAR(512) NULL COMMENT '业务说明，可空',
    MODIFY COLUMN icon VARCHAR(512) NULL COMMENT '界面图标标识或配置值，按所属应用菜单契约解释',
    MODIFY COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '同级显示排序权重，由查询排序规则使用',
    MODIFY COLUMN baseline BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为租户基础治理应用：1是、0否；基础应用开通不可关闭',
    MODIFY COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    COMMENT = '应用目录';

ALTER TABLE `iam_resource`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '应用资源及范围字段能力记录标识',
    MODIFY COLUMN application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN code VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '业务稳定编码，唯一性由本表索引限定',
    MODIFY COLUMN name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    MODIFY COLUMN scope_capabilities JSON NOT NULL COMMENT '资源支持的范围能力JSON数组，作为授权范围校验上限',
    MODIFY COLUMN field_capabilities JSON NOT NULL COMMENT '资源字段能力JSON数组，限制可见性、编辑及筛选等可声明能力',
    MODIFY COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    COMMENT = '应用资源及范围字段能力';

ALTER TABLE `iam_action`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '资源精确操作目录记录标识',
    MODIFY COLUMN application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN resource_id BIGINT UNSIGNED NOT NULL COMMENT '资源标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN code VARCHAR(192) COLLATE utf8mb4_bin NOT NULL COMMENT '全局唯一精确操作码，禁止通配符，例如iam-tenant:member:read',
    MODIFY COLUMN name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    MODIFY COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    COMMENT = '资源精确操作目录';

ALTER TABLE `iam_menu`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '应用菜单与前端路由记录标识',
    MODIFY COLUMN application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN parent_id BIGINT UNSIGNED NULL COMMENT '父节点ID；NULL表示根节点，必须属于相同归属域',
    MODIFY COLUMN name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    MODIFY COLUMN path VARCHAR(512) NULL COMMENT '前端菜单路由路径，可空',
    MODIFY COLUMN view_path VARCHAR(256) NULL COMMENT '前端视图注册键；目录布局使用约定布局键',
    MODIFY COLUMN route_name VARCHAR(128) NULL COMMENT '前端路由稳定名称，可空',
    MODIFY COLUMN icon VARCHAR(512) NULL COMMENT '界面图标标识或配置值，按所属应用菜单契约解释',
    MODIFY COLUMN kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '菜单类别：DIRECTORY目录、PAGE页面',
    MODIFY COLUMN match_mode VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ANY' COMMENT '菜单操作匹配方式：ANY任一、ALL全部；取值ANY,ALL',
    MODIFY COLUMN access_mode VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTION' COMMENT '菜单准入模式：OPEN开放、ACTION按绑定操作判断；取值OPEN,ACTION',
    MODIFY COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '同级显示排序权重，由查询排序规则使用',
    MODIFY COLUMN hidden BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否在菜单中隐藏：1隐藏、0展示；不授予访问权限',
    MODIFY COLUMN is_cache BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否启用前端页面缓存：1是、0否',
    MODIFY COLUMN props BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否将路由参数作为页面属性传入：1是、0否',
    MODIFY COLUMN route_params JSON NULL COMMENT '路由参数JSON，可空',
    MODIFY COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    COMMENT = '应用菜单与前端路由';

ALTER TABLE `iam_menu_action`
    MODIFY COLUMN application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN menu_id BIGINT UNSIGNED NOT NULL COMMENT '菜单标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN action_id BIGINT UNSIGNED NOT NULL COMMENT '精确操作标识；引用归属由本表外键及业务事务共同校验',
    COMMENT = '菜单操作绑定';

ALTER TABLE `iam_tenant_app_entitlement`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '租户应用开通记录标识',
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN enabled BOOLEAN NOT NULL COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    MODIFY COLUMN source VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '记录来源，取值及对应来源标识由所属业务契约解释；取值INITIALIZATION,MANUAL,PLAN,MIGRATION',
    MODIFY COLUMN source_id BIGINT UNSIGNED NULL COMMENT '开通来源对象ID；PLAN来源记录套餐ID，其他来源按业务规则可空',
    MODIFY COLUMN valid_from DATETIME(6) NULL COMMENT '生效时间点，UTC；可空时表示不限制开始时间',
    MODIFY COLUMN valid_until DATETIME(6) NULL COMMENT '失效时间点，UTC；NULL表示不限制结束时间',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    COMMENT = '租户应用开通';

ALTER TABLE `iam_app_audience`
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    MODIFY COLUMN audience_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '应用可用人群：ALL全部成员、SELECTED指定来源；开通不等于操作授权；取值ALL,SELECTED',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    COMMENT = '租户应用可用人群配置';

ALTER TABLE `iam_audience_member`
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN member_id BIGINT UNSIGNED NOT NULL COMMENT '成员标识；引用归属由本表外键及业务事务共同校验',
    COMMENT = '应用人群显式成员';

ALTER TABLE `iam_audience_department`
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN department_id BIGINT UNSIGNED NOT NULL COMMENT '部门标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN include_descendants BOOLEAN NOT NULL DEFAULT FALSE COMMENT '选择该部门时是否包含下级部门成员：1包含、0仅直属',
    COMMENT = '应用人群部门来源';

ALTER TABLE `iam_audience_group`
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN group_id BIGINT UNSIGNED NOT NULL COMMENT '用户组标识；引用归属由本表外键及业务事务共同校验',
    COMMENT = '应用人群用户组来源';

ALTER TABLE `iam_plan`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '套餐目录记录标识',
    MODIFY COLUMN name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    MODIFY COLUMN description VARCHAR(512) NULL COMMENT '业务说明，可空',
    MODIFY COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    COMMENT = '套餐目录';

ALTER TABLE `iam_plan_application`
    MODIFY COLUMN plan_id BIGINT UNSIGNED NOT NULL COMMENT '套餐标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    COMMENT = '套餐包含的应用';

ALTER TABLE `iam_role_definition`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '角色身份与当前快照指针记录标识',
    MODIFY COLUMN domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NULL COMMENT '所属租户组织ID；NULL表示平台域或共享定义',
    MODIFY COLUMN tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED COMMENT '自动生成的归属唯一键：tenant_id为空取0，否则取租户ID；用于复合唯一索引及外键',
    MODIFY COLUMN kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '角色类别：SYSTEM系统、SHARED共享、PLATFORM_CUSTOM平台自有、TENANT_CUSTOM租户私有',
    MODIFY COLUMN code VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '业务稳定编码，唯一性由本表索引限定',
    MODIFY COLUMN name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    MODIFY COLUMN description VARCHAR(512) NULL COMMENT '业务说明，可空',
    MODIFY COLUMN group_name VARCHAR(128) NULL COMMENT '角色展示分组名称，可空',
    MODIFY COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    MODIFY COLUMN current_revision_id BIGINT UNSIGNED NULL COMMENT '共享或租户私有角色当前完整权限快照ID；平台固定版本路径不依赖此指针',
    COMMENT = '角色身份与当前快照指针';

ALTER TABLE `iam_role_revision`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '角色不可变权限快照与可编辑版本元数据记录标识',
    MODIFY COLUMN role_id BIGINT UNSIGNED NOT NULL COMMENT '角色标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '权限快照所属角色类别，与role_id共同引用角色；快照权限内容不可变',
    MODIFY COLUMN revision BIGINT UNSIGNED NOT NULL COMMENT '不可变快照的发布序号，从1开始；与记录乐观锁版本不同',
    MODIFY COLUMN base_revision_id BIGINT UNSIGNED NULL COMMENT '历史定制角色引用的共享基础版本ID；NULL表示无基础版本',
    MODIFY COLUMN base_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin
        GENERATED ALWAYS AS (CASE WHEN base_revision_id IS NULL THEN NULL ELSE 'SHARED' END) STORED COMMENT '自动生成的基础版本种类：有base_revision_id时为SHARED，否则NULL，用于复合外键',
    MODIFY COLUMN metadata_overrides JSON NOT NULL COMMENT '角色快照元数据覆盖JSON对象；空对象表示无覆盖',
    MODIFY COLUMN resource_field_permissions JSON NOT NULL DEFAULT (JSON_OBJECT()) COMMENT '固定资源字段权限快照，未声明字段不授予权限',
    MODIFY COLUMN version_name VARCHAR(128) NULL COMMENT '版本显示名称；空值回退vN',
    MODIFY COLUMN remark VARCHAR(512) NULL COMMENT '版本说明',
    MODIFY COLUMN metadata_version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '独立元数据乐观锁',
    MODIFY COLUMN published_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '权限快照发布时间点，UTC',
    COMMENT = '角色不可变权限快照与可编辑版本元数据';

ALTER TABLE `iam_role_parameter`
    MODIFY COLUMN revision_id BIGINT UNSIGNED NOT NULL COMMENT '角色固定版本标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN parameter_key VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '快照内唯一的范围参数名称，用于分配参数绑定',
    MODIFY COLUMN resource_id BIGINT UNSIGNED NULL COMMENT '资源标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN binding_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '范围参数绑定类别：DEPARTMENTS部门、OBJECTS业务对象；取值DEPARTMENTS,OBJECTS',
    COMMENT = '角色快照范围参数定义';

ALTER TABLE `iam_role_grant`
    MODIFY COLUMN revision_id BIGINT UNSIGNED NOT NULL COMMENT '角色固定版本标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN action_id BIGINT UNSIGNED NOT NULL COMMENT '精确操作标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN scopes JSON NOT NULL COMMENT '授权或上限的范围表达式JSON数组；具体空数组语义由对应求值器判定',
    COMMENT = '角色快照操作授权';

ALTER TABLE `iam_role_delta`
    MODIFY COLUMN revision_id BIGINT UNSIGNED NOT NULL COMMENT '角色固定版本标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN action_id BIGINT UNSIGNED NOT NULL COMMENT '精确操作标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN operation VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '历史定制差异操作：ADD增加、REMOVE移除、REPLACE_SCOPE替换范围',
    MODIFY COLUMN scopes JSON NOT NULL COMMENT '授权或上限的范围表达式JSON数组；具体空数组语义由对应求值器判定',
    COMMENT = '历史租户角色定制差异';

ALTER TABLE `iam_delegation_grant`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '受限委派及期限上限记录标识',
    MODIFY COLUMN domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NULL COMMENT '所属租户组织ID；NULL表示平台域或共享定义',
    MODIFY COLUMN tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED COMMENT '自动生成的归属唯一键：tenant_id为空取0，否则取租户ID；用于复合唯一索引及外键',
    MODIFY COLUMN platform_administrator_id BIGINT UNSIGNED NULL COMMENT '被委派的平台管理员成员ID，平台域使用',
    MODIFY COLUMN tenant_administrator_id BIGINT UNSIGNED NULL COMMENT '被委派的租户管理员成员ID，租户域使用',
    MODIFY COLUMN valid_from DATETIME(6) NULL COMMENT '生效时间点，UTC；可空时表示不限制开始时间',
    MODIFY COLUMN valid_until DATETIME(6) NULL COMMENT '失效时间点，UTC；NULL表示不限制结束时间',
    MODIFY COLUMN assignment_duration_mode VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'LIMITED' COMMENT '单次授权期限模式：LIMITED限制时长、UNLIMITED无上限；后者仅平台域可用',
    MODIFY COLUMN max_assignment_duration_seconds BIGINT UNSIGNED NULL COMMENT '单次授权最大时长的秒部分；UNLIMITED时为NULL',
    MODIFY COLUMN max_assignment_duration_nanos INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '单次授权最大时长的纳秒部分，范围0至999999999',
    MODIFY COLUMN status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE' COMMENT '委派状态：ACTIVE有效、REVOKED已撤销；时效和派生授权仍按业务校验',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    COMMENT = '受限委派及期限上限';

ALTER TABLE `iam_delegation_role_revision`
    MODIFY COLUMN delegation_id BIGINT UNSIGNED NOT NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN revision_id BIGINT UNSIGNED NOT NULL COMMENT '角色固定版本标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN revision_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '被引用固定版本的角色种类，与版本ID共同验证引用合法性',
    COMMENT = '平台委派允许的固定角色版本';

ALTER TABLE `iam_delegation_recipient_member`
    MODIFY COLUMN delegation_id BIGINT UNSIGNED NOT NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NULL COMMENT '所属租户组织ID；NULL表示平台域或共享定义',
    MODIFY COLUMN tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED COMMENT '自动生成的归属唯一键：tenant_id为空取0，否则取租户ID；用于复合唯一索引及外键',
    MODIFY COLUMN platform_member_id BIGINT UNSIGNED NULL COMMENT '平台域接收成员ID；非平台成员主体时为空',
    MODIFY COLUMN tenant_member_id BIGINT UNSIGNED NULL COMMENT '租户域接收成员ID；非租户成员主体时为空',
    MODIFY COLUMN member_id BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(platform_member_id, tenant_member_id)) STORED COMMENT '自动生成的接收成员键：取platform_member_id或tenant_member_id；与delegation_id唯一，禁止重复接收者',
    COMMENT = '委派允许的接收成员';

ALTER TABLE `iam_delegation_recipient_department`
    MODIFY COLUMN delegation_id BIGINT UNSIGNED NOT NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN department_id BIGINT UNSIGNED NOT NULL COMMENT '部门标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN include_descendants BOOLEAN NOT NULL DEFAULT FALSE COMMENT '选择该部门时是否包含下级部门成员：1包含、0仅直属',
    COMMENT = '委派允许的接收部门';

ALTER TABLE `iam_delegation_action_ceiling`
    MODIFY COLUMN delegation_id BIGINT UNSIGNED NOT NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN action_id BIGINT UNSIGNED NOT NULL COMMENT '精确操作标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN scopes JSON NOT NULL COMMENT '授权或上限的范围表达式JSON数组；具体空数组语义由对应求值器判定',
    MODIFY COLUMN scope_bindings JSON NOT NULL COMMENT '参数名到范围对象的绑定JSON对象；空对象表示无显式绑定',
    COMMENT = '平台委派逐操作范围上限';

ALTER TABLE `iam_role_assignment`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '成员或用户组角色分配记录标识',
    MODIFY COLUMN domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NULL COMMENT '所属租户组织ID；NULL表示平台域或共享定义',
    MODIFY COLUMN tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED COMMENT '自动生成的归属唯一键：tenant_id为空取0，否则取租户ID；用于复合唯一索引及外键',
    MODIFY COLUMN subject_type VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权接收主体类别：MEMBER成员、GROUP用户组',
    MODIFY COLUMN platform_member_id BIGINT UNSIGNED NULL COMMENT '平台域接收成员ID；非平台成员主体时为空',
    MODIFY COLUMN platform_group_id BIGINT UNSIGNED NULL COMMENT '平台域接收用户组ID；非平台组主体时为空',
    MODIFY COLUMN tenant_member_id BIGINT UNSIGNED NULL COMMENT '租户域接收成员ID；非租户成员主体时为空',
    MODIFY COLUMN tenant_group_id BIGINT UNSIGNED NULL COMMENT '租户域接收用户组ID；非租户组主体时为空',
    MODIFY COLUMN role_id BIGINT UNSIGNED NULL COMMENT '租户分配引用的角色身份ID；平台固定版本路径为空；撤销历史记录可保留旧版本引用',
    MODIFY COLUMN role_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '被引用角色种类，与角色ID及归属共同验证合法性',
    MODIFY COLUMN role_tenant_key BIGINT UNSIGNED NULL COMMENT '角色归属键：共享角色为0，租户私有角色为对应租户ID，配合复合外键校验',
    MODIFY COLUMN revision_id BIGINT UNSIGNED NULL COMMENT '平台分配引用的固定版本ID；租户有效分配按role_id求当前快照',
    MODIFY COLUMN revision_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '被引用固定版本的角色种类，与版本ID共同验证引用合法性',
    MODIFY COLUMN scope_bindings JSON NOT NULL COMMENT '参数名到范围对象的绑定JSON对象；空对象表示无显式绑定',
    MODIFY COLUMN delegation_grant_id BIGINT UNSIGNED NULL COMMENT '来源委派标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN valid_from DATETIME(6) NOT NULL COMMENT '生效时间点，UTC；可空时表示不限制开始时间',
    MODIFY COLUMN valid_until DATETIME(6) NULL COMMENT '失效时间点，UTC；NULL表示不限制结束时间',
    MODIFY COLUMN status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE' COMMENT '分配状态：ACTIVE有效、REVOKED已撤销；有效期与主体状态还须独立校验',
    MODIFY COLUMN source VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '记录来源，取值及对应来源标识由所属业务契约解释；取值MANUAL,INITIALIZATION,MIGRATION',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    COMMENT = '成员或用户组角色分配';

ALTER TABLE `iam_delegation_role`
    MODIFY COLUMN delegation_id BIGINT UNSIGNED NOT NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'TENANT' COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    MODIFY COLUMN role_id BIGINT UNSIGNED NOT NULL COMMENT '角色标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN role_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '被引用角色种类，与角色ID及归属共同验证合法性',
    MODIFY COLUMN role_tenant_key BIGINT UNSIGNED NOT NULL COMMENT '角色归属键：共享角色为0，租户私有角色为对应租户ID，配合复合外键校验',
    COMMENT = '租户委派允许的角色身份';

ALTER TABLE `iam_delegation_resource_ceiling`
    MODIFY COLUMN delegation_id BIGINT UNSIGNED NOT NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN resource_id BIGINT UNSIGNED NOT NULL COMMENT '资源标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN scopes JSON NOT NULL COMMENT '授权或上限的范围表达式JSON数组；具体空数组语义由对应求值器判定',
    MODIFY COLUMN scope_bindings JSON NOT NULL COMMENT '参数名到范围对象的绑定JSON对象；空对象表示无显式绑定',
    COMMENT = '租户委派逐资源范围上限';

ALTER TABLE `iam_default_policy_revision`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '默认通讯录或字段策略固定版本记录标识',
    MODIFY COLUMN kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '默认策略类别：DIRECTORY通讯录、FIELD字段',
    MODIFY COLUMN revision BIGINT UNSIGNED NOT NULL COMMENT '不可变快照的发布序号，从1开始；与记录乐观锁版本不同',
    MODIFY COLUMN definition JSON NOT NULL COMMENT '默认策略不可变定义JSON对象，由对应通讯录或字段求值器解释',
    COMMENT = '默认通讯录或字段策略固定版本';

ALTER TABLE `iam_policy_selector`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '租户策略人员选择器记录标识',
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    COMMENT = '租户策略人员选择器';

ALTER TABLE `iam_policy_selector_member`
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN selector_id BIGINT UNSIGNED NOT NULL COMMENT '人员选择器标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN member_id BIGINT UNSIGNED NOT NULL COMMENT '成员标识；引用归属由本表外键及业务事务共同校验',
    COMMENT = '策略选择器显式成员';

ALTER TABLE `iam_policy_selector_department`
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN selector_id BIGINT UNSIGNED NOT NULL COMMENT '人员选择器标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN department_id BIGINT UNSIGNED NOT NULL COMMENT '部门标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN include_descendants BOOLEAN NOT NULL DEFAULT FALSE COMMENT '选择该部门时是否包含下级部门成员：1包含、0仅直属',
    COMMENT = '策略选择器部门来源';

ALTER TABLE `iam_directory_policy`
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN default_revision_id BIGINT UNSIGNED NOT NULL COMMENT '绑定的默认策略固定版本ID，与default_kind共同校验引用',
    MODIFY COLUMN default_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'DIRECTORY' COMMENT '默认策略种类，固定为当前策略表对应的DIRECTORY或FIELD',
    MODIFY COLUMN default_scope VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '空表示继承固定默认版本',
    MODIFY COLUMN default_selector_id BIGINT UNSIGNED NULL COMMENT '默认范围为SELECTED时的人员选择器ID，其他情况为空',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    COMMENT = '租户通讯录策略及默认版本引用';

ALTER TABLE `iam_directory_rule`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '租户通讯录允许或拒绝规则记录标识',
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN effect VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '通讯录规则效果：ALLOW允许、DENY拒绝；取值ALLOW,DENY',
    MODIFY COLUMN viewer_selector_id BIGINT UNSIGNED NOT NULL COMMENT '规则适用的查看者选择器ID，须属于本租户',
    MODIFY COLUMN target_selector_id BIGINT UNSIGNED NOT NULL COMMENT '通讯录可见目标的选择器ID，须属于本租户',
    COMMENT = '租户通讯录允许或拒绝规则';

ALTER TABLE `iam_field_policy`
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN default_revision_id BIGINT UNSIGNED NOT NULL COMMENT '绑定的默认策略固定版本ID，与default_kind共同校验引用',
    MODIFY COLUMN default_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'FIELD' COMMENT '默认策略种类，固定为当前策略表对应的DIRECTORY或FIELD',
    MODIFY COLUMN operation_rules JSON NOT NULL DEFAULT (JSON_ARRAY()) COMMENT '租户字段操作规则JSON数组，限定已接入的编辑及筛选等能力',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    COMMENT = '租户字段策略及操作规则';

ALTER TABLE `iam_field_rule`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '租户场景字段可见性规则记录标识',
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN scenario VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '字段策略使用场景：MANAGEMENT管理、DIRECTORY通讯录；取值MANAGEMENT,DIRECTORY',
    MODIFY COLUMN field_key VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '资源字段稳定标识，用于匹配目录能力及字段规则',
    MODIFY COLUMN viewer_selector_id BIGINT UNSIGNED NOT NULL COMMENT '规则适用的查看者选择器ID，须属于本租户',
    MODIFY COLUMN target_scope JSON NOT NULL COMMENT '字段规则目标范围表达式JSON数组',
    MODIFY COLUMN scope_bindings JSON NOT NULL COMMENT '参数名到范围对象的绑定JSON对象；空对象表示无显式绑定',
    MODIFY COLUMN visibility VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '字段可见性：HIDDEN隐藏、MASKED脱敏、FULL完整；取值HIDDEN,MASKED,FULL',
    COMMENT = '租户场景字段可见性规则';

ALTER TABLE `iam_member_export`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '租户成员导出任务与成员快照记录标识',
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    MODIFY COLUMN actor_member_id BIGINT UNSIGNED NOT NULL COMMENT '发起操作的当前域成员ID',
    MODIFY COLUMN tenant_version VARCHAR(32) COLLATE utf8mb4_bin NOT NULL COMMENT '登记导出任务时的组织版本，取iam_tenant.version',
    MODIFY COLUMN status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值PENDING,RUNNING,SUCCEEDED,FAILED,EXPIRED',
    MODIFY COLUMN member_ids JSON NULL COMMENT '导出成员ID的JSON快照数组；任务尚未生成快照时可空',
    MODIFY COLUMN failure_reason VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '任务失败或阻塞的安全原因，不包含凭证及敏感原值',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '记录创建时间点，UTC',
    MODIFY COLUMN completed_at DATETIME(6) NULL COMMENT '任务完成时间点，UTC；未完成时为空',
    MODIFY COLUMN expires_at DATETIME(6) NOT NULL COMMENT '到期时间点，UTC',
    COMMENT = '租户成员导出任务与成员快照';

ALTER TABLE `iam_authorization_audit`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '授权与组织操作脱敏审计及投递状态记录标识',
    MODIFY COLUMN event_id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '业务事件幂等标识，用于防止重复投递或重复保存',
    MODIFY COLUMN actor_account_id BIGINT UNSIGNED NOT NULL COMMENT '发起操作的全局账号ID',
    MODIFY COLUMN actor_member_id BIGINT UNSIGNED NOT NULL COMMENT '发起操作的当前域成员ID',
    MODIFY COLUMN domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    MODIFY COLUMN tenant_id BIGINT UNSIGNED NULL COMMENT '审计所属租户ID；平台域为空，租户域必填',
    MODIFY COLUMN target_type VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '审计或迁移目标的业务类型',
    MODIFY COLUMN target_id VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '目标业务标识，具体类型由target_type或entity_type解释',
    MODIFY COLUMN change_type VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '审计变更类型，对应AuditChangeType稳定字面量',
    MODIFY COLUMN safe_before JSON NOT NULL COMMENT '变更前脱敏摘要JSON对象，不保存凭证或敏感原值',
    MODIFY COLUMN safe_after JSON NOT NULL COMMENT '变更后脱敏摘要JSON对象，不保存凭证或敏感原值',
    MODIFY COLUMN revisions JSON NOT NULL COMMENT '本次变更涉及的对象版本JSON对象',
    MODIFY COLUMN delegation_id BIGINT UNSIGNED NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN assignment_id BIGINT UNSIGNED NULL COMMENT '角色分配标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN trace_id VARCHAR(128) NULL COMMENT '请求链路追踪标识，可空',
    MODIFY COLUMN occurred_at DATETIME(6) NOT NULL COMMENT '业务发生时间点，UTC',
    MODIFY COLUMN delivered_at DATETIME(6) NULL COMMENT '审计投递完成时间点，UTC；NULL表示尚未确认投递',
    COMMENT = '授权与组织操作脱敏审计及投递状态';

ALTER TABLE `iam_migration_batch`
    MODIFY COLUMN id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '历史身份迁移批次及验收状态记录标识',
    MODIFY COLUMN source_fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '源数据结构或内容指纹，用于识别迁移输入',
    MODIFY COLUMN target_identifier VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '明确选择的迁移目标标识，防止误用目标',
    MODIFY COLUMN rules_version VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '本批次采用的迁移规则版本',
    MODIFY COLUMN status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'NEW' COMMENT '当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值NEW,PREFLIGHTED,IMPORTED,VERIFIED,FAILED',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    MODIFY COLUMN verified_at DATETIME(6) NULL COMMENT '人工验收完成时间点，UTC；仅VERIFIED状态必填',
    COMMENT = '历史身份迁移批次及验收状态';

ALTER TABLE `iam_migration_mapping`
    MODIFY COLUMN batch_id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '迁移批次标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN source_table VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '原始来源表名',
    MODIFY COLUMN source_id VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '原始记录标识',
    MODIFY COLUMN target_type VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '审计或迁移目标的业务类型',
    MODIFY COLUMN target_id VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '目标业务标识，具体类型由target_type或entity_type解释',
    MODIFY COLUMN disposition VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '人工迁移处置：KEEP_MAPPED/NARROW/RECONFIGURE/ARCHIVE/SKIP；取值KEEP_MAPPED,NARROW,RECONFIGURE,ARCHIVE,SKIP',
    MODIFY COLUMN actor VARCHAR(128) NOT NULL COMMENT '作出处置的操作者标识',
    MODIFY COLUMN reason VARCHAR(1024) NOT NULL COMMENT '明确的人工处置依据',
    COMMENT = '历史标识到目标实体的人工处置映射';

ALTER TABLE `iam_migration_issue`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '历史迁移阻塞项及处置记录记录标识',
    MODIFY COLUMN batch_id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '迁移批次标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN issue_code VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '迁移问题稳定代码',
    MODIFY COLUMN source_table VARCHAR(64) COLLATE utf8mb4_bin NULL COMMENT '原始来源表名',
    MODIFY COLUMN source_id VARCHAR(128) COLLATE utf8mb4_bin NULL COMMENT '原始记录标识',
    MODIFY COLUMN safe_description VARCHAR(1024) NOT NULL COMMENT '迁移问题的脱敏说明',
    MODIFY COLUMN disposition VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '人工迁移处置：KEEP_MAPPED/NARROW/RECONFIGURE/ARCHIVE/SKIP；取值KEEP_MAPPED,NARROW,RECONFIGURE,ARCHIVE,SKIP',
    MODIFY COLUMN resolved_by VARCHAR(128) NULL COMMENT '问题处置人；未处置时为空',
    MODIFY COLUMN resolution_reason VARCHAR(1024) NULL COMMENT '问题处置依据；未处置时为空',
    COMMENT = '历史迁移阻塞项及处置记录';

ALTER TABLE `iam_role_application_task`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '平台角色固定版本批量应用任务记录标识',
    MODIFY COLUMN role_id BIGINT UNSIGNED NOT NULL COMMENT '角色标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN target_revision_id BIGINT UNSIGNED NOT NULL COMMENT '目标角色固定版本标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN actor_account_id BIGINT UNSIGNED NOT NULL COMMENT '发起操作的全局账号ID',
    MODIFY COLUMN actor_member_id BIGINT UNSIGNED NOT NULL COMMENT '发起操作的当前域成员ID',
    MODIFY COLUMN created_by BIGINT UNSIGNED NOT NULL COMMENT '创建操作人标识',
    MODIFY COLUMN selection VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '版本应用选择方式：ALL/SOURCE_REVISIONS/ASSIGNMENTS；取值ALL,SOURCE_REVISIONS,ASSIGNMENTS',
    MODIFY COLUMN selection_snapshot JSON NOT NULL COMMENT '提交时冻结的分配选择条件JSON对象',
    MODIFY COLUMN status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值PREPARING,READY,BLOCKED,RUNNING,PAUSED,SUCCEEDED',
    MODIFY COLUMN version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    MODIFY COLUMN prepare_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '同角色任务准备幂等键',
    MODIFY COLUMN confirm_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '任务确认幂等键；未确认时为空',
    MODIFY COLUMN lease_owner VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '领取任务的节点标识；无租约时为空',
    MODIFY COLUMN lease_until DATETIME(6) NULL COMMENT '任务租约失效时间点，UTC；无租约时为空',
    MODIFY COLUMN failure_reason VARCHAR(512) NULL COMMENT '任务失败或阻塞的安全原因，不包含凭证及敏感原值',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    MODIFY COLUMN running_role_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN status='RUNNING' THEN role_id ELSE NULL END) STORED COMMENT '自动生成的运行角色键：RUNNING时取role_id，否则NULL；唯一索引限制同角色一个运行任务',
    COMMENT = '平台角色固定版本批量应用任务';

ALTER TABLE `iam_role_application_item`
    MODIFY COLUMN id BIGINT UNSIGNED NOT NULL COMMENT '角色版本应用的分配明细记录标识',
    MODIFY COLUMN task_id BIGINT UNSIGNED NOT NULL COMMENT '版本应用任务标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN assignment_id BIGINT UNSIGNED NOT NULL COMMENT '角色分配标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN original_snapshot JSON NOT NULL COMMENT '被选分配的原始快照JSON对象，用于预览与执行重验',
    MODIFY COLUMN expected_assignment_version BIGINT UNSIGNED NOT NULL COMMENT '执行前要求匹配的分配乐观锁版本',
    MODIFY COLUMN configured_bindings JSON NOT NULL COMMENT '任务内明确配置的范围参数JSON对象',
    MODIFY COLUMN preview_snapshot JSON NULL COMMENT '最近一次预览结果JSON快照；未预览时为空',
    MODIFY COLUMN status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值PENDING,BLOCKED,SUCCEEDED,EXCLUDED,REVOKED,EXPIRED,ALREADY_TARGET',
    MODIFY COLUMN failure_reason VARCHAR(512) NULL COMMENT '任务失败或阻塞的安全原因，不包含凭证及敏感原值',
    COMMENT = '角色版本应用的分配明细';

ALTER TABLE `iam_live_authorization_batch`
    MODIFY COLUMN batch_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '迁移批次标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN source_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '即时授权迁移前的源内容摘要',
    MODIFY COLUMN reverse_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '反向迁移核验摘要；尚未生成时为空',
    MODIFY COLUMN state VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '迁移方向状态：FORWARD正向、REVERSE反向；取值FORWARD,REVERSE',
    MODIFY COLUMN decisions JSON NOT NULL DEFAULT (JSON_ARRAY()) COMMENT '已确认迁移处置决策JSON数组',
    MODIFY COLUMN applied_at DATETIME(6) NULL COMMENT '迁移应用时间点，UTC；尚未应用时为空',
    COMMENT = '租户即时授权模型迁移批次';

ALTER TABLE `iam_live_authorization_mapping`
    MODIFY COLUMN batch_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '迁移批次标识；引用归属由本表外键及业务事务共同校验',
    MODIFY COLUMN entity_type VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '迁移记录对应的实体类型',
    MODIFY COLUMN entity_id BIGINT UNSIGNED NOT NULL COMMENT '迁移记录对应的实体标识',
    MODIFY COLUMN source_row JSON NOT NULL COMMENT '迁移前原行JSON快照，仅供受控迁移与回退使用',
    MODIFY COLUMN target_id BIGINT UNSIGNED NULL COMMENT '目标业务标识，具体类型由target_type或entity_type解释',
    COMMENT = '租户即时授权迁移原行快照与目标映射';

ALTER TABLE `biz_leaf_alloc`
    MODIFY COLUMN `biz_tag` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT 'Leaf业务号段名称，不同业务独立分配',
    MODIFY COLUMN `max_id` bigint NOT NULL DEFAULT '1' COMMENT '已分配号段的最大ID高水位，更新必须保持发号原子性',
    MODIFY COLUMN `step` int NOT NULL COMMENT '每次申请的号段长度',
    MODIFY COLUMN `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '业务说明，可空',
    MODIFY COLUMN `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '号段高水位最近更新时间点，UTC',
    COMMENT = 'Leaf业务号段分配高水位';

ALTER TABLE `platform_dict`
    MODIFY COLUMN `id` bigint NOT NULL COMMENT '多作用域字典及字典项记录标识',
    MODIFY COLUMN `pid` bigint NOT NULL DEFAULT '0' COMMENT '父ID',
    MODIFY COLUMN `code` varchar(64) NOT NULL COMMENT '编码',
    MODIFY COLUMN `name` varchar(128) NOT NULL COMMENT '名称',
    MODIFY COLUMN `value` varchar(128) DEFAULT NULL COMMENT '字典项值（仅字典项有效）',
    MODIFY COLUMN `label` varchar(128) DEFAULT NULL COMMENT '字典项展示文本（仅字典项有效）',
    MODIFY COLUMN `type` char(1) NOT NULL COMMENT '字典节点类别：0字典类型、1字典项',
    MODIFY COLUMN `scope_type` char(1) NOT NULL DEFAULT '0' COMMENT '作用域, 0:平台,1:租户,2:应用',
    MODIFY COLUMN `tenant_id` bigint DEFAULT NULL COMMENT '租户ID（scope_type=1时必填）',
    MODIFY COLUMN `app_id` bigint DEFAULT NULL COMMENT '作用域为应用时的IAM应用目录ID；其他作用域可空',
    MODIFY COLUMN `org_type` char(1) NOT NULL DEFAULT '0' COMMENT '组织类型：0平台、1组织',
    MODIFY COLUMN `sort` int NOT NULL DEFAULT '0' COMMENT '排序权重',
    MODIFY COLUMN `system_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否内置字典',
    MODIFY COLUMN `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
    MODIFY COLUMN `remark` varchar(255) DEFAULT NULL COMMENT '备注',
    MODIFY COLUMN `extra` json DEFAULT NULL COMMENT '扩展属性',
    MODIFY COLUMN `created_by` bigint DEFAULT NULL COMMENT '创建人',
    MODIFY COLUMN `updated_by` bigint DEFAULT NULL COMMENT '更新人',
    MODIFY COLUMN `created_at` datetime DEFAULT NULL COMMENT '记录创建时间点，UTC',
    MODIFY COLUMN `updated_at` datetime DEFAULT NULL COMMENT '记录最后更新时间点，UTC',
    MODIFY COLUMN `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间点，UTC；NULL表示未删除',
    COMMENT = '多作用域字典及字典项';

ALTER TABLE `security_event`
    MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT COMMENT '统一安全事件持久记录记录标识；数据库自增',
    MODIFY COLUMN `event_id` varchar(32) DEFAULT NULL COMMENT 'producer 幂等 ID',
    MODIFY COLUMN `event_type` varchar(64) NOT NULL COMMENT '事件类型',
    MODIFY COLUMN `event_category` varchar(20) NOT NULL COMMENT 'AUTH/ACCOUNT/CREDENTIAL/ACCESS',
    MODIFY COLUMN `priority` varchar(16) NOT NULL DEFAULT 'BEST_EFFORT' COMMENT 'BEST_EFFORT/DURABLE',
    MODIFY COLUMN `occurred_at` datetime DEFAULT NULL COMMENT '业务发生时间点，UTC',
    MODIFY COLUMN `received_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '服务端接收安全事件的时间点，UTC',
    MODIFY COLUMN `tenant_id` bigint DEFAULT NULL COMMENT '事件所属租户ID；NULL表示平台事件或未提供租户上下文',
    MODIFY COLUMN `user_id` bigint DEFAULT NULL COMMENT '安全事件关联的用户标识，结合user_type解释；可空',
    MODIFY COLUMN `user_type` varchar(20) DEFAULT NULL COMMENT '安全用户类型，对应UserTypeEnum：0系统账号、1会员账号',
    MODIFY COLUMN `account` varchar(128) DEFAULT NULL COMMENT '事件涉及的账号显示标识，可空',
    MODIFY COLUMN `client_id` varchar(64) DEFAULT NULL COMMENT '认证客户端标识，可空',
    MODIFY COLUMN `app_id` varchar(64) DEFAULT NULL COMMENT '上报方提供的应用标识，可空',
    MODIFY COLUMN `session_id` varchar(64) DEFAULT NULL COMMENT '认证会话标识，可空',
    MODIFY COLUMN `device_id` varchar(128) DEFAULT NULL COMMENT '设备标识，可空',
    MODIFY COLUMN `client_ip` varchar(64) DEFAULT NULL COMMENT '客户端IP地址，可空',
    MODIFY COLUMN `request_uri` varchar(512) DEFAULT NULL COMMENT '触发事件的请求路径，可空',
    MODIFY COLUMN `user_agent` varchar(512) DEFAULT NULL COMMENT '客户端User-Agent，可空',
    MODIFY COLUMN `result` varchar(20) DEFAULT NULL COMMENT '安全操作结果稳定标识，可空',
    MODIFY COLUMN `reason_code` varchar(50) DEFAULT NULL COMMENT '安全事件原因代码，可空',
    MODIFY COLUMN `reason_detail` varchar(500) DEFAULT NULL COMMENT '安全事件原因说明，不保存凭证原文',
    MODIFY COLUMN `source_module` varchar(64) NOT NULL COMMENT '上报模块',
    MODIFY COLUMN `source` varchar(50) DEFAULT NULL COMMENT '安全事件来源渠道或触发来源，可空',
    MODIFY COLUMN `operator_id` bigint DEFAULT NULL COMMENT '操作人标识，可空',
    MODIFY COLUMN `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人姓名快照，可空',
    MODIFY COLUMN `trace_id` varchar(64) DEFAULT NULL COMMENT '请求链路追踪标识，可空',
    MODIFY COLUMN `extension` json DEFAULT NULL COMMENT '安全事件扩展JSON，须遵守脱敏白名单',
    COMMENT = '统一安全事件（canonical）';

ALTER TABLE `sys_social_details`
    MODIFY COLUMN `id` bigint unsigned NOT NULL COMMENT '社交登录渠道配置记录标识',
    MODIFY COLUMN `tenant_id` bigint unsigned NOT NULL COMMENT '租户ID',
    MODIFY COLUMN `app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '第三方平台分配的客户端App ID，不是IAM应用目录数字ID',
    MODIFY COLUMN `app_secret` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '第三方应用密钥，可空；禁止在审计及字典示例中泄露',
    MODIFY COLUMN `redirect_url` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '重定向地址',
    MODIFY COLUMN `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '社交名称',
    MODIFY COLUMN `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '第三方登录渠道类型，对应社交登录渠道配置',
    MODIFY COLUMN `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
    MODIFY COLUMN `created_at` datetime DEFAULT NULL COMMENT '记录创建时间点，UTC',
    MODIFY COLUMN `updated_at` datetime DEFAULT NULL COMMENT '记录最后更新时间点，UTC',
    MODIFY COLUMN `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间点，UTC；NULL表示未删除',
    COMMENT = '社交登录渠道配置';

ALTER TABLE `sys_user_social`
    MODIFY COLUMN `id` bigint NOT NULL COMMENT '全局账号社交身份绑定记录标识',
    MODIFY COLUMN `tenant_id` bigint NOT NULL COMMENT '组织ID',
    MODIFY COLUMN `user_id` bigint NOT NULL COMMENT '被绑定的全局Account标识，旧列名保留；不表示租户成员ID',
    MODIFY COLUMN `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道类型',
    MODIFY COLUMN `unique_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道唯一ID',
    MODIFY COLUMN `bind_at` datetime NOT NULL COMMENT '社交身份绑定时间点，UTC',
    COMMENT = '全局账号社交身份绑定';

ALTER TABLE `sys_tenant_plan_record`
    MODIFY COLUMN `id` bigint NOT NULL COMMENT '历史组织套餐购买记录记录标识',
    MODIFY COLUMN `tenant_id` bigint NOT NULL COMMENT '租户ID',
    MODIFY COLUMN `plan_id` int NOT NULL COMMENT '历史套餐标识，保留INT库存类型；不自动关联当前套餐目录',
    MODIFY COLUMN `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '历史套餐类别：0长期、1短期',
    MODIFY COLUMN `duration` int NOT NULL COMMENT '历史套餐购买时长数值，与unit共同表示有效时长',
    MODIFY COLUMN `unit` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '历史套餐时长单位：1月、2年',
    MODIFY COLUMN `created_at` datetime NOT NULL COMMENT '记录创建时间点，UTC',
    COMMENT = '历史组织套餐购买记录';

ALTER TABLE `account_lock_state`
    MODIFY COLUMN id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '安全框架账号锁定状态记录标识；数据库自增',
    MODIFY COLUMN user_id         BIGINT       NOT NULL COMMENT '被锁定的安全用户ID，与user_type共同唯一标识用户',
    MODIFY COLUMN user_type       VARCHAR(20)  NOT NULL DEFAULT '0' COMMENT '安全用户类型，对应UserTypeEnum：0系统账号、1会员账号',
    MODIFY COLUMN locked          TINYINT(1)   NOT NULL DEFAULT 0     COMMENT '是否锁定（0-否 1-是）',
    MODIFY COLUMN lock_type       VARCHAR(20)  DEFAULT NULL            COMMENT '锁定类型（MANUAL-手动 AUTO-自动）',
    MODIFY COLUMN lock_reason_code   VARCHAR(50)  DEFAULT NULL         COMMENT '锁定原因代码',
    MODIFY COLUMN lock_reason_detail VARCHAR(500) DEFAULT NULL         COMMENT '锁定原因详情',
    MODIFY COLUMN locked_at       DATETIME     DEFAULT NULL COMMENT '锁定时间；时间点按UTC保存',
    MODIFY COLUMN locked_until    DATETIME     DEFAULT NULL COMMENT '锁定到期时间（NULL=永久锁定）；时间点按UTC保存',
    MODIFY COLUMN operator_id     BIGINT       DEFAULT NULL            COMMENT '操作人ID',
    MODIFY COLUMN operator_name   VARCHAR(64)  DEFAULT NULL            COMMENT '操作人姓名',
    MODIFY COLUMN failed_login_count INT        NOT NULL DEFAULT 0    COMMENT '连续登录失败次数',
    MODIFY COLUMN last_failed_at  DATETIME     DEFAULT NULL COMMENT '最后一次失败时间；时间点按UTC保存',
    MODIFY COLUMN created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间点，UTC',
    MODIFY COLUMN updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间点，UTC',
    COMMENT = '账号锁定状态表';

ALTER TABLE `password_history`
    MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '安全框架密码历史环形缓冲记录标识；数据库自增',
    MODIFY COLUMN `user_id` BIGINT NOT NULL COMMENT '安全框架全局用户ID；同用户按sequence_number维护环形历史',
    MODIFY COLUMN `password_hash` VARCHAR(255) NOT NULL COMMENT '不可逆密码哈希，禁止作为响应明文返回',
    MODIFY COLUMN `sequence_number` INT NOT NULL COMMENT '序号（用于环形缓冲，从1开始）',
    MODIFY COLUMN `version` bigint NOT NULL DEFAULT '1' COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    MODIFY COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间点，UTC',
    MODIFY COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间点，UTC',
    COMMENT = '密码历史记录（环形缓冲）';

ALTER TABLE `password_expiration`
    MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '安全框架密码过期状态记录标识；数据库自增',
    MODIFY COLUMN `user_id` BIGINT NOT NULL COMMENT '安全框架全局用户ID，唯一对应一份密码过期状态',
    MODIFY COLUMN `last_changed_at` DATETIME NOT NULL COMMENT '最后修改密码时间；时间点按UTC保存',
    MODIFY COLUMN `expires_at` DATETIME NOT NULL COMMENT '到期时间点，UTC',
    MODIFY COLUMN `force_change` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否强制修改（0-否 1-是）',
    MODIFY COLUMN `grace_login_remaining` INT NOT NULL DEFAULT 0 COMMENT '剩余宽限登录次数',
    MODIFY COLUMN `next_warning_at` DATETIME NULL COMMENT '下次提醒时间；时间点按UTC保存',
    MODIFY COLUMN `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间点，UTC',
    MODIFY COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间点，UTC',
    COMMENT = '密码过期信息';
