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
