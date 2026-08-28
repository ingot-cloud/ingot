-- 回滚 020_challenge_login_always_seed.sql
-- 目标库：ingot_security

USE ingot_security;

DELETE FROM `security_challenge_policy` WHERE `code` = 'login-always';
