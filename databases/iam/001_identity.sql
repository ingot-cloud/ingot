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
