-- ============================================================
-- PMS 权限遗留列收口：DROP platform_permission.type
-- 版本: V25.0
-- 日期: 2026-09-11
--
-- 前置：已执行 024。旧 type（0 菜单权限 / 1 API 权限）不再参与授权，
-- 节点形态只认 node_type。本脚本可重复执行：列不存在则跳过。
-- ============================================================

USE ingot_core;

SET @drop_type = (
    SELECT IF(COUNT(*) = 0,
              'SELECT 1',
              'ALTER TABLE `platform_permission` DROP COLUMN `type`')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'platform_permission'
      AND COLUMN_NAME = 'type'
);
PREPARE drop_type_stmt FROM @drop_type;
EXECUTE drop_type_stmt;
DEALLOCATE PREPARE drop_type_stmt;
