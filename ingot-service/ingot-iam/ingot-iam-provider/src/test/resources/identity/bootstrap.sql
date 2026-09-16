-- H2 JDBC 夹具，仅包含冷启动流程读写的列；正式 MySQL DDL 与种子由独立容器测试验证。
CREATE TABLE iam_account(id BIGINT PRIMARY KEY, username VARCHAR(64) UNIQUE, password_hash VARCHAR(300),
    phone VARCHAR(32), email VARCHAR(128), enabled BOOLEAN DEFAULT TRUE, must_change_password BOOLEAN DEFAULT FALSE,
    password_changed_at TIMESTAMP, last_login_at TIMESTAMP, version BIGINT DEFAULT 0,
    created_at TIMESTAMP, updated_at TIMESTAMP, deleted_at TIMESTAMP);
CREATE TABLE iam_platform_member(id BIGINT PRIMARY KEY, account_id BIGINT REFERENCES iam_account(id) UNIQUE,
    display_name VARCHAR(128), avatar VARCHAR(512), status VARCHAR(16), version BIGINT DEFAULT 0);
CREATE TABLE iam_role_definition(id BIGINT PRIMARY KEY, domain VARCHAR(16), tenant_id BIGINT, kind VARCHAR(24),
    code VARCHAR(128), name VARCHAR(128), description VARCHAR(512), enabled BOOLEAN DEFAULT TRUE,
    version BIGINT DEFAULT 0);
CREATE TABLE iam_role_revision(id BIGINT PRIMARY KEY, role_id BIGINT REFERENCES iam_role_definition(id),
    kind VARCHAR(24), revision BIGINT DEFAULT 1, base_revision_id BIGINT, metadata_overrides VARCHAR(1024));
CREATE TABLE iam_role_assignment(id BIGINT PRIMARY KEY, domain VARCHAR(16), tenant_id BIGINT,
    subject_type VARCHAR(16), platform_member_id BIGINT REFERENCES iam_platform_member(id), platform_group_id BIGINT,
    tenant_member_id BIGINT, tenant_group_id BIGINT, revision_id BIGINT REFERENCES iam_role_revision(id),
    revision_kind VARCHAR(24), scope_bindings VARCHAR(1024), delegation_grant_id BIGINT, valid_from TIMESTAMP,
    valid_until TIMESTAMP, status VARCHAR(16) DEFAULT 'ACTIVE', source VARCHAR(16), version BIGINT DEFAULT 0);
-- 冷启动种子提供的固定治理目录：每域恰好一个启用的系统角色及其版本。
INSERT INTO iam_role_definition(id,domain,tenant_id,kind,code,name) VALUES
    (140001,'PLATFORM',NULL,'SYSTEM','platform-governance','平台治理'),
    (140002,'TENANT',NULL,'SYSTEM','tenant-governance','组织治理');
INSERT INTO iam_role_revision(id,role_id,kind,revision,metadata_overrides) VALUES
    (141001,140001,'SYSTEM',1,'{}'),
    (141002,140002,'SYSTEM',1,'{}');
