-- ============================================================
-- 用户表改造脚本 - 精简版
-- 版本: V2.0
-- 日期: 2026-02-13
-- 说明: 添加新字段（精简版），支持账号生命周期管理
-- ============================================================

USE ingot_core;

-- ============================================================
-- Step 1: 添加新字段（17个字段的精简版）
-- ============================================================
ALTER TABLE sys_user
  -- 凭证管理
  ADD COLUMN must_change_pwd TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必须修改密码（0-否 1-是）' AFTER password,
  ADD COLUMN password_changed_at DATETIME DEFAULT NULL COMMENT '密码最后修改时间' AFTER must_change_pwd,
  
  -- 状态控制（精简版 - 只保留enabled和locked冗余字段）
  ADD COLUMN enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用（0-禁用 1-启用）' AFTER avatar,
  ADD COLUMN locked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否锁定（冗余字段，详情见 account_lock_state）' AFTER enabled,
  
  -- 登录审计（精简版）
  ADD COLUMN last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间' AFTER locked,
  ADD COLUMN last_login_ip VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP' AFTER last_login_at,
  
  -- 并发控制
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER last_login_ip;

-- ============================================================
-- Step 2: 数据迁移
-- ============================================================
-- 从旧字段迁移数据到新字段
UPDATE sys_user SET 
  must_change_pwd = COALESCE(init_pwd, 1),
  enabled = CASE WHEN status = '0' THEN 1 ELSE 0 END,
  locked = CASE WHEN status = '9' THEN 1 ELSE 0 END,
  password_changed_at = COALESCE(updated_at, created_at);

-- ============================================================
-- Step 3: 添加索引
-- ============================================================
ALTER TABLE sys_user
  ADD INDEX idx_enabled (enabled) USING BTREE COMMENT '启用状态索引',
  ADD INDEX idx_locked (locked) USING BTREE COMMENT '锁定状态索引',
  ADD INDEX idx_last_login (last_login_at) USING BTREE COMMENT '最后登录时间索引';

-- ============================================================
-- Step 4: 调整唯一索引（支持软删除后重用用户名）
-- ============================================================
-- 删除旧索引
ALTER TABLE sys_user DROP INDEX idx_username;

-- 添加新的唯一索引（全局唯一，软删除友好）
-- 注意：使用 COALESCE(deleted_at, 0) 实现软删除后可重用用户名
ALTER TABLE sys_user 
  ADD UNIQUE KEY uk_username (username, (COALESCE(deleted_at, 0))) COMMENT '用户名全局唯一（软删除友好）';

-- ============================================================
-- Step 5: 标记旧字段为废弃（保留3个月，逐步迁移）
-- ============================================================
ALTER TABLE sys_user 
  MODIFY COLUMN init_pwd TINYINT(1) DEFAULT 1 COMMENT '【废弃 2026-05-13】请使用 must_change_pwd',
  MODIFY COLUMN status CHAR(1) DEFAULT '0' COMMENT '【废弃 2026-05-13】请使用 enabled 和 locked';

-- ============================================================
-- 完成
-- ============================================================
SELECT '001_upgrade_sys_user.sql 执行完成' AS message;
