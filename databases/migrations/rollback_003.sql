-- ============================================================
-- 回滚脚本：platform_* -> meta_*
-- 版本: V3.0
-- 日期: 2026-04-23
-- 说明: 如果 003 改造出现问题，执行此脚本回滚到原始状态
-- ============================================================

USE ingot_core;

-- ============================================================
-- Step 1: 表名回滚
-- ============================================================
RENAME TABLE `platform_app`              TO `meta_app`,
             `platform_dict`             TO `meta_dict`,
             `platform_menu`             TO `meta_menu`,
             `platform_permission`       TO `meta_permission`,
             `platform_role`             TO `meta_role`,
             `platform_role_permission`  TO `meta_role_permission`;

-- ============================================================
-- Step 2: tenant_app_config 列名回滚
-- ============================================================
ALTER TABLE `tenant_app_config`
    DROP INDEX `idx_tenant_app`,
    CHANGE COLUMN `app_id` `meta_id` bigint NOT NULL DEFAULT '0' COMMENT '元数据ID',
    ADD INDEX `idx_tenant_meta` (`tenant_id`, `meta_id`) USING BTREE;

-- ============================================================
-- Step 3: tenant_role_permission_private / tenant_role_user_private 列名回滚
-- ============================================================
ALTER TABLE `tenant_role_permission_private`
    CHANGE COLUMN `platform_role` `meta_role` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否为元数据角色';

ALTER TABLE `tenant_role_user_private`
    CHANGE COLUMN `platform_role` `meta_role` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否为元数据角色';

-- ============================================================
-- Step 4: 权限码回滚
-- ============================================================
UPDATE `meta_permission`
   SET `code` = CONCAT('platform:meta', SUBSTRING(`code`, CHAR_LENGTH('platform:base') + 1))
 WHERE `code` = 'platform:base'
    OR `code` LIKE 'platform:base:%';

-- ============================================================
-- Step 5: 前端路由与视图路径回滚
-- ============================================================
UPDATE `meta_menu`
   SET `path`      = REPLACE(`path`,      '/platform/base', '/platform/meta'),
       `redirect`  = REPLACE(`redirect`,  '/platform/base', '/platform/meta'),
       `view_path` = REPLACE(`view_path`, '/platform/base', '/platform/meta')
 WHERE `path`      LIKE '/platform/base%'
    OR `redirect`  LIKE '/platform/base%'
    OR `view_path` LIKE '%/platform/base%';

-- ============================================================
-- Step 6: 菜单名称回滚
-- ============================================================
UPDATE `meta_menu`
   SET `name` = '元数据管理'
 WHERE `name` = '基础管理';

-- ============================================================
-- 完成
-- ============================================================
SELECT 'Rollback 003: platform_* reverted to meta_*.' AS result;
