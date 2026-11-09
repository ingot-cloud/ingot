-- rollback 011

USE ingot_security;

DELETE FROM `gateway_rate_limit_rule` WHERE `code` IN ('login-ip', 'pms-ip', 'member-ip', 'security-ip');
DELETE FROM `gateway_endpoint_group` WHERE `code` IN ('login-auth', 'api-business');
DROP TABLE IF EXISTS `login_failure_protection_policy`;
