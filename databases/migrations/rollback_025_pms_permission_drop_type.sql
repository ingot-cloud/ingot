-- ============================================================
-- 回滚 V25：加回 platform_permission.type，不恢复菜单权限/API 语义
-- 须与整体备份一起使用。回填缺省 '1'（API）。
-- ============================================================

USE ingot_core;

SET @add_type = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `platform_permission` ADD COLUMN `type` char(1) NOT NULL DEFAULT ''1'' COMMENT ''类型'' AFTER `code`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'platform_permission'
      AND COLUMN_NAME = 'type'
);
PREPARE add_type_stmt FROM @add_type;
EXECUTE add_type_stmt;
DEALLOCATE PREPARE add_type_stmt;
