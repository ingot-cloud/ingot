-- ============================================================
-- 回滚 V22 PMS RBAC schema
-- 警告：会删除新表数据。须在整体回滚窗口内与服务版本一起恢复。
-- ============================================================

USE ingot_core;

DELETE FROM platform_permission
WHERE id IN (
    1275201000000000001, 1275201000000000002, 1275201000000000003, 1275201000000000004,
    1275201000000000005, 1275201000000000006, 1275201000000000007, 1275201000000000008
);

DROP TABLE IF EXISTS `tenant_role_data_rule_private`;
DROP TABLE IF EXISTS `platform_role_data_rule`;
DROP TABLE IF EXISTS `platform_resource`;
DROP TABLE IF EXISTS `platform_menu_permission`;

ALTER TABLE `tenant_role_user_private`
    DROP INDEX `uk_tenant_role_user_dept`,
    DROP COLUMN `dept_id_uk`;

ALTER TABLE `tenant_app_config`
    DROP INDEX `uk_tenant_app`;

ALTER TABLE `platform_permission`
    DROP INDEX `idx_permission_resource`,
    DROP COLUMN `resource_id`;

ALTER TABLE `platform_menu`
    DROP COLUMN `permission_match_mode`;

ALTER TABLE `platform_app`
    DROP COLUMN `default_access_mode`;
