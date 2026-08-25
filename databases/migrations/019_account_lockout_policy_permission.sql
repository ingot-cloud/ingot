-- 账号锁定策略管理面 API 权限码
-- 目标库：ingot_core
--
-- 挂在安全中心根权限（platform:security:**）下。超级管理员走 admin 短路不依赖它们。

USE ingot_core;

-- app_id: 安全中心应用；pid: platform:security:** 权限节点
-- type='1'(API权限) node_type='2'(操作) source_type='2'(手工) managed=0
INSERT INTO `platform_permission`
  (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`,
   `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`)
SELECT 1247137066804862985, 1247136775485284354, 1247136775460118530,
       '账号锁定策略查询', 'platform:security:account:lockout:query', '1', '2', '2', NULL,
       b'0', '0', '0', '账号锁定策略列表与按用户类型查询', NOW(), NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM `platform_permission` WHERE `code` = 'platform:security:account:lockout:query'
);

INSERT INTO `platform_permission`
  (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`,
   `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`)
SELECT 1247137066804862986, 1247136775485284354, 1247136775460118530,
       '账号锁定策略维护', 'platform:security:account:lockout:update', '1', '2', '2', NULL,
       b'0', '0', '0', '账号锁定策略更新（按用户类型 upsert，不允许删除）', NOW(), NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM `platform_permission` WHERE `code` = 'platform:security:account:lockout:update'
);
