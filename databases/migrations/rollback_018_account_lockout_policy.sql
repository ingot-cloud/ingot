-- 回滚 018_account_lockout_policy.sql
-- 目标库：ingot_security
--
-- 执行前提：PMS / Member 先把 ingot.security.account.mode 改回 local，
-- 否则 remote 模式读不到策略会 fail-closed。

USE ingot_security;

DROP TABLE IF EXISTS `account_lockout_policy_config`;
