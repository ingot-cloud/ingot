-- IAM 完整初始化（可重复执行；每次清空并重建清单内全部表，不是已有库升级脚本）。
-- 自动生成：python3 tools/iam/generate_database.py；不要手工编辑。
-- 来源顺序：databases/iam/manifest.json；框架DDL仍由原模块维护。
-- 在调用者选定的数据库执行；先备份并停止服务，无CREATE/DROP DATABASE或固定USE。
-- 不包含账号、凭证、业务组织、授权记录或登录运行状态。
-- MySQL 8.0.16+；仅删除阶段关闭外键，建表/种子阶段开启，最后恢复原设置；数据时间按UTC写入。
SET NAMES utf8mb4;
SET SESSION time_zone = '+00:00';

-- 重建：组织与所有者等循环外键要求先删除全部目标表，再开始建表。
SET @iam_init_previous_foreign_key_checks = @@SESSION.FOREIGN_KEY_CHECKS;
SET SESSION FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `password_expiration`;
DROP TABLE IF EXISTS `password_history`;
DROP TABLE IF EXISTS `account_lock_state`;
DROP TABLE IF EXISTS `sys_tenant_plan_record`;
DROP TABLE IF EXISTS `sys_user_social`;
DROP TABLE IF EXISTS `sys_social_details`;
DROP TABLE IF EXISTS `security_event`;
DROP TABLE IF EXISTS `platform_dict`;
DROP TABLE IF EXISTS `biz_leaf_alloc`;
DROP TABLE IF EXISTS `iam_migration_issue`;
DROP TABLE IF EXISTS `iam_migration_mapping`;
DROP TABLE IF EXISTS `iam_migration_batch`;
DROP TABLE IF EXISTS `iam_authorization_audit`;
DROP TABLE IF EXISTS `iam_member_export`;
DROP TABLE IF EXISTS `iam_field_rule`;
DROP TABLE IF EXISTS `iam_field_policy`;
DROP TABLE IF EXISTS `iam_directory_rule`;
DROP TABLE IF EXISTS `iam_directory_policy`;
DROP TABLE IF EXISTS `iam_policy_selector_department`;
DROP TABLE IF EXISTS `iam_policy_selector_member`;
DROP TABLE IF EXISTS `iam_policy_selector`;
DROP TABLE IF EXISTS `iam_default_policy_revision`;
DROP TABLE IF EXISTS `iam_role_assignment`;
DROP TABLE IF EXISTS `iam_delegation_action_ceiling`;
DROP TABLE IF EXISTS `iam_delegation_recipient_department`;
DROP TABLE IF EXISTS `iam_delegation_recipient_member`;
DROP TABLE IF EXISTS `iam_delegation_role_revision`;
DROP TABLE IF EXISTS `iam_delegation_grant`;
DROP TABLE IF EXISTS `iam_role_delta`;
DROP TABLE IF EXISTS `iam_role_grant`;
DROP TABLE IF EXISTS `iam_role_parameter`;
DROP TABLE IF EXISTS `iam_role_revision`;
DROP TABLE IF EXISTS `iam_role_definition`;
DROP TABLE IF EXISTS `iam_plan_application`;
DROP TABLE IF EXISTS `iam_plan`;
DROP TABLE IF EXISTS `iam_audience_group`;
DROP TABLE IF EXISTS `iam_audience_department`;
DROP TABLE IF EXISTS `iam_audience_member`;
DROP TABLE IF EXISTS `iam_app_audience`;
DROP TABLE IF EXISTS `iam_tenant_app_entitlement`;
DROP TABLE IF EXISTS `iam_menu_action`;
DROP TABLE IF EXISTS `iam_menu`;
DROP TABLE IF EXISTS `iam_action`;
DROP TABLE IF EXISTS `iam_resource`;
DROP TABLE IF EXISTS `iam_application`;
DROP TABLE IF EXISTS `iam_tenant_group_department`;
DROP TABLE IF EXISTS `iam_tenant_group_member`;
DROP TABLE IF EXISTS `iam_tenant_group`;
DROP TABLE IF EXISTS `iam_platform_group_member`;
DROP TABLE IF EXISTS `iam_platform_group`;
DROP TABLE IF EXISTS `iam_member_department`;
DROP TABLE IF EXISTS `iam_department`;
DROP TABLE IF EXISTS `iam_tenant_member`;
DROP TABLE IF EXISTS `iam_tenant`;
DROP TABLE IF EXISTS `iam_platform_member`;
DROP TABLE IF EXISTS `iam_account`;
SET SESSION FOREIGN_KEY_CHECKS = 1;

-- ====================================================================
-- Source: databases/iam/001_identity.sql
-- ====================================================================
-- IAM 独立目标库：身份与组织结构。MySQL 8.0.16+；连接时区须为 UTC。
-- 仅在显式选择的全新目标库执行，不包含 USE、DROP、源库修改或默认数据。
-- 不是存量库升级脚本。组织创建事务须在提交前补齐有效 owner_member_id。

