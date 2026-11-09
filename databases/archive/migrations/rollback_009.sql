-- ============================================================
-- 回滚脚本 - Member 账号保护表
-- 版本: V2.0
-- 日期: 2026-07-24
-- 说明: 回滚 009，删除 ingot_member 库的 account_lock_state / account_security_event 表。
--       回滚后 Member 账号保护退回 NoOp（不计失败、不落锁定/事件）。
-- ============================================================

USE ingot_member;

DROP TABLE IF EXISTS `account_security_event`;
DROP TABLE IF EXISTS `account_lock_state`;

SELECT 'rollback_009.sql 执行完成，已删除 Member 账号保护表' AS message;
