/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : localhost:3306
 Source Schema         : ingot_core

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 30/12/2025 16:52:03
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for biz_leaf_alloc
-- ----------------------------
DROP TABLE IF EXISTS `biz_leaf_alloc`;
CREATE TABLE `biz_leaf_alloc` (
  `biz_tag` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `max_id` bigint NOT NULL DEFAULT '1',
  `step` int NOT NULL,
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`biz_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of biz_leaf_alloc
-- ----------------------------
BEGIN;
INSERT INTO `biz_leaf_alloc` (`biz_tag`, `max_id`, `step`, `description`, `update_time`) VALUES ('app_id', 1, 1000, 'AppID', '2024-01-10 10:44:38');
INSERT INTO `biz_leaf_alloc` (`biz_tag`, `max_id`, `step`, `description`, `update_time`) VALUES ('org_code', 4001, 1000, '组织编码', '2025-11-26 02:47:41');
INSERT INTO `biz_leaf_alloc` (`biz_tag`, `max_id`, `step`, `description`, `update_time`) VALUES ('org_role_code', 38001, 1000, '组织角色编码', '2025-11-26 01:19:25');
COMMIT;

-- ----------------------------
-- Table structure for member_role
-- ----------------------------
DROP TABLE IF EXISTS `member_role`;
CREATE TABLE `member_role` (
  `id` bigint unsigned NOT NULL COMMENT 'ID',
  `pid` bigint NOT NULL COMMENT '组ID',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT '角色名称',
  `code` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT '角色编码',
  `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '角色类型',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_code` (`code`) USING BTREE COMMENT '编码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of member_role
-- ----------------------------
BEGIN;
INSERT INTO `member_role` (`id`, `pid`, `name`, `code`, `type`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (930517119423283201, 930516778518642689, '用户', 'role_user', '1', '0', '2024-01-30 17:28:51', NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for member_role_user
-- ----------------------------
DROP TABLE IF EXISTS `member_role_user`;
CREATE TABLE `member_role_user` (
  `id` bigint NOT NULL COMMENT 'ID',
  `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user` (`user_id`) USING BTREE,
  KEY `idx_role` (`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of member_role_user
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for member_user
-- ----------------------------
DROP TABLE IF EXISTS `member_user`;
CREATE TABLE `member_user` (
  `id` bigint unsigned NOT NULL COMMENT 'ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '用户名',
  `password` varchar(300) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '密码',
  `init_pwd` tinyint(1) NOT NULL DEFAULT '1' COMMENT '初始化密码标识',
  `nickname` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '昵称',
  `phone` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '手机号',
  `email` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '邮件地址',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '头像',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`),
  KEY `idx_username` (`username`) USING BTREE COMMENT '用户名',
  KEY `idx_phone` (`phone`) USING BTREE COMMENT '手机号',
  KEY `idx_email` (`email`) USING BTREE COMMENT '邮箱'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of member_user
-- ----------------------------
BEGIN;
INSERT INTO `member_user` (`id`, `username`, `password`, `init_pwd`, `nickname`, `phone`, `email`, `avatar`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (931155715964600322, '123123', '{bcrypt}$2a$10$Rek2iUR0EyBy79u06yfi0ec83N3zEicQPHvltWTlhCDjeWn4X7qRm', 1, 'test1', '123123', NULL, 'http://ingot-cloud:9090/ingot/public/user/avatar/tctools.png?t=1706772523411', '0', '2024-02-01 11:46:24', '2024-02-01 15:28:49', NULL);
COMMIT;

-- ----------------------------
-- Table structure for member_user_social
-- ----------------------------
DROP TABLE IF EXISTS `member_user_social`;
CREATE TABLE `member_user_social` (
  `id` bigint NOT NULL COMMENT 'ID',
  `tenant_id` bigint NOT NULL COMMENT '组织ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道类型',
  `unique_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道唯一ID',
  `bind_at` datetime NOT NULL COMMENT '绑定时间',
  PRIMARY KEY (`id`),
  KEY `idx_unique_type_user` (`unique_id`,`type`,`user_id`) USING BTREE COMMENT '渠道用户索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of member_user_social
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for member_user_tenant
-- ----------------------------
DROP TABLE IF EXISTS `member_user_tenant`;
CREATE TABLE `member_user_tenant` (
  `id` bigint NOT NULL COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `main` bit(1) NOT NULL COMMENT '是否为主要租户',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户名称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '头像',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user` (`user_id`) USING BTREE,
  KEY `idx_tenant_user` (`tenant_id`,`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of member_user_tenant
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for meta_app
-- ----------------------------
DROP TABLE IF EXISTS `meta_app`;
CREATE TABLE `meta_app` (
  `id` bigint NOT NULL COMMENT 'ID',
  `menu_id` bigint NOT NULL DEFAULT '0' COMMENT '菜单ID',
  `permission_id` bigint NOT NULL DEFAULT '0' COMMENT '权限ID',
  `name` varchar(32) NOT NULL COMMENT '应用名称',
  `icon` varchar(255) DEFAULT NULL COMMENT '应用图标',
  `intro` varchar(255) NOT NULL COMMENT '应用介绍',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '状态',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of meta_app
-- ----------------------------
BEGIN;
INSERT INTO `meta_app` (`id`, `menu_id`, `permission_id`, `name`, `icon`, `intro`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171488916134948866, 881187162883403778, 881187314109034498, '通讯录', 'typcn:contacts', '通讯录', '0', '2025-11-25 16:24:44', '2025-11-25 16:24:44', '2025-11-25 10:57:44');
INSERT INTO `meta_app` (`id`, `menu_id`, `permission_id`, `name`, `icon`, `intro`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171527459418927105, 881187162883403778, 881187314109034498, '通讯录', 'typcn:contacts', '通讯录', '0', '2025-11-25 18:57:53', '2025-11-25 18:57:53', '2025-11-25 10:58:55');
INSERT INTO `meta_app` (`id`, `menu_id`, `permission_id`, `name`, `icon`, `intro`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171528976788090881, 881187162883403778, 881187314109034498, '通讯录', 'typcn:contacts', 'to', '0', '2025-11-25 19:03:55', '2025-11-25 19:03:55', '2025-11-25 11:04:06');
INSERT INTO `meta_app` (`id`, `menu_id`, `permission_id`, `name`, `icon`, `intro`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171529080861356034, 881187162883403778, 881187314109034498, '通讯录', 'typcn:contacts', '通讯录', '0', '2025-11-25 19:04:20', '2025-11-25 19:04:20', NULL);
INSERT INTO `meta_app` (`id`, `menu_id`, `permission_id`, `name`, `icon`, `intro`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1172811256819085314, 968448105293086721, 969898113037041666, '链接', 'tdesign:link', '12213', '0', '2025-11-29 07:59:15', '2025-11-29 07:59:15', NULL);
COMMIT;

-- ----------------------------
-- Table structure for meta_dict
-- ----------------------------
DROP TABLE IF EXISTS `meta_dict`;
CREATE TABLE `meta_dict` (
  `id` bigint NOT NULL COMMENT 'ID',
  `pid` bigint NOT NULL DEFAULT '0' COMMENT '父ID',
  `code` varchar(64) NOT NULL COMMENT '编码',
  `name` varchar(128) NOT NULL COMMENT '名称',
  `type` char(1) NOT NULL COMMENT '字典类型',
  `org_type` char(1) NOT NULL COMMENT '组织类型',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of meta_dict
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for meta_menu
-- ----------------------------
DROP TABLE IF EXISTS `meta_menu`;
CREATE TABLE `meta_menu` (
  `id` bigint unsigned NOT NULL COMMENT 'ID',
  `pid` bigint NOT NULL DEFAULT '0' COMMENT '父ID',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单名称',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单类型',
  `path` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单url',
  `enable_permission` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否开启权限',
  `permission_id` bigint NOT NULL DEFAULT '0' COMMENT '权限ID',
  `custom_view_path` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否自定义视图路径',
  `view_path` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '视图路径',
  `route_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '命名路由',
  `redirect` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '重定向',
  `icon` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '图标',
  `sort` int NOT NULL DEFAULT '999' COMMENT '排序',
  `is_cache` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否缓存',
  `hidden` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否隐藏',
  `hide_breadcrumb` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否隐藏面包屑',
  `props` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否匹配props',
  `org_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '组织类型',
  `link_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '链接类型',
  `link_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '链接url',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of meta_menu
-- ----------------------------
BEGIN;
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (782579756306313217, 0, '系统管理', '0', '/platform/system', b'1', 782647310861250562, 1, '@/layouts/InAppLayout.vue', NULL, '/platform/system/user', 'ingot:basic', 1, 0, 0, 0, 0, '0', '0', NULL, '0', '2022-12-18 11:58:35', '2025-12-05 10:48:53', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (868164540688019458, 0, '开发平台', '0', '/platform/develop', b'1', 868163807997636610, 1, '@/layouts/InAppLayout.vue', NULL, '/platform/develop/qrcode', 'material-symbols:developer-mode-tv-outline', 5, 0, 0, 0, 0, '0', '0', NULL, '0', '2023-08-11 16:01:57', '2025-11-26 09:55:20', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (868164933065158657, 868164540688019458, '生成二维码', '1', '/platform/develop/qrcode', b'1', 868164119483428866, 0, '@/pages/platform/develop/qrcode/IndexPage.vue', NULL, NULL, NULL, 10, 0, 0, 0, 0, '0', '0', NULL, '0', '2023-08-11 16:03:31', '2025-11-25 17:39:20', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (881182442898894850, 0, '客户端管理', '0', '/platform/member', b'1', 881181673671929858, 1, '@/layouts/InAppLayout.vue', NULL, '/platform/member/user', 'carbon:app', 5, 0, 0, 0, 0, '0', '0', NULL, '0', '2023-09-16 14:10:27', '2025-12-05 11:01:51', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (881182648029720577, 881182442898894850, '用户管理', '1', '/platform/member/user', b'1', 881181747638480898, 0, '@/pages/platform/member/user/IndexPage.vue', NULL, NULL, NULL, 1, 0, 0, 0, 0, '0', '0', NULL, '0', '2023-09-16 14:11:16', '2025-12-05 11:01:45', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (881182743173312514, 881182442898894850, '角色管理', '1', '/platform/member/role', b'1', 881181819205890049, 0, '@/pages/platform/member/role/IndexPage.vue', NULL, NULL, NULL, 1, 0, 0, 0, 0, '0', '0', NULL, '0', '2023-09-16 14:11:39', '2025-12-05 11:01:54', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (881187162883403778, 0, '通讯录', '0', '/org/contacts', b'1', 881187314109034498, 1, '@/layouts/InAppLayout.vue', NULL, '/org/contacts/user', 'typcn:contacts', 100, 0, 0, 0, 0, '1', '0', NULL, '0', '2023-09-16 14:29:12', '2025-11-25 18:09:03', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (881188246884495361, 881187162883403778, '成员管理', '1', '/org/contacts/user', b'1', 881187363425660929, 0, '@/pages/org/contacts/user/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '1', '0', NULL, '0', '2023-09-16 14:33:31', '2025-11-25 18:10:43', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (881188426534924289, 881187162883403778, '部门管理', '1', '/org/contacts/dept', b'1', 881187407549739010, 1, '@/pages/org/contacts/dept/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '1', '0', NULL, '0', '2023-09-16 14:34:14', '2024-05-19 10:23:30', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (881188564993093634, 881187162883403778, '角色管理', '1', '/org/contacts/role', b'1', 881187486947913730, 1, '@/pages/org/contacts/role/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '1', '0', NULL, '0', '2023-09-16 14:34:47', '2024-05-19 10:23:26', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (894887232728248321, 868164540688019458, '客户端管理', '1', '/platform/develop/client', b'1', 894886463954268162, 0, '@/pages/platform/develop/client/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2023-10-24 09:48:24', '2025-11-25 17:41:19', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (894887953284509698, 868164540688019458, '社交管理', '1', '/platform/develop/social', b'1', 894886696582950913, 0, '@/pages/platform/develop/social/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2023-10-24 09:51:15', '2025-11-25 17:39:28', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (894981446371684354, 868164540688019458, '业务ID管理', '1', '/platform/develop/id', b'1', 894979974569439234, 0, '@/pages/platform/develop/id/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2023-10-24 16:02:46', '2025-11-25 17:39:32', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (908759165842030594, 881187162883403778, '权限管理', '1', '/org/contacts/auth', b'1', 908756155371945986, 0, '@/pages/org/contacts/auth/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '1', '0', NULL, '0', '2023-12-01 16:30:30', '2025-05-08 10:05:50', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (968448105293086721, 0, '链接', '0', '/link', b'1', 969898113037041666, 1, '@/layouts/InAppLayout.vue', NULL, NULL, 'tdesign:link', 999, 0, 0, 0, 0, '1', '0', NULL, '0', '2024-05-14 09:33:02', '2025-02-01 19:31:13', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (968449951562792962, 968448105293086721, 'Vue', '1', '/link/ZAWG4gOT', b'1', 969898113037041666, 1, '@/layouts/InIFrameLayout.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '1', '1', 'https://cn.vuejs.org/', '0', '2024-05-14 09:40:22', '2024-05-19 10:23:55', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (968455189648703489, 968448105293086721, '阿里云', '1', '/link/ulahCLJw', b'1', 969898113037041666, 1, '@/layouts/InIFrameLayout.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '1', '1', 'http://www.aliyun.com', '0', '2024-05-14 10:01:11', '2024-05-19 10:23:59', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (968555619909173250, 968448105293086721, '百度', '1', '/link/CtA3FqHR', b'1', 969898113037041666, 1, '@/layouts/InExtLinkLayout.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '1', '2', 'https://www.baidu.com', '0', '2024-05-14 16:40:16', '2024-05-19 10:24:02', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (969900188395438082, 968448105293086721, '阿里云测试2', '1', '/link/NgEObclX', b'1', 970014721428488193, 1, '@/layouts/InIFrameLayout.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '1', '1', 'https://www.aliyun.com', '0', '2024-05-18 09:43:06', '2024-05-19 10:37:18', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1075465479671361538, 782579756306313217, '用户管理', '1', '/platform/system/user', b'1', 1075465479608446977, 0, '@/pages/platform/system/user/IndexPage.vue', NULL, NULL, NULL, 5, 0, 0, 0, 0, '0', '0', NULL, '0', '2025-03-05 17:01:53', '2025-12-21 14:11:52', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1098553864240046081, 881187162883403778, '组织架构', '1', '/org/contacts/structure', b'1', 1098553864210685954, 0, '@/pages/org/contacts/structure/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '1', '0', NULL, '0', '2025-05-08 10:06:52', '2025-11-25 17:15:45', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171383124115320834, 0, '元数据管理', '0', '/platform/meta', b'1', 1171383124023046146, 1, '@/layouts/InAppLayout.vue', NULL, '/platform/meta/menu', 'bi:meta', 1, 0, 0, 0, 0, '0', '0', NULL, '0', '2025-11-25 09:24:21', '2025-11-25 17:19:05', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171384799689437186, 1171383124115320834, '菜单管理', '1', '/platform/meta/menu', b'1', 1171384799576190978, 0, '@/pages/platform/meta/menu/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2025-11-25 09:31:01', '2025-11-25 17:19:32', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171385137007947777, 1171383124115320834, '角色管理', '1', '/platform/meta/role', b'1', 1171385136957616129, 0, '@/pages/platform/meta/role/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2025-11-25 09:32:21', '2025-11-25 17:19:11', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171385232201871361, 1171383124115320834, '权限管理', '1', '/platform/meta/permission', b'1', 1171385232147345409, 0, '@/pages/platform/meta/permission/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2025-11-25 09:32:44', '2025-11-25 17:19:15', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171385548041351169, 1171383124115320834, '应用管理', '1', '/platform/meta/app', b'1', 1171385547961659394, 0, '@/pages/platform/meta/app/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2025-11-25 09:33:59', '2025-11-25 17:19:26', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171506377827807234, 0, '组织管理', '0', '/platform/org', b'1', 1171506377735532545, 0, '@/layouts/InAppLayout.vue', NULL, '/platform/org/manage', 'clarity:organization-line', 3, 0, 0, 0, 0, '0', '0', NULL, '0', '2025-11-25 17:34:07', '2025-11-25 17:34:54', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171506495809384449, 1171506377827807234, '组织管理', '1', '/platform/org/tenant', b'1', 1171506495763247105, 0, '@/pages/platform/org/tenant/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2025-11-25 17:34:35', '2025-11-25 17:34:35', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1175028897327607810, 881182442898894850, '权限管理', '1', '/platform/member/permission', b'1', 1175028897264693250, 0, '@/pages/platform/member/permission/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2025-12-05 10:51:21', '2025-12-05 11:01:57', NULL);
INSERT INTO `meta_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1180877894980829185, 782579756306313217, '在线用户', '1', '/platform/system/onlinetoken', b'1', 1180877894834028546, 0, '@/pages/platform/system/onlinetoken/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2025-12-21 14:13:11', '2025-12-21 14:14:31', NULL);
COMMIT;

-- ----------------------------
-- Table structure for meta_permission
-- ----------------------------
DROP TABLE IF EXISTS `meta_permission`;
CREATE TABLE `meta_permission` (
  `id` bigint unsigned NOT NULL COMMENT 'ID',
  `pid` bigint unsigned NOT NULL DEFAULT '0' COMMENT '父ID',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限名称',
  `code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限编码',
  `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型',
  `org_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组织类型',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `remark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '' COMMENT '备注',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_code` (`code`) USING BTREE COMMENT '编码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of meta_permission
-- ----------------------------
BEGIN;
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (782647310861250562, 0, '系统管理', 'platform:system', '0', '0', '0', '', '2022-12-18 16:27:01', '2025-12-05 10:48:53', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (782650139818635265, 782647310861250562, '租户管理', 'platform:system:tenant', '0', '0', '0', '', '2022-12-18 16:38:15', NULL, NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (868163807997636610, 0, '开发平台', 'platform:develop', '0', '0', '0', '', '2023-08-11 15:59:03', '2025-11-26 09:55:20', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (868164119483428866, 868163807997636610, '生成二维码', 'platform:develop:qrcode', '0', '0', '0', '', '2023-08-11 16:00:17', '2025-11-25 17:39:20', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (881181673671929858, 0, '客户端管理', 'platform:member', '0', '0', '0', '', '2023-09-16 14:07:24', '2025-12-05 11:01:51', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (881181747638480898, 881181673671929858, '用户管理', 'platform:member:user', '0', '0', '0', '', '2023-09-16 14:07:41', '2025-12-05 11:01:45', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (881181819205890049, 881181673671929858, '角色管理', 'platform:member:role', '0', '0', '0', '', '2023-09-16 14:07:58', '2025-12-05 11:01:54', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (881187314109034498, 0, '通讯录', 'org:contacts', '0', '1', '0', '', '2023-09-16 14:29:48', '2025-11-25 18:09:03', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (881187363425660929, 881187314109034498, '成员管理', 'org:contacts:user', '0', '1', '0', '', '2023-09-16 14:30:00', '2025-11-25 18:10:43', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (881187407549739010, 881187314109034498, '部门管理', 'org:contacts:dept', '0', '1', '0', '', '2023-09-16 14:30:11', '2024-05-19 10:23:30', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (881187486947913730, 881187314109034498, '角色管理', 'org:contacts:role', '0', '1', '0', '', '2023-09-16 14:30:30', '2024-05-19 10:23:26', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (894886463954268162, 868163807997636610, '客户端管理', 'platform:develop:client', '0', '0', '0', '', '2023-10-24 09:45:20', '2025-11-25 17:41:19', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (894886696582950913, 868163807997636610, '社交管理', 'platform:develop:social', '0', '0', '0', '', '2023-10-24 09:46:16', '2025-11-25 17:39:28', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (894979974569439234, 868163807997636610, 'ID管理', 'platform:develop:id', '0', '0', '0', '', '2023-10-24 15:56:55', '2025-11-25 17:39:32', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (908756155371945986, 881187314109034498, '权限管理', 'org:contacts:auth', '0', '1', '0', '', '2023-12-01 16:18:32', '2025-05-08 10:05:50', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (969898113037041666, 0, '链接', 'link', '0', '1', '0', '', '2024-05-18 09:34:51', '2025-02-01 19:31:13', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (970014721428488193, 969898113037041666, '阿里云测试2', 'link:asdasd', '0', '1', '0', '', '2024-05-18 17:18:13', '2024-05-19 10:37:18', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1075465479608446977, 782647310861250562, '用户管理', 'platform:system:user', '0', '0', '0', '', '2025-03-05 17:01:53', '2025-12-21 14:11:52', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1098553864210685954, 881187314109034498, '组织架构', 'org:contacts:structure', '0', '1', '0', '', '2025-05-08 10:06:52', '2025-11-25 17:15:45', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171383124023046146, 0, '元数据管理', 'platform:meta', '0', '0', '0', '', '2025-11-25 09:24:21', '2025-11-25 17:19:05', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171384799576190978, 1171383124023046146, '菜单管理', 'platform:meta:menu', '0', '0', '0', '', '2025-11-25 09:31:01', '2025-11-25 17:19:32', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171385136957616129, 1171383124023046146, '角色管理', 'platform:meta:role', '0', '0', '0', '', '2025-11-25 09:32:21', '2025-11-25 17:19:11', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171385232147345409, 1171383124023046146, '权限管理', 'platform:meta:permission', '0', '0', '0', '', '2025-11-25 09:32:44', '2025-11-25 17:19:15', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171385547961659394, 1171383124023046146, '应用管理', 'platform:meta:app', '0', '0', '0', '', '2025-11-25 09:33:59', '2025-11-25 17:19:26', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171506377735532545, 0, '组织管理', 'platform:org', '0', '0', '0', '', '2025-11-25 17:34:07', '2025-11-25 17:34:54', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171506495763247105, 1171506377735532545, '组织管理', 'platform:org:tenant', '0', '0', '0', '', '2025-11-25 17:34:35', '2025-11-25 17:34:35', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1175028897264693250, 881181673671929858, '权限管理', 'platform:member:permission', '0', '0', '0', '', '2025-12-05 10:51:21', '2025-12-05 11:01:57', NULL);
INSERT INTO `meta_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1180877894834028546, 782647310861250562, '在线用户', 'system:onlientoken', '0', '0', '0', '', '2025-12-21 14:13:11', '2025-12-21 14:14:31', NULL);
COMMIT;

-- ----------------------------
-- Table structure for meta_role
-- ----------------------------
DROP TABLE IF EXISTS `meta_role`;
CREATE TABLE `meta_role` (
  `id` bigint unsigned NOT NULL COMMENT 'ID',
  `pid` bigint NOT NULL DEFAULT '0' COMMENT 'PID',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT '角色名称',
  `code` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT '角色编码',
  `subject` char(1) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '角色主题',
  `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '角色类型',
  `org_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '组织类型',
  `filter_dept` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否过滤部门',
  `scope_type` int NOT NULL DEFAULT '0' COMMENT '数据范围类型',
  `scopes` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '[]' COMMENT '数据范围',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_code` (`code`) USING BTREE COMMENT '编码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of meta_role
-- ----------------------------
BEGIN;
INSERT INTO `meta_role` (`id`, `pid`, `name`, `code`, `subject`, `type`, `org_type`, `filter_dept`, `scope_type`, `scopes`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1, 0, '超级管理员', 'role_admin', '0', '0', '0', 0, 0, '[]', '0', '2021-01-03 11:07:59', '2025-04-03 16:46:14', NULL);
INSERT INTO `meta_role` (`id`, `pid`, `name`, `code`, `subject`, `type`, `org_type`, `filter_dept`, `scope_type`, `scopes`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (2, 1000, '管理员', 'role_org_admin', '0', '0', '1', 0, 0, '[]', '0', '2021-06-23 09:28:19', '2024-05-13 14:04:55', NULL);
INSERT INTO `meta_role` (`id`, `pid`, `name`, `code`, `subject`, `type`, `org_type`, `filter_dept`, `scope_type`, `scopes`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (3, 1000, '子管理员', 'role_org_sub_admin', '0', '0', '1', 0, 0, '[]', '0', '2021-06-23 09:28:33', '2025-11-27 14:15:06', NULL);
INSERT INTO `meta_role` (`id`, `pid`, `name`, `code`, `subject`, `type`, `org_type`, `filter_dept`, `scope_type`, `scopes`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000, 0, '默认', '', '0', '1', '1', 0, 0, '[]', '0', '2025-11-24 16:59:34', '2025-11-25 13:04:57', NULL);
INSERT INTO `meta_role` (`id`, `pid`, `name`, `code`, `subject`, `type`, `org_type`, `filter_dept`, `scope_type`, `scopes`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1075807902184353794, 1000, '主管', 'role_org_manager', '0', '0', '1', 1, 2, '[]', '0', '2025-03-06 15:42:33', '2025-04-29 17:29:02', NULL);
INSERT INTO `meta_role` (`id`, `pid`, `name`, `code`, `subject`, `type`, `org_type`, `filter_dept`, `scope_type`, `scopes`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171439550737149954, 0, '测试', '', '0', '1', '0', 0, 0, '[]', '0', '2025-11-25 13:08:34', '2025-11-25 13:11:55', '2025-11-25 05:20:10');
INSERT INTO `meta_role` (`id`, `pid`, `name`, `code`, `subject`, `type`, `org_type`, `filter_dept`, `scope_type`, `scopes`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171439627882983426, 0, '123', '412', '0', '1', '0', 0, 0, '[]', '9', '2025-11-25 13:08:53', '2025-11-25 13:11:45', '2025-11-25 05:11:48');
INSERT INTO `meta_role` (`id`, `pid`, `name`, `code`, `subject`, `type`, `org_type`, `filter_dept`, `scope_type`, `scopes`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171442381980430338, 1171439550737149954, '1231222', 'role_asad', '0', '0', '0', 0, 0, '[]', '0', '2025-11-25 13:19:49', '2025-11-25 13:19:56', '2025-11-25 05:20:06');
COMMIT;

-- ----------------------------
-- Table structure for meta_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `meta_role_permission`;
CREATE TABLE `meta_role_permission` (
  `id` bigint NOT NULL COMMENT 'ID',
  `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
  `permission_id` bigint unsigned NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of meta_role_permission
-- ----------------------------
BEGIN;
INSERT INTO `meta_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1172810250299371522, 1, 782647310861250562);
INSERT INTO `meta_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1172810250307760129, 1, 868163807997636610);
INSERT INTO `meta_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1172810250311954434, 1, 881181673671929858);
INSERT INTO `meta_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1172810250311954435, 1, 1171383124023046146);
INSERT INTO `meta_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1172810250316148737, 1, 1171506377735532545);
INSERT INTO `meta_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1172814842718318593, 2, 881187314109034498);
COMMIT;

-- ----------------------------
-- Table structure for sys_social_details
-- ----------------------------
DROP TABLE IF EXISTS `sys_social_details`;
CREATE TABLE `sys_social_details` (
  `id` bigint unsigned NOT NULL COMMENT 'ID',
  `tenant_id` bigint unsigned NOT NULL COMMENT '租户ID',
  `app_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'App ID',
  `app_secret` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'App Secret',
  `redirect_url` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '重定向地址',
  `name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '社交名称',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '类型',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of sys_social_details
-- ----------------------------
BEGIN;
INSERT INTO `sys_social_details` (`id`, `tenant_id`, `app_id`, `app_secret`, `redirect_url`, `name`, `type`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (925397365717008385, 1, 'wx18dc0ea8c6ca1d7d', '405c60badf7b620189aae44f5de635a9', NULL, 'ingot开源小程序', 'wechat_miniprogram', '0', '2024-01-16 14:24:46', '2025-12-07 14:41:26', NULL);
INSERT INTO `sys_social_details` (`id`, `tenant_id`, `app_id`, `app_secret`, `redirect_url`, `name`, `type`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1175813676360687618, 1, 'wx123124124', '12312dwaswd', NULL, 'aaaa', 'wechat_miniprogram', '0', '2025-12-07 14:49:47', '2025-12-07 14:49:47', '2025-12-07 06:50:08');
INSERT INTO `sys_social_details` (`id`, `tenant_id`, `app_id`, `app_secret`, `redirect_url`, `name`, `type`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1175838348993257473, 1, '123123', '123123', NULL, '123123', 'wechat_miniprogram', '0', '2025-12-07 16:27:50', '2025-12-07 16:27:50', '2025-12-07 08:36:08');
INSERT INTO `sys_social_details` (`id`, `tenant_id`, `app_id`, `app_secret`, `redirect_url`, `name`, `type`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1177538662951268353, 1, '123', '123', NULL, '123124', 'wechat_miniprogram', '0', '2025-12-12 09:04:16', '2025-12-12 09:04:16', '2025-12-12 01:08:38');
COMMIT;

-- ----------------------------
-- Table structure for sys_tenant
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant` (
  `id` bigint unsigned NOT NULL COMMENT 'ID',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户名称',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户编号',
  `org_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '组织类型',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '头像',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `plan_id` bigint NOT NULL DEFAULT '0' COMMENT '计划ID',
  `end_at` datetime DEFAULT NULL COMMENT '结束日期',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of sys_tenant
-- ----------------------------
BEGIN;
INSERT INTO `sys_tenant` (`id`, `name`, `code`, `org_type`, `avatar`, `status`, `plan_id`, `end_at`, `created_at`, `updated_at`, `deleted_at`) VALUES (1, '英格特云', 'ingot', '0', 'http://ingot-cloud:9090/ingot/public/tenant/logo.png?t=1710140674513', '0', 0, NULL, '2021-01-06 13:48:26', '2025-11-25 17:01:06', NULL);
INSERT INTO `sys_tenant` (`id`, `name`, `code`, `org_type`, `avatar`, `status`, `plan_id`, `end_at`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171513454855974913, '测试组织', 'org_%d128122', '1', 'ingot/tenant/ic_logo.png', '0', 0, NULL, '2025-11-25 18:02:14', '2025-11-25 18:06:55', '2025-11-25 10:06:58');
INSERT INTO `sys_tenant` (`id`, `name`, `code`, `org_type`, `avatar`, `status`, `plan_id`, `end_at`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171766486562762754, '测试组织', 'org_%d192114', '1', 'ingot/user/avatar/ic_logo.png', '0', 0, NULL, '2025-11-26 10:47:42', NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_tenant_plan
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant_plan`;
CREATE TABLE `sys_tenant_plan` (
  `id` bigint NOT NULL COMMENT 'ID',
  `name` varchar(32) NOT NULL COMMENT '计划名字',
  `type` char(1) NOT NULL COMMENT '计划类型',
  `duration` int NOT NULL COMMENT '持续时间',
  `unit` char(1) NOT NULL COMMENT '单位',
  `created_at` datetime NOT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of sys_tenant_plan
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sys_tenant_plan_record
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant_plan_record`;
CREATE TABLE `sys_tenant_plan_record` (
  `id` bigint NOT NULL COMMENT 'ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `plan_id` int NOT NULL COMMENT '计划ID',
  `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '计划类型',
  `duration` int NOT NULL COMMENT '持续时间',
  `unit` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '单位',
  `created_at` datetime NOT NULL COMMENT '创建日期',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of sys_tenant_plan_record
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint unsigned NOT NULL COMMENT 'ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '用户名',
  `password` varchar(300) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '密码',
  `init_pwd` tinyint(1) NOT NULL DEFAULT '1' COMMENT '初始化密码标识',
  `nickname` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '昵称',
  `phone` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '手机号',
  `email` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '邮件地址',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '头像',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_username` (`username`) USING BTREE COMMENT '用户名',
  KEY `idx_phone` (`phone`) USING BTREE COMMENT '手机号',
  KEY `idx_email` (`email`) USING BTREE COMMENT '邮箱'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
BEGIN;
INSERT INTO `sys_user` (`id`, `username`, `password`, `init_pwd`, `nickname`, `phone`, `email`, `avatar`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1, 'admin', '{bcrypt}$2a$10$PMLmpD0dmDn2Dkc23bshJ.IG8EtcJ9CxfYCQoPgbr9qKhC2DwuohK', 0, '超级管理员', '18888888888', 'admin@ingot.com', 'ingot/user/avatar/1/logo.png', '0', '2021-01-03 11:02:46', '2025-11-25 18:28:48', NULL);
INSERT INTO `sys_user` (`id`, `username`, `password`, `init_pwd`, `nickname`, `phone`, `email`, `avatar`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (883383523896766465, '18603243837', '{bcrypt}$2a$10$WklERpnX/5sqH.qkEDG9Kep9RfWC6hGgD7ksZQXHtxI/R5epa4zWO', 0, '王超', '18603243837', NULL, 'ingot/user/avatar/883383523896766465/logo.png', '0', '2023-09-22 15:56:46', '2025-09-05 07:45:50', NULL);
INSERT INTO `sys_user` (`id`, `username`, `password`, `init_pwd`, `nickname`, `phone`, `email`, `avatar`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (968109737946447873, '11111111111', '{bcrypt}$2a$10$2Lu9D4.M/dTzoXFgbNh5cOMfQI.zCNY6sHym8ZnuYvs3R6oOv/Udu', 0, '测试用户', '11111111111', NULL, 'ingot/user/avatar/968109737946447873/logo.png', '0', '2024-05-13 11:08:29', '2025-11-26 14:18:11', NULL);
INSERT INTO `sys_user` (`id`, `username`, `password`, `init_pwd`, `nickname`, `phone`, `email`, `avatar`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1009463137401110530, '12312312312', '{bcrypt}$2a$10$dWq5aNMSQJM2BP4bYPaCZuNK0APKrb0G3z8WVlMrUd3zfwc9pxpHO', 1, '1231233', '12312312312', NULL, 'ingot/user/avatar/1009463137401110530/ic_logo.png', '0', '2024-09-04 13:52:08', '2025-11-26 10:47:16', NULL);
INSERT INTO `sys_user` (`id`, `username`, `password`, `init_pwd`, `nickname`, `phone`, `email`, `avatar`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1095389297838202881, '18603243838', '{bcrypt}$2a$10$9wod3QviKn2YYzslhjeQhew98DoV4qfMb6HipqOPyMyCQQwk5XsBK', 1, '测试1人员', '18603243838', NULL, 'ingot/user/avatar/1095389297838202881/logo.png', '0', '2025-04-29 16:32:01', '2025-09-05 08:48:37', NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_user_social
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_social`;
CREATE TABLE `sys_user_social` (
  `id` bigint NOT NULL COMMENT 'ID',
  `tenant_id` bigint NOT NULL COMMENT '组织ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道类型',
  `unique_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '渠道唯一ID',
  `bind_at` datetime NOT NULL COMMENT '绑定时间',
  PRIMARY KEY (`id`),
  KEY `idx_unique_type_user` (`unique_id`,`type`,`user_id`) USING BTREE COMMENT '渠道用户索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of sys_user_social
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sys_user_tenant
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_tenant`;
CREATE TABLE `sys_user_tenant` (
  `id` bigint NOT NULL COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `main` bit(1) NOT NULL COMMENT '是否为主要租户',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '租户名称',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '头像',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user` (`user_id`) USING BTREE,
  KEY `idx_tenant_user` (`tenant_id`,`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of sys_user_tenant
-- ----------------------------
BEGIN;
INSERT INTO `sys_user_tenant` (`id`, `user_id`, `tenant_id`, `main`, `name`, `avatar`, `created_at`) VALUES (1, 1, 1, b'1', '英格特云', 'http://ingot-cloud:9090/ingot/public/tenant/logo.png?t=1710140674513', '2023-09-22 16:21:52');
INSERT INTO `sys_user_tenant` (`id`, `user_id`, `tenant_id`, `main`, `name`, `avatar`, `created_at`) VALUES (1171517865699307521, 883383523896766465, 1, b'1', '英格特云', 'http://ingot-cloud:9090/ingot/public/tenant/logo.png?t=1710140674513', '2025-11-25 18:19:46');
INSERT INTO `sys_user_tenant` (`id`, `user_id`, `tenant_id`, `main`, `name`, `avatar`, `created_at`) VALUES (1171766486688591873, 883383523896766465, 1171766486562762754, b'0', '测试组织', 'ingot/user/avatar/ic_logo.png', '2025-11-26 10:47:42');
INSERT INTO `sys_user_tenant` (`id`, `user_id`, `tenant_id`, `main`, `name`, `avatar`, `created_at`) VALUES (1171818507911294978, 968109737946447873, 1171766486562762754, b'1', '测试组织', 'ingot/user/avatar/ic_logo.png', '2025-11-26 14:14:25');
COMMIT;

-- ----------------------------
-- Table structure for tenant_app_config
-- ----------------------------
DROP TABLE IF EXISTS `tenant_app_config`;
CREATE TABLE `tenant_app_config` (
  `id` bigint NOT NULL COMMENT 'ID',
  `meta_id` bigint NOT NULL DEFAULT '0' COMMENT '元数据ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `enabled` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否启用',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_meta` (`tenant_id`,`meta_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of tenant_app_config
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for tenant_dept
-- ----------------------------
DROP TABLE IF EXISTS `tenant_dept`;
CREATE TABLE `tenant_dept` (
  `id` bigint unsigned NOT NULL COMMENT 'ID',
  `tenant_id` bigint unsigned NOT NULL COMMENT '租户ID',
  `pid` bigint unsigned NOT NULL DEFAULT '0' COMMENT '父ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门名称',
  `sort` int NOT NULL DEFAULT '999' COMMENT '排序',
  `main_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '主部门标识',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of tenant_dept
-- ----------------------------
BEGIN;
INSERT INTO `tenant_dept` (`id`, `tenant_id`, `pid`, `name`, `sort`, `main_flag`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1, 1, 0, '英格特云', 0, b'1', '1', '2025-11-24 16:36:06', '2025-11-24 16:36:08', NULL);
INSERT INTO `tenant_dept` (`id`, `tenant_id`, `pid`, `name`, `sort`, `main_flag`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171513454902112258, 1171513454855974913, 0, '测试组织', 0, b'1', '0', '2025-11-25 18:02:14', NULL, '2025-11-25 10:06:58');
INSERT INTO `tenant_dept` (`id`, `tenant_id`, `pid`, `name`, `sort`, `main_flag`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171517787697836033, 1, 1, '测试部门', 999, b'0', '0', '2025-11-25 18:19:27', '2025-12-23 09:33:41', NULL);
INSERT INTO `tenant_dept` (`id`, `tenant_id`, `pid`, `name`, `sort`, `main_flag`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171520634774614018, 1, 1171517787697836033, '测试小组', 999, b'0', '0', '2025-11-25 18:30:46', NULL, NULL);
INSERT INTO `tenant_dept` (`id`, `tenant_id`, `pid`, `name`, `sort`, `main_flag`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171766486592122882, 1171766486562762754, 0, '测试组织', 0, b'1', '0', '2025-11-26 10:47:42', NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for tenant_role_permission_private
-- ----------------------------
DROP TABLE IF EXISTS `tenant_role_permission_private`;
CREATE TABLE `tenant_role_permission_private` (
  `id` bigint NOT NULL COMMENT 'ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
  `meta_role` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否为元数据角色',
  `permission_id` bigint unsigned NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_role` (`tenant_id`,`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of tenant_role_permission_private
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for tenant_role_private
-- ----------------------------
DROP TABLE IF EXISTS `tenant_role_private`;
CREATE TABLE `tenant_role_private` (
  `id` bigint unsigned NOT NULL COMMENT 'ID',
  `pid` bigint NOT NULL DEFAULT '0' COMMENT 'PID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT '角色名称',
  `code` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT '角色编码',
  `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '角色类型',
  `filter_dept` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否过滤部门',
  `scope_type` int NOT NULL DEFAULT '0' COMMENT '数据范围类型',
  `scopes` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '[]' COMMENT '数据范围',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`),
  KEY `idx_code` (`tenant_id`,`code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of tenant_role_private
-- ----------------------------
BEGIN;
INSERT INTO `tenant_role_private` (`id`, `pid`, `tenant_id`, `name`, `code`, `type`, `filter_dept`, `scope_type`, `scopes`, `status`, `sort`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171742951152807937, 0, 1, '测试组', '', '1', 0, 0, '[]', '0', 0, '2025-11-26 09:14:11', '2025-11-26 09:14:35', NULL);
INSERT INTO `tenant_role_private` (`id`, `pid`, `tenant_id`, `name`, `code`, `type`, `filter_dept`, `scope_type`, `scopes`, `status`, `sort`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171744270194307074, 1171742951152807937, 1, '测试角色1', 'role_org_2368118', '0', 0, 1, '[1171517787697836033]', '0', 0, '2025-11-26 09:19:25', '2025-11-26 09:19:40', '2025-11-26 01:19:49');
INSERT INTO `tenant_role_private` (`id`, `pid`, `tenant_id`, `name`, `code`, `type`, `filter_dept`, `scope_type`, `scopes`, `status`, `sort`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171746929370460162, 0, 1, '测试组2', '', '1', 0, 0, '[]', '0', 0, '2025-11-26 09:29:59', '2025-11-26 09:29:59', NULL);
INSERT INTO `tenant_role_private` (`id`, `pid`, `tenant_id`, `name`, `code`, `type`, `filter_dept`, `scope_type`, `scopes`, `status`, `sort`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171747701302751233, 1171742951152807937, 1, '测试角色', 'role_org_2368148', '0', 1, 2, '[]', '0', 0, '2025-11-26 09:33:03', '2025-11-27 16:05:48', NULL);
COMMIT;

-- ----------------------------
-- Table structure for tenant_role_user_private
-- ----------------------------
DROP TABLE IF EXISTS `tenant_role_user_private`;
CREATE TABLE `tenant_role_user_private` (
  `id` bigint NOT NULL COMMENT 'ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
  `meta_role` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否为元数据角色',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID，可以为空，部门角色该字段不为空',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`) USING BTREE,
  KEY `idx_tenant_role` (`tenant_id`,`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of tenant_role_user_private
-- ----------------------------
BEGIN;
INSERT INTO `tenant_role_user_private` (`id`, `tenant_id`, `role_id`, `meta_role`, `user_id`, `dept_id`) VALUES (1171818434175430657, 1171766486562762754, 3, b'1', 883383523896766465, NULL);
INSERT INTO `tenant_role_user_private` (`id`, `tenant_id`, `role_id`, `meta_role`, `user_id`, `dept_id`) VALUES (1171818508146176001, 1171766486562762754, 3, b'1', 968109737946447873, NULL);
INSERT INTO `tenant_role_user_private` (`id`, `tenant_id`, `role_id`, `meta_role`, `user_id`, `dept_id`) VALUES (1172813477321371650, 1, 1, b'1', 1, NULL);
INSERT INTO `tenant_role_user_private` (`id`, `tenant_id`, `role_id`, `meta_role`, `user_id`, `dept_id`) VALUES (1172813477325565954, 1, 2, b'1', 1, NULL);
INSERT INTO `tenant_role_user_private` (`id`, `tenant_id`, `role_id`, `meta_role`, `user_id`, `dept_id`) VALUES (1181257390797959170, 1, 3, b'1', 883383523896766465, NULL);
INSERT INTO `tenant_role_user_private` (`id`, `tenant_id`, `role_id`, `meta_role`, `user_id`, `dept_id`) VALUES (1181532332944863234, 1, 1075807902184353794, b'1', 883383523896766465, 1171517787697836033);
COMMIT;

-- ----------------------------
-- Table structure for tenant_user_dept_private
-- ----------------------------
DROP TABLE IF EXISTS `tenant_user_dept_private`;
CREATE TABLE `tenant_user_dept_private` (
  `id` bigint NOT NULL COMMENT 'ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_dept` (`tenant_id`,`dept_id`) USING BTREE,
  KEY `idx_tenant_user` (`tenant_id`,`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of tenant_user_dept_private
-- ----------------------------
BEGIN;
INSERT INTO `tenant_user_dept_private` (`id`, `tenant_id`, `user_id`, `dept_id`) VALUES (1171818434003464194, 1171766486562762754, 883383523896766465, 1171766486592122882);
INSERT INTO `tenant_user_dept_private` (`id`, `tenant_id`, `user_id`, `dept_id`) VALUES (1171818508011958274, 1171766486562762754, 968109737946447873, 1171766486592122882);
INSERT INTO `tenant_user_dept_private` (`id`, `tenant_id`, `user_id`, `dept_id`) VALUES (1171818619999875073, 1, 883383523896766465, 1);
INSERT INTO `tenant_user_dept_private` (`id`, `tenant_id`, `user_id`, `dept_id`) VALUES (1171818619999875074, 1, 883383523896766465, 1171520634774614018);
INSERT INTO `tenant_user_dept_private` (`id`, `tenant_id`, `user_id`, `dept_id`) VALUES (1172813477086490626, 1, 1, 1);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
