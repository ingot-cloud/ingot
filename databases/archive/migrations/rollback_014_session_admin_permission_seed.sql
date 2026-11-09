-- 回滚 014_session_admin_permission_seed.sql
-- 目标库：ingot_core
--
-- 先摘角色授权再删权限节点，避免留下指向不存在权限的授权记录。
-- 回滚后管理面接口只有超管可访问（@AdminOrHasAnyAuthority 的 admin 分支）。

USE ingot_core;

DELETE FROM `platform_role_permission`
WHERE `permission_id` IN (
  SELECT `id` FROM `platform_permission`
  WHERE `code` IN ('platform:security:session:query', 'platform:security:session:revoke')
);

DELETE FROM `tenant_role_permission_private`
WHERE `permission_id` IN (
  SELECT `id` FROM `platform_permission`
  WHERE `code` IN ('platform:security:session:query', 'platform:security:session:revoke')
);

DELETE FROM `platform_permission`
WHERE `code` IN ('platform:security:session:query', 'platform:security:session:revoke');
