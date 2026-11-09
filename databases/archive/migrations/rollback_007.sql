-- rollback migration 007 (non-destructive: drop added columns/indexes only)

USE ingot_core;

ALTER TABLE `tenant_app_config`
    DROP COLUMN IF EXISTS `valid_until`,
    DROP COLUMN IF EXISTS `valid_from`,
    DROP COLUMN IF EXISTS `source`;

ALTER TABLE `platform_permission`
    DROP INDEX IF EXISTS `idx_permission_source`,
    DROP INDEX IF EXISTS `idx_permission_app_pid_status`,
    DROP COLUMN IF EXISTS `managed`,
    DROP COLUMN IF EXISTS `source_id`,
    DROP COLUMN IF EXISTS `source_type`,
    DROP COLUMN IF EXISTS `node_type`,
    DROP COLUMN IF EXISTS `app_id`;

ALTER TABLE `platform_menu`
    DROP INDEX IF EXISTS `idx_menu_app_pid_status_sort`,
    DROP COLUMN IF EXISTS `access_mode`,
    DROP COLUMN IF EXISTS `app_id`;

ALTER TABLE `platform_app`
    DROP COLUMN IF EXISTS `sort`,
    DROP COLUMN IF EXISTS `app_type`,
    DROP COLUMN IF EXISTS `code`;

SELECT 'Rollback 007 completed.' AS result;
