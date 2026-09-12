-- ============================================================
-- PMS 角色遗留列收口：DROP 角色级 scope_type / scopes
-- 版本: V26.0
-- 日期: 2026-09-12
--
-- 前置：已执行 022 schema。数据范围只存在于
--       platform_role_data_rule / tenant_role_data_rule_private。
-- 本脚本可重复执行：列不存在则跳过。
-- 保留 filter_dept（部门角色绑定语义，不是行范围）。
-- ============================================================

USE ingot_core;

SET @drop_platform_scope_type = (
    SELECT IF(COUNT(*) = 0,
              'SELECT 1',
              'ALTER TABLE `platform_role` DROP COLUMN `scope_type`')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'platform_role'
      AND COLUMN_NAME = 'scope_type'
);
PREPARE drop_platform_scope_type_stmt FROM @drop_platform_scope_type;
EXECUTE drop_platform_scope_type_stmt;
DEALLOCATE PREPARE drop_platform_scope_type_stmt;

SET @drop_platform_scopes = (
    SELECT IF(COUNT(*) = 0,
              'SELECT 1',
              'ALTER TABLE `platform_role` DROP COLUMN `scopes`')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'platform_role'
      AND COLUMN_NAME = 'scopes'
);
PREPARE drop_platform_scopes_stmt FROM @drop_platform_scopes;
EXECUTE drop_platform_scopes_stmt;
DEALLOCATE PREPARE drop_platform_scopes_stmt;

SET @drop_tenant_scope_type = (
    SELECT IF(COUNT(*) = 0,
              'SELECT 1',
              'ALTER TABLE `tenant_role_private` DROP COLUMN `scope_type`')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tenant_role_private'
      AND COLUMN_NAME = 'scope_type'
);
PREPARE drop_tenant_scope_type_stmt FROM @drop_tenant_scope_type;
EXECUTE drop_tenant_scope_type_stmt;
DEALLOCATE PREPARE drop_tenant_scope_type_stmt;

SET @drop_tenant_scopes = (
    SELECT IF(COUNT(*) = 0,
              'SELECT 1',
              'ALTER TABLE `tenant_role_private` DROP COLUMN `scopes`')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tenant_role_private'
      AND COLUMN_NAME = 'scopes'
);
PREPARE drop_tenant_scopes_stmt FROM @drop_tenant_scopes;
EXECUTE drop_tenant_scopes_stmt;
DEALLOCATE PREPARE drop_tenant_scopes_stmt;