CREATE TABLE iam_account (
    id BIGINT UNSIGNED NOT NULL,
    username VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    password_hash VARCHAR(300) NOT NULL,
    phone VARCHAR(32) NULL,
    email VARCHAR(128) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    password_changed_at DATETIME(6) NULL,
    last_login_at DATETIME(6) NULL,
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_account_username (username),
    KEY idx_iam_account_phone (phone),
    KEY idx_iam_account_email (email),
    CONSTRAINT ck_iam_account_id CHECK (id > 0),
    CONSTRAINT ck_iam_account_flags CHECK (enabled IN (0, 1) AND must_change_password IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_platform_member (
    id BIGINT UNSIGNED NOT NULL,
    account_id BIGINT UNSIGNED NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    avatar VARCHAR(512) NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_platform_member_account (account_id),
    KEY idx_iam_platform_member_status (status, id),
    CONSTRAINT fk_iam_platform_member_account FOREIGN KEY (account_id) REFERENCES iam_account (id),
    CONSTRAINT ck_iam_platform_member_id CHECK (id > 0),
    CONSTRAINT ck_iam_platform_member_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REMOVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_tenant (
    id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(128) NOT NULL,
    avatar VARCHAR(512) NULL,
    owner_member_id BIGINT UNSIGNED NULL COMMENT '创建事务内允许暂空；提交前必须是本租户有效成员',
    plan_id BIGINT UNSIGNED NULL COMMENT '最近一次提交的套餐；修改套餐目录不自动回写',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    KEY idx_iam_tenant_owner (id, owner_member_id),
    CONSTRAINT ck_iam_tenant_id CHECK (id > 0),
    CONSTRAINT ck_iam_tenant_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_tenant_member (
    id BIGINT UNSIGNED NOT NULL,
    tenant_id BIGINT UNSIGNED NOT NULL,
    account_id BIGINT UNSIGNED NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    avatar VARCHAR(512) NULL,
    phone VARCHAR(32) NULL COMMENT '组织通讯录资料，不修改全局登录手机号',
    email VARCHAR(128) NULL COMMENT '组织通讯录资料，不修改全局登录邮箱',
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_tenant_member_account (tenant_id, account_id),
    UNIQUE KEY uk_iam_tenant_member_domain (tenant_id, id),
    KEY idx_iam_tenant_member_status (tenant_id, status, id),
    KEY idx_iam_tenant_member_account (account_id, status),
    CONSTRAINT fk_iam_tenant_member_account FOREIGN KEY (account_id) REFERENCES iam_account (id),
    CONSTRAINT fk_iam_tenant_member_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT ck_iam_tenant_member_id CHECK (id > 0),
    CONSTRAINT ck_iam_tenant_member_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REMOVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE iam_tenant ADD CONSTRAINT fk_iam_tenant_owner
    FOREIGN KEY (id, owner_member_id) REFERENCES iam_tenant_member (tenant_id, id);

CREATE TABLE iam_department (
    id BIGINT UNSIGNED NOT NULL,
    tenant_id BIGINT UNSIGNED NOT NULL,
    parent_id BIGINT UNSIGNED NULL,
    name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_department_domain (tenant_id, id),
    KEY idx_iam_department_parent (tenant_id, parent_id, id),
    CONSTRAINT fk_iam_department_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT fk_iam_department_parent FOREIGN KEY (tenant_id, parent_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT ck_iam_department_id CHECK (id > 0),
    CONSTRAINT ck_iam_department_parent CHECK (parent_id IS NULL OR parent_id <> id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_member_department (
    tenant_id BIGINT UNSIGNED NOT NULL,
    member_id BIGINT UNSIGNED NOT NULL,
    department_id BIGINT UNSIGNED NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    primary_member_id BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN is_primary = 1 THEN member_id ELSE NULL END) STORED,
    PRIMARY KEY (tenant_id, member_id, department_id),
    UNIQUE KEY uk_iam_member_primary_department (tenant_id, primary_member_id),
    KEY idx_iam_department_members (tenant_id, department_id, member_id),
    CONSTRAINT fk_iam_member_department_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id),
    CONSTRAINT fk_iam_member_department_department FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT ck_iam_member_department_primary CHECK (is_primary IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_platform_group (
    id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512) NULL,
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT ck_iam_platform_group_id CHECK (id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_platform_group_member (
    group_id BIGINT UNSIGNED NOT NULL,
    member_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (group_id, member_id),
    KEY idx_iam_platform_member_groups (member_id, group_id),
    CONSTRAINT fk_iam_platform_group_entry_group FOREIGN KEY (group_id) REFERENCES iam_platform_group (id),
    CONSTRAINT fk_iam_platform_group_entry_member FOREIGN KEY (member_id) REFERENCES iam_platform_member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_tenant_group (
    id BIGINT UNSIGNED NOT NULL,
    tenant_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512) NULL,
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_tenant_group_domain (tenant_id, id),
    CONSTRAINT fk_iam_tenant_group_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT ck_iam_tenant_group_id CHECK (id > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_tenant_group_member (
    tenant_id BIGINT UNSIGNED NOT NULL,
    group_id BIGINT UNSIGNED NOT NULL,
    member_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (tenant_id, group_id, member_id),
    KEY idx_iam_tenant_member_groups (tenant_id, member_id, group_id),
    CONSTRAINT fk_iam_tenant_group_entry_group FOREIGN KEY (tenant_id, group_id) REFERENCES iam_tenant_group (tenant_id, id),
    CONSTRAINT fk_iam_tenant_group_entry_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_tenant_group_department (
    tenant_id BIGINT UNSIGNED NOT NULL,
    group_id BIGINT UNSIGNED NOT NULL,
    department_id BIGINT UNSIGNED NOT NULL,
    include_descendants BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (tenant_id, group_id, department_id),
    KEY idx_iam_tenant_department_groups (tenant_id, department_id, group_id),
    CONSTRAINT fk_iam_tenant_group_dept_group FOREIGN KEY (tenant_id, group_id) REFERENCES iam_tenant_group (tenant_id, id),
    CONSTRAINT fk_iam_tenant_group_dept_department FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT ck_iam_group_include_descendants CHECK (include_descendants IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ====================================================================
-- Source: databases/iam/002_catalog_role.sql
-- ====================================================================
-- IAM 独立目标库：先执行 001_identity.sql。无源库写入或授权默认数据。
CREATE TABLE iam_application (
    id BIGINT UNSIGNED NOT NULL,
    code VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512) NULL,
    icon VARCHAR(512) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    baseline BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_application_code (code),
    CONSTRAINT ck_iam_application_domain CHECK (domain IN ('PLATFORM', 'TENANT')),
    CONSTRAINT ck_iam_application_baseline CHECK (baseline IN (0, 1)),
    CONSTRAINT ck_iam_application_baseline_domain CHECK (baseline = FALSE OR domain = 'TENANT'),
    CONSTRAINT ck_iam_application_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_resource (
    id BIGINT UNSIGNED NOT NULL,
    application_id BIGINT UNSIGNED NOT NULL,
    code VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    name VARCHAR(128) NOT NULL,
    scope_capabilities JSON NOT NULL,
    field_capabilities JSON NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_resource_code (application_id, code),
    UNIQUE KEY uk_iam_resource_application (application_id, id),
    CONSTRAINT fk_iam_resource_application FOREIGN KEY (application_id) REFERENCES iam_application (id),
    CONSTRAINT ck_iam_resource_scopes CHECK (JSON_TYPE(scope_capabilities) = 'ARRAY'),
    CONSTRAINT ck_iam_resource_fields CHECK (JSON_TYPE(field_capabilities) = 'ARRAY'),
    CONSTRAINT ck_iam_resource_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_action (
    id BIGINT UNSIGNED NOT NULL,
    application_id BIGINT UNSIGNED NOT NULL,
    resource_id BIGINT UNSIGNED NOT NULL,
    code VARCHAR(192) COLLATE utf8mb4_bin NOT NULL,
    name VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_action_code (code),
    UNIQUE KEY uk_iam_action_application (application_id, id),
    KEY idx_iam_action_resource (application_id, resource_id, id),
    CONSTRAINT fk_iam_action_resource FOREIGN KEY (application_id, resource_id) REFERENCES iam_resource (application_id, id),
    CONSTRAINT ck_iam_action_exact CHECK (LOCATE('*', code) = 0 AND CHAR_LENGTH(code) > 0),
    CONSTRAINT ck_iam_action_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_menu (
    id BIGINT UNSIGNED NOT NULL,
    application_id BIGINT UNSIGNED NOT NULL,
    parent_id BIGINT UNSIGNED NULL,
    name VARCHAR(128) NOT NULL,
    path VARCHAR(512) NULL,
    view_path VARCHAR(256) NULL,
    route_name VARCHAR(128) NULL,
    icon VARCHAR(512) NULL,
    kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    match_mode VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ANY',
    access_mode VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTION',
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_menu_action (
    application_id BIGINT UNSIGNED NOT NULL,
    menu_id BIGINT UNSIGNED NOT NULL,
    action_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (application_id, menu_id, action_id),
    KEY idx_iam_action_menus (application_id, action_id, menu_id),
    CONSTRAINT fk_iam_menu_action_menu FOREIGN KEY (application_id, menu_id) REFERENCES iam_menu (application_id, id),
    CONSTRAINT fk_iam_menu_action_action FOREIGN KEY (application_id, action_id) REFERENCES iam_action (application_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_tenant_app_entitlement (
    id BIGINT UNSIGNED NOT NULL,
    tenant_id BIGINT UNSIGNED NOT NULL,
    application_id BIGINT UNSIGNED NOT NULL,
    enabled BOOLEAN NOT NULL,
    source VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_id BIGINT UNSIGNED NULL,
    valid_from DATETIME(6) NULL,
    valid_until DATETIME(6) NULL,
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_entitlement (tenant_id, application_id),
    CONSTRAINT fk_iam_entitlement_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT fk_iam_entitlement_app FOREIGN KEY (application_id) REFERENCES iam_application (id),
    CONSTRAINT ck_iam_entitlement_source CHECK (source IN ('INITIALIZATION', 'MANUAL', 'PLAN', 'MIGRATION')),
    CONSTRAINT ck_iam_entitlement_interval CHECK (valid_from IS NULL OR valid_until IS NULL OR valid_from < valid_until),
    CONSTRAINT ck_iam_entitlement_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_app_audience (
    tenant_id BIGINT UNSIGNED NOT NULL,
    application_id BIGINT UNSIGNED NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    audience_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (tenant_id, application_id),
    CONSTRAINT fk_iam_audience_entitlement FOREIGN KEY (tenant_id, application_id) REFERENCES iam_tenant_app_entitlement (tenant_id, application_id),
    CONSTRAINT ck_iam_audience_kind CHECK (audience_kind IN ('ALL', 'SELECTED')),
    CONSTRAINT ck_iam_audience_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_audience_member (
    tenant_id BIGINT UNSIGNED NOT NULL,
    application_id BIGINT UNSIGNED NOT NULL,
    member_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (tenant_id, application_id, member_id),
    CONSTRAINT fk_iam_audience_member_config FOREIGN KEY (tenant_id, application_id) REFERENCES iam_app_audience (tenant_id, application_id),
    CONSTRAINT fk_iam_audience_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_audience_department (
    tenant_id BIGINT UNSIGNED NOT NULL,
    application_id BIGINT UNSIGNED NOT NULL,
    department_id BIGINT UNSIGNED NOT NULL,
    include_descendants BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (tenant_id, application_id, department_id),
    CONSTRAINT fk_iam_audience_dept_config FOREIGN KEY (tenant_id, application_id) REFERENCES iam_app_audience (tenant_id, application_id),
    CONSTRAINT fk_iam_audience_dept FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT ck_iam_audience_descendants CHECK (include_descendants IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_audience_group (
    tenant_id BIGINT UNSIGNED NOT NULL,
    application_id BIGINT UNSIGNED NOT NULL,
    group_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (tenant_id, application_id, group_id),
    CONSTRAINT fk_iam_audience_group_config FOREIGN KEY (tenant_id, application_id) REFERENCES iam_app_audience (tenant_id, application_id),
    CONSTRAINT fk_iam_audience_group FOREIGN KEY (tenant_id, group_id) REFERENCES iam_tenant_group (tenant_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_plan (
    id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT ck_iam_plan_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_plan_application (
    plan_id BIGINT UNSIGNED NOT NULL,
    application_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (plan_id, application_id),
    CONSTRAINT fk_iam_plan_application_plan FOREIGN KEY (plan_id) REFERENCES iam_plan (id),
    CONSTRAINT fk_iam_plan_application_app FOREIGN KEY (application_id) REFERENCES iam_application (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_role_definition (
    id BIGINT UNSIGNED NOT NULL,
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    tenant_id BIGINT UNSIGNED NULL,
    tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED,
    kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    code VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512) NULL,
    group_name VARCHAR(128) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_role_code (domain, tenant_key, code),
    UNIQUE KEY uk_iam_role_kind (id, kind),
    CONSTRAINT fk_iam_role_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT ck_iam_role_domain CHECK (domain IN ('PLATFORM', 'TENANT')),
    CONSTRAINT ck_iam_role_kind CHECK (
        (kind = 'SYSTEM' AND tenant_id IS NULL) OR
        (kind = 'SHARED' AND domain = 'TENANT' AND tenant_id IS NULL) OR
        (kind = 'PLATFORM_CUSTOM' AND domain = 'PLATFORM' AND tenant_id IS NULL) OR
        (kind = 'TENANT_CUSTOM' AND domain = 'TENANT' AND tenant_id IS NOT NULL)),
    CONSTRAINT ck_iam_role_enabled CHECK (enabled IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_role_revision (
    id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    revision BIGINT UNSIGNED NOT NULL,
    base_revision_id BIGINT UNSIGNED NULL,
    base_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin
        GENERATED ALWAYS AS (CASE WHEN base_revision_id IS NULL THEN NULL ELSE 'SHARED' END) STORED,
    metadata_overrides JSON NOT NULL,
    resource_field_permissions JSON NOT NULL DEFAULT (JSON_OBJECT()) COMMENT '固定资源字段权限快照，未声明字段不授予权限',
    published_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_revision_number (role_id, revision),
    UNIQUE KEY uk_iam_revision_kind (id, kind),
    CONSTRAINT fk_iam_revision_role FOREIGN KEY (role_id, kind) REFERENCES iam_role_definition (id, kind),
    CONSTRAINT fk_iam_revision_base FOREIGN KEY (base_revision_id, base_kind) REFERENCES iam_role_revision (id, kind),
    CONSTRAINT ck_iam_revision_number CHECK (revision > 0),
    CONSTRAINT ck_iam_revision_base CHECK (base_revision_id IS NULL OR (kind = 'TENANT_CUSTOM' AND base_revision_id <> id)),
    CONSTRAINT ck_iam_revision_metadata CHECK (JSON_TYPE(metadata_overrides) = 'OBJECT'),
    CONSTRAINT ck_iam_revision_fields CHECK (JSON_TYPE(resource_field_permissions) = 'OBJECT')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_role_parameter (
    revision_id BIGINT UNSIGNED NOT NULL,
    parameter_key VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
    binding_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (revision_id, parameter_key),
    CONSTRAINT fk_iam_parameter_revision FOREIGN KEY (revision_id) REFERENCES iam_role_revision (id),
    CONSTRAINT ck_iam_parameter_kind CHECK (binding_kind IN ('DEPARTMENTS', 'OBJECTS'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_role_grant (
    revision_id BIGINT UNSIGNED NOT NULL,
    action_id BIGINT UNSIGNED NOT NULL,
    scopes JSON NOT NULL,
    PRIMARY KEY (revision_id, action_id),
    CONSTRAINT fk_iam_grant_revision FOREIGN KEY (revision_id) REFERENCES iam_role_revision (id),
    CONSTRAINT fk_iam_grant_action FOREIGN KEY (action_id) REFERENCES iam_action (id),
    CONSTRAINT ck_iam_grant_scopes CHECK (JSON_TYPE(scopes) = 'ARRAY')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_role_delta (
    revision_id BIGINT UNSIGNED NOT NULL,
    action_id BIGINT UNSIGNED NOT NULL,
    operation VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    scopes JSON NOT NULL,
    PRIMARY KEY (revision_id, action_id),
    CONSTRAINT fk_iam_delta_revision FOREIGN KEY (revision_id) REFERENCES iam_role_revision (id),
    CONSTRAINT fk_iam_delta_action FOREIGN KEY (action_id) REFERENCES iam_action (id),
    CONSTRAINT ck_iam_delta_operation CHECK (operation IN ('ADD', 'REMOVE', 'REPLACE_SCOPE')),
    CONSTRAINT ck_iam_delta_scopes CHECK (JSON_TYPE(scopes) = 'ARRAY' AND (operation <> 'REMOVE' OR JSON_LENGTH(scopes) = 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ====================================================================
-- Source: databases/iam/003_assignment_delegation.sql
-- ====================================================================
-- IAM 独立目标库：先执行 001、002。跨域关系由复合外键约束；权限上限仍由事务服务校验。
CREATE TABLE iam_delegation_grant (
    id BIGINT UNSIGNED NOT NULL,
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    tenant_id BIGINT UNSIGNED NULL,
    tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED,
    platform_administrator_id BIGINT UNSIGNED NULL,
    tenant_administrator_id BIGINT UNSIGNED NULL,
    valid_from DATETIME(6) NULL,
    valid_until DATETIME(6) NULL,
    assignment_duration_mode VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'LIMITED',
    max_assignment_duration_seconds BIGINT UNSIGNED NULL,
    max_assignment_duration_nanos INT UNSIGNED NOT NULL DEFAULT 0,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_delegation_role_revision (
    delegation_id BIGINT UNSIGNED NOT NULL,
    revision_id BIGINT UNSIGNED NOT NULL,
    revision_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (delegation_id, revision_id),
    CONSTRAINT fk_iam_delegation_role_source FOREIGN KEY (delegation_id) REFERENCES iam_delegation_grant (id),
    CONSTRAINT fk_iam_delegation_role_revision FOREIGN KEY (revision_id, revision_kind) REFERENCES iam_role_revision (id, kind)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_delegation_recipient_member (
    delegation_id BIGINT UNSIGNED NOT NULL,
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    tenant_id BIGINT UNSIGNED NULL,
    tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED,
    platform_member_id BIGINT UNSIGNED NULL,
    tenant_member_id BIGINT UNSIGNED NULL,
    member_id BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(platform_member_id, tenant_member_id)) STORED,
    UNIQUE KEY uk_iam_delegation_recipient (delegation_id, member_id),
    CONSTRAINT fk_iam_recipient_delegation FOREIGN KEY (domain, tenant_key, delegation_id) REFERENCES iam_delegation_grant (domain, tenant_key, id),
    CONSTRAINT fk_iam_recipient_platform FOREIGN KEY (platform_member_id) REFERENCES iam_platform_member (id),
    CONSTRAINT fk_iam_recipient_tenant FOREIGN KEY (tenant_id, tenant_member_id) REFERENCES iam_tenant_member (tenant_id, id),
    CONSTRAINT ck_iam_recipient_identity CHECK (
        (domain = 'PLATFORM' AND tenant_id IS NULL AND platform_member_id IS NOT NULL AND tenant_member_id IS NULL) OR
        (domain = 'TENANT' AND tenant_id IS NOT NULL AND tenant_member_id IS NOT NULL AND platform_member_id IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_delegation_recipient_department (
    delegation_id BIGINT UNSIGNED NOT NULL,
    tenant_id BIGINT UNSIGNED NOT NULL,
    department_id BIGINT UNSIGNED NOT NULL,
    include_descendants BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (delegation_id, department_id),
    CONSTRAINT fk_iam_recipient_dept_delegation FOREIGN KEY (tenant_id, delegation_id) REFERENCES iam_delegation_grant (tenant_id, id),
    CONSTRAINT fk_iam_recipient_dept_department FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT ck_iam_recipient_descendants CHECK (include_descendants IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_delegation_action_ceiling (
    delegation_id BIGINT UNSIGNED NOT NULL,
    action_id BIGINT UNSIGNED NOT NULL,
    scopes JSON NOT NULL,
    scope_bindings JSON NOT NULL,
    PRIMARY KEY (delegation_id, action_id),
    CONSTRAINT fk_iam_ceiling_delegation FOREIGN KEY (delegation_id) REFERENCES iam_delegation_grant (id),
    CONSTRAINT fk_iam_ceiling_action FOREIGN KEY (action_id) REFERENCES iam_action (id),
    CONSTRAINT ck_iam_ceiling_scopes CHECK (JSON_TYPE(scopes) = 'ARRAY'),
    CONSTRAINT ck_iam_ceiling_bindings CHECK (JSON_TYPE(scope_bindings) = 'OBJECT')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_role_assignment (
    id BIGINT UNSIGNED NOT NULL,
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    tenant_id BIGINT UNSIGNED NULL,
    tenant_key BIGINT UNSIGNED GENERATED ALWAYS AS (COALESCE(tenant_id, 0)) STORED,
    subject_type VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    platform_member_id BIGINT UNSIGNED NULL,
    platform_group_id BIGINT UNSIGNED NULL,
    tenant_member_id BIGINT UNSIGNED NULL,
    tenant_group_id BIGINT UNSIGNED NULL,
    revision_id BIGINT UNSIGNED NOT NULL,
    revision_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    scope_bindings JSON NOT NULL,
    delegation_grant_id BIGINT UNSIGNED NULL,
    valid_from DATETIME(6) NOT NULL,
    valid_until DATETIME(6) NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE',
    source VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ====================================================================
-- Source: databases/iam/004_policy_audit_migration.sql
-- ====================================================================
-- IAM 独立目标库：策略引用、审计事实与迁移记录，不初始化或授予任何默认权限。
CREATE TABLE iam_default_policy_revision (
    id BIGINT UNSIGNED NOT NULL,
    kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    revision BIGINT UNSIGNED NOT NULL,
    definition JSON NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_default_policy_kind (id, kind),
    UNIQUE KEY uk_iam_default_policy_revision (kind, revision),
    CONSTRAINT ck_iam_default_policy_kind CHECK (kind IN ('DIRECTORY', 'FIELD')),
    CONSTRAINT ck_iam_default_policy_definition CHECK (JSON_TYPE(definition) = 'OBJECT'),
    CONSTRAINT ck_iam_default_policy_revision CHECK (revision > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_policy_selector (
    id BIGINT UNSIGNED NOT NULL,
    tenant_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_selector_tenant (tenant_id, id),
    CONSTRAINT fk_iam_selector_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_policy_selector_member (
    tenant_id BIGINT UNSIGNED NOT NULL,
    selector_id BIGINT UNSIGNED NOT NULL,
    member_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (tenant_id, selector_id, member_id),
    CONSTRAINT fk_iam_selector_member_selector FOREIGN KEY (tenant_id, selector_id) REFERENCES iam_policy_selector (tenant_id, id),
    CONSTRAINT fk_iam_selector_member_member FOREIGN KEY (tenant_id, member_id) REFERENCES iam_tenant_member (tenant_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_policy_selector_department (
    tenant_id BIGINT UNSIGNED NOT NULL,
    selector_id BIGINT UNSIGNED NOT NULL,
    department_id BIGINT UNSIGNED NOT NULL,
    include_descendants BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (tenant_id, selector_id, department_id),
    CONSTRAINT fk_iam_selector_dept_selector FOREIGN KEY (tenant_id, selector_id) REFERENCES iam_policy_selector (tenant_id, id),
    CONSTRAINT fk_iam_selector_dept_dept FOREIGN KEY (tenant_id, department_id) REFERENCES iam_department (tenant_id, id),
    CONSTRAINT ck_iam_selector_descendants CHECK (include_descendants IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_directory_policy (
    tenant_id BIGINT UNSIGNED NOT NULL,
    default_revision_id BIGINT UNSIGNED NOT NULL,
    default_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'DIRECTORY',
    default_scope VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '空表示继承固定默认版本',
    default_selector_id BIGINT UNSIGNED NULL,
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (tenant_id),
    CONSTRAINT fk_iam_directory_policy_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT fk_iam_directory_default FOREIGN KEY (default_revision_id, default_kind) REFERENCES iam_default_policy_revision (id, kind),
    CONSTRAINT fk_iam_directory_default_selector FOREIGN KEY (tenant_id, default_selector_id) REFERENCES iam_policy_selector (tenant_id, id),
    CONSTRAINT ck_iam_directory_default_kind CHECK (default_kind = 'DIRECTORY'),
    CONSTRAINT ck_iam_directory_default_scope CHECK (
        (default_scope IS NULL AND default_selector_id IS NULL) OR
        (default_scope IS NOT NULL AND ((default_scope IN ('ALL', 'SELF') AND default_selector_id IS NULL) OR
        (default_scope = 'SELECTED' AND default_selector_id IS NOT NULL))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_directory_rule (
    id BIGINT UNSIGNED NOT NULL,
    tenant_id BIGINT UNSIGNED NOT NULL,
    effect VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    viewer_selector_id BIGINT UNSIGNED NOT NULL,
    target_selector_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    KEY idx_iam_directory_rule (tenant_id, effect, id),
    CONSTRAINT fk_iam_directory_rule_policy FOREIGN KEY (tenant_id) REFERENCES iam_directory_policy (tenant_id),
    CONSTRAINT fk_iam_directory_rule_viewer FOREIGN KEY (tenant_id, viewer_selector_id) REFERENCES iam_policy_selector (tenant_id, id),
    CONSTRAINT fk_iam_directory_rule_target FOREIGN KEY (tenant_id, target_selector_id) REFERENCES iam_policy_selector (tenant_id, id),
    CONSTRAINT ck_iam_directory_effect CHECK (effect IN ('ALLOW', 'DENY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_field_policy (
    tenant_id BIGINT UNSIGNED NOT NULL,
    default_revision_id BIGINT UNSIGNED NOT NULL,
    default_kind VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'FIELD',
    version BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (tenant_id),
    CONSTRAINT fk_iam_field_policy_tenant FOREIGN KEY (tenant_id) REFERENCES iam_tenant (id),
    CONSTRAINT fk_iam_field_default FOREIGN KEY (default_revision_id, default_kind) REFERENCES iam_default_policy_revision (id, kind),
    CONSTRAINT ck_iam_field_default_kind CHECK (default_kind = 'FIELD')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_field_rule (
    id BIGINT UNSIGNED NOT NULL,
    tenant_id BIGINT UNSIGNED NOT NULL,
    scenario VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    field_key VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
    viewer_selector_id BIGINT UNSIGNED NOT NULL,
    target_scope JSON NOT NULL,
    scope_bindings JSON NOT NULL,
    visibility VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    editable BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    KEY idx_iam_field_rule_match (tenant_id, scenario, field_key, id),
    CONSTRAINT fk_iam_field_rule_policy FOREIGN KEY (tenant_id) REFERENCES iam_field_policy (tenant_id),
    CONSTRAINT fk_iam_field_rule_viewer FOREIGN KEY (tenant_id, viewer_selector_id) REFERENCES iam_policy_selector (tenant_id, id),
    CONSTRAINT ck_iam_field_rule_scenario CHECK (scenario IN ('MANAGEMENT', 'DIRECTORY')),
    CONSTRAINT ck_iam_field_rule_visibility CHECK (visibility IN ('HIDDEN', 'MASKED', 'FULL')),
    CONSTRAINT ck_iam_field_rule_editable CHECK (editable IN (0, 1) AND (editable = 0 OR visibility = 'FULL')),
    CONSTRAINT ck_iam_field_rule_scope CHECK (JSON_TYPE(target_scope) = 'ARRAY'),
    CONSTRAINT ck_iam_field_rule_bindings CHECK (JSON_TYPE(scope_bindings) = 'OBJECT')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 成员导出任务：共享库存储，供多实例读取状态、成员 ID 快照与过期清理。
CREATE TABLE iam_member_export (
    id BIGINT UNSIGNED NOT NULL,
    tenant_id BIGINT UNSIGNED NOT NULL,
    actor_member_id BIGINT UNSIGNED NOT NULL,
    tenant_version VARCHAR(32) COLLATE utf8mb4_bin NOT NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    member_ids JSON NULL,
    failure_reason VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    created_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_iam_member_export_tenant_status (tenant_id, status, expires_at),
    CONSTRAINT ck_iam_member_export_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'EXPIRED')),
    CONSTRAINT ck_iam_member_export_ids CHECK (member_ids IS NULL OR JSON_TYPE(member_ids) = 'ARRAY')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_authorization_audit (
    id BIGINT UNSIGNED NOT NULL,
    event_id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    actor_account_id BIGINT UNSIGNED NOT NULL,
    actor_member_id BIGINT UNSIGNED NOT NULL,
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    tenant_id BIGINT UNSIGNED NULL,
    target_type VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    target_id VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
    change_type VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    safe_before JSON NOT NULL,
    safe_after JSON NOT NULL,
    revisions JSON NOT NULL,
    delegation_id BIGINT UNSIGNED NULL,
    assignment_id BIGINT UNSIGNED NULL,
    trace_id VARCHAR(128) NULL,
    occurred_at DATETIME(6) NOT NULL,
    delivered_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_audit_event (event_id),
    KEY idx_iam_audit_tenant_time (tenant_id, occurred_at, id),
    KEY idx_iam_audit_actor (actor_account_id, occurred_at, id),
    KEY idx_iam_audit_assignment_create (assignment_id, change_type, occurred_at, id),
    KEY idx_iam_audit_delivery (delivered_at, id),
    CONSTRAINT ck_iam_audit_context CHECK ((domain = 'PLATFORM' AND tenant_id IS NULL) OR (domain = 'TENANT' AND tenant_id IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_migration_batch (
    id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    source_fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    target_identifier VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
    rules_version VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'NEW',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    verified_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_iam_batch_status CHECK (status IN ('NEW', 'PREFLIGHTED', 'IMPORTED', 'VERIFIED', 'FAILED')),
    CONSTRAINT ck_iam_batch_verified CHECK ((status = 'VERIFIED' AND verified_at IS NOT NULL) OR (status <> 'VERIFIED' AND verified_at IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_migration_mapping (
    batch_id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    source_table VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    source_id VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
    target_type VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    target_id VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
    disposition VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    actor VARCHAR(128) NOT NULL,
    reason VARCHAR(1024) NOT NULL,
    PRIMARY KEY (batch_id, source_table, source_id, target_type, target_id),
    CONSTRAINT fk_iam_mapping_batch FOREIGN KEY (batch_id) REFERENCES iam_migration_batch (id),
    CONSTRAINT ck_iam_mapping_disposition CHECK (disposition IN ('KEEP_MAPPED', 'NARROW', 'RECONFIGURE', 'ARCHIVE', 'SKIP'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_migration_issue (
    id BIGINT UNSIGNED NOT NULL,
    batch_id VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    issue_code VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    source_table VARCHAR(64) COLLATE utf8mb4_bin NULL,
    source_id VARCHAR(128) COLLATE utf8mb4_bin NULL,
    safe_description VARCHAR(1024) NOT NULL,
    disposition VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NULL,
    resolved_by VARCHAR(128) NULL,
    resolution_reason VARCHAR(1024) NULL,
    PRIMARY KEY (id),
    KEY idx_iam_issue_unresolved (batch_id, disposition, id),
    CONSTRAINT fk_iam_issue_batch FOREIGN KEY (batch_id) REFERENCES iam_migration_batch (id),
    CONSTRAINT ck_iam_issue_disposition CHECK (disposition IS NULL OR disposition IN ('KEEP_MAPPED', 'NARROW', 'RECONFIGURE', 'ARCHIVE', 'SKIP')),
    CONSTRAINT ck_iam_issue_resolution CHECK (
        (disposition IS NULL AND resolved_by IS NULL AND resolution_reason IS NULL) OR
        (disposition IS NOT NULL AND resolved_by IS NOT NULL AND resolution_reason IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ====================================================================
-- Source: databases/iam/005_auxiliary.sql
-- ====================================================================
-- IAM 独立目标库：保留辅助领域结构，仅提取源结构定义，不包含数据、DROP 或 USE。
-- 这些旧列名继续表示新 Account/Tenant/Application/Plan 的映射 ID；真实映射在迁移阶段验证。
--
-- 归属说明：account_lock_state、password_history、password_expiration 的权威 DDL 由安全框架自带，
-- 不在本文件重复定义。部署 IAM 数据库时另行执行：
--   ingot-framework/ingot-security/ingot-security-account/ingot-security-account-adapter/src/main/resources/sql/account_lock_state.sql
--   ingot-framework/ingot-security/ingot-security-credential-data/src/main/resources/sql/add_password_history.sql
-- 表可以部署在 IAM 库，但持久化职责仍归框架适配器，IAM 不得新增同表实体或 Mapper。


CREATE TABLE `biz_leaf_alloc` (
  `biz_tag` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `max_id` bigint NOT NULL DEFAULT '1',
  `step` int NOT NULL,
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`biz_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `platform_dict` (
  `id` bigint NOT NULL COMMENT 'ID',
  `pid` bigint NOT NULL DEFAULT '0' COMMENT '父ID',
  `code` varchar(64) NOT NULL COMMENT '编码',
  `name` varchar(128) NOT NULL COMMENT '名称',
  `value` varchar(128) DEFAULT NULL COMMENT '字典项值（仅字典项有效）',
  `label` varchar(128) DEFAULT NULL COMMENT '字典项展示文本（仅字典项有效）',
  `type` char(1) NOT NULL COMMENT '字典类型',
  `scope_type` char(1) NOT NULL DEFAULT '0' COMMENT '作用域, 0:平台,1:租户,2:应用',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID（scope_type=1时必填）',
  `app_id` bigint DEFAULT NULL COMMENT '应用ID（scope_type=2时必填）',
  `org_type` char(1) NOT NULL DEFAULT '0' COMMENT '组织类型',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序权重',
  `system_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否内置字典',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `extra` json DEFAULT NULL COMMENT '扩展属性',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`),
  KEY `idx_dict_pid` (`pid`) USING BTREE,
  KEY `idx_dict_code` (`code`) USING BTREE,
  KEY `idx_dict_type_status` (`type`,`status`) USING BTREE,
  KEY `idx_dict_scope` (`scope_type`,`tenant_id`,`app_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `security_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_id` varchar(32) DEFAULT NULL COMMENT 'producer 幂等 ID',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `event_category` varchar(20) NOT NULL COMMENT 'AUTH/ACCOUNT/CREDENTIAL/ACCESS',
  `priority` varchar(16) NOT NULL DEFAULT 'BEST_EFFORT' COMMENT 'BEST_EFFORT/DURABLE',
  `occurred_at` datetime DEFAULT NULL COMMENT '业务发生时间',
  `received_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '接收时间',
  `tenant_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `user_type` varchar(20) DEFAULT NULL,
  `account` varchar(128) DEFAULT NULL,
  `client_id` varchar(64) DEFAULT NULL,
  `app_id` varchar(64) DEFAULT NULL,
  `session_id` varchar(64) DEFAULT NULL,
  `device_id` varchar(128) DEFAULT NULL,
  `client_ip` varchar(64) DEFAULT NULL,
  `request_uri` varchar(512) DEFAULT NULL,
  `user_agent` varchar(512) DEFAULT NULL,
  `result` varchar(20) DEFAULT NULL,
  `reason_code` varchar(50) DEFAULT NULL,
  `reason_detail` varchar(500) DEFAULT NULL,
  `source_module` varchar(64) NOT NULL COMMENT '上报模块',
  `source` varchar(50) DEFAULT NULL,
  `operator_id` bigint DEFAULT NULL,
  `operator_name` varchar(64) DEFAULT NULL,
  `trace_id` varchar(64) DEFAULT NULL,
  `extension` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_received_id` (`received_at`,`id`),
  KEY `idx_event_type` (`event_type`,`received_at`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_trace` (`trace_id`)
) ENGINE=InnoDB AUTO_INCREMENT=93243 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一安全事件（canonical）';

CREATE TABLE `sys_social_details` (
  `id` bigint unsigned NOT NULL COMMENT 'ID',
  `tenant_id` bigint unsigned NOT NULL COMMENT '租户ID',
  `app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'App ID',
  `app_secret` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'App Secret',
  `redirect_url` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '重定向地址',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '社交名称',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '类型',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `sys_user_social` (
  `id` bigint NOT NULL COMMENT 'ID',
  `tenant_id` bigint NOT NULL COMMENT '组织ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道类型',
  `unique_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道唯一ID',
  `bind_at` datetime NOT NULL COMMENT '绑定时间',
  PRIMARY KEY (`id`),
  KEY `idx_unique_type_user` (`unique_id`,`type`,`user_id`) USING BTREE COMMENT '渠道用户索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `sys_tenant_plan_record` (
  `id` bigint NOT NULL COMMENT 'ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `plan_id` int NOT NULL COMMENT '计划ID',
  `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '计划类型',
  `duration` int NOT NULL COMMENT '持续时间',
  `unit` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '单位',
  `created_at` datetime NOT NULL COMMENT '创建日期',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ====================================================================
-- Source: ingot-framework/ingot-security/ingot-security-account/ingot-security-account-adapter/src/main/resources/sql/account_lock_state.sql
-- ====================================================================
CREATE TABLE IF NOT EXISTS account_lock_state (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id         BIGINT       NOT NULL               COMMENT '用户ID',
  user_type       VARCHAR(20)  NOT NULL DEFAULT '0'   COMMENT '用户类型（同 UserTypeEnum.value：0-系统用户 1-C端用户）',

  -- 锁定状态
  locked          TINYINT(1)   NOT NULL DEFAULT 0     COMMENT '是否锁定（0-否 1-是）',
  lock_type       VARCHAR(20)  DEFAULT NULL            COMMENT '锁定类型（MANUAL-手动 AUTO-自动）',
  lock_reason_code   VARCHAR(50)  DEFAULT NULL         COMMENT '锁定原因代码',
  lock_reason_detail VARCHAR(500) DEFAULT NULL         COMMENT '锁定原因详情',

  -- 锁定时间信息
  locked_at       DATETIME     DEFAULT NULL            COMMENT '锁定时间',
  locked_until    DATETIME     DEFAULT NULL            COMMENT '锁定到期时间（NULL=永久锁定）',

  -- 操作信息（手动锁定时填写）
  operator_id     BIGINT       DEFAULT NULL            COMMENT '操作人ID',
  operator_name   VARCHAR(64)  DEFAULT NULL            COMMENT '操作人姓名',

  -- 登录失败计数
  failed_login_count INT        NOT NULL DEFAULT 0    COMMENT '连续登录失败次数',
  last_failed_at  DATETIME     DEFAULT NULL            COMMENT '最后一次失败时间',

  -- 时间戳
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (id),
  UNIQUE KEY uk_user_id_type (user_id, user_type)       COMMENT '用户ID + 用户类型联合唯一',
  KEY idx_locked (locked, locked_until) USING BTREE     COMMENT '锁定状态 + 到期时间索引（自动解锁任务使用）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号锁定状态表';

-- ====================================================================
-- Source: ingot-framework/ingot-security/ingot-security-credential-data/src/main/resources/sql/add_password_history.sql
-- ====================================================================
CREATE TABLE IF NOT EXISTS `password_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希值',
  `sequence_number` INT NOT NULL COMMENT '序号（用于环形缓冲，从1开始）',
  `version` bigint NOT NULL DEFAULT '1' COMMENT '版本',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_sequence` (`user_id`, `sequence_number`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密码历史记录（环形缓冲）';

CREATE TABLE IF NOT EXISTS `password_expiration` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `last_changed_at` DATETIME NOT NULL COMMENT '最后修改密码时间',
  `expires_at` DATETIME NOT NULL COMMENT '密码过期时间',
  `force_change` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否强制修改（0-否 1-是）',
  `grace_login_remaining` INT NOT NULL DEFAULT 0 COMMENT '剩余宽限登录次数',
  `next_warning_at` DATETIME NULL COMMENT '下次提醒时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_expires_at` (`expires_at`),
  KEY `idx_next_warning_at` (`next_warning_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密码过期信息';

-- ====================================================================
-- Source: databases/iam/006_bootstrap.sql
-- ====================================================================
-- IAM 正式冷启动种子：先按顺序执行 001–005 再执行本文件，可重复执行。
-- 由 tools/iam/generate_bootstrap.py 从IAM契约及现有开发者目录生成，不要手工编辑。
-- 全部语句存在即跳过，不覆盖任何人工或业务修改；不含账号与凭证，
-- 新建行的保留号若已被占用，则落到所属区间内下一个空号。
-- 平台首个账号由 provider 冷启动器经安全框架注册用例创建。

-- 发号准备：静态保留标识全部小于起点，运行时发号不会与种子冲突。
INSERT INTO biz_leaf_alloc (biz_tag, max_id, step, description)
SELECT 'iam', 1000000, 100, 'IAM 冷启动发号' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM biz_leaf_alloc WHERE biz_tag = 'iam');

-- 应用：租户域基础应用在组织创建时缺省开通。
INSERT INTO iam_application (id, code, domain, name, description, sort_order, baseline, enabled)
SELECT 100001, 'iam-platform', 'PLATFORM', '平台治理', '平台域身份、目录与授权治理', 1, FALSE, TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_application WHERE code = 'iam-platform');

INSERT INTO iam_application (id, code, domain, name, description, sort_order, baseline, enabled)
SELECT 100002, 'iam-tenant', 'TENANT', '组织治理', '组织域成员、部门与授权治理', 1, TRUE, TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_application WHERE code = 'iam-tenant');

INSERT INTO iam_application (id, code, domain, name, description, sort_order, baseline, enabled)
SELECT 100003, 'platform:develop', 'PLATFORM', '开发者平台', '二维码、OAuth2客户端、社交配置与业务ID管理', 2, FALSE, TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_application WHERE code = 'platform:develop');

-- 资源：声明合法范围与字段能力，供角色发布与字段策略校验取值。
INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110001 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110001)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'account', '全局账号', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'account');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110002 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110002)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'action', '操作', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'action');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110003 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110003)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'application', '应用', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'application');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110004 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110004)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'assignment', '角色授权', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'assignment');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110005 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110005)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'audit', '审计记录', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'audit');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110006 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110006)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'authorization', '授权诊断', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'authorization');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110007 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110007)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'delegation', '授权委派', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'delegation');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110008 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110008)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'dictionary', '字典', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'dictionary');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110009 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110009)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'entitlement', '应用开通', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'entitlement');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110010 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110010)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'group', '用户组', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'group');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110011 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110011)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'member', '成员', CAST('["ALL", "SELF", "OBJECT_SET"]' AS JSON), CAST('[{"key": "displayName", "label": "显示名", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": true}, {"key": "avatar", "label": "头像", "visibilities": ["HIDDEN", "FULL"], "editable": true, "filterable": false, "sortable": false}, {"key": "phone", "label": "手机号", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": false}, {"key": "email", "label": "邮箱", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": false}]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'member');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110012 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110012)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'menu', '菜单', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'menu');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110013 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110013)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'plan', '套餐', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'plan');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110014 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110014)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'resource', '资源', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'resource');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110015 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110015)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'role', '角色', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'role');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110016 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110016)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'shared-role', '共享角色', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'shared-role');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110017 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110017)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'tenant', '组织', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'tenant');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110018 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110018)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'application', '应用', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'application');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110019 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110019)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'assignment', '角色授权', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'assignment');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110020 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110020)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'audience', '应用可用人群', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'audience');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110021 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110021)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'audit', '审计记录', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'audit');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110022 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110022)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'authorization', '授权诊断', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'authorization');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110023 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110023)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'delegation', '授权委派', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'delegation');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110024 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110024)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'department', '部门', CAST('["ALL", "MANAGED_DEPARTMENTS", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'department');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110025 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110025)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'directory', '通讯录', CAST('["ALL", "SELF", "MEMBER_DEPARTMENTS", "MANAGED_DEPARTMENTS", "OBJECT_SET"]' AS JSON), CAST('[{"key": "displayName", "label": "显示名", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": true}, {"key": "avatar", "label": "头像", "visibilities": ["HIDDEN", "FULL"], "editable": true, "filterable": false, "sortable": false}, {"key": "phone", "label": "手机号", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": false}, {"key": "email", "label": "邮箱", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": false}]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'directory');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110026 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110026)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'directory-policy', '通讯录可见范围策略', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'directory-policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110027 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110027)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'field-policy', '成员字段策略', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'field-policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110028 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110028)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'group', '用户组', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'group');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110029 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110029)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'member', '成员', CAST('["ALL", "SELF", "MEMBER_DEPARTMENTS", "MANAGED_DEPARTMENTS", "OBJECT_SET"]' AS JSON), CAST('[{"key": "displayName", "label": "显示名", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": true}, {"key": "avatar", "label": "头像", "visibilities": ["HIDDEN", "FULL"], "editable": true, "filterable": false, "sortable": false}, {"key": "phone", "label": "手机号", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": false}, {"key": "email", "label": "邮箱", "visibilities": ["HIDDEN", "MASKED", "FULL"], "editable": true, "filterable": true, "sortable": false}]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'member');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110030 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110030)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'policy', '策略预览', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'policy');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110031 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110031)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'role', '角色', CAST('["ALL", "OBJECT_SET"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'role');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110032 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110032)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'settings', '组织设置', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'settings');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110033 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110033)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'client', '客户端', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'platform:develop' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'client');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110034 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110034)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'id-allocation', '发号', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'platform:develop' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'id-allocation');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110035 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110035)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'qrcode', '二维码', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'platform:develop' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'qrcode');

INSERT INTO iam_resource (id, application_id, code, name, scope_capabilities, field_capabilities, enabled)
SELECT COALESCE((SELECT 110036 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_resource taken WHERE taken.id = 110036)), (SELECT COALESCE(MAX(taken.id), 110000) + 1 FROM iam_resource taken WHERE taken.id >= 110000 AND taken.id < 120000)), app.id, 'social-config', '社会化登录配置', CAST('["ALL"]' AS JSON), CAST('[]' AS JSON), TRUE FROM iam_application app
WHERE app.code = 'platform:develop' AND NOT EXISTS (SELECT 1 FROM iam_resource WHERE application_id = app.id AND code = 'social-config');

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
SELECT COALESCE((SELECT 120008 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120008)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:reset-password', '重置密码', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:reset-password');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120009 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120009)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:unlock', '解锁全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:unlock');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120010 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120010)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:account:update', '编辑全局账号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'account' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:account:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120011 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120011)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:action:create', '创建操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120012 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120012)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:action:delete', '删除操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120013 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120013)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:action:read', '查看操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120014 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120014)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:action:status', '启停操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120015 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120015)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:action:update', '编辑操作', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'action' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:action:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120016 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120016)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application:create', '创建应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120017 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120017)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application:delete', '删除应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120018 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120018)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application:purge', '强制清除应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:purge');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120019 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120019)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application:read', '查看应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120020 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120020)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application:status', '启停应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120021 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120021)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:application:update', '编辑应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:application:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120022 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120022)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:assignment:create', '创建角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:assignment:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120023 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120023)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:assignment:delete', '删除角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:assignment:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120024 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120024)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:assignment:read', '查看角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:assignment:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120025 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120025)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:assignment:update', '编辑角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:assignment:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120026 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120026)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:assignment:upgrade', '升级角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:assignment:upgrade');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120027 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120027)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:audit:read', '查看审计记录', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'audit' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:audit:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120028 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120028)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:authorization:diagnose', '诊断', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'authorization' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:authorization:diagnose');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120029 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120029)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:delegation:create', '创建授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:delegation:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120030 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120030)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:delegation:delete', '删除授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:delegation:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120031 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120031)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:delegation:preview', '预览授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:delegation:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120032 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120032)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:delegation:read', '查看授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:delegation:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120033 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120033)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:delegation:update', '编辑授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
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
SELECT COALESCE((SELECT 120038 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120038)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:entitlement:preview', '预览应用开通', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'entitlement' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:entitlement:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120039 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120039)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:entitlement:read', '查看应用开通', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'entitlement' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:entitlement:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120040 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120040)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:entitlement:update', '编辑应用开通', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'entitlement' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:entitlement:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120041 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120041)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:group:create', '创建用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120042 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120042)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:group:delete', '删除用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120043 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120043)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:group:preview', '预览用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120044 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120044)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:group:read', '查看用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120045 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120045)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:group:update', '编辑用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:group:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120046 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120046)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:member:create', '创建成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120047 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120047)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:member:read', '查看成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120048 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120048)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:member:remove', '移出成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:remove');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120049 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120049)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:member:status', '启停成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120050 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120050)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:member:update', '编辑成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:member:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120051 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120051)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:menu:create', '创建菜单', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'menu' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:menu:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120052 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120052)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:menu:delete', '删除菜单', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'menu' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:menu:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120053 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120053)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:menu:read', '查看菜单', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'menu' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:menu:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120054 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120054)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:menu:update', '编辑菜单', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'menu' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:menu:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120055 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120055)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:plan:create', '创建套餐', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'plan' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:plan:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120056 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120056)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:plan:read', '查看套餐', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'plan' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:plan:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120057 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120057)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:plan:update', '编辑套餐', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'plan' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:plan:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120058 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120058)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:resource:create', '创建资源', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'resource' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:resource:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120059 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120059)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:resource:delete', '删除资源', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'resource' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:resource:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120060 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120060)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:resource:read', '查看资源', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'resource' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:resource:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120061 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120061)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:resource:update', '编辑资源', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'resource' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:resource:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120062 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120062)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role:create', '创建角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120063 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120063)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role:delete', '删除角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120064 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120064)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role:preview', '预览角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120065 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120065)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role:publish', '发布版本角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:publish');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120066 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120066)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role:read', '查看角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120067 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120067)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:role:status', '启停角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:role:status');

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
SELECT COALESCE((SELECT 120071 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120071)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:shared-role:publish', '发布版本共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:publish');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120072 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120072)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:shared-role:read', '查看共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120073 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120073)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:shared-role:status', '启停共享角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'shared-role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:shared-role:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120074 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120074)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:tenant:create', '创建组织', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'tenant' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:tenant:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120075 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120075)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:tenant:preview', '预览组织', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'tenant' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:tenant:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120076 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120076)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:tenant:read', '查看组织', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'tenant' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:tenant:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120077 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120077)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:tenant:update', '编辑组织', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-platform' AND res.code = 'tenant' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:tenant:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120078 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120078)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:application:read', '查看应用', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'application' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:application:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120079 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120079)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:assignment:create', '创建角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:assignment:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120080 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120080)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:assignment:delete', '删除角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:assignment:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120081 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120081)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:assignment:read', '查看角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:assignment:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120082 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120082)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:assignment:update', '编辑角色授权', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'assignment' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:assignment:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120083 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120083)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:audience:read', '查看应用可用人群', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'audience' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:audience:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120084 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120084)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:audience:update', '编辑应用可用人群', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'audience' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:audience:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120085 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120085)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:audit:read', '查看审计记录', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'audit' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:audit:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120086 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120086)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:authorization:diagnose', '诊断', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'authorization' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:authorization:diagnose');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120087 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120087)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:delegation:create', '创建授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120088 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120088)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:delegation:delete', '删除授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120089 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120089)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:delegation:preview', '预览授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120090 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120090)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:delegation:read', '查看授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120091 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120091)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:delegation:update', '编辑授权委派', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'delegation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:delegation:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120092 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120092)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:department:create', '创建部门', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120093 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120093)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:department:delete', '删除部门', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120094 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120094)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:department:read', '查看部门', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120095 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120095)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:department:update', '编辑部门', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'department' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:department:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120096 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120096)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:directory:read', '查看通讯录', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'directory' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:directory:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120097 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120097)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:directory-policy:read', '查看通讯录可见范围策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'directory-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:directory-policy:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120098 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120098)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:directory-policy:update', '编辑通讯录可见范围策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'directory-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:directory-policy:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120099 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120099)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:field-policy:read', '查看成员字段策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'field-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:field-policy:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120100 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120100)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:field-policy:update', '编辑成员字段策略', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'field-policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:field-policy:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120101 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120101)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:group:create', '创建用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120102 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120102)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:group:delete', '删除用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120103 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120103)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:group:preview', '预览用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120104 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120104)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:group:read', '查看用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120105 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120105)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:group:update', '编辑用户组', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'group' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:group:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120106 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120106)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:create', '创建成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120107 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120107)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:departments', '调整任职', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:departments');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120108 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120108)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:export', '导出成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:export');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120109 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120109)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:read', '查看成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120110 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120110)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:remove', '移出成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:remove');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120111 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120111)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:status', '启停成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120112 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120112)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:member:update', '编辑成员', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'member' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:member:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120113 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120113)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:policy:preview', '预览策略预览', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'policy' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:policy:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120114 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120114)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:create', '创建角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120115 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120115)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:delete', '删除角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120116 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120116)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:preview', '预览角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:preview');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120117 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120117)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:publish', '发布版本角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:publish');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120118 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120118)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:read', '查看角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120119 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120119)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:status', '启停角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:status');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120120 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120120)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:role:upgrade', '升级角色', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'role' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:role:upgrade');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120121 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120121)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:settings:owner-transfer', '转交所有者', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'settings' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:settings:owner-transfer');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120122 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120122)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:settings:read', '查看组织设置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'settings' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:settings:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120123 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120123)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-tenant:settings:update', '编辑组织设置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'iam-tenant' AND res.code = 'settings' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-tenant:settings:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120124 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120124)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:develop:client:create', '创建客户端', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'client' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:develop:client:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120125 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120125)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:develop:client:delete', '删除客户端', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'client' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:develop:client:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120126 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120126)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:develop:client:detail', '查看客户端详情', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'client' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:develop:client:detail');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120127 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120127)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:develop:client:query', '查看客户端', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'client' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:develop:client:query');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120128 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120128)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:develop:client:reset', '重置客户端密钥', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'client' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:develop:client:reset');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120129 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120129)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:develop:client:update', '编辑客户端', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'client' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:develop:client:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120130 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120130)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:id-allocation:create', '创建发号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'id-allocation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:id-allocation:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120131 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120131)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:id-allocation:delete', '删除发号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'id-allocation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:id-allocation:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120132 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120132)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:id-allocation:read', '查看发号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'id-allocation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:id-allocation:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120133 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120133)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:id-allocation:update', '编辑发号', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'id-allocation' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:id-allocation:update');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120134 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120134)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'platform:develop:qrcode', '生成二维码', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'qrcode' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'platform:develop:qrcode');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120135 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120135)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:social-config:create', '创建社会化登录配置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'social-config' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:social-config:create');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120136 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120136)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:social-config:delete', '删除社会化登录配置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'social-config' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:social-config:delete');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120137 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120137)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:social-config:read', '查看社会化登录配置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'social-config' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:social-config:read');

INSERT INTO iam_action (id, application_id, resource_id, code, name, enabled)
SELECT COALESCE((SELECT 120138 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_action taken WHERE taken.id = 120138)), (SELECT COALESCE(MAX(taken.id), 120000) + 1 FROM iam_action taken WHERE taken.id >= 120000 AND taken.id < 130000)), res.application_id, res.id, 'iam-platform:social-config:update', '编辑社会化登录配置', TRUE FROM iam_resource res JOIN iam_application app ON app.id = res.application_id
WHERE app.code = 'platform:develop' AND res.code = 'social-config' AND NOT EXISTS (SELECT 1 FROM iam_action WHERE code = 'iam-platform:social-config:update');

-- 菜单：目录携带子页操作并集，子页全部不可见时目录随之隐藏。
INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130001 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130001)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '组织与租户', '/platform/iam/tenant', 'layout.main', 'platform.iam.tenant', 'DIRECTORY', 'ANY', 'ACTION', 1, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'platform.iam.tenant');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130002 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130002)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '租户管理', '/platform/iam/tenants', 'platform.iam.tenants', 'platform.iam.tenants', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.tenants');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130003 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130003)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '应用与配置', '/platform/iam/config', 'layout.main', 'platform.iam.config', 'DIRECTORY', 'ANY', 'ACTION', 2, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'platform.iam.config');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130004 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130004)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '应用目录', '/platform/iam/applications', 'platform.iam.applications', 'platform.iam.applications', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.config' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.applications');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130005 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130005)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '套餐', '/platform/iam/plans', 'platform.iam.plans', 'platform.iam.plans', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.config' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.plans');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130006 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130006)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '共享角色', '/platform/iam/shared/roles', 'platform.iam.shared.roles', 'platform.iam.shared.roles', 'PAGE', 'ANY', 'ACTION', 3, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.config' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.shared.roles');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130007 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130007)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '平台管理', '/platform/iam/manage', 'layout.main', 'platform.iam.manage', 'DIRECTORY', 'ANY', 'ACTION', 3, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'platform.iam.manage');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130008 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130008)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '全局账号', '/platform/iam/accounts', 'platform.iam.accounts', 'platform.iam.accounts', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.manage' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.accounts');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130009 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130009)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '平台人员', '/platform/iam/personnel', 'platform.iam.personnel', 'platform.iam.personnel', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.manage' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.personnel');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130010 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130010)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '角色与授权', '/platform/iam/authorization', 'platform.iam.authorization', 'platform.iam.authorization', 'PAGE', 'ANY', 'ACTION', 3, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.manage' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.iam.authorization');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130011 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130011)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '安全与运维', '/platform/iam/security', 'layout.main', 'platform.iam.security', 'DIRECTORY', 'ANY', 'ACTION', 4, TRUE FROM iam_application app
WHERE app.code = 'iam-platform' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'platform.iam.security');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130012 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130012)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '事件与审计', '/security/iam/authorization/audit', 'security.iam.authorization.audit', 'security.iam.authorization.audit', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-platform' AND parent.route_name = 'platform.iam.security' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'security.iam.authorization.audit');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130013 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130013)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '组织管理', '/org/iam/organization', 'layout.main', 'org.iam.organization', 'DIRECTORY', 'ANY', 'ACTION', 1, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'org.iam.organization');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130014 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130014)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '成员与部门', '/org/iam/members', 'org.iam.members', 'org.iam.members', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.organization' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.members');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130015 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130015)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '用户组', '/org/iam/groups', 'org.iam.groups', 'org.iam.groups', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.organization' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.groups');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130016 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130016)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '组织设置', '/org/iam/settings', 'org.iam.settings', 'org.iam.settings', 'PAGE', 'ANY', 'ACTION', 3, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.organization' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.settings');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130017 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130017)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '权限与应用', '/org/iam/authorization/root', 'layout.main', 'org.iam.authorization.root', 'DIRECTORY', 'ANY', 'ACTION', 2, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'org.iam.authorization.root');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130018 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130018)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '角色与授权', '/org/iam/authorization', 'org.iam.authorization', 'org.iam.authorization', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.authorization.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.authorization');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130019 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130019)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '应用管理', '/org/iam/applications', 'org.iam.applications', 'org.iam.applications', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.authorization.root' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.applications');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130020 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130020)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '安全与合规', '/org/iam/compliance', 'layout.main', 'org.iam.compliance', 'DIRECTORY', 'ANY', 'ACTION', 3, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'org.iam.compliance');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130021 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130021)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '成员权限', '/security/iam/member/permissions', 'security.iam.member.permissions', 'security.iam.member.permissions', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.compliance' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'security.iam.member.permissions');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130022 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130022)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '审计', '/security/iam/authorization/audit', 'security.iam.authorization.audit', 'security.iam.authorization.audit', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.compliance' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'security.iam.authorization.audit');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130023 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130023)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '工作台', '/org/iam/workspace', 'layout.main', 'org.iam.workspace', 'DIRECTORY', 'ANY', 'ACTION', 4, TRUE FROM iam_application app
WHERE app.code = 'iam-tenant' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'org.iam.workspace');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130024 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130024)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '工作台', '/org/iam/workbench', 'org.iam.workbench', 'org.iam.workbench', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.workspace' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.workbench');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130025 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130025)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '通讯录', '/org/iam/directory', 'org.iam.directory', 'org.iam.directory', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'iam-tenant' AND parent.route_name = 'org.iam.workspace' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'org.iam.directory');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130026 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130026)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), app.id, NULL, '开发者平台', '/platform/develop', 'layout.main', 'platform.develop', 'DIRECTORY', 'ANY', 'ACTION', 1, TRUE FROM iam_application app
WHERE app.code = 'platform:develop' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = app.id AND route_name = 'platform.develop');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130027 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130027)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '生成二维码', '/platform/develop/qrcode', 'platform.develop.qrcode', 'platform.develop.qrcode', 'PAGE', 'ANY', 'ACTION', 1, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform:develop' AND parent.route_name = 'platform.develop' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.develop.qrcode');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130028 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130028)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '客户端管理', '/platform/develop/client', 'platform.develop.client', 'platform.develop.client', 'PAGE', 'ANY', 'ACTION', 2, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform:develop' AND parent.route_name = 'platform.develop' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.develop.client');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130029 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130029)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '社交管理', '/platform/develop/social', 'platform.develop.social', 'platform.develop.social', 'PAGE', 'ANY', 'ACTION', 3, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform:develop' AND parent.route_name = 'platform.develop' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.develop.social');

INSERT INTO iam_menu (id, application_id, parent_id, name, path, view_path, route_name, kind, match_mode, access_mode, sort_order, enabled)
SELECT COALESCE((SELECT 130030 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_menu taken WHERE taken.id = 130030)), (SELECT COALESCE(MAX(taken.id), 130000) + 1 FROM iam_menu taken WHERE taken.id >= 130000 AND taken.id < 140000)), parent.application_id, parent.id, '业务ID管理', '/platform/develop/id', 'platform.develop.id', 'platform.develop.id', 'PAGE', 'ANY', 'ACTION', 4, TRUE FROM iam_menu parent JOIN iam_application app ON app.id = parent.application_id
WHERE app.code = 'platform:develop' AND parent.route_name = 'platform.develop' AND NOT EXISTS (SELECT 1 FROM iam_menu WHERE application_id = parent.application_id AND route_name = 'platform.develop.id');

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
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:assignment:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:delegation:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:member:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.manage' AND action.code = 'iam-platform:role:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.accounts' AND action.code = 'iam-platform:account:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.personnel' AND action.code = 'iam-platform:member:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.authorization' AND action.code = 'iam-platform:assignment:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.authorization' AND action.code = 'iam-platform:delegation:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.authorization' AND action.code = 'iam-platform:role:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'platform.iam.security' AND action.code = 'iam-platform:audit:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-platform' AND menu.route_name = 'security.iam.authorization.audit' AND action.code = 'iam-platform:audit:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

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
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.members' AND action.code = 'iam-tenant:department:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.members' AND action.code = 'iam-tenant:member:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

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
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization.root' AND action.code = 'iam-tenant:assignment:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization.root' AND action.code = 'iam-tenant:delegation:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization.root' AND action.code = 'iam-tenant:role:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization' AND action.code = 'iam-tenant:assignment:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization' AND action.code = 'iam-tenant:delegation:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.authorization' AND action.code = 'iam-tenant:role:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.applications' AND action.code = 'iam-tenant:application:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.compliance' AND action.code = 'iam-tenant:audit:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.compliance' AND action.code = 'iam-tenant:directory-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.compliance' AND action.code = 'iam-tenant:field-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'security.iam.member.permissions' AND action.code = 'iam-tenant:directory-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'security.iam.member.permissions' AND action.code = 'iam-tenant:field-policy:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'security.iam.authorization.audit' AND action.code = 'iam-tenant:audit:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.workspace' AND action.code = 'iam-tenant:application:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.workspace' AND action.code = 'iam-tenant:directory:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.workbench' AND action.code = 'iam-tenant:application:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'iam-tenant' AND menu.route_name = 'org.iam.directory' AND action.code = 'iam-tenant:directory:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:develop' AND menu.route_name = 'platform.develop' AND action.code = 'iam-platform:id-allocation:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:develop' AND menu.route_name = 'platform.develop' AND action.code = 'iam-platform:social-config:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:develop' AND menu.route_name = 'platform.develop' AND action.code = 'platform:develop:client:query' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:develop' AND menu.route_name = 'platform.develop' AND action.code = 'platform:develop:qrcode' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:develop' AND menu.route_name = 'platform.develop.qrcode' AND action.code = 'platform:develop:qrcode' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:develop' AND menu.route_name = 'platform.develop.client' AND action.code = 'platform:develop:client:query' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:develop' AND menu.route_name = 'platform.develop.social' AND action.code = 'iam-platform:social-config:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

INSERT INTO iam_menu_action (application_id, menu_id, action_id)
SELECT menu.application_id, menu.id, action.id FROM iam_menu menu JOIN iam_application app ON app.id = menu.application_id JOIN iam_action action ON action.application_id = menu.application_id
WHERE app.code = 'platform:develop' AND menu.route_name = 'platform.develop.id' AND action.code = 'iam-platform:id-allocation:read' AND NOT EXISTS (SELECT 1 FROM iam_menu_action WHERE application_id = menu.application_id AND menu_id = menu.id AND action_id = action.id);

CREATE TEMPORARY TABLE IF NOT EXISTS iam_bootstrap_new_revision (domain VARCHAR(16) PRIMARY KEY);
DELETE FROM iam_bootstrap_new_revision;
INSERT INTO iam_bootstrap_new_revision(domain) SELECT 'PLATFORM' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_role_revision v JOIN iam_role_definition r ON r.id=v.role_id WHERE r.domain='PLATFORM' AND r.code='platform-governance' AND r.tenant_key=0 AND v.revision=1);
INSERT INTO iam_bootstrap_new_revision(domain) SELECT 'TENANT' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM iam_role_revision v JOIN iam_role_definition r ON r.id=v.role_id WHERE r.domain='TENANT' AND r.code='tenant-governance' AND r.tenant_key=0 AND v.revision=1);
-- 治理角色：每个域恰好一个启用的 SYSTEM 角色，组织初始化依赖这一唯一性。
INSERT INTO iam_role_definition (id, domain, tenant_id, kind, code, name, description, enabled)
SELECT 140001, 'PLATFORM', NULL, 'SYSTEM', 'platform-governance', '平台治理', '平台域全部治理操作', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_role_definition WHERE domain = 'PLATFORM' AND tenant_key = 0 AND code = 'platform-governance');

INSERT INTO iam_role_revision (id, role_id, kind, revision, base_revision_id, metadata_overrides, resource_field_permissions)
SELECT 141001, role.id, 'SYSTEM', 1, NULL, CAST('{}' AS JSON), (SELECT JSON_OBJECT(CAST(resource.id AS CHAR), CAST('{"displayName": {"visibility": "FULL", "editable": true}, "avatar": {"visibility": "FULL", "editable": true}, "phone": {"visibility": "MASKED", "editable": false}, "email": {"visibility": "MASKED", "editable": false}}' AS JSON)) FROM iam_resource resource JOIN iam_application app ON app.id=resource.application_id WHERE app.code='iam-platform' AND resource.code='member') FROM iam_role_definition role
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND NOT EXISTS (SELECT 1 FROM iam_role_revision WHERE role_id = role.id AND revision = 1);

INSERT INTO iam_role_definition (id, domain, tenant_id, kind, code, name, description, enabled)
SELECT 140002, 'TENANT', NULL, 'SYSTEM', 'tenant-governance', '组织治理', '组织所有者的全部治理操作', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_role_definition WHERE domain = 'TENANT' AND tenant_key = 0 AND code = 'tenant-governance');

INSERT INTO iam_role_revision (id, role_id, kind, revision, base_revision_id, metadata_overrides, resource_field_permissions)
SELECT 141002, role.id, 'SYSTEM', 1, NULL, CAST('{}' AS JSON), CAST('{}' AS JSON) FROM iam_role_definition role
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND NOT EXISTS (SELECT 1 FROM iam_role_revision WHERE role_id = role.id AND revision = 1);

-- 治理授权：治理角色覆盖本域全部操作，范围为全域。
INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:disable' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:enable' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:lock' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:lookup' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:reset-password' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:unlock' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:account:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:action:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:action:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:action:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:action:status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:action:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application:purge' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application:status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:application:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:assignment:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:assignment:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:assignment:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:assignment:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:assignment:upgrade' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:audit:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:authorization:diagnose' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:delegation:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:delegation:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:delegation:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:delegation:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:delegation:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:dictionary:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:dictionary:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:dictionary:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:dictionary:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:entitlement:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:entitlement:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:entitlement:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:group:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:group:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:group:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:group:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:group:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:id-allocation:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:id-allocation:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:id-allocation:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:id-allocation:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:member:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:member:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:member:remove' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:member:status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:member:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:menu:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:menu:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:menu:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:menu:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:plan:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:plan:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:plan:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:resource:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:resource:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:resource:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:resource:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role:publish' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:role:status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:shared-role:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:shared-role:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:shared-role:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:shared-role:publish' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:shared-role:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:shared-role:status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:social-config:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:social-config:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:social-config:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:social-config:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:tenant:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:tenant:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:tenant:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-platform:tenant:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform:develop:client:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform:develop:client:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform:develop:client:detail' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform:develop:client:query' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform:develop:client:reset' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform:develop:client:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'PLATFORM' AND role.tenant_key = 0 AND role.code = 'platform-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'platform:develop:qrcode' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:application:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:assignment:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:assignment:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:assignment:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:assignment:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:audience:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:audience:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:audit:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:authorization:diagnose' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:delegation:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:delegation:delete' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:delegation:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:delegation:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

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
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:department:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:department:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:directory-policy:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:directory-policy:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:directory:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

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
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:member:create' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:member:departments' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

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
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:member:status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:member:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:policy:preview' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

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
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role:publish' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role:status' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:role:upgrade' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:settings:owner-transfer' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:settings:read' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

INSERT INTO iam_role_grant (revision_id, action_id, scopes)
SELECT revision.id, action.id, CAST('[{"kind": "ALL"}]' AS JSON) FROM iam_role_revision revision JOIN iam_role_definition role ON role.id = revision.role_id JOIN iam_action action JOIN iam_application app ON app.id = action.application_id
WHERE role.domain = 'TENANT' AND role.tenant_key = 0 AND role.code = 'tenant-governance' AND revision.revision = 1 AND app.domain = role.domain AND action.code = 'iam-tenant:settings:update' AND EXISTS(SELECT 1 FROM iam_bootstrap_new_revision n WHERE n.domain=role.domain) AND NOT EXISTS (SELECT 1 FROM iam_role_grant WHERE revision_id = revision.id AND action_id = action.id);

-- 默认策略版本：固定版本被租户策略引用；通讯录默认全组织，字段默认脱敏手机邮箱，上限缺省不额外收紧。
INSERT INTO iam_default_policy_revision (id, kind, revision, definition)
SELECT 150001, 'DIRECTORY', 1, CAST('{"scope": "ALL"}' AS JSON) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_default_policy_revision WHERE kind = 'DIRECTORY' AND revision = 1);

INSERT INTO iam_default_policy_revision (id, kind, revision, definition)
SELECT 150002, 'FIELD', 1, CAST('{"fields": {"displayName": {"visibility": "FULL", "editable": true}, "avatar": {"visibility": "FULL", "editable": true}, "phone": {"visibility": "MASKED", "editable": false}, "email": {"visibility": "MASKED", "editable": false}}}' AS JSON) FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM iam_default_policy_revision WHERE kind = 'FIELD' AND revision = 1);

-- 恢复调用者原有的会话外键设置。
SET SESSION FOREIGN_KEY_CHECKS = @iam_init_previous_foreign_key_checks;
