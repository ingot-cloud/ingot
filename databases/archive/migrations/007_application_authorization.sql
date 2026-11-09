-- ============================================================
-- PMS 应用中心化授权 schema 扩展与数据回填
-- 版本: V7.0
-- 日期: 2026-06-12
-- ============================================================

USE ingot_core;

-- platform_app
ALTER TABLE `platform_app`
    ADD COLUMN `code`     varchar(64) DEFAULT NULL COMMENT '应用编码' AFTER `id`,
    ADD COLUMN `app_type` char(1)     DEFAULT NULL COMMENT '应用类型,0:平台,1:租户' AFTER `code`,
    ADD COLUMN `sort`     int         DEFAULT 999 COMMENT '排序' AFTER `intro`;

-- platform_menu
ALTER TABLE `platform_menu`
    ADD COLUMN `app_id`       bigint DEFAULT NULL COMMENT '所属应用' AFTER `pid`,
    ADD COLUMN `access_mode`  char(1) DEFAULT NULL COMMENT '访问模式,0:开放,1:权限' AFTER `enable_permission`;

ALTER TABLE `platform_menu`
    ADD INDEX `idx_menu_app_pid_status_sort` (`app_id`, `pid`, `status`, `sort`);

-- platform_permission
ALTER TABLE `platform_permission`
    ADD COLUMN `app_id`       bigint DEFAULT NULL COMMENT '所属应用' AFTER `id`,
    ADD COLUMN `node_type`    char(1) DEFAULT NULL COMMENT '节点类型,0:分组,1:导航,2:操作' AFTER `type`,
    ADD COLUMN `source_type`  char(1) DEFAULT NULL COMMENT '来源类型,0:系统,1:菜单,2:手工' AFTER `node_type`,
    ADD COLUMN `source_id`    bigint DEFAULT NULL COMMENT '来源资源ID' AFTER `source_type`,
    ADD COLUMN `managed`      bit(1) NOT NULL DEFAULT b'0' COMMENT '是否托管' AFTER `source_id`;

ALTER TABLE `platform_permission`
    ADD INDEX `idx_permission_app_pid_status` (`app_id`, `pid`, `status`),
    ADD INDEX `idx_permission_source` (`source_type`, `source_id`);

-- tenant_app_config
ALTER TABLE `tenant_app_config`
    ADD COLUMN `source`      varchar(32) DEFAULT NULL COMMENT '授权来源' AFTER `enabled`,
    ADD COLUMN `valid_from`  datetime    DEFAULT NULL COMMENT '生效时间' AFTER `source`,
    ADD COLUMN `valid_until` datetime    DEFAULT NULL COMMENT '失效时间' AFTER `valid_from`;

-- ============================================================
-- 回填：租户应用 code / type / sort
-- ============================================================
-- UPDATE `platform_app` app
--     INNER JOIN `platform_permission` perm ON perm.id = app.permission_id AND perm.deleted_at IS NULL
-- SET app.`code`     = perm.`code`,
--     app.`app_type` = '1',
--     app.`sort`     = COALESCE((SELECT m.sort FROM platform_menu m WHERE m.id = app.menu_id AND m.deleted_at IS NULL LIMIT 1), 999)
-- WHERE app.deleted_at IS NULL
--   AND app.`code` IS NULL;

-- ============================================================
-- 回填：菜单 app_id / access_mode
-- ============================================================
-- UPDATE `platform_menu` menu
--     INNER JOIN `platform_app` app ON app.menu_id = menu.id AND app.deleted_at IS NULL
-- SET menu.app_id = app.id
-- WHERE menu.deleted_at IS NULL
--   AND menu.app_id IS NULL;

-- UPDATE `platform_menu` child
--     INNER JOIN `platform_menu` parent ON parent.id = child.pid AND parent.deleted_at IS NULL
-- SET child.app_id = parent.app_id
-- WHERE child.deleted_at IS NULL
--   AND child.app_id IS NULL
--   AND parent.app_id IS NOT NULL;

UPDATE `platform_menu`
SET access_mode = CASE WHEN enable_permission = b'1' THEN '1' ELSE '0' END
WHERE deleted_at IS NULL
  AND access_mode IS NULL;

-- ============================================================
-- 回填：权限 app_id / node_type / source_type
-- ============================================================
-- UPDATE `platform_permission` perm
--     INNER JOIN `platform_menu` menu ON menu.permission_id = perm.id AND menu.deleted_at IS NULL
-- SET perm.app_id       = menu.app_id,
--     perm.node_type    = '1',
--     perm.source_type  = '1',
--     perm.source_id    = menu.id,
--     perm.managed      = b'1'
-- WHERE perm.deleted_at IS NULL
--   AND perm.app_id IS NULL;
--
-- UPDATE `platform_permission` child
--     INNER JOIN `platform_permission` parent ON parent.id = child.pid AND parent.deleted_at IS NULL
-- SET child.app_id = parent.app_id
-- WHERE child.deleted_at IS NULL
--   AND child.app_id IS NULL
--   AND parent.app_id IS NOT NULL;
--
-- UPDATE `platform_permission`
-- SET node_type   = '2',
--     source_type = '2',
--     managed     = b'0'
-- WHERE deleted_at IS NULL
--   AND node_type IS NULL;

-- ============================================================
-- 回填：为已有租户生成显式应用授权（继承当前可用状态）
-- ============================================================
-- INSERT INTO `tenant_app_config` (`id`, `tenant_id`, `app_id`, `enabled`, `source`, `created_at`, `updated_at`)
-- SELECT
--     (CAST(CONV(SUBSTRING(MD5(CONCAT('tac:', t.id, ':', a.id)), 1, 15), 16, 10) AS SIGNED) & 9223372036854775807),
--     t.id,
--     a.id,
--     CASE WHEN a.status = '0' THEN b'1' ELSE b'0' END,
--     'migration',
--     NOW(),
--     NOW()
-- FROM `sys_tenant` t
-- CROSS JOIN `platform_app` a
-- LEFT JOIN `tenant_app_config` tac ON tac.tenant_id = t.id AND tac.app_id = a.id
-- WHERE t.deleted_at IS NULL
--   AND a.deleted_at IS NULL
--   AND a.app_type = '1'
--   AND tac.id IS NULL;

SELECT 'Migration 007: application-centric authorization schema completed.' AS result;
