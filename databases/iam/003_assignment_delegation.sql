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
    max_assignment_duration_seconds BIGINT UNSIGNED NOT NULL,
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
        (max_assignment_duration_seconds > 0 OR max_assignment_duration_nanos > 0))
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
