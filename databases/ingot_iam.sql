-- IAM 首次部署完整初始化：直接执行本文件即可建库、建表并写入默认数据。
-- 每次执行都会删除并重建 ingot_iam 库，该库已有表及数据全部清空。
-- 自动生成：python3 tools/iam/generate_database.py；不要手工编辑。
-- 来源顺序：databases/iam/manifest.json；框架DDL仍由原模块维护。
-- 固定作用于 ingot_iam，不需要预先建库或选择数据库；执行前停止使用该库的服务。
-- 包含应用、资源、权限操作、菜单、角色及关联；不导入测试租户或运行记录。
-- 默认平台账号 platform，初始密码 11111111，首次登录必须修改密码。
-- MySQL 8.0.16+；仅删除阶段关闭外键，建表/种子阶段开启，最后恢复原设置；数据时间按UTC写入。
SET NAMES utf8mb4;
SET SESSION time_zone = '+00:00';

-- 整库重建同时移除历史迁移表和其他遗留表；不处理其他数据库。
SET @iam_init_previous_foreign_key_checks = @@SESSION.FOREIGN_KEY_CHECKS;
SET SESSION FOREIGN_KEY_CHECKS = 0;
DROP DATABASE IF EXISTS `ingot_iam`;
CREATE DATABASE `ingot_iam` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `ingot_iam`;
SET SESSION FOREIGN_KEY_CHECKS = 1;

-- ====================================================================
-- Source: databases/iam/001_identity.sql
-- ====================================================================
-- 业务时间点统一 UTC；初始化连接也必须明确会话时区。
SET time_zone = '+00:00';

-- IAM 独立目标库：身份与组织结构。MySQL 8.0.16+；连接时区须为 UTC。
-- 仅在显式选择的全新目标库执行，不包含 USE、DROP、源库修改或默认数据。
-- 不是存量库升级脚本。组织创建事务须在提交前补齐有效 owner_member_id。

