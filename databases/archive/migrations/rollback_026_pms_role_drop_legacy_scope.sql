-- ============================================================
-- 回滚 V26：加回角色级 scope_type / scopes，不恢复为授权事实来源
-- 须与整体备份一起使用。缺省 ALL、空部门列表。
-- ============================================================

USE ingot_core;

SET @add_platform_scope_type = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `platform_role` ADD COLUMN `scope_type` int NOT NULL DEFAULT 0 COMMENT ''数据范围类型'' AFTER `filter_dept`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'platform_role'
      AND COLUMN_NAME = 'scope_type'
);
PREPARE add_platform_scope_type_stmt FROM @add_platform_scope_type;
EXECUTE add_platform_scope_type_stmt;
DEALLOCATE PREPARE add_platform_scope_type_stmt;

SET @add_platform_scopes = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `platform_role` ADD COLUMN `scopes` varchar(1000) NOT NULL DEFAULT ''[]'' COMMENT ''数据范围'' AFTER `scope_type`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'platform_role'
      AND COLUMN_NAME = 'scopes'
);
PREPARE add_platform_scopes_stmt FROM @add_platform_scopes;
EXECUTE add_platform_scopes_stmt;
DEALLOCATE PREPARE add_platform_scopes_stmt;

SET @add_tenant_scope_type = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `tenant_role_private` ADD COLUMN `scope_type` int NOT NULL DEFAULT 0 COMMENT ''数据范围类型'' AFTER `filter_dept`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tenant_role_private'
      AND COLUMN_NAME = 'scope_type'
);
PREPARE add_tenant_scope_type_stmt FROM @add_tenant_scope_type;
EXECUTE add_tenant_scope_type_stmt;
DEALLOCATE PREPARE add_tenant_scope_type_stmt;

SET @add_tenant_scopes = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `tenant_role_private` ADD COLUMN `scopes` varchar(1000) NOT NULL DEFAULT ''[]'' COMMENT ''数据范围'' AFTER `scope_type`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'tenant_role_private'
      AND COLUMN_NAME = 'scopes'
);
PREPARE add_tenant_scopes_stmt FROM @add_tenant_scopes;
EXECUTE add_tenant_scopes_stmt;
DEALLOCATE PREPARE add_tenant_scopes_stmt;
