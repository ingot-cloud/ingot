-- 回退 App 用户管理发布。停写并选择目标 IAM 库，执行后刷新授权与路由缓存。
-- 保留目录主键、角色版本及授权引用。重新上线时经 IAM 校验目录后显式启用。
START TRANSACTION;
UPDATE iam_application
SET enabled = FALSE, version = version + 1
WHERE code = 'platform:member' AND domain = 'PLATFORM' AND enabled = TRUE;
COMMIT;
