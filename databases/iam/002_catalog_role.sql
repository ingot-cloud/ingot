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
    published_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_revision_number (role_id, revision),
    UNIQUE KEY uk_iam_revision_kind (id, kind),
    CONSTRAINT fk_iam_revision_role FOREIGN KEY (role_id, kind) REFERENCES iam_role_definition (id, kind),
    CONSTRAINT fk_iam_revision_base FOREIGN KEY (base_revision_id, base_kind) REFERENCES iam_role_revision (id, kind),
    CONSTRAINT ck_iam_revision_number CHECK (revision > 0),
    CONSTRAINT ck_iam_revision_base CHECK (base_revision_id IS NULL OR (kind = 'TENANT_CUSTOM' AND base_revision_id <> id)),
    CONSTRAINT ck_iam_revision_metadata CHECK (JSON_TYPE(metadata_overrides) = 'OBJECT')
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
