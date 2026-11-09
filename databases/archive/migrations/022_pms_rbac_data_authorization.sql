-- ============================================================
-- PMS 多租户 RBAC / 菜单解耦 / 数据权限 schema
-- 版本: V22.0
-- 日期: 2026-09-10
-- 说明: 仅结构变更，可重复执行前请确认列/表不存在。
--       数据映射见 022_pms_rbac_data_authorization_data.sql
-- ============================================================

USE ingot_core;

-- 应用默认访问策略，既有应用缺省 OPEN
ALTER TABLE `platform_app`
    ADD COLUMN `default_access_mode` char(1) NOT NULL DEFAULT '0' COMMENT '默认访问策略,0:开放,1:关闭' AFTER `status`;

-- 菜单可见性匹配；旧 permission_id 由 023 在映射收口后 DROP
ALTER TABLE `platform_menu`
    ADD COLUMN `permission_match_mode` char(1) NOT NULL DEFAULT '0' COMMENT '权限匹配模式,0:ANY,1:ALL' AFTER `access_mode`;

ALTER TABLE `platform_permission`
    ADD COLUMN `resource_id` bigint DEFAULT NULL COMMENT '关联资源ID，数据操作权限必填' AFTER `managed`;

ALTER TABLE `platform_permission`
    ADD INDEX `idx_permission_resource` (`resource_id`);

-- 租户应用覆盖唯一
ALTER TABLE `tenant_app_config`
    ADD UNIQUE INDEX `uk_tenant_app` (`tenant_id`, `app_id`);

-- 部门任职唯一：空部门映射哨兵 0，真实部门不得使用 0
ALTER TABLE `tenant_role_user_private`
    ADD COLUMN `dept_id_uk` bigint GENERATED ALWAYS AS (IFNULL(`dept_id`, 0)) STORED COMMENT '空部门唯一索引哨兵，实际部门不得使用 0';

ALTER TABLE `tenant_role_user_private`
    ADD UNIQUE INDEX `uk_tenant_role_user_dept` (`tenant_id`, `user_id`, `platform_role`, `role_id`, `dept_id_uk`);

CREATE TABLE IF NOT EXISTS `platform_menu_permission` (
  `id` bigint NOT NULL COMMENT 'ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_menu_permission` (`menu_id`, `permission_id`),
  KEY `idx_menu_permission_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单可见性关联';

CREATE TABLE IF NOT EXISTS `platform_resource` (
  `id` bigint NOT NULL COMMENT 'ID',
  `app_id` bigint NOT NULL COMMENT '所属应用',
  `code` varchar(64) NOT NULL COMMENT '资源编码',
  `name` varchar(64) NOT NULL COMMENT '资源名称',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_app_code` (`app_id`, `code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='应用资源目录';

CREATE TABLE IF NOT EXISTS `platform_role_data_rule` (
  `id` bigint NOT NULL COMMENT 'ID',
  `role_id` bigint NOT NULL COMMENT '平台角色ID',
  `permission_id` bigint NOT NULL COMMENT '功能权限ID',
  `resource_id` bigint NOT NULL COMMENT '资源ID',
  `scope_type` int NOT NULL COMMENT '数据范围类型',
  `scopes` json NOT NULL COMMENT 'CUSTOM 部门ID列表',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_role_data_rule` (`role_id`, `permission_id`, `resource_id`, `scope_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台角色默认数据规则';

CREATE TABLE IF NOT EXISTS `tenant_role_data_rule_private` (
  `id` bigint NOT NULL COMMENT 'ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `platform_role` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否平台角色',
  `permission_id` bigint NOT NULL COMMENT '功能权限ID',
  `resource_id` bigint NOT NULL COMMENT '资源ID',
  `scope_type` int NOT NULL COMMENT '数据范围类型',
  `scopes` json NOT NULL COMMENT 'CUSTOM 部门ID列表',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_role_data_rule` (`tenant_id`, `role_id`, `platform_role`, `permission_id`, `resource_id`, `scope_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户追加数据规则';