CREATE TABLE iam_account (
    id BIGINT UNSIGNED NOT NULL COMMENT '全局登录账号记录标识',
    username VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '全局唯一登录名，大小写按二进制排序规则区分',
    password_hash VARCHAR(300) NOT NULL COMMENT '不可逆密码哈希，禁止作为响应明文返回',
    phone VARCHAR(32) NULL COMMENT '账号登录手机号，可空；成员联系手机号独立保存',
    email VARCHAR(128) NULL COMMENT '账号邮箱，可空；成员联系邮箱独立保存',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否要求下次登录修改密码：1是、0否',
    password_changed_at DATETIME(6) NULL COMMENT '最近密码变更时间点，UTC；NULL表示尚无记录',
    last_login_at DATETIME(6) NULL COMMENT '最近登录时间点，UTC；NULL表示尚无记录',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    deleted_at DATETIME(6) NULL COMMENT '软删除时间点，UTC；NULL表示未删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_account_username (username),
    KEY idx_iam_account_phone (phone),
    KEY idx_iam_account_email (email),
    CONSTRAINT ck_iam_account_id CHECK (id > 0),
    CONSTRAINT ck_iam_account_flags CHECK (enabled IN (0, 1) AND must_change_password IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='全局登录账号';

CREATE TABLE iam_platform_member (
    id BIGINT UNSIGNED NOT NULL COMMENT '平台成员资格与资料记录标识',
    account_id BIGINT UNSIGNED NOT NULL COMMENT '关联全局登录账号ID，成员资格及联系资料与账号独立',
    display_name VARCHAR(128) NOT NULL COMMENT '当前管理域内的成员显示姓名',
    avatar VARCHAR(512) NULL COMMENT '头像对象路径，仅存bucket/objectName；响应按权限签发临时链接',
    phone VARCHAR(32) NULL COMMENT '平台联系资料，不修改全局登录手机号',
    email VARCHAR(128) NULL COMMENT '平台联系资料，不修改全局账号邮箱',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE' COMMENT '平台成员资格：ACTIVE有效、SUSPENDED暂停、REMOVED移出；与账号启用状态独立',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_platform_member_account (account_id),
    KEY idx_iam_platform_member_status (status, id),
    CONSTRAINT fk_iam_platform_member_account FOREIGN KEY (account_id) REFERENCES iam_account (id),
    CONSTRAINT ck_iam_platform_member_id CHECK (id > 0),
    CONSTRAINT ck_iam_platform_member_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REMOVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台成员资格与资料';

CREATE TABLE iam_tenant (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户组织记录标识',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    avatar VARCHAR(512) NULL COMMENT '头像对象路径，仅存bucket/objectName；响应按权限签发临时链接',
    owner_member_id BIGINT UNSIGNED NULL COMMENT '创建事务内允许暂空；提交前必须是本租户有效成员',
    plan_id BIGINT UNSIGNED NULL COMMENT '最近一次明确提交的套餐ID；套餐目录变化不自动回写组织开通',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    deleted_at DATETIME(6) NULL COMMENT '软删除时间点，UTC；NULL表示未删除',
    PRIMARY KEY (id),
    KEY idx_iam_tenant_owner (id, owner_member_id),
    CONSTRAINT ck_iam_tenant_id CHECK (id > 0),
    CONSTRAINT ck_iam_tenant_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户组织';

CREATE TABLE iam_tenant_member (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户成员资格与通讯录资料记录标识',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    account_id BIGINT UNSIGNED NOT NULL COMMENT '关联全局登录账号ID，成员资格及联系资料与账号独立',
    display_name VARCHAR(128) NOT NULL COMMENT '当前管理域内的成员显示姓名',
    avatar VARCHAR(512) NULL COMMENT '头像对象路径，仅存bucket/objectName；响应按权限签发临时链接',
    phone VARCHAR(32) NULL COMMENT '组织通讯录资料，不修改全局登录手机号',
    email VARCHAR(128) NULL COMMENT '组织通讯录资料，不修改全局登录邮箱',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE' COMMENT '当前租户成员资格：ACTIVE有效、SUSPENDED暂停、REMOVED移出；移出保留历史标识',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_tenant_member_account (tenant_id, account_id),
    UNIQUE KEY uk_iam_tenant_member_domain (tenant_id, id),
    KEY idx_iam_tenant_member_status (tenant_id, status, id),
    KEY idx_iam_tenant_member_account (account_id, status),
    CONSTRAINT fk_iam_tenant_member_account FOREIGN KEY (account_id) REFERENCES iam_account (id),
    CONSTRAINT fk_iam_tenant_member_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT ck_iam_tenant_member_id CHECK (id > 0),
    CONSTRAINT ck_iam_tenant_member_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REMOVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户成员资格与通讯录资料';

ALTER TABLE iam_tenant ADD CONSTRAINT fk_iam_tenant_owner
    FOREIGN KEY (id, owner_member_id) REFERENCES iam_tenant_member (tenant_id, id);

CREATE TABLE iam_department (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户部门树记录标识',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    parent_id BIGINT UNSIGNED NULL COMMENT '父节点ID；NULL表示根节点，必须属于相同归属域',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '同级显示排序权重，由查询排序规则使用',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_department_domain (tenant_id, id),
    KEY idx_iam_department_parent (tenant_id, parent_id, id),
    CONSTRAINT fk_iam_department_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT fk_iam_department_parent FOREIGN KEY (tenant_id, parent_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT ck_iam_department_id CHECK (id > 0),
    CONSTRAINT ck_iam_department_parent CHECK (parent_id IS NULL OR parent_id <> id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户部门树';

CREATE TABLE iam_member_department (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    member_id BIGINT UNSIGNED NOT NULL COMMENT '成员标识；引用归属由本表外键及业务事务共同校验',
    department_id BIGINT UNSIGNED NOT NULL COMMENT '部门标识；引用归属由本表外键及业务事务共同校验',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为该成员的主部门：1是、0否；生成列唯一索引保证每成员至多一个',
    primary_member_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN is_primary = 1 THEN member_id ELSE NULL END) STORED COMMENT '自动约束键：主关系取member_id，否则NULL；与tenant_id唯一，保证成员至多一个主部门，非空集合业务规则由应用校验',
    PRIMARY KEY (tenant_id, member_id, department_id),
    UNIQUE KEY uk_iam_member_primary_department (tenant_id, primary_member_id),
    KEY idx_iam_department_members (tenant_id, department_id, member_id),
    CONSTRAINT fk_iam_member_department_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id),
    CONSTRAINT fk_iam_member_department_department FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT ck_iam_member_department_primary CHECK (is_primary IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成员任职部门关系';

CREATE TABLE iam_department_leader (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    department_id BIGINT UNSIGNED NOT NULL COMMENT '部门标识；引用归属由本表外键及业务事务共同校验',
    member_id BIGINT UNSIGNED NOT NULL COMMENT '成员标识；引用归属由本表外键及业务事务共同校验',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为该部门的主负责人：1是、0否；非空集合恰有一个由业务事务校验',
    primary_department_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN is_primary = 1 THEN department_id ELSE NULL END) STORED COMMENT '自动约束键：主关系取department_id，否则NULL；与tenant_id唯一，保证部门至多一个主负责人，非空集合须恰有一个由应用校验',
    PRIMARY KEY (tenant_id, department_id, member_id),
    UNIQUE KEY uk_iam_department_primary_leader (tenant_id, primary_department_id),
    KEY idx_iam_member_led_departments (tenant_id, member_id, department_id),
    CONSTRAINT fk_iam_department_leader_department FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT fk_iam_department_leader_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id),
    CONSTRAINT ck_iam_department_leader_primary CHECK (is_primary IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门负责人关系';

CREATE TABLE iam_platform_group (
    id BIGINT UNSIGNED NOT NULL COMMENT '平台静态用户组记录标识',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    description VARCHAR(512) NULL COMMENT '业务说明，可空',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    PRIMARY KEY (id),
    CONSTRAINT ck_iam_platform_group_id CHECK (id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台静态用户组';

CREATE TABLE iam_platform_group_member (
    group_id BIGINT UNSIGNED NOT NULL COMMENT '用户组标识；引用归属由本表外键及业务事务共同校验',
    member_id BIGINT UNSIGNED NOT NULL COMMENT '成员标识；引用归属由本表外键及业务事务共同校验',
    PRIMARY KEY (group_id, member_id),
    KEY idx_iam_platform_member_groups (member_id, group_id),
    CONSTRAINT fk_iam_platform_group_entry_group FOREIGN KEY (group_id) REFERENCES iam_platform_group (id),
    CONSTRAINT fk_iam_platform_group_entry_member FOREIGN KEY (member_id) REFERENCES iam_platform_member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台用户组显式成员';

CREATE TABLE iam_tenant_group (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户静态用户组记录标识',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    description VARCHAR(512) NULL COMMENT '业务说明，可空',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_tenant_group_domain (tenant_id, id),
    CONSTRAINT fk_iam_tenant_group_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT ck_iam_tenant_group_id CHECK (id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户静态用户组';

CREATE TABLE iam_tenant_group_member (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    group_id BIGINT UNSIGNED NOT NULL COMMENT '用户组标识；引用归属由本表外键及业务事务共同校验',
    member_id BIGINT UNSIGNED NOT NULL COMMENT '成员标识；引用归属由本表外键及业务事务共同校验',
    PRIMARY KEY (tenant_id, group_id, member_id),
    KEY idx_iam_tenant_member_groups (tenant_id, member_id, group_id),
    CONSTRAINT fk_iam_tenant_group_entry_group FOREIGN KEY (tenant_id, group_id) REFERENCES iam_tenant_group (tenant_id, id),
    CONSTRAINT fk_iam_tenant_group_entry_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户用户组显式成员';

CREATE TABLE iam_tenant_group_department (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    group_id BIGINT UNSIGNED NOT NULL COMMENT '用户组标识；引用归属由本表外键及业务事务共同校验',
    department_id BIGINT UNSIGNED NOT NULL COMMENT '部门标识；引用归属由本表外键及业务事务共同校验',
    include_descendants BOOLEAN NOT NULL DEFAULT FALSE COMMENT '选择该部门时是否包含下级部门成员：1包含、0仅直属',
    PRIMARY KEY (tenant_id, group_id, department_id),
    KEY idx_iam_tenant_department_groups (tenant_id, department_id, group_id),
    CONSTRAINT fk_iam_tenant_group_dept_group FOREIGN KEY (tenant_id, group_id) REFERENCES iam_tenant_group (tenant_id, id),
    CONSTRAINT fk_iam_tenant_group_dept_department FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT ck_iam_group_include_descendants CHECK (include_descendants IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户用户组部门来源';

-- ====================================================================
-- Source: databases/iam/002_catalog_role.sql
-- ====================================================================
-- 业务时间点统一 UTC；初始化连接也必须明确会话时区。
SET time_zone = '+00:00';

-- IAM 独立目标库：先执行 001_identity.sql。无源库写入或授权默认数据。
CREATE TABLE iam_application (
    id BIGINT UNSIGNED NOT NULL COMMENT '应用目录记录标识',
    code VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '业务稳定编码，唯一性由本表索引限定',
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    description VARCHAR(512) NULL COMMENT '业务说明，可空',
    icon VARCHAR(512) NULL COMMENT '界面图标标识或配置值，按所属应用菜单契约解释',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '同级显示排序权重，由查询排序规则使用',
    baseline BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为租户基础治理应用：1是、0否；基础应用开通不可关闭',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_application_code (code),
    CONSTRAINT ck_iam_application_domain CHECK (domain IN ('PLATFORM', 'TENANT')),
    CONSTRAINT ck_iam_application_baseline CHECK (baseline IN (0, 1)),
    CONSTRAINT ck_iam_application_baseline_domain CHECK (baseline = FALSE OR domain = 'TENANT'),
    CONSTRAINT ck_iam_application_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用目录';

CREATE TABLE iam_resource (
    id BIGINT UNSIGNED NOT NULL COMMENT '应用资源及范围字段能力记录标识',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    code VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '业务稳定编码，唯一性由本表索引限定',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    scope_capabilities JSON NOT NULL COMMENT '资源支持的范围能力JSON数组，作为授权范围校验上限',
    field_capabilities JSON NOT NULL COMMENT '资源字段能力JSON数组，限制可见性、编辑及筛选等可声明能力',
    field_policy BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否接入租户字段策略：1接入、0仅使用角色字段权限',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_resource_code (application_id, code),
    UNIQUE KEY uk_iam_resource_application (application_id, id),
    CONSTRAINT fk_iam_resource_application FOREIGN KEY (application_id) REFERENCES iam_application (id),
    CONSTRAINT ck_iam_resource_scopes CHECK (JSON_TYPE(scope_capabilities) = 'ARRAY'),
    CONSTRAINT ck_iam_resource_fields CHECK (JSON_TYPE(field_capabilities) = 'ARRAY'),
    CONSTRAINT ck_iam_resource_field_policy CHECK (field_policy IN (0, 1)),
    CONSTRAINT ck_iam_resource_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用资源及范围字段能力';

CREATE TABLE iam_action (
    id BIGINT UNSIGNED NOT NULL COMMENT '资源精确操作目录记录标识',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    resource_id BIGINT UNSIGNED NOT NULL COMMENT '资源标识；引用归属由本表外键及业务事务共同校验',
    code VARCHAR(192) COLLATE utf8mb4_bin NOT NULL COMMENT '全局唯一精确操作码，禁止通配符，例如iam-tenant:member:read',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_action_code (code),
    UNIQUE KEY uk_iam_action_application (application_id, id),
    KEY idx_iam_action_resource (application_id, resource_id, id),
    CONSTRAINT fk_iam_action_resource FOREIGN KEY (application_id, resource_id) REFERENCES iam_resource (application_id, id),
    CONSTRAINT ck_iam_action_exact CHECK (LOCATE('*', code) = 0 AND CHAR_LENGTH(code) > 0),
    CONSTRAINT ck_iam_action_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资源精确操作目录';

CREATE TABLE iam_menu (
    id BIGINT UNSIGNED NOT NULL COMMENT '应用菜单与前端路由记录标识',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    parent_id BIGINT UNSIGNED NULL COMMENT '父节点ID；NULL表示根节点，必须属于相同归属域',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    path VARCHAR(512) NULL COMMENT '前端菜单路由路径，可空',
    view_path VARCHAR(256) NULL COMMENT '前端视图注册键；目录布局使用约定布局键',
    route_name VARCHAR(128) NULL COMMENT '前端路由稳定名称，可空',
    icon VARCHAR(512) NULL COMMENT '界面图标标识或配置值，按所属应用菜单契约解释',
    kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '菜单类别：DIRECTORY目录、PAGE页面',
    match_mode VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ANY' COMMENT '菜单操作匹配方式：ANY任一、ALL全部；取值ANY,ALL',
    access_mode VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTION' COMMENT '菜单准入模式：OPEN开放、ACTION按绑定操作判断；取值OPEN,ACTION',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '同级显示排序权重，由查询排序规则使用',
    hidden BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否在菜单中隐藏：1隐藏、0展示；不授予访问权限',
    is_cache BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否启用前端页面缓存：1是、0否',
    props BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否将路由参数作为页面属性传入：1是、0否',
    route_params JSON NULL COMMENT '路由参数JSON，可空',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_menu_application (application_id, id),
    KEY idx_iam_menu_parent (application_id, parent_id, sort_order, id),
    CONSTRAINT fk_iam_menu_application FOREIGN KEY (application_id) REFERENCES iam_application (id),
    CONSTRAINT fk_iam_menu_parent FOREIGN KEY (application_id, parent_id) REFERENCES iam_menu (application_id, id),
    CONSTRAINT ck_iam_menu_kind CHECK (kind IN ('DIRECTORY', 'PAGE')),
    CONSTRAINT ck_iam_menu_match CHECK (match_mode IN ('ANY', 'ALL')),
    CONSTRAINT ck_iam_menu_access CHECK (access_mode IN ('OPEN', 'ACTION')),
    CONSTRAINT ck_iam_menu_parent CHECK (parent_id IS NULL OR parent_id <> id),
    CONSTRAINT ck_iam_menu_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用菜单与前端路由';

CREATE TABLE iam_menu_action (
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    menu_id BIGINT UNSIGNED NOT NULL COMMENT '菜单标识；引用归属由本表外键及业务事务共同校验',
    action_id BIGINT UNSIGNED NOT NULL COMMENT '精确操作标识；引用归属由本表外键及业务事务共同校验',
    PRIMARY KEY (application_id, menu_id, action_id),
    KEY idx_iam_action_menus (application_id, action_id, menu_id),
    CONSTRAINT fk_iam_menu_action_menu FOREIGN KEY (application_id, menu_id) REFERENCES iam_menu (application_id, id),
    CONSTRAINT fk_iam_menu_action_action FOREIGN KEY (application_id, action_id) REFERENCES iam_action (application_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单操作绑定';

CREATE TABLE iam_tenant_app_entitlement (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户应用开通记录标识',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    enabled BOOLEAN NOT NULL COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    source VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '记录来源，取值及对应来源标识由所属业务契约解释；取值INITIALIZATION,MANUAL,PLAN,MIGRATION',
    source_id BIGINT UNSIGNED NULL COMMENT '开通来源对象ID；PLAN来源记录套餐ID，其他来源按业务规则可空',
    valid_from DATETIME(6) NULL COMMENT '生效时间点，UTC；可空时表示不限制开始时间',
    valid_until DATETIME(6) NULL COMMENT '失效时间点，UTC；NULL表示不限制结束时间',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_entitlement (tenant_id, application_id),
    CONSTRAINT fk_iam_entitlement_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT fk_iam_entitlement_app FOREIGN KEY (application_id) REFERENCES iam_application (id),
    CONSTRAINT ck_iam_entitlement_source CHECK (source IN ('INITIALIZATION', 'MANUAL', 'PLAN', 'MIGRATION')),
    CONSTRAINT ck_iam_entitlement_interval CHECK (valid_from IS NULL OR valid_until IS NULL OR valid_from < valid_until),
    CONSTRAINT ck_iam_entitlement_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户应用开通';

CREATE TABLE iam_app_audience (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    audience_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '应用可用人群：ALL全部成员、SELECTED指定来源；开通不等于操作授权；取值ALL,SELECTED',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    PRIMARY KEY (tenant_id, application_id),
    CONSTRAINT fk_iam_audience_entitlement FOREIGN KEY (tenant_id, application_id) REFERENCES iam_tenant_app_entitlement (tenant_id, application_id),
    CONSTRAINT ck_iam_audience_kind CHECK (audience_kind IN ('ALL', 'SELECTED')),
    CONSTRAINT ck_iam_audience_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户应用可用人群配置';

CREATE TABLE iam_audience_member (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    member_id BIGINT UNSIGNED NOT NULL COMMENT '成员标识；引用归属由本表外键及业务事务共同校验',
    PRIMARY KEY (tenant_id, application_id, member_id),
    CONSTRAINT fk_iam_audience_member_config FOREIGN KEY (tenant_id, application_id) REFERENCES iam_app_audience (tenant_id, application_id),
    CONSTRAINT fk_iam_audience_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用人群显式成员';

CREATE TABLE iam_audience_department (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    department_id BIGINT UNSIGNED NOT NULL COMMENT '部门标识；引用归属由本表外键及业务事务共同校验',
    include_descendants BOOLEAN NOT NULL DEFAULT FALSE COMMENT '选择该部门时是否包含下级部门成员：1包含、0仅直属',
    PRIMARY KEY (tenant_id, application_id, department_id),
    CONSTRAINT fk_iam_audience_dept_config FOREIGN KEY (tenant_id, application_id) REFERENCES iam_app_audience (tenant_id, application_id),
    CONSTRAINT fk_iam_audience_dept FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT ck_iam_audience_descendants CHECK (include_descendants IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用人群部门来源';

CREATE TABLE iam_audience_group (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    group_id BIGINT UNSIGNED NOT NULL COMMENT '用户组标识；引用归属由本表外键及业务事务共同校验',
    PRIMARY KEY (tenant_id, application_id, group_id),
    CONSTRAINT fk_iam_audience_group_config FOREIGN KEY (tenant_id, application_id) REFERENCES iam_app_audience (tenant_id, application_id),
    CONSTRAINT fk_iam_audience_group FOREIGN KEY (tenant_id, group_id) REFERENCES iam_tenant_group (tenant_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用人群用户组来源';

CREATE TABLE iam_plan (
    id BIGINT UNSIGNED NOT NULL COMMENT '套餐目录记录标识',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    description VARCHAR(512) NULL COMMENT '业务说明，可空',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    PRIMARY KEY (id),
    CONSTRAINT ck_iam_plan_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='套餐目录';

CREATE TABLE iam_plan_application (
    plan_id BIGINT UNSIGNED NOT NULL COMMENT '套餐标识；引用归属由本表外键及业务事务共同校验',
    application_id BIGINT UNSIGNED NOT NULL COMMENT '应用标识；引用归属由本表外键及业务事务共同校验',
    PRIMARY KEY (plan_id, application_id),
    CONSTRAINT fk_iam_plan_application_plan FOREIGN KEY (plan_id) REFERENCES iam_plan (id),
    CONSTRAINT fk_iam_plan_application_app FOREIGN KEY (application_id) REFERENCES iam_application (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='套餐包含的应用';

CREATE TABLE iam_role_definition (
    id BIGINT UNSIGNED NOT NULL COMMENT '角色身份与当前快照指针记录标识',
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    tenant_id BIGINT UNSIGNED NULL COMMENT '所属租户组织ID；NULL表示平台域或共享定义',
    tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED COMMENT '自动生成的归属唯一键：tenant_id为空取0，否则取租户ID；用于复合唯一索引及外键',
    kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '角色类别：SYSTEM系统、SHARED共享、PLATFORM_CUSTOM平台自有、TENANT_CUSTOM租户私有',
    code VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '业务稳定编码，唯一性由本表索引限定',
    name VARCHAR(128) NOT NULL COMMENT '业务显示名称',
    description VARCHAR(512) NULL COMMENT '业务说明，可空',
    group_name VARCHAR(128) NULL COMMENT '角色展示分组名称，可空',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1启用、0停用；不代表已授予操作权限',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    current_revision_id BIGINT UNSIGNED NULL COMMENT '共享或租户私有角色当前完整权限快照ID；平台固定版本路径不依赖此指针',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_role_code (domain, tenant_key, code),
    UNIQUE KEY uk_iam_role_kind (id, kind),
    UNIQUE KEY uk_iam_role_owner (domain, tenant_key, id, kind),
    CONSTRAINT fk_iam_role_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT ck_iam_role_domain CHECK (domain IN ('PLATFORM', 'TENANT')),
    CONSTRAINT ck_iam_role_kind CHECK (
        (kind = 'SYSTEM' AND tenant_id IS NULL) OR
        (kind = 'SHARED' AND domain = 'TENANT' AND tenant_id IS NULL) OR
        (kind = 'PLATFORM_CUSTOM' AND domain = 'PLATFORM' AND tenant_id IS NULL) OR
        (kind = 'TENANT_CUSTOM' AND domain = 'TENANT' AND tenant_id IS NOT NULL)),
    CONSTRAINT ck_iam_role_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色身份与当前快照指针';

CREATE TABLE iam_role_revision (
    id BIGINT UNSIGNED NOT NULL COMMENT '角色不可变权限快照与可编辑版本元数据记录标识',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色标识；引用归属由本表外键及业务事务共同校验',
    kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '权限快照所属角色类别，与role_id共同引用角色；快照权限内容不可变',
    revision BIGINT UNSIGNED NOT NULL COMMENT '不可变快照的发布序号，从1开始；与记录乐观锁版本不同',
    base_revision_id BIGINT UNSIGNED NULL COMMENT '历史定制角色引用的共享基础版本ID；NULL表示无基础版本',
    base_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin
        GENERATED ALWAYS AS (CASE WHEN base_revision_id IS NULL THEN NULL ELSE 'SHARED' END) STORED COMMENT '自动生成的基础版本种类：有base_revision_id时为SHARED，否则NULL，用于复合外键',
    metadata_overrides JSON NOT NULL COMMENT '角色快照元数据覆盖JSON对象；空对象表示无覆盖',
    resource_field_permissions JSON NOT NULL DEFAULT (JSON_OBJECT()) COMMENT '固定资源字段权限快照，未声明字段不授予权限',
    version_name VARCHAR(128) NULL COMMENT '版本显示名称；空值回退vN',
    remark VARCHAR(512) NULL COMMENT '版本说明',
    metadata_version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '独立元数据乐观锁',
    published_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '权限快照发布时间点，UTC',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_revision_owner (id, role_id),
    UNIQUE KEY uk_iam_revision_number (role_id, revision),
    UNIQUE KEY uk_iam_revision_kind (id, kind),
    CONSTRAINT fk_iam_revision_role FOREIGN KEY (role_id, kind) REFERENCES iam_role_definition (id, kind),
    CONSTRAINT fk_iam_revision_base FOREIGN KEY (base_revision_id, base_kind) REFERENCES iam_role_revision (id, kind),
    CONSTRAINT ck_iam_revision_number CHECK (revision > 0),
    CONSTRAINT ck_iam_revision_base CHECK (base_revision_id IS NULL OR (kind = 'TENANT_CUSTOM' AND base_revision_id <> id)),
    CONSTRAINT ck_iam_revision_metadata CHECK (JSON_TYPE(metadata_overrides) = 'OBJECT'),
    CONSTRAINT ck_iam_revision_fields CHECK (JSON_TYPE(resource_field_permissions) = 'OBJECT')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色不可变权限快照与可编辑版本元数据';

CREATE TABLE iam_role_parameter (
    revision_id BIGINT UNSIGNED NOT NULL COMMENT '角色固定版本标识；引用归属由本表外键及业务事务共同校验',
    parameter_key VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '快照内唯一的范围参数名称，用于分配参数绑定',
    resource_id BIGINT UNSIGNED NULL COMMENT '资源标识；引用归属由本表外键及业务事务共同校验',
    binding_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '范围参数绑定类别：DEPARTMENTS部门、OBJECTS业务对象；取值DEPARTMENTS,OBJECTS',
    PRIMARY KEY (revision_id, parameter_key),
    CONSTRAINT fk_iam_parameter_revision FOREIGN KEY (revision_id) REFERENCES iam_role_revision (id),
    CONSTRAINT fk_iam_parameter_resource FOREIGN KEY(resource_id) REFERENCES iam_resource(id),
    CONSTRAINT ck_iam_parameter_kind CHECK (binding_kind IN ('DEPARTMENTS', 'OBJECTS'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色快照范围参数定义';

CREATE TABLE iam_role_grant (
    revision_id BIGINT UNSIGNED NOT NULL COMMENT '角色固定版本标识；引用归属由本表外键及业务事务共同校验',
    action_id BIGINT UNSIGNED NOT NULL COMMENT '精确操作标识；引用归属由本表外键及业务事务共同校验',
    scopes JSON NOT NULL COMMENT '授权或上限的范围表达式JSON数组；具体空数组语义由对应求值器判定',
    PRIMARY KEY (revision_id, action_id),
    CONSTRAINT fk_iam_grant_revision FOREIGN KEY (revision_id) REFERENCES iam_role_revision (id),
    CONSTRAINT fk_iam_grant_action FOREIGN KEY (action_id) REFERENCES iam_action (id),
    CONSTRAINT ck_iam_grant_scopes CHECK (JSON_TYPE(scopes) = 'ARRAY')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色快照操作授权';

CREATE TABLE iam_role_delta (
    revision_id BIGINT UNSIGNED NOT NULL COMMENT '角色固定版本标识；引用归属由本表外键及业务事务共同校验',
    action_id BIGINT UNSIGNED NOT NULL COMMENT '精确操作标识；引用归属由本表外键及业务事务共同校验',
    operation VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '历史定制差异操作：ADD增加、REMOVE移除、REPLACE_SCOPE替换范围',
    scopes JSON NOT NULL COMMENT '授权或上限的范围表达式JSON数组；具体空数组语义由对应求值器判定',
    PRIMARY KEY (revision_id, action_id),
    CONSTRAINT fk_iam_delta_revision FOREIGN KEY (revision_id) REFERENCES iam_role_revision (id),
    CONSTRAINT fk_iam_delta_action FOREIGN KEY (action_id) REFERENCES iam_action (id),
    CONSTRAINT ck_iam_delta_operation CHECK (operation IN ('ADD', 'REMOVE', 'REPLACE_SCOPE')),
    CONSTRAINT ck_iam_delta_scopes CHECK (JSON_TYPE(scopes) = 'ARRAY' AND (operation <> 'REMOVE' OR JSON_LENGTH(scopes) = 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='历史租户角色定制差异';

-- 当前完整权限快照只能指向本角色。平台固定版本路径不依赖此指针。
ALTER TABLE iam_role_definition ADD CONSTRAINT fk_iam_role_current
    FOREIGN KEY (current_revision_id, id) REFERENCES iam_role_revision (id, role_id);

-- ====================================================================
-- Source: databases/iam/003_assignment_delegation.sql
-- ====================================================================
-- 业务时间点统一 UTC；初始化连接也必须明确会话时区。
SET time_zone = '+00:00';

-- IAM 独立目标库：先执行 001、002。跨域关系由复合外键约束；权限上限仍由事务服务校验。
CREATE TABLE iam_delegation_grant (
    id BIGINT UNSIGNED NOT NULL COMMENT '受限委派及期限上限记录标识',
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    tenant_id BIGINT UNSIGNED NULL COMMENT '所属租户组织ID；NULL表示平台域或共享定义',
    tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED COMMENT '自动生成的归属唯一键：tenant_id为空取0，否则取租户ID；用于复合唯一索引及外键',
    platform_administrator_id BIGINT UNSIGNED NULL COMMENT '被委派的平台管理员成员ID，平台域使用',
    tenant_administrator_id BIGINT UNSIGNED NULL COMMENT '被委派的租户管理员成员ID，租户域使用',
    valid_from DATETIME(6) NULL COMMENT '生效时间点，UTC；可空时表示不限制开始时间',
    valid_until DATETIME(6) NULL COMMENT '失效时间点，UTC；NULL表示不限制结束时间',
    assignment_duration_mode VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'LIMITED' COMMENT '单次授权期限模式：LIMITED限制时长、UNLIMITED无上限；后者仅平台域可用',
    max_assignment_duration_seconds BIGINT UNSIGNED NULL COMMENT '单次授权最大时长的秒部分；UNLIMITED时为NULL',
    max_assignment_duration_nanos INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '单次授权最大时长的纳秒部分，范围0至999999999',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE' COMMENT '委派状态：ACTIVE有效、REVOKED已撤销；时效和派生授权仍按业务校验',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_delegation_domain (domain, tenant_key, id),
    UNIQUE KEY uk_iam_delegation_tenant (tenant_id, id),
    KEY idx_iam_delegation_platform_admin (platform_administrator_id, status, valid_until),
    KEY idx_iam_delegation_tenant_admin (tenant_id, tenant_administrator_id, status, valid_until),
    CONSTRAINT fk_iam_delegation_platform_admin FOREIGN KEY (platform_administrator_id) REFERENCES iam_platform_member (id),
    CONSTRAINT fk_iam_delegation_tenant_admin FOREIGN KEY (tenant_id, tenant_administrator_id) REFERENCES iam_tenant_member (tenant_id, id),
    CONSTRAINT ck_iam_delegation_identity CHECK (
        (domain = 'PLATFORM' AND tenant_id IS NULL AND platform_administrator_id IS NOT NULL AND tenant_administrator_id IS NULL) OR
        (domain = 'TENANT' AND tenant_id IS NOT NULL AND tenant_administrator_id IS NOT NULL AND platform_administrator_id IS NULL)),
    CONSTRAINT ck_iam_delegation_status CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT ck_iam_delegation_interval CHECK (valid_from IS NULL OR valid_until IS NULL OR valid_from < valid_until),
    CONSTRAINT ck_iam_delegation_duration CHECK (max_assignment_duration_nanos < 1000000000 AND
        ((assignment_duration_mode='LIMITED' AND max_assignment_duration_seconds IS NOT NULL
          AND (max_assignment_duration_seconds > 0 OR max_assignment_duration_nanos > 0))
         OR (assignment_duration_mode='UNLIMITED' AND domain='PLATFORM'
          AND max_assignment_duration_seconds IS NULL AND max_assignment_duration_nanos=0)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='受限委派及期限上限';

CREATE TABLE iam_delegation_role_revision (
    delegation_id BIGINT UNSIGNED NOT NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    revision_id BIGINT UNSIGNED NOT NULL COMMENT '角色固定版本标识；引用归属由本表外键及业务事务共同校验',
    revision_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '被引用固定版本的角色种类，与版本ID共同验证引用合法性',
    PRIMARY KEY (delegation_id, revision_id),
    CONSTRAINT fk_iam_delegation_role_source FOREIGN KEY (delegation_id) REFERENCES iam_delegation_grant (id),
    CONSTRAINT fk_iam_delegation_role_revision FOREIGN KEY (revision_id, revision_kind) REFERENCES iam_role_revision (id, kind)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台委派允许的固定角色版本';

CREATE TABLE iam_delegation_recipient_member (
    delegation_id BIGINT UNSIGNED NOT NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    tenant_id BIGINT UNSIGNED NULL COMMENT '所属租户组织ID；NULL表示平台域或共享定义',
    tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED COMMENT '自动生成的归属唯一键：tenant_id为空取0，否则取租户ID；用于复合唯一索引及外键',
    platform_member_id BIGINT UNSIGNED NULL COMMENT '平台域接收成员ID；非平台成员主体时为空',
    tenant_member_id BIGINT UNSIGNED NULL COMMENT '租户域接收成员ID；非租户成员主体时为空',
    member_id BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(platform_member_id, tenant_member_id)) STORED COMMENT '自动生成的接收成员键：取platform_member_id或tenant_member_id；与delegation_id唯一，禁止重复接收者',
    id BIGINT UNSIGNED NOT NULL COMMENT '委派接收成员关系记录标识；由IAM的Leaf发号器生成，不承载业务顺序',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_delegation_recipient (delegation_id, member_id),
    CONSTRAINT fk_iam_recipient_delegation FOREIGN KEY (domain, tenant_key, delegation_id) REFERENCES iam_delegation_grant (domain, tenant_key, id),
    CONSTRAINT fk_iam_recipient_platform FOREIGN KEY (platform_member_id) REFERENCES iam_platform_member (id),
    CONSTRAINT fk_iam_recipient_tenant FOREIGN KEY (tenant_id, tenant_member_id) REFERENCES iam_tenant_member (tenant_id, id),
    CONSTRAINT ck_iam_recipient_identity CHECK (
        (domain = 'PLATFORM' AND tenant_id IS NULL AND platform_member_id IS NOT NULL AND tenant_member_id IS NULL) OR
        (domain = 'TENANT' AND tenant_id IS NOT NULL AND tenant_member_id IS NOT NULL AND platform_member_id IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='委派允许的接收成员';

CREATE TABLE iam_delegation_recipient_department (
    delegation_id BIGINT UNSIGNED NOT NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    department_id BIGINT UNSIGNED NOT NULL COMMENT '部门标识；引用归属由本表外键及业务事务共同校验',
    include_descendants BOOLEAN NOT NULL DEFAULT FALSE COMMENT '选择该部门时是否包含下级部门成员：1包含、0仅直属',
    PRIMARY KEY (delegation_id, department_id),
    CONSTRAINT fk_iam_recipient_dept_delegation FOREIGN KEY (tenant_id, delegation_id) REFERENCES iam_delegation_grant (tenant_id, id),
    CONSTRAINT fk_iam_recipient_dept_department FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT ck_iam_recipient_descendants CHECK (include_descendants IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='委派允许的接收部门';

CREATE TABLE iam_delegation_action_ceiling (
    delegation_id BIGINT UNSIGNED NOT NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    action_id BIGINT UNSIGNED NOT NULL COMMENT '精确操作标识；引用归属由本表外键及业务事务共同校验',
    scopes JSON NOT NULL COMMENT '授权或上限的范围表达式JSON数组；具体空数组语义由对应求值器判定',
    scope_bindings JSON NOT NULL COMMENT '参数名到范围对象的绑定JSON对象；空对象表示无显式绑定',
    PRIMARY KEY (delegation_id, action_id),
    CONSTRAINT fk_iam_ceiling_delegation FOREIGN KEY (delegation_id) REFERENCES iam_delegation_grant (id),
    CONSTRAINT fk_iam_ceiling_action FOREIGN KEY (action_id) REFERENCES iam_action (id),
    CONSTRAINT ck_iam_ceiling_scopes CHECK (JSON_TYPE(scopes) = 'ARRAY'),
    CONSTRAINT ck_iam_ceiling_bindings CHECK (JSON_TYPE(scope_bindings) = 'OBJECT')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台委派逐操作范围上限';

CREATE TABLE iam_role_assignment (
    id BIGINT UNSIGNED NOT NULL COMMENT '成员或用户组角色分配记录标识',
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    tenant_id BIGINT UNSIGNED NULL COMMENT '所属租户组织ID；NULL表示平台域或共享定义',
    tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED COMMENT '自动生成的归属唯一键：tenant_id为空取0，否则取租户ID；用于复合唯一索引及外键',
    subject_type VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权接收主体类别：MEMBER成员、GROUP用户组',
    platform_member_id BIGINT UNSIGNED NULL COMMENT '平台域接收成员ID；非平台成员主体时为空',
    platform_group_id BIGINT UNSIGNED NULL COMMENT '平台域接收用户组ID；非平台组主体时为空',
    tenant_member_id BIGINT UNSIGNED NULL COMMENT '租户域接收成员ID；非租户成员主体时为空',
    tenant_group_id BIGINT UNSIGNED NULL COMMENT '租户域接收用户组ID；非租户组主体时为空',
    role_id BIGINT UNSIGNED NULL COMMENT '租户分配引用的角色身份ID；平台固定版本路径为空；撤销历史记录可保留旧版本引用',
    role_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '被引用角色种类，与角色ID及归属共同验证合法性',
    role_tenant_key BIGINT UNSIGNED NULL COMMENT '角色归属键：共享角色为0，租户私有角色为对应租户ID，配合复合外键校验',
    revision_id BIGINT UNSIGNED NULL COMMENT '平台分配引用的固定版本ID；租户有效分配按role_id求当前快照',
    revision_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '被引用固定版本的角色种类，与版本ID共同验证引用合法性',
    scope_bindings JSON NOT NULL COMMENT '参数名到范围对象的绑定JSON对象；空对象表示无显式绑定',
    delegation_grant_id BIGINT UNSIGNED NULL COMMENT '来源委派标识；引用归属由本表外键及业务事务共同校验',
    valid_from DATETIME(6) NOT NULL COMMENT '生效时间点，UTC；可空时表示不限制开始时间',
    valid_until DATETIME(6) NULL COMMENT '失效时间点，UTC；NULL表示不限制结束时间',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE' COMMENT '分配状态：ACTIVE有效、REVOKED已撤销；有效期与主体状态还须独立校验',
    source VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '记录来源，取值及对应来源标识由所属业务契约解释；取值MANUAL,INITIALIZATION,MIGRATION',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    PRIMARY KEY (id),
    KEY ix_iam_assignment_revision_status (revision_id,status,id),
    KEY idx_iam_assignment_platform_member (platform_member_id, status, valid_until),
    KEY idx_iam_assignment_platform_group (platform_group_id, status, valid_until),
    KEY idx_iam_assignment_tenant_member (tenant_id, tenant_member_id, status, valid_until),
    KEY idx_iam_assignment_tenant_group (tenant_id, tenant_group_id, status, valid_until),
    KEY idx_iam_assignment_delegation (delegation_grant_id, status),
    CONSTRAINT fk_iam_assignment_platform_member FOREIGN KEY (platform_member_id) REFERENCES iam_platform_member (id),
    CONSTRAINT fk_iam_assignment_platform_group FOREIGN KEY (platform_group_id) REFERENCES iam_platform_group (id),
    CONSTRAINT fk_iam_assignment_tenant_member FOREIGN KEY (tenant_id, tenant_member_id) REFERENCES iam_tenant_member (tenant_id, id),
    CONSTRAINT fk_iam_assignment_tenant_group FOREIGN KEY (tenant_id, tenant_group_id) REFERENCES iam_tenant_group (tenant_id, id),
    CONSTRAINT fk_iam_assignment_revision FOREIGN KEY (revision_id, revision_kind) REFERENCES iam_role_revision (id, kind),
    CONSTRAINT fk_iam_assignment_delegation FOREIGN KEY (domain, tenant_key, delegation_grant_id) REFERENCES iam_delegation_grant (domain, tenant_key, id),
    KEY idx_iam_assignment_current_role (role_id, status),
    CONSTRAINT fk_iam_assignment_current_role FOREIGN KEY (domain, role_tenant_key, role_id, role_kind)
        REFERENCES iam_role_definition (domain, tenant_key, id, kind),
    CONSTRAINT ck_iam_assignment_current_role CHECK (
        (domain='PLATFORM' AND role_id IS NULL AND revision_id IS NOT NULL AND revision_kind IS NOT NULL) OR
        (domain='TENANT' AND role_id IS NOT NULL AND role_kind IS NOT NULL AND role_tenant_key IS NOT NULL AND
          ((role_kind='SHARED' AND role_tenant_key=0) OR
           (role_kind='TENANT_CUSTOM' AND role_tenant_key=tenant_id))) OR
        (domain='TENANT' AND status='REVOKED' AND role_id IS NULL AND revision_id IS NOT NULL)),
    CONSTRAINT ck_iam_assignment_subject CHECK (
        (domain = 'PLATFORM' AND tenant_id IS NULL AND tenant_member_id IS NULL AND tenant_group_id IS NULL AND
            ((subject_type = 'MEMBER' AND platform_member_id IS NOT NULL AND platform_group_id IS NULL) OR
             (subject_type = 'GROUP' AND platform_group_id IS NOT NULL AND platform_member_id IS NULL))) OR
        (domain = 'TENANT' AND tenant_id IS NOT NULL AND platform_member_id IS NULL AND platform_group_id IS NULL AND
            ((subject_type = 'MEMBER' AND tenant_member_id IS NOT NULL AND tenant_group_id IS NULL) OR
             (subject_type = 'GROUP' AND tenant_group_id IS NOT NULL AND tenant_member_id IS NULL)))),
    CONSTRAINT ck_iam_assignment_status CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT ck_iam_assignment_source CHECK (source IN ('MANUAL', 'INITIALIZATION', 'MIGRATION')),
    CONSTRAINT ck_iam_assignment_interval CHECK (valid_until IS NULL OR valid_from < valid_until),
    CONSTRAINT ck_iam_assignment_bindings CHECK (JSON_TYPE(scope_bindings) = 'OBJECT')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='成员或用户组角色分配';

CREATE TABLE iam_delegation_role (
    delegation_id BIGINT UNSIGNED NOT NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'TENANT' COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色标识；引用归属由本表外键及业务事务共同校验',
    role_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '被引用角色种类，与角色ID及归属共同验证合法性',
    role_tenant_key BIGINT UNSIGNED NOT NULL COMMENT '角色归属键：共享角色为0，租户私有角色为对应租户ID，配合复合外键校验',
    PRIMARY KEY (delegation_id, role_id),
    CONSTRAINT fk_iam_live_delegation FOREIGN KEY (tenant_id, delegation_id) REFERENCES iam_delegation_grant (tenant_id, id),
    CONSTRAINT fk_iam_live_delegation_role FOREIGN KEY (domain, role_tenant_key, role_id, role_kind)
        REFERENCES iam_role_definition (domain, tenant_key, id, kind),
    CONSTRAINT ck_iam_live_delegation_role CHECK (domain='TENANT' AND
        ((role_kind='SHARED' AND role_tenant_key=0) OR (role_kind='TENANT_CUSTOM' AND role_tenant_key=tenant_id)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户委派允许的角色身份';

CREATE TABLE iam_delegation_resource_ceiling (
    delegation_id BIGINT UNSIGNED NOT NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    resource_id BIGINT UNSIGNED NOT NULL COMMENT '资源标识；引用归属由本表外键及业务事务共同校验',
    scopes JSON NOT NULL COMMENT '授权或上限的范围表达式JSON数组；具体空数组语义由对应求值器判定',
    scope_bindings JSON NOT NULL COMMENT '参数名到范围对象的绑定JSON对象；空对象表示无显式绑定',
    PRIMARY KEY (delegation_id, resource_id),
    CONSTRAINT fk_iam_live_resource_delegation FOREIGN KEY (delegation_id) REFERENCES iam_delegation_grant (id),
    CONSTRAINT fk_iam_live_resource FOREIGN KEY (resource_id) REFERENCES iam_resource (id),
    CONSTRAINT ck_iam_live_resource_scopes CHECK (JSON_TYPE(scopes)='ARRAY'),
    CONSTRAINT ck_iam_live_resource_bindings CHECK (JSON_TYPE(scope_bindings)='OBJECT')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户委派逐资源范围上限';

-- ====================================================================
-- Source: databases/iam/004_policy_audit_migration.sql
-- ====================================================================
-- 业务时间点统一 UTC；初始化连接也必须明确会话时区。
SET time_zone = '+00:00';

-- IAM 独立目标库：策略引用、审计事实与迁移记录，不初始化或授予任何默认权限。
CREATE TABLE iam_default_policy_revision (
    id BIGINT UNSIGNED NOT NULL COMMENT '默认字段策略固定版本记录标识',
    resource_id BIGINT UNSIGNED NOT NULL COMMENT '所属资源ID，该修订只作为这一资源的默认基线',
    kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '默认策略类别：FIELD字段',
    revision BIGINT UNSIGNED NOT NULL COMMENT '不可变快照的发布序号，从1开始；与记录乐观锁版本不同',
    definition JSON NOT NULL COMMENT '默认字段策略不可变定义JSON对象，由字段求值器解释',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_default_policy_kind (id, kind),
    UNIQUE KEY uk_iam_default_policy_resource (id, resource_id),
    UNIQUE KEY uk_iam_default_policy_revision (resource_id, kind, revision),
    CONSTRAINT fk_iam_default_policy_resource FOREIGN KEY (resource_id) REFERENCES iam_resource (id),
    CONSTRAINT ck_iam_default_policy_kind CHECK (kind IN ('FIELD')),
    CONSTRAINT ck_iam_default_policy_definition CHECK (JSON_TYPE(definition) = 'OBJECT'),
    CONSTRAINT ck_iam_default_policy_revision CHECK (revision > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='默认字段策略固定版本';

CREATE TABLE iam_policy_selector (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户策略人员选择器记录标识',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_selector_tenant (tenant_id, id),
    CONSTRAINT fk_iam_selector_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户策略人员选择器';

CREATE TABLE iam_policy_selector_member (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    selector_id BIGINT UNSIGNED NOT NULL COMMENT '人员选择器标识；引用归属由本表外键及业务事务共同校验',
    member_id BIGINT UNSIGNED NOT NULL COMMENT '成员标识；引用归属由本表外键及业务事务共同校验',
    PRIMARY KEY (tenant_id, selector_id, member_id),
    CONSTRAINT fk_iam_selector_member_selector FOREIGN KEY (tenant_id, selector_id) REFERENCES iam_policy_selector (tenant_id, id),
    CONSTRAINT fk_iam_selector_member_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='策略选择器显式成员';

CREATE TABLE iam_policy_selector_department (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    selector_id BIGINT UNSIGNED NOT NULL COMMENT '人员选择器标识；引用归属由本表外键及业务事务共同校验',
    department_id BIGINT UNSIGNED NOT NULL COMMENT '部门标识；引用归属由本表外键及业务事务共同校验',
    include_descendants BOOLEAN NOT NULL DEFAULT FALSE COMMENT '选择该部门时是否包含下级部门成员：1包含、0仅直属',
    PRIMARY KEY (tenant_id, selector_id, department_id),
    CONSTRAINT fk_iam_selector_dept_selector FOREIGN KEY (tenant_id, selector_id) REFERENCES iam_policy_selector (tenant_id, id),
    CONSTRAINT fk_iam_selector_dept_dept FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT ck_iam_selector_descendants CHECK (include_descendants IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='策略选择器部门来源';

CREATE TABLE iam_field_policy (
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    resource_id BIGINT UNSIGNED NOT NULL COMMENT '接入字段策略的资源ID',
    default_revision_id BIGINT UNSIGNED NOT NULL COMMENT '绑定的默认策略固定版本ID，须属于同一资源',
    default_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'FIELD' COMMENT '默认策略种类，固定为FIELD',
    operation_rules JSON NOT NULL DEFAULT (JSON_ARRAY()) COMMENT '本资源字段操作规则JSON数组，限定已接入的编辑及筛选等能力',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    PRIMARY KEY (tenant_id, resource_id),
    CONSTRAINT fk_iam_field_policy_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT fk_iam_field_policy_resource FOREIGN KEY (resource_id) REFERENCES iam_resource (id),
    CONSTRAINT fk_iam_field_default FOREIGN KEY (default_revision_id, default_kind) REFERENCES iam_default_policy_revision (id, kind),
    CONSTRAINT fk_iam_field_policy_revision FOREIGN KEY (default_revision_id, resource_id) REFERENCES iam_default_policy_revision (id, resource_id),
    CONSTRAINT ck_iam_field_default_kind CHECK (default_kind = 'FIELD'),
    CONSTRAINT ck_iam_field_operations CHECK (JSON_TYPE(operation_rules) = 'ARRAY')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户字段策略及操作规则';

CREATE TABLE iam_field_rule (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户资源字段可见性规则记录标识',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    resource_id BIGINT UNSIGNED NOT NULL COMMENT '规则所属资源ID，与策略行一致',
    field_key VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '资源字段稳定标识，用于匹配目录能力及字段规则',
    viewer_selector_id BIGINT UNSIGNED NOT NULL COMMENT '规则适用的查看者选择器ID，须属于本租户',
    target_scope JSON NOT NULL COMMENT '字段规则目标范围表达式JSON数组',
    scope_bindings JSON NOT NULL COMMENT '参数名到范围对象的绑定JSON对象；空对象表示无显式绑定',
    visibility VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '字段可见性：HIDDEN隐藏、MASKED脱敏、FULL完整；取值HIDDEN,MASKED,FULL',
    PRIMARY KEY (id),
    KEY idx_iam_field_rule_match (tenant_id, resource_id, field_key, id),
    CONSTRAINT fk_iam_field_rule_policy FOREIGN KEY (tenant_id, resource_id) REFERENCES iam_field_policy (tenant_id, resource_id),
    CONSTRAINT fk_iam_field_rule_viewer FOREIGN KEY (tenant_id, viewer_selector_id) REFERENCES iam_policy_selector (tenant_id, id),
    CONSTRAINT ck_iam_field_rule_visibility CHECK (visibility IN ('HIDDEN', 'MASKED', 'FULL')),
    CONSTRAINT ck_iam_field_rule_scope CHECK (JSON_TYPE(target_scope) = 'ARRAY'),
    CONSTRAINT ck_iam_field_rule_bindings CHECK (JSON_TYPE(scope_bindings) = 'OBJECT')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户资源字段可见性规则';

-- 成员导出任务：共享库存储，供多实例读取状态、成员 ID 快照与过期清理。
CREATE TABLE iam_member_export (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户成员导出任务与成员快照记录标识',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    actor_member_id BIGINT UNSIGNED NOT NULL COMMENT '发起操作的当前域成员ID',
    tenant_version VARCHAR(32) COLLATE utf8mb4_bin NOT NULL COMMENT '登记导出任务时的组织版本，取iam_tenant.version',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值PENDING,RUNNING,SUCCEEDED,FAILED,EXPIRED',
    member_ids JSON NULL COMMENT '导出成员ID的JSON快照数组；任务尚未生成快照时可空',
    failure_reason VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '任务失败或阻塞的安全原因，不包含凭证及敏感原值',
    created_at DATETIME(6) NOT NULL COMMENT '记录创建时间点，UTC',
    completed_at DATETIME(6) NULL COMMENT '任务完成时间点，UTC；未完成时为空',
    expires_at DATETIME(6) NOT NULL COMMENT '到期时间点，UTC',
    PRIMARY KEY (id),
    KEY idx_iam_member_export_tenant_status (tenant_id, status, expires_at),
    CONSTRAINT ck_iam_member_export_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'EXPIRED')),
    CONSTRAINT ck_iam_member_export_ids CHECK (member_ids IS NULL OR JSON_TYPE(member_ids) = 'ARRAY')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户成员导出任务与成员快照';

-- 成员表格导入导出任务：与 JSON 导出分离，保存工作簿、错误报告与一次性密码。
CREATE TABLE iam_member_workbook (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户成员表格导入或导出任务标识',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    actor_member_id BIGINT UNSIGNED NOT NULL COMMENT '发起操作的当前域成员ID',
    kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '任务种类；取值EXPORT,IMPORT',
    mode VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '表格模式；取值CREATE,UPDATE',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值PENDING,RUNNING,SUCCEEDED,FAILED,EXPIRED',
    tenant_version VARCHAR(32) COLLATE utf8mb4_bin NOT NULL COMMENT '登记任务时的组织版本，取iam_tenant.version',
    member_ids JSON NULL COMMENT '导出范围成员ID的JSON数组；导入或尚未快照时可空',
    result JSON NULL COMMENT '成功失败计数与逐行原因，不含初始密码明文',
    workbook MEDIUMBLOB NULL COMMENT '导出或上传校验后的xlsx工作簿',
    error_report MEDIUMBLOB NULL COMMENT '逐行失败报告xlsx；无失败时为空',
    password_file MEDIUMBLOB NULL COMMENT '新账号一次性初始密码xlsx；下载后清除',
    password_downloaded TINYINT(1) NOT NULL DEFAULT 0 COMMENT '初始密码是否已一次性下载',
    failure_reason VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '整份任务失败的安全原因，不包含凭证及敏感原值',
    created_at DATETIME(6) NOT NULL COMMENT '记录创建时间点，UTC',
    completed_at DATETIME(6) NULL COMMENT '任务完成时间点，UTC；未完成时为空',
    expires_at DATETIME(6) NOT NULL COMMENT '到期时间点，UTC',
    PRIMARY KEY (id),
    KEY idx_iam_member_workbook_actor (tenant_id, actor_member_id, created_at),
    KEY idx_iam_member_workbook_status (tenant_id, status, expires_at),
    CONSTRAINT ck_iam_member_workbook_kind CHECK (kind IN ('EXPORT', 'IMPORT')),
    CONSTRAINT ck_iam_member_workbook_mode CHECK (mode IN ('CREATE', 'UPDATE')),
    CONSTRAINT ck_iam_member_workbook_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'EXPIRED')),
    CONSTRAINT ck_iam_member_workbook_ids CHECK (member_ids IS NULL OR JSON_TYPE(member_ids) = 'ARRAY'),
    CONSTRAINT ck_iam_member_workbook_downloaded CHECK (password_downloaded IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户成员表格导入导出任务';

-- 部门表格导入任务：导出走下载中心，原始工作簿与错误报告存对象存储。
CREATE TABLE iam_department_workbook (
    id BIGINT UNSIGNED NOT NULL COMMENT '租户部门表格导入任务标识',
    tenant_id BIGINT UNSIGNED NOT NULL COMMENT '所属租户组织ID',
    actor_member_id BIGINT UNSIGNED NOT NULL COMMENT '发起操作的当前域成员ID',
    kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '任务种类；取值IMPORT',
    mode VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '表格模式；取值CREATE,UPDATE',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值PENDING,RUNNING,SUCCEEDED,FAILED,EXPIRED',
    tenant_version VARCHAR(32) COLLATE utf8mb4_bin NOT NULL COMMENT '登记任务时的组织版本，取iam_tenant.version',
    department_ids JSON NULL COMMENT '导入预留范围；可空',
    result JSON NULL COMMENT '成功失败计数与逐行原因',
    workbook_path VARCHAR(512) COLLATE utf8mb4_bin NULL COMMENT '原始工作簿对象路径bucket/objectName',
    error_report_path VARCHAR(512) COLLATE utf8mb4_bin NULL COMMENT '错误报告对象路径bucket/objectName；无失败时为空',
    failure_reason VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '整份任务失败的安全原因，不包含凭证及敏感原值',
    created_at DATETIME(6) NOT NULL COMMENT '记录创建时间点，UTC',
    completed_at DATETIME(6) NULL COMMENT '任务完成时间点，UTC；未完成时为空',
    expires_at DATETIME(6) NOT NULL COMMENT '到期时间点，UTC',
    PRIMARY KEY (id),
    KEY idx_iam_department_workbook_actor (tenant_id, actor_member_id, created_at),
    KEY idx_iam_department_workbook_status (tenant_id, status, expires_at),
    CONSTRAINT ck_iam_department_workbook_kind CHECK (kind IN ('IMPORT')),
    CONSTRAINT ck_iam_department_workbook_mode CHECK (mode IN ('CREATE', 'UPDATE')),
    CONSTRAINT ck_iam_department_workbook_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'EXPIRED')),
    CONSTRAINT ck_iam_department_workbook_ids CHECK (department_ids IS NULL OR JSON_TYPE(department_ids) = 'ARRAY')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户部门表格导入任务';

-- 下载中心任务：导出文件存对象存储，按认证域、租户与发起成员隔离。
CREATE TABLE iam_download_task (
    id BIGINT UNSIGNED NOT NULL COMMENT '下载任务标识',
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '发起人的认证域；取值PLATFORM,TENANT',
    tenant_id BIGINT UNSIGNED NULL COMMENT '所属租户组织ID；平台域为空',
    actor_member_id BIGINT UNSIGNED NOT NULL COMMENT '发起导出的当前域成员ID',
    source VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '导出来源编码，下载时据此复核权限',
    title VARCHAR(128) NOT NULL COMMENT '列表展示名称',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值PENDING,RUNNING,SUCCEEDED,FAILED,EXPIRED',
    filename VARCHAR(255) NULL COMMENT '下载文件名；成功后有值',
    content_type VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '文件MIME类型',
    file_size BIGINT UNSIGNED NULL COMMENT '文件字节数',
    object_path VARCHAR(512) COLLATE utf8mb4_bin NULL COMMENT '对象存储路径bucket/objectName；过期删除后清空',
    failure_reason VARCHAR(255) NULL COMMENT '可展示的失败原因，不包含异常堆栈及敏感原值',
    created_at DATETIME(6) NOT NULL COMMENT '记录创建时间点，UTC',
    started_at DATETIME(6) NULL COMMENT '开始生成时间点，UTC',
    completed_at DATETIME(6) NULL COMMENT '任务完成时间点，UTC；未完成时为空',
    expires_at DATETIME(6) NOT NULL COMMENT '文件到期时间点，UTC',
    PRIMARY KEY (id),
    KEY idx_iam_download_task_owner (tenant_id, actor_member_id, created_at),
    KEY idx_iam_download_task_status (status, expires_at),
    CONSTRAINT ck_iam_download_task_domain CHECK (domain IN ('PLATFORM', 'TENANT')),
    CONSTRAINT ck_iam_download_task_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'EXPIRED')),
    CONSTRAINT ck_iam_download_task_tenant CHECK ((domain = 'TENANT' AND tenant_id IS NOT NULL) OR (domain = 'PLATFORM' AND tenant_id IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='下载中心导出任务';

CREATE TABLE iam_authorization_audit (
    id BIGINT UNSIGNED NOT NULL COMMENT '授权与组织操作脱敏审计及投递状态记录标识',
    event_id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '业务事件幂等标识，用于防止重复投递或重复保存',
    actor_account_id BIGINT UNSIGNED NOT NULL COMMENT '发起操作的全局账号ID',
    actor_member_id BIGINT UNSIGNED NOT NULL COMMENT '发起操作的当前域成员ID',
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '授权管理域：PLATFORM平台、TENANT租户',
    tenant_id BIGINT UNSIGNED NULL COMMENT '审计所属租户ID；平台域为空，租户域必填',
    target_type VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '审计或迁移目标的业务类型',
    target_id VARCHAR(128) COLLATE utf8mb4_bin NOT NULL COMMENT '目标业务标识，具体类型由target_type或entity_type解释',
    change_type VARCHAR(64) COLLATE utf8mb4_bin NOT NULL COMMENT '审计变更类型，对应AuditChangeType稳定字面量',
    safe_before JSON NOT NULL COMMENT '变更前脱敏摘要JSON对象，不保存凭证或敏感原值',
    safe_after JSON NOT NULL COMMENT '变更后脱敏摘要JSON对象，不保存凭证或敏感原值',
    revisions JSON NOT NULL COMMENT '本次变更涉及的对象版本JSON对象',
    delegation_id BIGINT UNSIGNED NULL COMMENT '委派标识；引用归属由本表外键及业务事务共同校验',
    assignment_id BIGINT UNSIGNED NULL COMMENT '角色分配标识；引用归属由本表外键及业务事务共同校验',
    trace_id VARCHAR(128) NULL COMMENT '请求链路追踪标识，可空',
    occurred_at DATETIME(6) NOT NULL COMMENT '业务发生时间点，UTC',
    delivered_at DATETIME(6) NULL COMMENT '审计投递完成时间点，UTC；NULL表示尚未确认投递',
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_audit_event (event_id),
    KEY idx_iam_audit_tenant_time (tenant_id, occurred_at, id),
    KEY idx_iam_audit_tenant_actor_time (tenant_id, actor_member_id, occurred_at, id),
    KEY idx_iam_audit_tenant_target_time (tenant_id, target_type, target_id, occurred_at, id),
    KEY idx_iam_audit_actor (actor_account_id, occurred_at, id),
    KEY idx_iam_audit_assignment_create (assignment_id, change_type, occurred_at, id),
    KEY idx_iam_audit_delivery (delivered_at, id),
    CONSTRAINT ck_iam_audit_context CHECK ((domain = 'PLATFORM' AND tenant_id IS NULL) OR (domain = 'TENANT' AND tenant_id IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='授权与组织操作脱敏审计及投递状态';

-- 持久角色版本应用：平台域，明细不展开组成员。
CREATE TABLE IF NOT EXISTS iam_role_application_task (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT '平台角色固定版本批量应用任务记录标识',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色标识；引用归属由本表外键及业务事务共同校验',
    target_revision_id BIGINT UNSIGNED NOT NULL COMMENT '目标角色固定版本标识；引用归属由本表外键及业务事务共同校验',
    actor_account_id BIGINT UNSIGNED NOT NULL COMMENT '发起操作的全局账号ID',
    actor_member_id BIGINT UNSIGNED NOT NULL COMMENT '发起操作的当前域成员ID',
    created_by BIGINT UNSIGNED NOT NULL COMMENT '创建操作人标识',
    selection VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '版本应用选择方式：ALL/SOURCE_REVISIONS/ASSIGNMENTS；取值ALL,SOURCE_REVISIONS,ASSIGNMENTS',
    selection_snapshot JSON NOT NULL COMMENT '提交时冻结的分配选择条件JSON对象',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值PREPARING,READY,BLOCKED,RUNNING,PAUSED,SUCCEEDED',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
    prepare_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '同角色任务准备幂等键',
    confirm_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '任务确认幂等键；未确认时为空',
    lease_owner VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '领取任务的节点标识；无租约时为空',
    lease_until DATETIME(6) NULL COMMENT '任务租约失效时间点，UTC；无租约时为空',
    failure_reason VARCHAR(512) NULL COMMENT '任务失败或阻塞的安全原因，不包含凭证及敏感原值',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录创建时间点，UTC',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '记录最后更新时间点，UTC',
    running_role_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN status='RUNNING' THEN role_id ELSE NULL END) STORED COMMENT '自动生成的运行角色键：RUNNING时取role_id，否则NULL；唯一索引限制同角色一个运行任务',
    UNIQUE KEY uk_iam_role_application_prepare(role_id,prepare_key),
    UNIQUE KEY uk_iam_role_application_running(running_role_id),
    KEY ix_iam_role_application_role(role_id,id),
    KEY ix_iam_role_application_claim(status,lease_until,id),
    CONSTRAINT fk_iam_role_application_role FOREIGN KEY(role_id) REFERENCES iam_role_definition(id),
    CONSTRAINT fk_iam_role_application_target FOREIGN KEY(target_revision_id) REFERENCES iam_role_revision(id),
    CONSTRAINT ck_iam_role_application_status CHECK(status IN ('PREPARING','READY','BLOCKED','RUNNING','PAUSED','SUCCEEDED')),
    CONSTRAINT ck_iam_role_application_selection CHECK(selection IN ('ALL','SOURCE_REVISIONS','ASSIGNMENTS'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台角色固定版本批量应用任务';

CREATE TABLE IF NOT EXISTS iam_role_application_item (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT '角色版本应用的分配明细记录标识',
    task_id BIGINT UNSIGNED NOT NULL COMMENT '版本应用任务标识；引用归属由本表外键及业务事务共同校验',
    assignment_id BIGINT UNSIGNED NOT NULL COMMENT '角色分配标识；引用归属由本表外键及业务事务共同校验',
    original_snapshot JSON NOT NULL COMMENT '被选分配的原始快照JSON对象，用于预览与执行重验',
    expected_assignment_version BIGINT UNSIGNED NOT NULL COMMENT '执行前要求匹配的分配乐观锁版本',
    configured_bindings JSON NOT NULL COMMENT '任务内明确配置的范围参数JSON对象',
    preview_snapshot JSON NULL COMMENT '最近一次预览结果JSON快照；未预览时为空',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '当前业务记录状态，合法值由本表CHECK及对应业务枚举限定；取值PENDING,BLOCKED,SUCCEEDED,EXCLUDED,REVOKED,EXPIRED,ALREADY_TARGET',
    failure_reason VARCHAR(512) NULL COMMENT '任务失败或阻塞的安全原因，不包含凭证及敏感原值',
    UNIQUE KEY uk_iam_role_application_assignment(task_id,assignment_id),
    KEY ix_iam_role_application_items(task_id,status,id),
    CONSTRAINT fk_iam_role_application_item_task FOREIGN KEY(task_id) REFERENCES iam_role_application_task(id),
    CONSTRAINT fk_iam_role_application_item_assignment FOREIGN KEY(assignment_id) REFERENCES iam_role_assignment(id),
    CONSTRAINT ck_iam_role_application_item_status CHECK(status IN ('PENDING','BLOCKED','SUCCEEDED','EXCLUDED','REVOKED','EXPIRED','ALREADY_TARGET'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色版本应用的分配明细';

-- ====================================================================
-- Source: databases/iam/005_auxiliary.sql
-- ====================================================================
-- 业务时间点统一 UTC；初始化连接也必须明确会话时区。
SET time_zone = '+00:00';

-- IAM 自有辅助结构：多作用域字典、社交渠道配置和账号社交绑定。
-- 6张共享框架表由所属library的sql/schema.sql维护，manifest统一投影到完整初始化。
-- 完整部署只执行databases/ingot_iam.sql；依赖与脚本对应关系见databases/README.md。

CREATE TABLE `iam_dictionary` (
  `id` bigint NOT NULL COMMENT '多作用域字典及字典项记录标识',
  `pid` bigint NOT NULL DEFAULT '0' COMMENT '父ID',
  `code` varchar(64) NOT NULL COMMENT '编码',
  `name` varchar(128) NOT NULL COMMENT '名称',
  `value` varchar(128) DEFAULT NULL COMMENT '字典项值（仅字典项有效）',
  `label` varchar(128) DEFAULT NULL COMMENT '字典项展示文本（仅字典项有效）',
  `type` char(1) NOT NULL COMMENT '字典节点类别：0字典类型、1字典项',
  `scope_type` char(1) NOT NULL DEFAULT '0' COMMENT '作用域, 0:平台,1:租户,2:应用',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID（scope_type=1时必填）',
  `app_id` bigint DEFAULT NULL COMMENT '作用域为应用时的IAM应用目录ID；其他作用域可空',
  `org_type` char(1) NOT NULL DEFAULT '0' COMMENT '组织类型：0平台、1组织',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序权重',
  `system_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否内置字典',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `extra` json DEFAULT NULL COMMENT '扩展属性',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime DEFAULT NULL COMMENT '记录创建时间点，UTC',
  `updated_at` datetime DEFAULT NULL COMMENT '记录最后更新时间点，UTC',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间点，UTC；NULL表示未删除',
  PRIMARY KEY (`id`),
  KEY `idx_dict_pid` (`pid`) USING BTREE,
  KEY `idx_dict_code` (`code`) USING BTREE,
  KEY `idx_dict_type_status` (`type`,`status`) USING BTREE,
  KEY `idx_dict_scope` (`scope_type`,`tenant_id`,`app_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='多作用域字典及字典项';

CREATE TABLE `iam_social_config` (
  `id` bigint unsigned NOT NULL COMMENT '社交登录渠道配置记录标识',
  `tenant_id` bigint unsigned NOT NULL COMMENT '租户ID',
  `app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '第三方平台分配的客户端App ID，不是IAM应用目录数字ID',
  `app_secret` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '第三方应用密钥，可空；禁止在审计及字典示例中泄露',
  `redirect_url` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '重定向地址',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '社交名称',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '第三方登录渠道类型，对应社交登录渠道配置',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '记录创建时间点，UTC',
  `updated_at` datetime DEFAULT NULL COMMENT '记录最后更新时间点，UTC',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间点，UTC；NULL表示未删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='社交登录渠道配置';

CREATE TABLE `iam_account_social_binding` (
  `id` bigint NOT NULL COMMENT '全局账号社交身份绑定记录标识',
  `tenant_id` bigint NOT NULL COMMENT '组织ID',
  `user_id` bigint NOT NULL COMMENT '被绑定的全局Account标识，旧列名保留；不表示租户成员ID',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道类型',
  `unique_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道唯一ID',
  `bind_at` datetime NOT NULL COMMENT '社交身份绑定时间点，UTC',
  PRIMARY KEY (`id`),
  KEY `idx_unique_type_user` (`unique_id`,`type`,`user_id`) USING BTREE COMMENT '渠道用户索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='全局账号社交身份绑定';

-- ====================================================================
-- Source: ingot-framework/ingot-security/ingot-security-account/ingot-security-account-adapter/src/main/resources/sql/ingot-security-account-adapter/schema.sql
-- ====================================================================
CREATE TABLE IF NOT EXISTS fw_security_account_lock_state (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '安全框架账号锁定状态记录标识；数据库自增',
  user_id         BIGINT       NOT NULL COMMENT '被锁定的安全用户ID，与user_type共同唯一标识用户',
  user_type       VARCHAR(20)  NOT NULL DEFAULT '0' COMMENT '安全用户类型，对应UserTypeEnum：0系统账号、1会员账号',

  -- 锁定状态
  locked          TINYINT(1)   NOT NULL DEFAULT 0     COMMENT '是否锁定（0-否 1-是）',
  lock_type       VARCHAR(20)  DEFAULT NULL            COMMENT '锁定类型（MANUAL-手动 AUTO-自动）',
  lock_reason_code   VARCHAR(50)  DEFAULT NULL         COMMENT '锁定原因代码',
  lock_reason_detail VARCHAR(500) DEFAULT NULL         COMMENT '锁定原因详情',

  -- 锁定时间信息
  locked_at       DATETIME     DEFAULT NULL COMMENT '锁定时间；时间点按UTC保存',
  locked_until    DATETIME     DEFAULT NULL COMMENT '锁定到期时间（NULL=永久锁定）；时间点按UTC保存',

  -- 操作信息（手动锁定时填写）
  operator_id     BIGINT       DEFAULT NULL            COMMENT '操作人ID',
  operator_name   VARCHAR(64)  DEFAULT NULL            COMMENT '操作人姓名',

  -- 登录失败计数
  failed_login_count INT        NOT NULL DEFAULT 0    COMMENT '连续登录失败次数',
  last_failed_at  DATETIME     DEFAULT NULL COMMENT '最后一次失败时间；时间点按UTC保存',

  -- 时间戳
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间点，UTC',
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间点，UTC',

  PRIMARY KEY (id),
  UNIQUE KEY uk_user_id_type (user_id, user_type)       COMMENT '用户ID + 用户类型联合唯一',
  KEY idx_locked (locked, locked_until) USING BTREE     COMMENT '锁定状态 + 到期时间索引（自动解锁任务使用）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号锁定状态表；所属模块：ingot-security-account-adapter';

-- ====================================================================
-- Source: ingot-framework/ingot-security/ingot-security-credential-data/src/main/resources/sql/ingot-security-credential-data/schema.sql
-- ====================================================================
CREATE TABLE IF NOT EXISTS `fw_security_password_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '安全框架密码历史环形缓冲记录标识；数据库自增',
  `user_id` BIGINT NOT NULL COMMENT '安全框架全局用户ID；同用户按sequence_number维护环形历史',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '不可逆密码哈希，禁止作为响应明文返回',
  `sequence_number` INT NOT NULL COMMENT '序号（用于环形缓冲，从1开始）',
  `version` bigint NOT NULL DEFAULT '1' COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间点，UTC',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间点，UTC',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_sequence` (`user_id`, `sequence_number`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密码历史记录（环形缓冲）；所属模块：ingot-security-credential-data';

CREATE TABLE IF NOT EXISTS `fw_security_password_expiration` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '安全框架密码过期状态记录标识；数据库自增',
  `user_id` BIGINT NOT NULL COMMENT '安全框架全局用户ID，唯一对应一份密码过期状态',
  `last_changed_at` DATETIME NOT NULL COMMENT '最后修改密码时间；时间点按UTC保存',
  `expires_at` DATETIME NOT NULL COMMENT '到期时间点，UTC',
  `force_change` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否强制修改（0-否 1-是）',
  `grace_login_remaining` INT NOT NULL DEFAULT 0 COMMENT '剩余宽限登录次数',
  `next_warning_at` DATETIME NULL COMMENT '下次提醒时间；时间点按UTC保存',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间点，UTC',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间点，UTC',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_expires_at` (`expires_at`),
  KEY `idx_next_warning_at` (`next_warning_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密码过期信息；所属模块：ingot-security-credential-data';

-- ====================================================================
-- Source: ingot-framework/ingot-security/ingot-security-event-store-mysql/src/main/resources/sql/ingot-security-event-store-mysql/schema.sql
-- ====================================================================
CREATE TABLE IF NOT EXISTS `fw_security_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '统一安全事件持久记录记录标识；数据库自增',
  `event_id` varchar(32) DEFAULT NULL COMMENT 'producer 幂等 ID',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `event_category` varchar(20) NOT NULL COMMENT 'AUTH/ACCOUNT/CREDENTIAL/ACCESS',
  `priority` varchar(16) NOT NULL DEFAULT 'BEST_EFFORT' COMMENT 'BEST_EFFORT/DURABLE',
  `occurred_at` datetime DEFAULT NULL COMMENT '业务发生时间点，UTC',
  `received_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '服务端接收安全事件的时间点，UTC',
  `tenant_id` bigint DEFAULT NULL COMMENT '事件所属租户ID；NULL表示平台事件或未提供租户上下文',
  `user_id` bigint DEFAULT NULL COMMENT '安全事件关联的用户标识，结合user_type解释；可空',
  `user_type` varchar(20) DEFAULT NULL COMMENT '安全用户类型，对应UserTypeEnum：0系统账号、1会员账号',
  `account` varchar(128) DEFAULT NULL COMMENT '事件涉及的账号显示标识，可空',
  `client_id` varchar(64) DEFAULT NULL COMMENT '认证客户端标识，可空',
  `app_id` varchar(64) DEFAULT NULL COMMENT '上报方提供的应用标识，可空',
  `session_id` varchar(64) DEFAULT NULL COMMENT '认证会话标识，可空',
  `device_id` varchar(128) DEFAULT NULL COMMENT '设备标识，可空',
  `client_ip` varchar(64) DEFAULT NULL COMMENT '客户端IP地址，可空',
  `request_uri` varchar(512) DEFAULT NULL COMMENT '触发事件的请求路径，可空',
  `user_agent` varchar(512) DEFAULT NULL COMMENT '客户端User-Agent，可空',
  `result` varchar(20) DEFAULT NULL COMMENT '安全操作结果稳定标识，可空',
  `reason_code` varchar(50) DEFAULT NULL COMMENT '安全事件原因代码，可空',
  `reason_detail` varchar(500) DEFAULT NULL COMMENT '安全事件原因说明，不保存凭证原文',
  `source_module` varchar(64) NOT NULL COMMENT '上报模块',
  `source` varchar(50) DEFAULT NULL COMMENT '安全事件来源渠道或触发来源，可空',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人标识，可空',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人姓名快照，可空',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '请求链路追踪标识，可空',
  `extension` json DEFAULT NULL COMMENT '安全事件扩展JSON，须遵守脱敏白名单',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_received_id` (`received_at`,`id`),
  KEY `idx_event_type` (`event_type`,`received_at`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_trace` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一安全事件（canonical）；所属模块：ingot-security-event-store-mysql';

-- ====================================================================
-- Source: ingot-framework/ingot-id/src/main/resources/sql/ingot-id/schema.sql
-- ====================================================================
CREATE TABLE IF NOT EXISTS `fw_id_leaf_alloc` (
  `biz_tag` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT 'Leaf业务号段名称，不同业务独立分配',
  `max_id` bigint NOT NULL DEFAULT '1' COMMENT '已分配号段的最大ID高水位，更新必须保持发号原子性',
  `step` int NOT NULL COMMENT '每次申请的号段长度',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '业务说明，可空',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '号段高水位最近更新时间点，UTC',
  PRIMARY KEY (`biz_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Leaf业务号段分配高水位；所属模块：ingot-id';

-- ====================================================================
-- Source: ingot-framework/ingot-oss/ingot-oss-common/src/main/resources/sql/ingot-oss-common/schema.sql
-- ====================================================================
CREATE TABLE IF NOT EXISTS fw_oss_asset_upload (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '一次性上传凭证 ID',
    owner_key VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '服务端恢复的域、组织和操作者',
    purpose VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '固定业务用途编码',
    target_key VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '既有目标 ID 或空字符串',
    bucket_name VARCHAR(63) NOT NULL COMMENT '服务端选定的私有 bucket',
    object_name VARCHAR(255) NOT NULL COMMENT '服务端生成的对象名',
    state VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '暂存状态，消费成功随业务事务移除',
    expires_at DATETIME(6) NOT NULL COMMENT '凭证到期时间，UTC',
    PRIMARY KEY (id),
    KEY idx_asset_upload_expires (expires_at, id),
    CONSTRAINT ck_asset_upload_state CHECK (state IN ('PENDING','READY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务图片暂存与一次性绑定凭证；所属模块：ingot-oss-common';

-- ====================================================================
-- Source: databases/iam/006_bootstrap.sql
-- ====================================================================
-- 业务时间点统一 UTC；初始化连接也必须明确会话时区。
SET time_zone = '+00:00';

-- IAM 正式冷启动种子：先按顺序执行 001–005 再执行本文件，可重复执行。
-- 由 tools/iam/generate_bootstrap.py 从IAM契约及现有开发者目录生成，不要手工编辑。
-- 全部语句存在即跳过，不覆盖任何人工或业务修改；不含账号与凭证，
-- 新建行的保留号若已被占用，则落到所属区间内下一个空号。
-- 平台首个账号由 provider 冷启动器经安全框架注册用例创建。

-- 发号准备：静态保留标识全部小于起点，运行时发号不会与种子冲突。
INSERT INTO fw_id_leaf_alloc (biz_tag, max_id, step, description)
SELECT 'iam', 1000000, 100, 'IAM 冷启动发号' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM fw_id_leaf_alloc WHERE biz_tag = 'iam');

-- 应用：租户域基础应用在组织创建时缺省开通。
INSERT INTO iam_application (id, code, domain, name, icon, description, sort_order, baseline, enabled)
SELECT COALESCE((SELECT 100001 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_application taken WHERE taken.id = 100001)), (SELECT COALESCE(MAX(taken.id), 100000) + 1 FROM iam_application taken WHERE taken.id >= 100000 AND taken.id < 110000)), 'iam-platform', 'PLATFORM', '平台治理', 'ep:monitor', '平台域身份、目录与授权治理', 1, FALSE, TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_application WHERE code = 'iam-platform');

INSERT INTO iam_application (id, code, domain, name, icon, description, sort_order, baseline, enabled)
SELECT COALESCE((SELECT 100002 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_application taken WHERE taken.id = 100002)), (SELECT COALESCE(MAX(taken.id), 100000) + 1 FROM iam_application taken WHERE taken.id >= 100000 AND taken.id < 110000)), 'iam-tenant', 'TENANT', '组织治理', 'ep:office-building', '组织域成员、部门与授权治理', 1, TRUE, TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_application WHERE code = 'iam-tenant');

INSERT INTO iam_application (id, code, domain, name, icon, description, sort_order, baseline, enabled)
SELECT COALESCE((SELECT 100003 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_application taken WHERE taken.id = 100003)), (SELECT COALESCE(MAX(taken.id), 100000) + 1 FROM iam_application taken WHERE taken.id >= 100000 AND taken.id < 110000)), 'platform-developer', 'PLATFORM', '开发者平台', 'ep:cpu', '二维码、OAuth2客户端、社交配置与业务ID管理', 2, FALSE, TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_application WHERE code = 'platform-developer');

INSERT INTO iam_application (id, code, domain, name, icon, description, sort_order, baseline, enabled)
SELECT COALESCE((SELECT 100004 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_application taken WHERE taken.id = 100004)), (SELECT COALESCE(MAX(taken.id), 100000) + 1 FROM iam_application taken WHERE taken.id >= 100000 AND taken.id < 110000)), 'platform-security', 'PLATFORM', '安全中心', 'ep:lock', '凭证、账号、访问防护及会话安全管理', 3, FALSE, TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_application WHERE code = 'platform-security');

INSERT INTO iam_application (id, code, domain, name, icon, description, sort_order, baseline, enabled)
SELECT COALESCE((SELECT 100005 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_application taken WHERE taken.id = 100005)), (SELECT COALESCE(MAX(taken.id), 100000) + 1 FROM iam_application taken WHERE taken.id >= 100000 AND taken.id < 110000)), 'platform-member', 'PLATFORM', 'App 用户管理', 'ep:iphone', '管理 App 用户、角色及权限', 4, FALSE, TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_application WHERE code = 'platform-member');

-- 资源：声明合法范围与字段能力，供角色发布与字段策略校验取值。
INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110001 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110001)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'account', '全局账号', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'account');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110002 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110002)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'action', '应用操作', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'action');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110003 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110003)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'application', '应用目录', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'application');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110004 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110004)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'application-entitlement', '租户应用开通', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'application-entitlement');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110005 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110005)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'authorization-audit', '平台授权审计', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'authorization-audit');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110006 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110006)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'authorization-diagnosis', '平台权限诊断', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'authorization-diagnosis');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110007 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110007)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'delegation', '平台授权委派', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'delegation');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110008 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110008)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'dictionary', '字典', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'dictionary');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110009 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110009)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'group', '平台用户组', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'group');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110010 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110010)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'member', '平台成员', CAST('["ALL", "SELF", "OBJECT_SET"]' AS JSON), CAST('[{"key": "displayName", "label": "显示名", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "mask": {"kind": "ALL"}}, {"key": "avatar", "label": "头像", "visibilities": ["HIDDEN", "FULL"], "editable": true, "filterable": false}, {"key": "phone", "label": "手机号", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": false, "mask": {"kind": "PHONE"}}, {"key": "email", "label": "邮箱", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": false, "mask": {"kind": "EMAIL"}}, {"key": "joinedAt", "label": "加入时间", "visibilities": ["HIDDEN", "FULL"], "editable": false, "filterable": false}, {"key": "lastLoginAt", "label": "最后登录", "visibilities": ["HIDDEN", "FULL"], "editable": false, "filterable": false}, {"key": "updatedAt", "label": "更新时间", "visibilities": ["HIDDEN", "FULL"], "editable": false, "filterable": false}]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'member');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110011 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110011)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'menu', '应用菜单', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'menu');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110012 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110012)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'plan', '租户套餐', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'plan');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110013 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110013)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'resource', '应用资源', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'resource');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110014 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110014)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'role', '平台角色', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'role');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110015 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110015)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'role-assignment', '平台角色分配', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'role-assignment');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110016 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110016)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'shared-role', '共享角色', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'shared-role');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110017 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110017)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'tenant', '租户', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'tenant');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110018 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110018)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'application', '已开通应用', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'application');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110019 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110019)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'application-audience', '应用可用人群', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'application-audience');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110020 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110020)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'authorization-audit', '组织授权审计', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'authorization-audit');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110021 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110021)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'authorization-diagnosis', '组织权限诊断', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'authorization-diagnosis');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110022 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110022)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'delegation', '组织授权委派', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'delegation');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110023 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110023)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'department', '部门', CAST('["ALL", "MANAGED_DEPARTMENTS", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'department');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110024 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110024)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'field-policy', '资源字段策略', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'field-policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110025 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110025)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'group', '组织用户组', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'group');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110026 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110026)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'member', '组织成员', CAST('["ALL", "SELF", "MEMBER_DEPARTMENTS", "MANAGED_DEPARTMENTS", "OBJECT_SET"]' AS JSON), CAST('[{"key": "displayName", "label": "显示名", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "mask": {"kind": "ALL"}}, {"key": "avatar", "label": "头像", "visibilities": ["HIDDEN", "FULL"], "editable": true, "filterable": false}, {"key": "phone", "label": "手机号", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "mask": {"kind": "PHONE"}}, {"key": "email", "label": "邮箱", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "mask": {"kind": "EMAIL"}}]' AS JSON), TRUE, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'member');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110027 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110027)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'role', '组织角色', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'role');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110028 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110028)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'role-assignment', '组织角色分配', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'role-assignment');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110029 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110029)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'settings', '组织设置', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'settings');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110030 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110030)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'workbench', '组织工作台', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'workbench');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110031 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110031)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'id-allocation', '业务 ID 发号', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'platform-developer' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'id-allocation');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110032 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110032)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'oauth-client', 'OAuth 2.0 客户端', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'platform-developer' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'oauth-client');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110033 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110033)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'qr-code', '二维码', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'platform-developer' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'qr-code');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110034 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110034)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'social-config', '社会化登录配置', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'platform-developer' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'social-config');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110035 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110035)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'access-policy', '访问防护策略', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'platform-security' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'access-policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110036 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110036)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'account-lockout-policy', '账号锁定策略', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'platform-security' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'account-lockout-policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110037 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110037)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'block-event', '历史封禁记录', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'platform-security' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'block-event');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110038 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110038)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'credential-policy', '凭证策略', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'platform-security' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'credential-policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110039 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110039)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'login-failure-policy', '登录失败防护', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'platform-security' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'login-failure-policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110040 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110040)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'session', '在线会话', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'platform-security' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'session');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110041 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110041)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'session-concurrency-policy', '会话并发策略', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'platform-security' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'session-concurrency-policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110042 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110042)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'permission', 'App 权限', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'platform-member' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'permission');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110043 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110043)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'role', 'App 角色', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'platform-member' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'role');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, field_policy, enabled)
SELECT COALESCE((SELECT 110044 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110044)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'user', 'App 用户', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), FALSE, TRUE FROM iam_application app
WHERE app.code = 'platform-member' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'user');

-- 操作：精确操作码全局唯一，禁止通配。
INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120001 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120001)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:create', '创建全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120002 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120002)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:delete', '删除全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120003 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120003)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:disable', '停用全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:disable');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120004 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120004)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:enable', '启用全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:enable');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120005 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120005)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:lock', '锁定全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:lock');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120006 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120006)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:lookup', '查找全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:lookup');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120007 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120007)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:read', '查看全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120008 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120008)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:reset-password', '重置全局账号密码', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:reset-password');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120009 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120009)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:unlock', '解锁全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:unlock');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120010 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120010)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:update', '编辑全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120011 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120011)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:action:change-status', '启停应用操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:change-status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120011 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120011)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:action:create', '创建应用操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120012 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120012)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:action:delete', '删除应用操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120013 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120013)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:action:read', '查看应用操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120015 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120015)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:action:update', '编辑应用操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120016 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120016)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application:change-status', '启停应用目录', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:change-status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120016 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120016)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application:create', '创建应用目录', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120017 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120017)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application:delete', '删除应用目录', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120018 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120018)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application:purge', '强制清除应用目录', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:purge');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120019 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120019)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application:read', '查看应用目录', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120021 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120021)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application:update', '编辑应用目录', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120022 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120022)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application-entitlement:preview', '预览租户应用开通', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application-entitlement' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application-entitlement:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120023 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120023)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application-entitlement:read', '查看租户应用开通', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application-entitlement' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application-entitlement:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120024 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120024)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application-entitlement:update', '编辑租户应用开通', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application-entitlement' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application-entitlement:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120025 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120025)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:authorization-audit:read', '查看平台授权审计', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'authorization-audit' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:authorization-audit:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120026 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120026)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:authorization-diagnosis:run', '运行平台权限诊断', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'authorization-diagnosis' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:authorization-diagnosis:run');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120029 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120029)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:delegation:create', '创建平台授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:delegation:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120031 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120031)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:delegation:preview', '预览平台授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:delegation:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120032 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120032)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:delegation:read', '查看平台授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:delegation:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120030 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120030)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:delegation:revoke', '撤销', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:delegation:revoke');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120033 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120033)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:delegation:update', '编辑平台授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:delegation:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120034 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120034)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:dictionary:create', '创建字典', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'dictionary' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:dictionary:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120035 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120035)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:dictionary:delete', '删除字典', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'dictionary' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:dictionary:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120036 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120036)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:dictionary:read', '查看字典', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'dictionary' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:dictionary:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120037 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120037)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:dictionary:update', '编辑字典', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'dictionary' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:dictionary:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120041 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120041)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:group:create', '创建平台用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120042 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120042)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:group:delete', '删除平台用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120043 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120043)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:group:preview', '预览平台用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120044 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120044)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:group:read', '查看平台用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120045 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120045)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:group:update', '编辑平台用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120041 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120041)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:member:change-status', '启停平台成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:change-status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120046 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120046)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:member:create', '创建平台成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120047 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120047)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:member:read', '查看平台成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120048 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120048)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:member:remove', '移出平台成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:remove');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120050 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120050)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:member:update', '编辑平台成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120051 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120051)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:menu:create', '创建应用菜单', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'menu' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:menu:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120052 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120052)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:menu:delete', '删除应用菜单', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'menu' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:menu:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120053 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120053)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:menu:read', '查看应用菜单', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'menu' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:menu:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120054 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120054)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:menu:update', '编辑应用菜单', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'menu' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:menu:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120055 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120055)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:plan:create', '创建租户套餐', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'plan' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:plan:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120056 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120056)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:plan:read', '查看租户套餐', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'plan' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:plan:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120057 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120057)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:plan:update', '编辑租户套餐', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'plan' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:plan:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120058 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120058)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:resource:create', '创建应用资源', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'resource' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:resource:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120059 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120059)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:resource:delete', '删除应用资源', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'resource' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:resource:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120060 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120060)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:resource:read', '查看应用资源', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'resource' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:resource:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120061 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120061)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:resource:update', '编辑应用资源', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'resource' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:resource:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120057 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120057)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role:change-status', '启停平台角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:change-status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120062 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120062)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role:create', '创建平台角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120063 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120063)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role:delete', '删除平台角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120064 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120064)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role:preview', '预览平台角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120061 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120061)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role:publish-version', '发布平台角色版本', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:publish-version');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120066 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120066)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role:read', '查看平台角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120063 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120063)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role-assignment:apply-version', '应用角色版本', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role-assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role-assignment:apply-version');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120064 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120064)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role-assignment:create', '创建平台角色分配', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role-assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role-assignment:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120065 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120065)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role-assignment:read', '查看平台角色分配', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role-assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role-assignment:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120066 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120066)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role-assignment:revoke', '撤销', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role-assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role-assignment:revoke');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120067 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120067)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role-assignment:update', '编辑平台角色分配', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role-assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role-assignment:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120068 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120068)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:shared-role:change-status', '启停共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:change-status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120068 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120068)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:shared-role:create', '创建共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120069 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120069)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:shared-role:delete', '删除共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120070 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120070)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:shared-role:preview', '预览共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120072 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120072)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:shared-role:read', '查看共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120071 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120071)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:shared-role:update', '编辑共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120074 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120074)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:tenant:create', '创建租户', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'tenant' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:tenant:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120075 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120075)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:tenant:preview', '预览租户', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'tenant' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:tenant:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120076 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120076)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:tenant:read', '查看租户', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'tenant' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:tenant:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120077 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120077)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:tenant:update', '编辑租户', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'tenant' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:tenant:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120078 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120078)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:application:read', '查看已开通应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:application:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120079 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120079)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:application-audience:read', '查看应用可用人群', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'application-audience' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:application-audience:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120080 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120080)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:application-audience:update', '编辑应用可用人群', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'application-audience' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:application-audience:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120081 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120081)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:authorization-audit:read', '查看组织授权审计', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'authorization-audit' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:authorization-audit:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120082 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120082)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:authorization-diagnosis:run', '运行组织权限诊断', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'authorization-diagnosis' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:authorization-diagnosis:run');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120087 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120087)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:delegation:create', '创建组织授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120089 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120089)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:delegation:preview', '预览组织授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120090 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120090)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:delegation:read', '查看组织授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120086 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120086)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:delegation:revoke', '撤销', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:revoke');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120091 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120091)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:delegation:update', '编辑组织授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120092 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120092)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:department:create', '创建部门', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120093 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120093)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:department:delete', '删除部门', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 121200 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 121200)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:department:export', '导出部门', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:export');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120094 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120094)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:department:read', '查看部门', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120999 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120999)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:department:set-leaders', '设置部门负责人', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:set-leaders');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120095 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120095)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:department:update', '编辑部门', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120099 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120099)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:field-policy:read', '查看资源字段策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'field-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:field-policy:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120100 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120100)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:field-policy:update', '编辑资源字段策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'field-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:field-policy:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120101 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120101)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:group:create', '创建组织用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120102 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120102)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:group:delete', '删除组织用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120103 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120103)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:group:preview', '预览组织用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120104 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120104)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:group:read', '查看组织用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120105 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120105)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:group:update', '编辑组织用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120101 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120101)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:change-status', '启停组织成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:change-status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120106 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120106)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:create', '创建组织成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120108 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120108)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:export', '导出组织成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:export');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120109 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120109)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:read', '查看组织成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120110 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120110)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:remove', '移出组织成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:remove');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120106 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120106)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:set-departments', '调整任职', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:set-departments');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120112 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120112)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:update', '编辑组织成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120108 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120108)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:change-status', '启停组织角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:change-status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120114 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120114)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:create', '创建组织角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120115 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120115)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:delete', '删除组织角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120116 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120116)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:preview', '预览组织角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120118 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120118)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:read', '查看组织角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120117 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120117)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:update', '保存组织角色权限', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120114 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120114)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role-assignment:create', '创建组织角色分配', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role-assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role-assignment:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120115 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120115)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role-assignment:read', '查看组织角色分配', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role-assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role-assignment:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120116 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120116)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role-assignment:revoke', '撤销', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role-assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role-assignment:revoke');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120117 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120117)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role-assignment:update', '编辑组织角色分配', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role-assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role-assignment:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120122 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120122)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:settings:read', '查看组织设置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'settings' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:settings:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120119 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120119)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:settings:transfer-owner', '转交所有者', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'settings' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:settings:transfer-owner');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120123 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120123)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:settings:update', '编辑组织设置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'settings' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:settings:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120121 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120121)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:workbench:read', '查看组织工作台', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'workbench' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:workbench:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120122 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120122)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-developer:id-allocation:create', '创建业务 ID 发号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-developer' AND res.code = 'id-allocation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-developer:id-allocation:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120123 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120123)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-developer:id-allocation:delete', '删除业务 ID 发号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-developer' AND res.code = 'id-allocation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-developer:id-allocation:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120124 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120124)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-developer:id-allocation:read', '查看业务 ID 发号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-developer' AND res.code = 'id-allocation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-developer:id-allocation:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120125 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120125)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-developer:id-allocation:update', '编辑业务 ID 发号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-developer' AND res.code = 'id-allocation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-developer:id-allocation:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120126 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120126)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-developer:oauth-client:create', '创建客户端', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-developer' AND res.code = 'oauth-client' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-developer:oauth-client:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120127 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120127)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-developer:oauth-client:delete', '删除客户端', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-developer' AND res.code = 'oauth-client' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-developer:oauth-client:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120128 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120128)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-developer:oauth-client:read-detail', '查看客户端详情', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-developer' AND res.code = 'oauth-client' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-developer:oauth-client:read-detail');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120129 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120129)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-developer:oauth-client:read-list', '查看客户端', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-developer' AND res.code = 'oauth-client' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-developer:oauth-client:read-list');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120130 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120130)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-developer:oauth-client:reset-secret', '重置客户端密钥', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-developer' AND res.code = 'oauth-client' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-developer:oauth-client:reset-secret');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120131 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120131)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-developer:oauth-client:update', '编辑客户端', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-developer' AND res.code = 'oauth-client' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-developer:oauth-client:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120132 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120132)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-developer:qr-code:generate', '生成二维码', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-developer' AND res.code = 'qr-code' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-developer:qr-code:generate');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120133 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120133)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-developer:social-config:create', '创建社会化登录配置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-developer' AND res.code = 'social-config' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-developer:social-config:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120134 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120134)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-developer:social-config:delete', '删除社会化登录配置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-developer' AND res.code = 'social-config' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-developer:social-config:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120135 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120135)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-developer:social-config:read', '查看社会化登录配置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-developer' AND res.code = 'social-config' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-developer:social-config:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120136 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120136)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-developer:social-config:update', '编辑社会化登录配置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-developer' AND res.code = 'social-config' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-developer:social-config:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120137 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120137)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:access-policy:create', '创建访问防护策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'access-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:access-policy:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120138 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120138)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:access-policy:delete', '删除访问防护策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'access-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:access-policy:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120139 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120139)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:access-policy:read', '查看访问防护策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'access-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:access-policy:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120140 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120140)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:access-policy:refresh', '刷新访问防护策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'access-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:access-policy:refresh');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120141 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120141)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:access-policy:update', '编辑访问防护策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'access-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:access-policy:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120142 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120142)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:account-lockout-policy:read', '查看账号锁定策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'account-lockout-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:account-lockout-policy:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120143 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120143)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:account-lockout-policy:update', '编辑账号锁定策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'account-lockout-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:account-lockout-policy:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120144 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120144)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:block-event:read', '查看历史封禁记录', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'block-event' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:block-event:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120145 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120145)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:credential-policy:create', '创建凭证策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'credential-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:credential-policy:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120146 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120146)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:credential-policy:delete', '删除凭证策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'credential-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:credential-policy:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120147 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120147)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:credential-policy:read', '查看凭证策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'credential-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:credential-policy:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120148 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120148)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:credential-policy:update', '编辑凭证策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'credential-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:credential-policy:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120149 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120149)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:login-failure-policy:read', '查看登录失败防护', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'login-failure-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:login-failure-policy:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120150 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120150)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:login-failure-policy:update', '编辑登录失败防护', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'login-failure-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:login-failure-policy:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120151 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120151)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:session:read', '查看在线会话', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'session' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:session:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120152 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120152)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:session:revoke', '撤销', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'session' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:session:revoke');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120153 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120153)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:session-concurrency-policy:create', '创建会话并发策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'session-concurrency-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:session-concurrency-policy:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120154 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120154)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:session-concurrency-policy:delete', '删除会话并发策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'session-concurrency-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:session-concurrency-policy:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120155 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120155)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:session-concurrency-policy:read', '查看会话并发策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'session-concurrency-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:session-concurrency-policy:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120156 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120156)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-security:session-concurrency-policy:update', '编辑会话并发策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-security' AND res.code = 'session-concurrency-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-security:session-concurrency-policy:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120157 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120157)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:permission:change-status', '启停 App 权限', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'permission' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:permission:change-status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120158 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120158)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:permission:create', '创建 App 权限', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'permission' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:permission:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120159 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120159)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:permission:delete', '删除权限', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'permission' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:permission:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120160 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120160)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:permission:read', '查看权限', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'permission' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:permission:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120161 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120161)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:permission:update', '编辑 App 权限', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'permission' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:permission:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120162 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120162)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:role:assign-users', '分配角色用户', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:role:assign-users');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120163 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120163)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:role:change-status', '启停 App 角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:role:change-status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120164 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120164)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:role:create', '创建 App 角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:role:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120165 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120165)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:role:delete', '删除角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:role:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120166 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120166)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:role:read', '查看角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:role:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120167 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120167)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:role:read-permissions', '查看角色权限', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:role:read-permissions');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120168 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120168)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:role:set-permissions', '配置角色权限', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:role:set-permissions');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120169 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120169)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:role:update', '编辑 App 角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:role:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120170 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120170)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:user:create', '创建 App 用户', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:user:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120171 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120171)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:user:delete', '删除用户', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:user:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120172 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120172)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:user:disable', '停用 App 用户', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:user:disable');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120173 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120173)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:user:enable', '启用 App 用户', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:user:enable');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120174 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120174)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:user:lock', '锁定 App 用户', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:user:lock');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120175 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120175)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:user:read-detail', '查看用户详情', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:user:read-detail');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120176 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120176)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:user:read-list', '查看用户列表', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:user:read-list');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120177 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120177)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:user:read-roles', '查看用户角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:user:read-roles');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120178 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120178)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:user:reset-password', '重置用户密码', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:user:reset-password');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120179 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120179)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:user:set-roles', '配置用户角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:user:set-roles');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120180 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120180)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:user:unlock', '解锁 App 用户', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:user:unlock');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120181 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120181)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform-member:user:update', '编辑 App 用户', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform-member' AND res.code = 'user' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform-member:user:update');

-- 菜单：目录携带子页操作并集，子页全部不可见时目录随之隐藏。
INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130001 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130001)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '租户管理', '/platform/iam/tenant', 'layout.main', 'platform.iam.tenant', 'ep:office-building', 'DIRECTORY', 'ANY', 'ACTION', 1, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'platform.iam.tenant');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130002 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130002)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '租户列表', '/platform/iam/tenants', 'platform.iam.tenants', 'platform.iam.tenants', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.tenants');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130003 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130003)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '应用与配置', '/platform/iam/config', 'layout.main', 'platform.iam.config', 'ep:setting', 'DIRECTORY', 'ANY', 'ACTION', 2, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'platform.iam.config');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130004 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130004)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '应用目录', '/platform/iam/applications', 'platform.iam.applications', 'platform.iam.applications', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.config' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.applications');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130005 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130005)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '租户套餐', '/platform/iam/plans', 'platform.iam.plans', 'platform.iam.plans', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.config' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.plans');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130006 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130006)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '共享角色', '/platform/iam/shared/roles', 'platform.iam.shared.roles', 'platform.iam.shared.roles', 'PAGE', 'ANY', 'ACTION', 3, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.config' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.shared.roles');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130007 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130007)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '平台管理', '/platform/iam/manage', 'layout.main', 'platform.iam.manage', 'ep:monitor', 'DIRECTORY', 'ANY', 'ACTION', 3, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'platform.iam.manage');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130008 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130008)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '全局账号', '/platform/iam/accounts', 'platform.iam.accounts', 'platform.iam.accounts', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.manage' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.accounts');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130009 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130009)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '平台成员与用户组', '/platform/iam/personnel', 'platform.iam.personnel', 'platform.iam.personnel', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.manage' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.personnel');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130010 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130010)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '角色与授权', '/platform/iam/authorization', 'platform.iam.authorization', 'platform.iam.authorization', 'PAGE', 'ANY', 'ACTION', 3, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.manage' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.authorization');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130011 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130011)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '平台授权审计', '/platform/authorization-audit', 'platform.iam.authorization.audit', 'platform.iam.authorization.audit', 'PAGE', 'ANY', 'ACTION', 4, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.manage' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.authorization.audit');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130012 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130012)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '系统设置', '/platform/settings', 'layout.main', 'platform.settings', 'ep:setting', 'DIRECTORY', 'ANY', 'ACTION', 4, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'platform.settings');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130013 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130013)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '字典管理', '/platform/settings/dictionaries', 'platform.settings.dictionaries', 'platform.settings.dictionaries', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.settings' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.settings.dictionaries');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130014 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130014)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '组织管理', '/org/iam/organization', 'layout.main', 'org.iam.organization', 'ep:office-building', 'DIRECTORY', 'ANY', 'ACTION', 1, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'org.iam.organization');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130015 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130015)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '成员与部门', '/org/iam/members/root', 'layout.simple', 'org.iam.members.root', '', 'DIRECTORY', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.organization' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.members.root');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130016 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130016)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '成员', '/org/iam/members', 'org.iam.members', 'org.iam.members', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.members.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.members');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130017 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130017)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '部门', '/org/iam/departments', 'org.iam.departments', 'org.iam.departments', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.members.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.departments');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130018 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130018)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '用户组', '/org/iam/groups', 'org.iam.groups', 'org.iam.groups', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.organization' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.groups');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130019 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130019)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '组织设置', '/org/iam/settings', 'org.iam.settings', 'org.iam.settings', 'PAGE', 'ANY', 'ACTION', 3, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.organization' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.settings');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130020 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130020)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '权限与应用', '/org/iam/authorization/root', 'layout.main', 'org.iam.authorization.root', 'ep:connection', 'DIRECTORY', 'ANY', 'ACTION', 2, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'org.iam.authorization.root');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130021 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130021)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '角色与授权', '/org/iam/authorization', 'org.iam.authorization', 'org.iam.authorization', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.authorization.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.authorization');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130022 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130022)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '已开通应用', '/org/iam/applications', 'org.iam.applications', 'org.iam.applications', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.authorization.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.applications');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130023 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130023)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '安全与合规', '/org/iam/compliance', 'layout.main', 'org.iam.compliance', 'ep:lock', 'DIRECTORY', 'ANY', 'ACTION', 3, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'org.iam.compliance');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130024 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130024)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '资源字段策略', '/org/field-policies', 'org.iam.field.policies', 'org.iam.field.policies', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.compliance' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.field.policies');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130025 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130025)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '组织授权审计', '/org/authorization-audit', 'org.iam.authorization.audit', 'org.iam.authorization.audit', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.compliance' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.authorization.audit');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130026 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130026)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '工作台', '/org/iam/workspace', 'layout.main', 'org.iam.workspace', 'ep:grid', 'DIRECTORY', 'ANY', 'ACTION', 0, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'org.iam.workspace');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130027 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130027)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '组织概览', '/org/iam/workbench', 'org.iam.workbench', 'org.iam.workbench', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.workspace' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.workbench');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130028 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130028)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '开发者平台', '/platform/develop', 'layout.main', 'platform.develop', 'ep:cpu', 'DIRECTORY', 'ANY', 'ACTION', 1, TRUE FROM iam_application app
WHERE app.code = 'platform-developer' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'platform.develop');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130029 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130029)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '生成二维码', '/platform/develop/qrcode', 'platform.develop.qrcode', 'platform.develop.qrcode', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform-developer' AND parent.route_name = 'platform.develop' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.develop.qrcode');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130030 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130030)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, 'OAuth 2.0 客户端', '/platform/develop/client', 'platform.develop.client', 'platform.develop.client', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform-developer' AND parent.route_name = 'platform.develop' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.develop.client');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130031 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130031)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '社会化登录配置', '/platform/develop/social', 'platform.develop.social', 'platform.develop.social', 'PAGE', 'ANY', 'ACTION', 3, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform-developer' AND parent.route_name = 'platform.develop' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.develop.social');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130032 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130032)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '业务 ID 发号', '/platform/develop/id', 'platform.develop.id', 'platform.develop.id', 'PAGE', 'ANY', 'ACTION', 4, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform-developer' AND parent.route_name = 'platform.develop' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.develop.id');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130033 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130033)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '安全中心', '/security', 'layout.main', 'security.root', 'ep:lock', 'DIRECTORY', 'ANY', 'ACTION', 1, TRUE FROM iam_application app
WHERE app.code = 'platform-security' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'security.root');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130034 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130034)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '凭证策略', '/security/credential-policies', 'security.credential.policies', 'security.credential.policies', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform-security' AND parent.route_name = 'security.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'security.credential.policies');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130035 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130035)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '账号保护', '/security/account-protection', 'security.account.protection', 'security.account.protection', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform-security' AND parent.route_name = 'security.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'security.account.protection');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130036 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130036)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '访问防护', '/security/access-protection', 'security.access.protection', 'security.access.protection', 'PAGE', 'ANY', 'ACTION', 3, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform-security' AND parent.route_name = 'security.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'security.access.protection');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130037 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130037)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '会话管理', '/security/sessions', 'security.sessions', 'security.sessions', 'PAGE', 'ANY', 'ACTION', 4, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform-security' AND parent.route_name = 'security.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'security.sessions');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130038 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130038)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, 'App 用户管理', '/member', 'layout.main', 'member.root', 'ep:iphone', 'DIRECTORY', 'ANY', 'ACTION', 1, TRUE FROM iam_application app
WHERE app.code = 'platform-member' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'member.root');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130039 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130039)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, 'App 用户', '/member/users', 'member.user', 'member.user', 'ep:user', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform-member' AND parent.route_name = 'member.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'member.user');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130040 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130040)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, 'App 角色', '/member/roles', 'member.role', 'member.role', 'ep:avatar', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform-member' AND parent.route_name = 'member.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'member.role');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, icon, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130041 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130041)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, 'App 权限', '/member/permissions', 'member.permission', 'member.permission', 'ep:key', 'PAGE', 'ANY', 'ACTION', 3, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform-member' AND parent.route_name = 'member.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'member.permission');

-- 菜单与操作关联：ACTION 访问模式下缺少关联的菜单一律不可见。
INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.tenant' AND action.code = 'iam-platform:tenant:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.tenants' AND action.code = 'iam-platform:tenant:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.config' AND action.code = 'iam-platform:application:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.config' AND action.code = 'iam-platform:plan:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.config' AND action.code = 'iam-platform:shared-role:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.applications' AND action.code = 'iam-platform:application:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.plans' AND action.code = 'iam-platform:plan:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.shared.roles' AND action.code = 'iam-platform:shared-role:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:account:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:authorization-audit:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:authorization-diagnosis:run' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:delegation:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:group:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:member:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:role-assignment:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:role:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.accounts' AND action.code = 'iam-platform:account:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.personnel' AND action.code = 'iam-platform:group:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.personnel' AND action.code = 'iam-platform:member:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.authorization' AND action.code = 'iam-platform:authorization-diagnosis:run' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.authorization' AND action.code = 'iam-platform:delegation:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.authorization' AND action.code = 'iam-platform:role-assignment:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.authorization' AND action.code = 'iam-platform:role:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.authorization.audit' AND action.code = 'iam-platform:authorization-audit:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.settings' AND action.code = 'iam-platform:dictionary:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.settings.dictionaries' AND action.code = 'iam-platform:dictionary:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.organization' AND action.code = 'iam-tenant:department:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.organization' AND action.code = 'iam-tenant:group:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.organization' AND action.code = 'iam-tenant:member:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.organization' AND action.code = 'iam-tenant:settings:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.members.root' AND action.code = 'iam-tenant:department:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.members.root' AND action.code = 'iam-tenant:member:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.members' AND action.code = 'iam-tenant:member:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.departments' AND action.code = 'iam-tenant:department:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.groups' AND action.code = 'iam-tenant:group:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.settings' AND action.code = 'iam-tenant:settings:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization.root' AND action.code = 'iam-tenant:application:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization.root' AND action.code = 'iam-tenant:authorization-diagnosis:run' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization.root' AND action.code = 'iam-tenant:delegation:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization.root' AND action.code = 'iam-tenant:role-assignment:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization.root' AND action.code = 'iam-tenant:role:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization' AND action.code = 'iam-tenant:authorization-diagnosis:run' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization' AND action.code = 'iam-tenant:delegation:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization' AND action.code = 'iam-tenant:role-assignment:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization' AND action.code = 'iam-tenant:role:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.applications' AND action.code = 'iam-tenant:application:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.compliance' AND action.code = 'iam-tenant:authorization-audit:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.compliance' AND action.code = 'iam-tenant:field-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.field.policies' AND action.code = 'iam-tenant:field-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization.audit' AND action.code = 'iam-tenant:authorization-audit:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.workspace' AND action.code = 'iam-tenant:workbench:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.workbench' AND action.code = 'iam-tenant:workbench:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-developer' AND menu.route_name = 'platform.develop' AND action.code = 'platform-developer:id-allocation:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-developer' AND menu.route_name = 'platform.develop' AND action.code = 'platform-developer:oauth-client:read-list' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-developer' AND menu.route_name = 'platform.develop' AND action.code = 'platform-developer:qr-code:generate' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-developer' AND menu.route_name = 'platform.develop' AND action.code = 'platform-developer:social-config:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-developer' AND menu.route_name = 'platform.develop.qrcode' AND action.code = 'platform-developer:qr-code:generate' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-developer' AND menu.route_name = 'platform.develop.client' AND action.code = 'platform-developer:oauth-client:read-list' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-developer' AND menu.route_name = 'platform.develop.social' AND action.code = 'platform-developer:social-config:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-developer' AND menu.route_name = 'platform.develop.id' AND action.code = 'platform-developer:id-allocation:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-security' AND menu.route_name = 'security.root' AND action.code = 'platform-security:access-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-security' AND menu.route_name = 'security.root' AND action.code = 'platform-security:account-lockout-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-security' AND menu.route_name = 'security.root' AND action.code = 'platform-security:block-event:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-security' AND menu.route_name = 'security.root' AND action.code = 'platform-security:credential-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-security' AND menu.route_name = 'security.root' AND action.code = 'platform-security:login-failure-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-security' AND menu.route_name = 'security.root' AND action.code = 'platform-security:session-concurrency-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-security' AND menu.route_name = 'security.root' AND action.code = 'platform-security:session:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-security' AND menu.route_name = 'security.credential.policies' AND action.code = 'platform-security:credential-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-security' AND menu.route_name = 'security.account.protection' AND action.code = 'platform-security:account-lockout-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-security' AND menu.route_name = 'security.access.protection' AND action.code = 'platform-security:access-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-security' AND menu.route_name = 'security.access.protection' AND action.code = 'platform-security:block-event:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-security' AND menu.route_name = 'security.access.protection' AND action.code = 'platform-security:login-failure-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-security' AND menu.route_name = 'security.sessions' AND action.code = 'platform-security:session-concurrency-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-security' AND menu.route_name = 'security.sessions' AND action.code = 'platform-security:session:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-member' AND menu.route_name = 'member.root' AND action.code = 'platform-member:permission:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-member' AND menu.route_name = 'member.root' AND action.code = 'platform-member:role:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-member' AND menu.route_name = 'member.root' AND action.code = 'platform-member:user:read-list' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-member' AND menu.route_name = 'member.user' AND action.code = 'platform-member:user:read-list' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-member' AND menu.route_name = 'member.role' AND action.code = 'platform-member:role:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform-member' AND menu.route_name = 'member.permission' AND action.code = 'platform-member:permission:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

CREATE TEMPORARY TABLE IF NOT EXISTS iam_bootstrap_new_revision (domain VARCHAR(16) PRIMARY KEY);
DELETE FROM iam_bootstrap_new_revision;
INSERT INTO iam_bootstrap_new_revision(domain) SELECT 'PLATFORM' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_role_revision v JOIN iam_role_definition r ON r.id=v.role_id WHERE r.domain='PLATFORM' AND r.code='role_admin' AND r.tenant_key=0 AND v.revision=1);
INSERT INTO iam_bootstrap_new_revision(domain) SELECT 'TENANT' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_role_revision v JOIN iam_role_definition r ON r.id=v.role_id WHERE r.domain='TENANT' AND r.code='tenant-governance' AND r.tenant_key=0 AND v.revision=1);
-- 治理角色：每个域恰好一个启用的 SYSTEM 角色，组织初始化依赖这一唯一性。
INSERT INTO iam_role_definition (id, domain, tenant_id, kind, code, name, description, enabled)
SELECT 140001, 'PLATFORM', NULL, 'SYSTEM', 'role_admin', '超级管理员', '系统平台超级管理员，受控直接分配并覆盖新增平台操作', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_role_definition WHERE domain = 'PLATFORM' AND tenant_key = 0 AND code = 'role_admin');

INSERT INTO iam_role_revision (id, role_id, kind, revision, base_revision_id, metadata_overrides, resource_field_permissions)
SELECT 141001, role.id, 'SYSTEM', 1, NULL, CAST('{}' AS JSON), (SELECT JSON_OBJECT(CAST(resource.id AS CHAR), CAST('{"visibility": {"displayName": "FULL", "avatar": "FULL", "phone": "FULL", "email": "FULL", "joinedAt": "FULL", "lastLoginAt": "FULL", "updatedAt": "FULL"}, "operations": {"displayName": {"editable": true, "filterable": true}, "avatar": {"editable": true, "filterable": false}, "phone": {"editable": true, "filterable": false}, "email": {"editable": true, "filterable": false}, "joinedAt": {"editable": false, "filterable": false}, "lastLoginAt": {"editable": false, "filterable": false}, "updatedAt": {"editable": false, "filterable": false}}}' AS JSON)) FROM iam_resource resource JOIN iam_application app ON app.id=resource.application_id WHERE app.code='iam-platform' AND resource.code='member') FROM iam_role_definition role
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND NOT EXISTS (SELECT 1 FROM iam_role_revision WHERE role_id = role.id AND revision = 1);

INSERT INTO iam_role_definition (id, domain, tenant_id, kind, code, name, description, enabled)
SELECT 140002, 'TENANT', NULL, 'SYSTEM', 'tenant-governance', '组织治理系统角色', '组织治理的兼容系统角色', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_role_definition WHERE domain = 'TENANT' AND tenant_key = 0 AND code = 'tenant-governance');

INSERT INTO iam_role_revision (id, role_id, kind, revision, base_revision_id, metadata_overrides, resource_field_permissions)
SELECT 141002, role.id, 'SYSTEM', 1, NULL, CAST('{}' AS JSON), CAST('{}' AS JSON) FROM iam_role_definition role
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND NOT EXISTS (SELECT 1 FROM iam_role_revision WHERE role_id = role.id AND revision = 1);

-- 治理授权：治理角色覆盖本域全部操作，范围为全域。
INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:disable' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:enable' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:lock' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:lookup' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:reset-password' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:unlock' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:action:change-status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:action:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:action:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:action:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:action:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application-entitlement:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application-entitlement:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application-entitlement:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application:change-status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application:purge' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:authorization-audit:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:authorization-diagnosis:run' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:delegation:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:delegation:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:delegation:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:delegation:revoke' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:delegation:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:dictionary:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:dictionary:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:dictionary:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:dictionary:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:group:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:group:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:group:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:group:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:group:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:member:change-status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:member:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:member:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:member:remove' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:member:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:menu:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:menu:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:menu:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:menu:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:plan:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:plan:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:plan:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:resource:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:resource:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:resource:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:resource:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role-assignment:apply-version' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role-assignment:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role-assignment:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role-assignment:revoke' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role-assignment:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role:change-status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role:publish-version' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:shared-role:change-status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:shared-role:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:shared-role:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:shared-role:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:shared-role:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:shared-role:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:tenant:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:tenant:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:tenant:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:tenant:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-developer:id-allocation:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-developer:id-allocation:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-developer:id-allocation:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-developer:id-allocation:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-developer:oauth-client:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-developer:oauth-client:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-developer:oauth-client:read-detail' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-developer:oauth-client:read-list' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-developer:oauth-client:reset-secret' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-developer:oauth-client:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-developer:qr-code:generate' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-developer:social-config:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-developer:social-config:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-developer:social-config:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-developer:social-config:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:permission:change-status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:permission:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:permission:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:permission:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:permission:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:role:assign-users' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:role:change-status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:role:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:role:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:role:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:role:read-permissions' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:role:set-permissions' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:role:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:user:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:user:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:user:disable' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:user:enable' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:user:lock' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:user:read-detail' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:user:read-list' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:user:read-roles' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:user:reset-password' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:user:set-roles' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:user:unlock' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-member:user:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:access-policy:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:access-policy:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:access-policy:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:access-policy:refresh' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:access-policy:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:account-lockout-policy:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:account-lockout-policy:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:block-event:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:credential-policy:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:credential-policy:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:credential-policy:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:credential-policy:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:login-failure-policy:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:login-failure-policy:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:session-concurrency-policy:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:session-concurrency-policy:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:session-concurrency-policy:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:session-concurrency-policy:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:session:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'role_admin' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform-security:session:revoke' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:application-audience:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:application-audience:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:application:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:authorization-audit:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:authorization-diagnosis:run' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:delegation:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:delegation:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:delegation:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:delegation:revoke' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:delegation:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:department:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:department:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:department:export' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:department:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:department:set-leaders' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:department:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:field-policy:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:field-policy:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:group:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:group:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:group:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:group:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:group:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:member:change-status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:member:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:member:export' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:member:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:member:remove' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:member:set-departments' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:member:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role-assignment:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role-assignment:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role-assignment:revoke' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role-assignment:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role:change-status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:settings:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:settings:transfer-owner' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:settings:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:workbench:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

-- 默认策略版本：固定版本被租户字段策略引用；字段默认脱敏手机邮箱，上限缺省不额外收紧。
INSERT INTO iam_default_policy_revision (id, resource_id, kind, revision, definition)
SELECT 150002, resource.id, 'FIELD', 1, CAST('{"fields": {"displayName": "FULL", "avatar": "FULL", "phone": "MASKED", "email": "MASKED"}, "operations": {"displayName": {"editable": true, "filterable": false}, "avatar": {"editable": true, "filterable": false}, "phone": {"editable": true, "filterable": true}, "email": {"editable": true, "filterable": true}}}' AS JSON) FROM iam_resource resource JOIN iam_application app ON app.id = resource.application_id
WHERE app.domain = 'TENANT' AND app.code = 'iam-tenant' AND resource.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_default_policy_revision WHERE resource_id = resource.id AND kind = 'FIELD' AND revision = 1);

-- ====================================================================
-- Source: databases/iam/007_initial_platform_account.sql
-- ====================================================================
-- 初始化唯一的平台登录账号。导入 006_bootstrap.sql 后在空账号表上执行。
-- 保留 ID 低于 IAM 发号起点。初始口令使用 BCrypt 哈希，首次登录要求改密。
SET time_zone = '+00:00';

START TRANSACTION;

INSERT INTO iam_account (id, username, password_hash, enabled, must_change_password)
SELECT 900001, 'platform', '{bcrypt}$2a$10$e0mQjX7rHBoEYe1nOiOcQuhl2hFOhYW4gAczT9NyAJYsXMsvBMgk6', TRUE, TRUE
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_account);

SET @iam_initial_platform_account_inserted = ROW_COUNT();

INSERT INTO iam_platform_member (id, account_id, display_name, status)
SELECT 910001, 900001, '平台管理员', 'ACTIVE'
FROM DUAL
WHERE @iam_initial_platform_account_inserted = 1;

INSERT INTO iam_role_assignment
    (id, domain, subject_type, platform_member_id, revision_id, revision_kind, scope_bindings, valid_from, source, status)
SELECT 940001, 'PLATFORM', 'MEMBER', 910001,
    (SELECT revision.id
     FROM iam_role_revision revision
     JOIN iam_role_definition role ON role.id = revision.role_id
     WHERE role.domain = 'PLATFORM' AND role.tenant_id IS NULL AND role.kind = 'SYSTEM'
       AND role.code = 'role_admin' AND role.enabled
     ORDER BY revision.revision DESC
     LIMIT 1),
    'SYSTEM', JSON_OBJECT(), UTC_TIMESTAMP(6), 'INITIALIZATION', 'ACTIVE'
FROM DUAL
WHERE @iam_initial_platform_account_inserted = 1;

SET @iam_initial_platform_account_inserted = NULL;
COMMIT;

-- 恢复调用者原有的会话外键设置。
SET SESSION FOREIGN_KEY_CHECKS = @iam_init_previous_foreign_key_checks;
