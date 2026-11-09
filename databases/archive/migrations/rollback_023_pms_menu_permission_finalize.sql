-- ============================================================
-- 回滚 V23：仅加回 platform_menu.permission_id，不恢复旧单关联值
-- 须与整体备份一起使用；单独执行后菜单可见性仍以 platform_menu_permission 为准
-- ============================================================

USE ingot_core;

SET @add_permission_id = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `platform_menu` ADD COLUMN `permission_id` bigint NOT NULL DEFAULT 0 COMMENT ''权限ID'' AFTER `access_mode`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'platform_menu'
      AND COLUMN_NAME = 'permission_id'
);
PREPARE add_permission_id_stmt FROM @add_permission_id;
EXECUTE add_permission_id_stmt;
DEALLOCATE PREPARE add_permission_id_stmt;
