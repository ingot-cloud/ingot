-- 人工认证夹具，不是生产数据，不进入迁移导入。
-- 先在隔离库执行 001–005 及 006_bootstrap.sql，再执行本文件一次。默认口令均为 password。
-- Spring BCrypt 示例哈希对应明文 password。
-- 目录、治理角色、默认策略与发号高水位一律来自 006_bootstrap.sql，本文件只补两个可登录账号。

INSERT INTO iam_account (id, username, password_hash, phone, enabled, must_change_password)
VALUES
    (900001, 'platform', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '13800000001', TRUE, FALSE),
    (900002, 'owner', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '13800000002', TRUE, FALSE);

INSERT INTO iam_platform_member (id, account_id, display_name, status)
VALUES (910001, 900001, '平台治理', 'ACTIVE');

-- 治理授权引用冷启动种子写入的平台域系统角色版本，不在此处新建角色或版本。
INSERT INTO iam_role_assignment
    (id, domain, tenant_id, subject_type, platform_member_id, revision_id, revision_kind, scope_bindings, valid_from, source, status)
SELECT 940001, 'PLATFORM', NULL, 'MEMBER', 910001, revision.id, 'SYSTEM', JSON_OBJECT(), UTC_TIMESTAMP(6), 'MANUAL', 'ACTIVE'
FROM iam_role_revision revision
JOIN iam_role_definition role ON role.id = revision.role_id
WHERE role.domain = 'PLATFORM' AND role.kind = 'SYSTEM' AND role.enabled = TRUE
ORDER BY revision.revision DESC, revision.id DESC
LIMIT 1;
