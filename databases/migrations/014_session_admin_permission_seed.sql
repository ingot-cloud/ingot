-- L5: 会话安全 — 安全中心「在线会话」管理面 API 权限码
-- 目标库：ingot_core
--
-- 背景：会话查询与强制下线由 Auth 的 /auth/token/** 迁到安全中心
-- /platform/security/sessions，接口以 @AdminOrHasAnyAuthority 校验下列编码。
-- 「在线用户」菜单权限（platform:security:onlinetoken）已在基线中存在，
-- 本脚本只补挂在该菜单下的两个操作级 API 权限，超管本身走 admin 短路不依赖它们。

USE ingot_core;

-- app_id: 安全中心应用；pid: platform:security:onlinetoken 权限节点
-- type='1'(API权限) node_type='2'(操作) source_type='2'(手工) managed=0(可由权限管理页维护)
INSERT INTO `platform_permission`
  (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`,
   `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`)
SELECT 1247137066804862981, 1247136775485284354, 1247137066804862978,
       '会话查询', 'platform:security:session:query', '1', '2', '2', NULL,
       b'0', '0', '0', '安全中心在线会话列表与详情', NOW(), NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM `platform_permission` WHERE `code` = 'platform:security:session:query'
);

INSERT INTO `platform_permission`
  (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`,
   `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`)
SELECT 1247137066804862982, 1247136775485284354, 1247137066804862978,
       '会话下线', 'platform:security:session:revoke', '1', '2', '2', NULL,
       b'0', '0', '0', '安全中心按 sid / 按用户强制下线', NOW(), NOW(), NULL
WHERE NOT EXISTS (
  SELECT 1 FROM `platform_permission` WHERE `code` = 'platform:security:session:revoke'
);
