-- ============================================================
-- password_expiration 增加 force_change 字段
-- 版本: V2.0
-- 日期: 2026-07-17
-- 说明: 凭证安全收口（L1）——凭证域强制改密标记，与账号域 must_change_pwd 语义对齐
-- ============================================================

USE ingot_core;

-- ============================================================
-- Step 1: 添加 force_change 字段
-- ============================================================
ALTER TABLE password_expiration
  ADD COLUMN force_change TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否强制修改（0-否 1-是）' AFTER expires_at;

-- ============================================================
-- Step 2: 存量数据对齐——已标记必须改密的用户同步置位
-- ============================================================
UPDATE password_expiration pe
  JOIN sys_user su ON su.id = pe.user_id
  SET pe.force_change = 1
  WHERE su.must_change_pwd = 1;

-- ============================================================
-- 完成
-- ============================================================
SELECT '008_add_force_change_password_expiration.sql 执行完成' AS message;
