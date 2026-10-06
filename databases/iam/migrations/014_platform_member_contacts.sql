-- 已有 IAM 库的一次性升级；先停止 IAM 写入并备份，再执行本脚本。
-- 仅适用于 iam_platform_member 尚无 phone/email 两列的库；新建库不执行。
-- 重复执行 ALTER 会失败，防止再次回填覆盖已经独立维护的联系方式。
ALTER TABLE iam_platform_member
    ADD COLUMN phone VARCHAR(32) NULL COMMENT '平台联系资料，不修改全局登录手机号' AFTER avatar,
    ADD COLUMN email VARCHAR(128) NULL COMMENT '平台联系资料，不修改全局账号邮箱' AFTER phone;

-- 保留当前可见联系方式作为一次性初值；不修改账号或租户成员资料。
UPDATE iam_platform_member member
JOIN iam_account account ON account.id = member.account_id
SET member.phone = account.phone,
    member.email = account.email
WHERE account.deleted_at IS NULL;
