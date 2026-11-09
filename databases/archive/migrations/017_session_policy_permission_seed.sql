-- L5: 会话安全 — 并发会话策略管理面 API 权限码
-- 目标库：ingot_core
--
-- 与 014 同理，只补挂在「在线用户」权限节点（platform:security:onlinetoken）下的
-- 两个操作级 API 权限，超管走 admin 短路不依赖它们。

USE ingot_core;

-- app_id: 安全中心应用；pid: platform:security:onlinetoken 权限节点
-- type='1'(API权限) node_type='2'(操作) source_type='2'(手工) managed=0(可由权限管理页维护)
INSERT INTO `platform_permission`
  (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`,
   `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`)
SELECT 1247137066804862983, 1247136775485284354, 1247137066804862978,
       '并发策略查询', 'platform:security:session:policy:query', '1', '2', '2', NULL,
       b'0', '0', '0', '并发会话策略列表与详情', NOW(), NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM `platform_permission` WHERE `code` = 'platform:security:session:policy:query'
);

INSERT INTO `platform_permission`
  (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`,
   `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`)
SELECT 1247137066804862984, 1247136775485284354, 1247137066804862978,
       '并发策略维护', 'platform:security:session:policy:update', '1', '2', '2', NULL,
       b'0', '0', '0', '并发会话策略新增/修改/删除', NOW(), NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM `platform_permission` WHERE `code` = 'platform:security:session:policy:update'
);
