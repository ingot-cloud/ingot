-- ============================================================
-- 回滚脚本：还原 platform_dict 至 003 版本结构
-- 版本: V4.0
-- 日期: 2026-04-25
-- ============================================================

USE ingot_core;

-- ============================================================
-- Step 1: 删除新增的索引
-- ============================================================
ALTER TABLE `platform_dict`
    DROP INDEX `idx_dict_pid`,
    DROP INDEX `idx_dict_code`,
    DROP INDEX `idx_dict_type_status`,
    DROP INDEX `idx_dict_scope`;

-- ============================================================
-- Step 2: 删除新增字段
-- ============================================================
ALTER TABLE `platform_dict`
    DROP COLUMN `updated_by`,
    DROP COLUMN `created_by`,
    DROP COLUMN `extra`,
    DROP COLUMN `remark`,
    DROP COLUMN `system_flag`,
    DROP COLUMN `sort`,
    DROP COLUMN `app_id`,
    DROP COLUMN `tenant_id`,
    DROP COLUMN `scope_type`,
    DROP COLUMN `label`,
    DROP COLUMN `value`,
    MODIFY COLUMN `org_type` char(1) NOT NULL COMMENT '组织类型';

-- ============================================================
-- 完成
-- ============================================================
SELECT 'Rollback 004: platform_dict reverted to 003 schema.' AS result;
