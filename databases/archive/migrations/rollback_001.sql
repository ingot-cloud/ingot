-- ============================================================
-- 回滚脚本 - 用户表改造
-- 版本: V2.0
-- 日期: 2026-02-13
-- 说明: 如果改造出现问题，执行此脚本回滚到原始状态
-- ============================================================

USE ingot_core;

-- ============================================================
-- Step 1: 删除新增的字段
-- ============================================================
ALTER TABLE sys_user
  DROP COLUMN must_change_pwd,
  DROP COLUMN password_changed_at,
  DROP COLUMN enabled,
  DROP COLUMN locked,
  DROP COLUMN last_login_at,
  DROP COLUMN last_login_ip,
  DROP COLUMN version;

-- ============================================================
-- Step 2: 删除新增的索引
-- ============================================================
ALTER TABLE sys_user
  DROP INDEX uk_username,
  DROP INDEX idx_enabled,
  DROP INDEX idx_locked,
  DROP INDEX idx_last_login;

-- ============================================================
-- Step 3: 恢复旧索引
-- ============================================================
ALTER TABLE sys_user 
  ADD INDEX idx_username (username) USING BTREE COMMENT '用户名';

-- ============================================================
-- Step 4: 恢复旧字段注释
-- ============================================================
ALTER TABLE sys_user 
  MODIFY COLUMN init_pwd TINYINT(1) DEFAULT 1 COMMENT '初始化密码标识',
  MODIFY COLUMN status CHAR(1) DEFAULT '0' COMMENT '状态, 0:正常，9:禁用';

-- ============================================================
-- Step 5: 删除新增的表（可选，根据需要执行）
-- ============================================================
-- DROP TABLE IF EXISTS account_lock_state;
-- DROP TABLE IF EXISTS account_security_event;

-- ============================================================
-- 完成
-- ============================================================
SELECT 'rollback_001.sql 执行完成，已回滚到原始状态' AS message;
