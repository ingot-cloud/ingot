-- 回滚 019_account_lockout_policy_permission.sql
-- 目标库：ingot_core

USE ingot_core;

DELETE FROM `platform_permission`
 WHERE `code` IN (
   'platform:security:account:lockout:query',
   'platform:security:account:lockout:update'
 );
