-- 回滚 016_session_concurrency_policy.sql
-- 目标库：ingot_security
--
-- 执行前提：Auth 侧先把 ingot.security.session.concurrency.enabled 置 false
-- 或 mode 回退 local，否则 remote 模式读不到策略会 fail-closed 拒绝新登录。

USE ingot_security;

DROP TABLE IF EXISTS `session_concurrency_policy`;
