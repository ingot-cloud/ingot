-- 021 当前角色授权：仅结构扩展，停止所有写入并留存可恢复备份。
-- MySQL DDL自动提交；中断后按information_schema逐项检查，禁止 --force。
SET time_zone = '+00:00';
ALTER TABLE iam_role_definition ADD COLUMN current_revision_id BIGINT UNSIGNED NULL,
    ADD UNIQUE KEY uk_iam_role_owner(domain,tenant_key,id,kind);
ALTER TABLE iam_role_revision ADD UNIQUE KEY uk_iam_revision_owner(id,role_id);
ALTER TABLE iam_role_parameter ADD COLUMN resource_id BIGINT UNSIGNED NULL,
    ADD CONSTRAINT fk_iam_parameter_resource FOREIGN KEY(resource_id) REFERENCES iam_resource(id);
ALTER TABLE iam_role_definition ADD CONSTRAINT fk_iam_role_current
    FOREIGN KEY(current_revision_id,id) REFERENCES iam_role_revision(id,role_id);
ALTER TABLE iam_role_assignment ADD COLUMN role_id BIGINT UNSIGNED NULL,
    ADD COLUMN role_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD COLUMN role_tenant_key BIGINT UNSIGNED NULL,
    MODIFY revision_id BIGINT UNSIGNED NULL,
    MODIFY revision_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD KEY idx_iam_assignment_current_role(role_id,status),
    ADD CONSTRAINT fk_iam_assignment_current_role FOREIGN KEY(domain,role_tenant_key,role_id,role_kind)
        REFERENCES iam_role_definition(domain,tenant_key,id,kind);
CREATE TABLE iam_delegation_role (
    delegation_id BIGINT UNSIGNED NOT NULL,
    tenant_id BIGINT UNSIGNED NOT NULL,
    domain VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'TENANT',
    role_id BIGINT UNSIGNED NOT NULL,
    role_kind VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    role_tenant_key BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (delegation_id, role_id),
    CONSTRAINT fk_iam_live_delegation FOREIGN KEY (tenant_id, delegation_id) REFERENCES iam_delegation_grant (tenant_id, id),
    CONSTRAINT fk_iam_live_delegation_role FOREIGN KEY (domain, role_tenant_key, role_id, role_kind)
        REFERENCES iam_role_definition (domain, tenant_key, id, kind),
    CONSTRAINT ck_iam_live_delegation_role CHECK (domain='TENANT' AND
        ((role_kind='SHARED' AND role_tenant_key=0) OR (role_kind='TENANT_CUSTOM' AND role_tenant_key=tenant_id)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE iam_delegation_resource_ceiling (
    delegation_id BIGINT UNSIGNED NOT NULL,
    resource_id BIGINT UNSIGNED NOT NULL,
    scopes JSON NOT NULL,
    scope_bindings JSON NOT NULL,
    PRIMARY KEY (delegation_id, resource_id),
    CONSTRAINT fk_iam_live_resource_delegation FOREIGN KEY (delegation_id) REFERENCES iam_delegation_grant (id),
    CONSTRAINT fk_iam_live_resource FOREIGN KEY (resource_id) REFERENCES iam_resource (id),
    CONSTRAINT ck_iam_live_resource_scopes CHECK (JSON_TYPE(scopes)='ARRAY'),
    CONSTRAINT ck_iam_live_resource_bindings CHECK (JSON_TYPE(scope_bindings)='OBJECT')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 此时保留旧记录；正转换成功后安装ck_iam_assignment_current_role，不在扩展阶段拒绝旧分配。
CREATE TABLE iam_live_authorization_batch (
 batch_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 source_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 reverse_digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
 state VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 decisions JSON NOT NULL DEFAULT (JSON_ARRAY()),
 applied_at DATETIME(6) NULL,
 CONSTRAINT ck_iam_live_batch_state CHECK(state IN ('FORWARD','REVERSE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE iam_live_authorization_mapping (
 batch_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 entity_type VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 entity_id BIGINT UNSIGNED NOT NULL,
 source_row JSON NOT NULL,
 target_id BIGINT UNSIGNED NULL,
 PRIMARY KEY(batch_id,entity_type,entity_id),
 FOREIGN KEY(batch_id) REFERENCES iam_live_authorization_batch(batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
