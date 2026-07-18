-- ============================================================
-- 回滚脚本 - password_expiration.force_change
-- 版本: V2.0
-- 日期: 2026-07-17
-- 说明: 回滚 008，删除 force_change 字段（行为退回读取 must_change_pwd）
-- ============================================================

USE ingot_core;

ALTER TABLE password_expiration
  DROP COLUMN force_change;

SELECT 'rollback_008.sql 执行完成，已删除 force_change 字段' AS message;
