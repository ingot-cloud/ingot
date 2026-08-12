-- ============================================================
-- 013: 物理下线 legacy account_security_event
-- 库: ingot_core / ingot_member
-- 说明: 权威审计表为 canonical security_event（migration 012）。
--       本脚本销毁旧表全部行；上线前如需留存请自行 mysqldump。
-- ============================================================

USE ingot_core;
DROP TABLE IF EXISTS `account_security_event`;

USE ingot_member;
DROP TABLE IF EXISTS `account_security_event`;

SELECT '013_drop_account_security_event.sql 执行完成' AS message;
