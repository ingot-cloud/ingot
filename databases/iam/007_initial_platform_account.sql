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
