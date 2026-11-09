-- 回滚 021_menu_view_path_encoding.sql
-- 目标库：ingot_core
-- 未命中反向映射的 view_path 保持原值，需结合备份恢复

USE ingot_core;

ALTER TABLE `platform_menu`
  ADD COLUMN `custom_view_path` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否自定义视图路径' AFTER `permission_id`,
  MODIFY COLUMN `view_path` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '视图路径';

UPDATE `platform_menu`
SET
  `view_path` = CASE `view_path`
    WHEN 'platform.config.app.home' THEN '@/pages/platform/config/app/home/IndexPage.vue'
    WHEN 'platform.config.app.detail' THEN '@/pages/platform/config/app/detail/IndexPage.vue'
    WHEN 'platform.config.role' THEN '@/pages/platform/config/role/IndexPage.vue'
    WHEN 'platform.config.menu' THEN '@/pages/platform/config/menu/IndexPage.vue'
    WHEN 'platform.config.permission' THEN '@/pages/platform/config/permission/IndexPage.vue'
    WHEN 'platform.config.dict' THEN '@/pages/platform/config/dict/IndexPage.vue'
    WHEN 'platform.admin.user' THEN '@/pages/platform/admin/user/IndexPage.vue'
    WHEN 'platform.org.tenant' THEN '@/pages/platform/org/tenant/IndexPage.vue'
    WHEN 'platform.develop.qrcode' THEN '@/pages/platform/develop/qrcode/IndexPage.vue'
    WHEN 'platform.develop.client' THEN '@/pages/platform/develop/client/IndexPage.vue'
    WHEN 'platform.develop.social' THEN '@/pages/platform/develop/social/IndexPage.vue'
    WHEN 'platform.develop.id' THEN '@/pages/platform/develop/id/IndexPage.vue'
    WHEN 'security.credential' THEN '@/pages/platform/security/credential/IndexPage.vue'
    WHEN 'security.sessions' THEN '@/pages/platform/security/onlinetoken/IndexPage.vue'
    WHEN 'security.access.protection' THEN '@/pages/platform/security/access-protection/IndexPage.vue'
    WHEN 'security.account.protection' THEN '@/pages/platform/security/account-protection/IndexPage.vue'
    WHEN 'member.user' THEN '@/pages/platform/member/user/IndexPage.vue'
    WHEN 'member.role' THEN '@/pages/platform/member/role/IndexPage.vue'
    WHEN 'org.contacts.user' THEN '@/pages/org/contacts/user/IndexPage.vue'
    WHEN 'org.contacts.dept' THEN '@/pages/org/contacts/dept/IndexPage.vue'
    WHEN 'org.contacts.role' THEN '@/pages/org/contacts/role/IndexPage.vue'
    WHEN 'org.contacts.auth' THEN '@/pages/org/contacts/auth/IndexPage.vue'
    WHEN 'org.contacts.structure' THEN '@/pages/org/contacts/structure/IndexPage.vue'
    WHEN 'layout.main' THEN '@/layouts/InAppLayout.vue'
    WHEN 'layout.simple' THEN '@/layouts/InSimpleLayout.vue'
    WHEN 'layout.iframe' THEN '@/layouts/InIFrameLayout.vue'
    WHEN 'layout.external' THEN '@/layouts/InExtLinkLayout.vue'
    ELSE `view_path`
  END,
  `custom_view_path` = CASE WHEN `menu_type` = '0' THEN 1 ELSE 0 END,
  `updated_at` = NOW()
WHERE `menu_type` IN ('0', '1')
  AND `deleted_at` IS NULL;
