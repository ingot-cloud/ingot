-- 菜单 view_path 改为页面注册键，删除 custom_view_path
-- 目标库：ingot_core
-- 按钮（menu_type=9）的 view_path 不迁移

USE ingot_core;

UPDATE `platform_menu`
SET
  `view_path` = CASE `view_path`
    WHEN '@/pages/platform/config/app/home/IndexPage.vue' THEN 'platform.config.app.home'
    WHEN '@/pages/platform/config/app/detail/IndexPage.vue' THEN 'platform.config.app.detail'
    WHEN '@/pages/platform/config/role/IndexPage.vue' THEN 'platform.config.role'
    WHEN '@/pages/platform/config/menu/IndexPage.vue' THEN 'platform.config.menu'
    WHEN '@/pages/platform/config/permission/IndexPage.vue' THEN 'platform.config.permission'
    WHEN '@/pages/platform/config/dict/IndexPage.vue' THEN 'platform.config.dict'
    WHEN '@/pages/platform/admin/user/IndexPage.vue' THEN 'platform.admin.user'
    WHEN '@/pages/platform/org/tenant/IndexPage.vue' THEN 'platform.org.tenant'
    WHEN '@/pages/platform/develop/qrcode/IndexPage.vue' THEN 'platform.develop.qrcode'
    WHEN '@/pages/platform/develop/client/IndexPage.vue' THEN 'platform.develop.client'
    WHEN '@/pages/platform/develop/social/IndexPage.vue' THEN 'platform.develop.social'
    WHEN '@/pages/platform/develop/id/IndexPage.vue' THEN 'platform.develop.id'
    WHEN '@/pages/platform/security/credential/IndexPage.vue' THEN 'security.credential'
    WHEN '@/pages/platform/security/sessions/IndexPage.vue' THEN 'security.sessions'
    WHEN '@/pages/platform/security/onlinetoken/IndexPage.vue' THEN 'security.sessions'
    WHEN '@/pages/platform/security/access-protection/IndexPage.vue' THEN 'security.access.protection'
    WHEN '@/pages/platform/security/account-protection/IndexPage.vue' THEN 'security.account.protection'
    WHEN '@/pages/platform/member/user/IndexPage.vue' THEN 'member.user'
    WHEN '@/pages/platform/member/role/IndexPage.vue' THEN 'member.role'
    WHEN '@/pages/org/contacts/user/IndexPage.vue' THEN 'org.contacts.user'
    WHEN '@/pages/org/contacts/dept/IndexPage.vue' THEN 'org.contacts.dept'
    WHEN '@/pages/org/contacts/role/IndexPage.vue' THEN 'org.contacts.role'
    WHEN '@/pages/org/contacts/auth/IndexPage.vue' THEN 'org.contacts.auth'
    WHEN '@/pages/org/contacts/structure/IndexPage.vue' THEN 'org.contacts.structure'
    WHEN '@/layouts/InAppLayout.vue' THEN 'layout.main'
    WHEN '@/layouts/InSimpleLayout.vue' THEN 'layout.simple'
    WHEN '@/layouts/InIFrameLayout.vue' THEN 'layout.iframe'
    WHEN '@/layouts/InExtLinkLayout.vue' THEN 'layout.external'
    ELSE `view_path`
  END,
  `updated_at` = NOW()
WHERE `menu_type` IN ('0', '1')
  AND `deleted_at` IS NULL
  AND (
    `view_path` LIKE '@/pages/%'
    OR `view_path` LIKE '@/layouts/%'
  );

ALTER TABLE `platform_menu`
  MODIFY COLUMN `view_path` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '页面注册键',
  DROP COLUMN `custom_view_path`;
