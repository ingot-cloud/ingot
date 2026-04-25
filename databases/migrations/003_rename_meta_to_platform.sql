-- ============================================================
-- 元数据前缀重命名脚本：meta_* -> platform_*
-- 版本: V3.0
-- 日期: 2026-04-23
-- 说明: 将原有以 meta_ 为前缀的平台级资源表重命名为 platform_ 前缀，
--       并同步处理语义相关的列名、索引名、权限码与前端路由 seed。
--       原语义：meta_* 表示平台预定义、跨租户共享的资源目录
--              tenant_*_private 表示租户私有
--       新语义：platform_* 表达「平台级共享定义」，与 tenant_*_private
--              形成 platform + private 的清晰对仗。
-- ============================================================

USE ingot_core;

-- ============================================================
-- Step 1: 重命名 6 张平台级资源表
-- ============================================================
RENAME TABLE `meta_app`              TO `platform_app`,
             `meta_dict`             TO `platform_dict`,
             `meta_menu`             TO `platform_menu`,
             `meta_permission`       TO `platform_permission`,
             `meta_role`             TO `platform_role`,
             `meta_role_permission`  TO `platform_role_permission`;

-- ============================================================
-- Step 2: tenant_app_config 列重命名（meta_id -> app_id，同步索引名）
-- ============================================================
ALTER TABLE `tenant_app_config`
    DROP INDEX `idx_tenant_meta`,
    CHANGE COLUMN `meta_id` `app_id` bigint NOT NULL DEFAULT '0' COMMENT '应用ID',
    ADD INDEX `idx_tenant_app` (`tenant_id`, `app_id`) USING BTREE;

-- ============================================================
-- Step 3: tenant_role_permission_private / tenant_role_user_private
--          列 meta_role -> platform_role，注释同步更新
-- ============================================================
ALTER TABLE `tenant_role_permission_private`
    CHANGE COLUMN `meta_role` `platform_role` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否为平台角色';

ALTER TABLE `tenant_role_user_private`
    CHANGE COLUMN `meta_role` `platform_role` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否为平台角色';

-- ============================================================
-- Step 4: 修正 platform_permission.code 中的 platform:meta* 前缀为 platform:base*
-- ============================================================
UPDATE `platform_permission`
   SET `code` = CONCAT('platform:base', SUBSTRING(`code`, CHAR_LENGTH('platform:meta') + 1))
 WHERE `code` = 'platform:meta'
    OR `code` LIKE 'platform:meta:%';

-- ============================================================
-- Step 5: 修正 platform_menu 的前端路由与视图路径
-- ============================================================
UPDATE `platform_menu`
   SET `path`      = REPLACE(`path`,      '/platform/meta', '/platform/base'),
       `redirect`  = REPLACE(`redirect`,  '/platform/meta', '/platform/base'),
       `view_path` = REPLACE(`view_path`, '/platform/meta', '/platform/base')
 WHERE `path`      LIKE '/platform/meta%'
    OR `redirect`  LIKE '/platform/meta%'
    OR `view_path` LIKE '%/platform/meta%';

-- ============================================================
-- Step 6: 菜单名称同步更新（元数据管理 -> 基础管理）
-- ============================================================
UPDATE `platform_menu`
   SET `name` = '基础管理'
 WHERE `name` = '元数据管理';

-- ============================================================
-- 完成
-- ============================================================
SELECT 'Migration 003: rename meta_* to platform_* completed.' AS result;
