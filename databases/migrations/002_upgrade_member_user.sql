-- ============================================================
-- Member 用户表改造脚本（baseline 对齐）
-- 版本: V2.0
-- 日期: 2026-02-14
-- 说明: Member 用户表结构与 PMS 对齐，仅做基线字段拆分 / 迁移
--       本脚本不再创建 account_lock_state / account_security_event /
--       password_history / password_expiration 等账号域/凭证策略表，
--       这些表的 DDL 随 ingot-account-adapter / ingot-security-credential-data
--       模块的依赖 SQL 一并管理，未启用对应模块的服务无需创建。
-- ============================================================

USE ingot_member;

-- ============================================================
-- Step 1: 添加新字段
-- ============================================================
ALTER TABLE member_user
  -- 凭证管理
  ADD COLUMN must_change_pwd TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必须修改密码（0-否 1-是）' AFTER password,
  ADD COLUMN password_changed_at DATETIME DEFAULT NULL COMMENT '密码最后修改时间' AFTER must_change_pwd,

  -- 状态控制
  ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用（0-禁用 1-启用）' AFTER avatar,
  ADD COLUMN locked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否锁定（冗余字段，详情见 account_lock_state）' AFTER enabled,

  -- 登录审计
  ADD COLUMN last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间' AFTER locked,
  ADD COLUMN last_login_ip VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP' AFTER last_login_at,

  -- 并发控制
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER last_login_ip;

-- ============================================================
-- Step 2: 数据迁移
-- ============================================================
UPDATE member_user SET
  must_change_pwd = COALESCE(init_pwd, 1),
  enabled = CASE WHEN status = '0' THEN 1 ELSE 0 END,
  locked = CASE WHEN status = '9' THEN 1 ELSE 0 END,
  password_changed_at = COALESCE(updated_at, created_at);

-- ============================================================
-- Step 3: 添加索引
-- ============================================================
ALTER TABLE member_user
  ADD INDEX idx_enabled (enabled) USING BTREE COMMENT '启用状态索引',
  ADD INDEX idx_locked (locked) USING BTREE COMMENT '锁定状态索引',
  ADD INDEX idx_last_login (last_login_at) USING BTREE COMMENT '最后登录时间索引';

-- ============================================================
-- Step 4: 调整唯一索引
-- ============================================================
ALTER TABLE member_user DROP INDEX idx_username;
ALTER TABLE member_user
  ADD UNIQUE KEY uk_username (username, (COALESCE(deleted_at, 0))) COMMENT '用户名全局唯一（软删除友好）';

-- ============================================================
-- Step 5: 标记旧字段为废弃（与 001_upgrade_sys_user.sql 一致，保留 3 个月逐步迁移）
-- ============================================================
ALTER TABLE member_user
  MODIFY COLUMN init_pwd TINYINT(1) DEFAULT 1 COMMENT '【废弃 2026-05-14】请使用 must_change_pwd',
  MODIFY COLUMN status CHAR(1) DEFAULT '0' COMMENT '【废弃 2026-05-14】请使用 enabled 和 locked';

-- ============================================================
-- 完成
-- ============================================================
SELECT '002_upgrade_member_user.sql 执行完成' AS message,
       (SELECT COUNT(*) FROM member_user) AS '会员用户数';
