-- ============================================================
-- PMS 权限遗留列收口：NAVIGATION 再收口，DROP managed/source_type/source_id
-- 版本: V24.0
-- 日期: 2026-09-11
--
-- 前置：已执行 022 schema、022_data（@apply = 1）与 023。
-- 本脚本可重复执行：列/索引不存在则跳过。
-- 应用根此后只认 platform_app.permission_id。
-- ============================================================

USE ingot_core;

-- 残留精确 NAVIGATION -> ACTION
UPDATE platform_permission
SET node_type = '2',
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND node_type = '1'
  AND code NOT LIKE '%:*'
  AND code NOT LIKE '%:**';

-- 残留通配 NAVIGATION -> GROUP
UPDATE platform_permission
SET node_type = '0',
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND node_type = '1'
  AND (code LIKE '%:**' OR (code LIKE '%:*' AND code NOT LIKE '%:**'));

SET @drop_idx = (
    SELECT IF(COUNT(*) = 0,
              'SELECT 1',
              'ALTER TABLE `platform_permission` DROP INDEX `idx_permission_source`')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'platform_permission'
      AND INDEX_NAME = 'idx_permission_source'
);
PREPARE drop_idx_stmt FROM @drop_idx;
EXECUTE drop_idx_stmt;
DEALLOCATE PREPARE drop_idx_stmt;

SET @drop_managed = (
    SELECT IF(COUNT(*) = 0,
              'SELECT 1',
              'ALTER TABLE `platform_permission` DROP COLUMN `managed`')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'platform_permission'
      AND COLUMN_NAME = 'managed'
);
PREPARE drop_managed_stmt FROM @drop_managed;
EXECUTE drop_managed_stmt;
DEALLOCATE PREPARE drop_managed_stmt;

SET @drop_source_id = (
    SELECT IF(COUNT(*) = 0,
              'SELECT 1',
              'ALTER TABLE `platform_permission` DROP COLUMN `source_id`')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'platform_permission'
      AND COLUMN_NAME = 'source_id'
);
PREPARE drop_source_id_stmt FROM @drop_source_id;
EXECUTE drop_source_id_stmt;
DEALLOCATE PREPARE drop_source_id_stmt;

SET @drop_source_type = (
    SELECT IF(COUNT(*) = 0,
              'SELECT 1',
              'ALTER TABLE `platform_permission` DROP COLUMN `source_type`')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'platform_permission'
      AND COLUMN_NAME = 'source_type'
);
PREPARE drop_source_type_stmt FROM @drop_source_type;
EXECUTE drop_source_type_stmt;
DEALLOCATE PREPARE drop_source_type_stmt;

ALTER TABLE `platform_permission`
    MODIFY COLUMN `node_type` char(1) DEFAULT NULL COMMENT '节点类型,0:分组,2:操作';
