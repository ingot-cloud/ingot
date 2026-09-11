-- ============================================================
-- 回滚 V24：加回 source_type/source_id/managed，不恢复菜单托管语义
-- 须与整体备份一起使用。应用根 source_type 按 platform_app.permission_id 回填 SYSTEM，其余 MANUAL。
-- ============================================================

USE ingot_core;

SET @add_source_type = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `platform_permission` ADD COLUMN `source_type` char(1) DEFAULT NULL COMMENT ''来源类型,0:系统,1:菜单,2:手工'' AFTER `node_type`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'platform_permission'
      AND COLUMN_NAME = 'source_type'
);
PREPARE add_source_type_stmt FROM @add_source_type;
EXECUTE add_source_type_stmt;
DEALLOCATE PREPARE add_source_type_stmt;

SET @add_source_id = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `platform_permission` ADD COLUMN `source_id` bigint DEFAULT NULL COMMENT ''来源资源ID'' AFTER `source_type`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'platform_permission'
      AND COLUMN_NAME = 'source_id'
);
PREPARE add_source_id_stmt FROM @add_source_id;
EXECUTE add_source_id_stmt;
DEALLOCATE PREPARE add_source_id_stmt;

SET @add_managed = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `platform_permission` ADD COLUMN `managed` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否托管'' AFTER `source_id`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'platform_permission'
      AND COLUMN_NAME = 'managed'
);
PREPARE add_managed_stmt FROM @add_managed;
EXECUTE add_managed_stmt;
DEALLOCATE PREPARE add_managed_stmt;

SET @add_idx = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `platform_permission` ADD INDEX `idx_permission_source` (`source_type`, `source_id`)',
              'SELECT 1')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'platform_permission'
      AND INDEX_NAME = 'idx_permission_source'
);
PREPARE add_idx_stmt FROM @add_idx;
EXECUTE add_idx_stmt;
DEALLOCATE PREPARE add_idx_stmt;

UPDATE platform_permission
SET source_type = '2',
    source_id = NULL,
    managed = b'0'
WHERE source_type IS NULL;

UPDATE platform_permission p
INNER JOIN platform_app a ON a.permission_id = p.id
SET p.source_type = '0'
WHERE p.deleted_at IS NULL;

ALTER TABLE `platform_permission`
    MODIFY COLUMN `node_type` char(1) DEFAULT NULL COMMENT '节点类型,0:分组,1:导航,2:操作';
