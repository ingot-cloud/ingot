/*
 Navicat Premium Data Transfer

 Source Server         : mysql-localhost
 Source Server Type    : MySQL
 Source Server Version : 50735
 Source Host           : localhost:3306
 Source Schema         : ingot_core

 Target Server Type    : MySQL
 Target Server Version : 50735
 File Encoding         : 65001

 Date: 29/12/2022 19:45:06
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for oauth2_authorization
-- ----------------------------
DROP TABLE IF EXISTS `oauth2_authorization`;
CREATE TABLE `oauth2_authorization` (
  `id` varchar(100) NOT NULL,
  `registered_client_id` varchar(100) NOT NULL,
  `principal_name` varchar(200) NOT NULL,
  `authorization_grant_type` varchar(100) NOT NULL,
  `authorized_scopes` varchar(1000) DEFAULT NULL,
  `attributes` varchar(4000) DEFAULT NULL,
  `state` varchar(500) DEFAULT NULL,
  `authorization_code_value` blob,
  `authorization_code_issued_at` datetime DEFAULT NULL,
  `authorization_code_expires_at` datetime DEFAULT NULL,
  `authorization_code_metadata` varchar(2000) DEFAULT NULL,
  `access_token_value` blob,
  `access_token_issued_at` datetime DEFAULT NULL,
  `access_token_expires_at` datetime DEFAULT NULL,
  `access_token_metadata` varchar(2000) DEFAULT NULL,
  `access_token_type` varchar(100) DEFAULT NULL,
  `access_token_scopes` varchar(1000) DEFAULT NULL,
  `oidc_id_token_value` blob,
  `oidc_id_token_issued_at` datetime DEFAULT NULL,
  `oidc_id_token_expires_at` datetime DEFAULT NULL,
  `oidc_id_token_metadata` varchar(2000) DEFAULT NULL,
  `refresh_token_value` blob,
  `refresh_token_issued_at` datetime DEFAULT NULL,
  `refresh_token_expires_at` datetime DEFAULT NULL,
  `refresh_token_metadata` varchar(2000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for oauth2_authorization_consent
-- ----------------------------
DROP TABLE IF EXISTS `oauth2_authorization_consent`;
CREATE TABLE `oauth2_authorization_consent` (
  `registered_client_id` varchar(100) NOT NULL,
  `principal_name` varchar(200) NOT NULL,
  `authorities` varchar(1000) NOT NULL,
  `tenant_id` int(11) unsigned NOT NULL DEFAULT '1' COMMENT '租户ID',
  PRIMARY KEY (`registered_client_id`,`principal_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of oauth2_authorization_consent
-- ----------------------------
BEGIN;
INSERT INTO `oauth2_authorization_consent` VALUES ('ingot', 'admin', 'SCOPE_message.read', 1);
COMMIT;

-- ----------------------------
-- Table structure for oauth2_registered_client
-- ----------------------------
DROP TABLE IF EXISTS `oauth2_registered_client`;
CREATE TABLE `oauth2_registered_client` (
  `id` varchar(100) NOT NULL COMMENT 'ID',
  `client_id` varchar(100) NOT NULL COMMENT '客户端ID',
  `client_id_issued_at` datetime NOT NULL COMMENT 'client id 发布时间',
  `client_secret` varchar(200) CHARACTER SET utf8 DEFAULT NULL COMMENT '客户端秘钥',
  `client_secret_expires_at` datetime DEFAULT NULL COMMENT '秘钥过期时间',
  `client_name` varchar(200) CHARACTER SET utf8 NOT NULL COMMENT '客户端名称',
  `client_authentication_methods` varchar(1000) CHARACTER SET utf8 NOT NULL DEFAULT ',' COMMENT '客户端认证方法',
  `authorization_grant_types` varchar(1000) CHARACTER SET utf8 NOT NULL COMMENT '客户端可以使用的授权类型',
  `redirect_uris` varchar(1000) CHARACTER SET utf8 DEFAULT NULL COMMENT '重定向URL',
  `scopes` varchar(1000) CHARACTER SET utf8 NOT NULL COMMENT '客户端的访问范围',
  `client_settings` varchar(2000) CHARACTER SET utf8 NOT NULL COMMENT '客户端设置',
  `token_settings` varchar(2000) CHARACTER SET utf8 NOT NULL COMMENT 'token设置',
  `tenant_id` int(11) NOT NULL DEFAULT '1' COMMENT '租户ID',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`),
  KEY `clientId` (`tenant_id`,`client_id`) USING BTREE COMMENT '客户端ID',
  KEY `clientName` (`tenant_id`,`client_name`) USING BTREE COMMENT '客户端名称',
  KEY `clientIdIssuedAt` (`client_id_issued_at`) USING BTREE COMMENT '发布时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of oauth2_registered_client
-- ----------------------------
BEGIN;
INSERT INTO `oauth2_registered_client` VALUES ('ingot', 'ingot', '2020-11-20 15:57:29', '{noop}ingot', NULL, 'ingot-cloud', 'client_secret_basic', 'client_credentials,password,authorization_code,refresh_token', 'https://ingotcloud.com', 'message.read,message.write', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.client.require-authorization-consent\":true,\"ingot.settings.client.status\":\"0\",\"settings.client.require-proof-key\":false}', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.token.reuse-refresh-tokens\":false,\"settings.token.id-token-signature-algorithm\":[\"org.springframework.security.oauth2.jose.jws.SignatureAlgorithm\",\"RS256\"],\"settings.token.access-token-time-to-live\":[\"java.time.Duration\",7200.000000000],\"ingot.settings.token.auth-type\":\"1\",\"settings.token.access-token-format\":{\"@class\":\"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat\",\"value\":\"self-contained\"},\"settings.token.refresh-token-time-to-live\":[\"java.time.Duration\",604800.000000000],\"settings.token.authorization-code-time-to-live\":[\"java.time.Duration\",300.000000000]}', 1, '2022-12-29 19:33:07', NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_authority
-- ----------------------------
DROP TABLE IF EXISTS `sys_authority`;
CREATE TABLE `sys_authority` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'ID',
  `tenant_id` bigint(20) unsigned NOT NULL COMMENT '租户ID',
  `pid` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '父ID',
  `name` varchar(32) NOT NULL COMMENT '权限名称',
  `code` varchar(128) NOT NULL COMMENT '权限编码',
  `status` char(1) CHARACTER SET utf8 NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `remark` varchar(255) CHARACTER SET utf8 DEFAULT '' COMMENT '备注',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tenant-code` (`tenant_id`,`code`) USING BTREE COMMENT '编码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_authority
-- ----------------------------
BEGIN;
INSERT INTO `sys_authority` VALUES (782647310861250562, 1, 0, '基础管理', 'basic', '0', '', '2022-12-18 16:27:01', NULL, NULL);
INSERT INTO `sys_authority` VALUES (782649986701373442, 1, 782647310861250562, '用户管理', 'basic.user', '0', '', '2022-12-18 16:37:39', NULL, NULL);
INSERT INTO `sys_authority` VALUES (782650056679141377, 1, 782647310861250562, '部门管理', 'basic.dept', '0', '', '2022-12-18 16:37:56', NULL, NULL);
INSERT INTO `sys_authority` VALUES (782650095858135041, 1, 782647310861250562, '角色管理', 'basic.role', '0', '', '2022-12-18 16:38:05', NULL, NULL);
INSERT INTO `sys_authority` VALUES (782650139818635265, 1, 782647310861250562, '租户管理', 'basic.tenant', '0', '', '2022-12-18 16:38:15', NULL, NULL);
INSERT INTO `sys_authority` VALUES (782650179794546690, 1, 782647310861250562, '菜单管理', 'basic.menu', '0', '', '2022-12-18 16:38:25', NULL, NULL);
INSERT INTO `sys_authority` VALUES (782650237604638722, 1, 782647310861250562, '权限管理', 'basic.authority', '0', '', '2022-12-18 16:38:39', '2022-12-26 21:10:14', NULL);
INSERT INTO `sys_authority` VALUES (782650307704041474, 1, 782647310861250562, '客户端管理', 'basic.client', '0', '', '2022-12-18 16:38:55', '2022-12-27 12:19:35', NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'ID',
  `tenant_id` bigint(20) unsigned NOT NULL COMMENT '租户ID',
  `pid` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '父ID',
  `name` varchar(50) NOT NULL COMMENT '部门名称',
  `scope` char(1) NOT NULL DEFAULT '0' COMMENT '部门角色范围, 0:当前部门，1:当前部门和直接子部门',
  `sort` int(11) NOT NULL DEFAULT '999' COMMENT '排序',
  `status` char(1) CHARACTER SET utf8 NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tenant` (`tenant_id`) USING BTREE COMMENT '租户'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
BEGIN;
INSERT INTO `sys_dept` VALUES (1, 1, 0, 'Ingot管理平台', '0', 1, '0', '2022-12-17 15:50:07', '2022-12-20 12:58:43', NULL);
INSERT INTO `sys_dept` VALUES (782235460805898241, 1, 0, '测试部门', '0', 10, '0', '2022-12-17 13:10:28', '2022-12-20 12:59:10', NULL);
INSERT INTO `sys_dept` VALUES (783319588905201665, 1, 782235460805898241, '测试子部门', '0', 10, '0', '2022-12-20 12:58:24', '2022-12-20 12:59:15', NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'ID',
  `tenant_id` bigint(20) unsigned NOT NULL COMMENT '租户ID',
  `pid` bigint(20) NOT NULL DEFAULT '0' COMMENT '父ID',
  `name` varchar(32) NOT NULL COMMENT '菜单名称',
  `menu_type` char(1) NOT NULL COMMENT '菜单类型',
  `path` varchar(128) NOT NULL COMMENT '菜单url',
  `authority_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '权限ID',
  `route_name` varchar(32) DEFAULT NULL COMMENT '命名路由',
  `view_path` varchar(128) DEFAULT NULL COMMENT '视图路径',
  `redirect` varchar(128) DEFAULT NULL COMMENT '重定向',
  `icon` varchar(32) DEFAULT NULL COMMENT '图标',
  `sort` int(11) NOT NULL DEFAULT '999' COMMENT '排序',
  `is_cache` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否缓存',
  `hidden` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否隐藏',
  `hide_breadcrumb` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否隐藏面包屑',
  `props` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否匹配props',
  `status` char(1) CHARACTER SET utf8 NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tenant-code` (`tenant_id`,`route_name`) USING BTREE COMMENT '编码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
BEGIN;
INSERT INTO `sys_menu` VALUES (782579756306313217, 1, 0, '基础管理', '0', '/basic', 782647310861250562, 'Basic', '@/components/layout/InAppLayout.vue', '/basic/user', 'ingot:basic', 10, 0, 0, 0, 0, '0', '2022-12-18 11:58:35', '2022-12-27 20:46:33', NULL);
INSERT INTO `sys_menu` VALUES (782582623704494082, 1, 782579756306313217, '用户管理', '0', '/basic/user', 782649986701373442, 'BasicUser', '@/views/basic/user/UserLayout.vue', '/basic/user/list', NULL, 10, 0, 0, 0, 0, '0', '2022-12-18 12:09:58', '2022-12-27 20:46:46', NULL);
INSERT INTO `sys_menu` VALUES (782583360945696769, 1, 782579756306313217, '部门管理', '1', '/basic/dept', 782650056679141377, 'BasicDept', '@/views/basic/dept/DeptPage.vue', NULL, NULL, 12, 0, 0, 0, 0, '0', '2022-12-18 12:12:54', '2022-12-27 20:47:05', NULL);
INSERT INTO `sys_menu` VALUES (782583497235410945, 1, 782579756306313217, '角色管理', '0', '/basic/role', 782650095858135041, 'BasicRole', '@/views/basic/role/RoleLayout.vue', '/basic/role/list', NULL, 14, 0, 0, 0, 0, '0', '2022-12-18 12:13:27', '2022-12-27 20:47:13', NULL);
INSERT INTO `sys_menu` VALUES (782583633701285889, 1, 782583497235410945, '角色管理', '1', '/basic/role/list', 782650095858135041, 'BasicRoleList', '@/views/basic/role/home/RolePage.vue', NULL, NULL, 10, 0, 0, 0, 0, '0', '2022-12-18 12:13:59', '2022-12-27 20:47:20', NULL);
INSERT INTO `sys_menu` VALUES (782584241699205121, 1, 782579756306313217, '租户管理', '1', '/basic/tenant', 782650139818635265, 'BasicTenant', '@/views/basic/tenant/TenantPage.vue', NULL, NULL, 16, 0, 0, 0, 0, '0', '2022-12-18 12:16:24', '2022-12-27 20:48:45', NULL);
INSERT INTO `sys_menu` VALUES (782584370229456897, 1, 782579756306313217, '菜单管理', '1', '/basic/menu', 782650179794546690, 'BasicMenu', '@/views/basic/menu/MenuPage.vue', NULL, NULL, 18, 0, 0, 0, 0, '0', '2022-12-18 12:16:55', '2022-12-27 20:48:51', NULL);
INSERT INTO `sys_menu` VALUES (782586637955411969, 1, 782579756306313217, '权限管理', '0', '/basic/authority', 782650237604638722, 'BasicAuthority', '@/views/basic/authority/AuthorityLayout.vue', '/basic/authority/list', NULL, 20, 0, 0, 0, 0, '0', '2022-12-18 12:25:55', '2022-12-27 20:48:59', NULL);
INSERT INTO `sys_menu` VALUES (782586740736831489, 1, 782586637955411969, '权限管理', '1', '/basic/authority/list', 782650237604638722, 'BasicAuthorityList', '@/views/basic/authority/home/AuthorityPage.vue', NULL, NULL, 10, 0, 0, 0, 0, '0', '2022-12-18 12:26:20', '2022-12-27 20:49:04', NULL);
INSERT INTO `sys_menu` VALUES (782587038901514241, 1, 782579756306313217, '客户端管理', '0', '/basic/client', 782650307704041474, 'BasicClient', '@/views/basic/client/ClientLayout.vue', '/basic/client/list', NULL, 22, 0, 0, 0, 0, '0', '2022-12-18 12:27:31', '2022-12-27 20:56:09', NULL);
INSERT INTO `sys_menu` VALUES (782587211480346625, 1, 782587038901514241, '客户端列表', '1', '/basic/client/list', 782650307704041474, 'BasicClientList', '@/views/basic/client/home/ClientPage.vue', NULL, NULL, 10, 0, 0, 0, 0, '0', '2022-12-18 12:28:12', '2022-12-27 20:56:27', NULL);
INSERT INTO `sys_menu` VALUES (782661688801144834, 1, 782587038901514241, '编辑客户端', '1', '/basic/client/:id', 782650307704041474, 'BasicClientManager', '@/views/basic/client/manager/ManagerPage.vue', NULL, NULL, 20, 0, 1, 0, 1, '0', '2022-12-18 17:24:09', '2022-12-29 19:32:17', NULL);
INSERT INTO `sys_menu` VALUES (784784987182116866, 1, 782582623704494082, '用户列表', '1', '/basic/user/list', 782649986701373442, 'BasicUserList', '@/views/basic/user/home/UserPage.vue', NULL, NULL, 10, 0, 0, 0, 0, '0', '2022-12-24 14:01:23', '2022-12-29 18:03:46', NULL);
INSERT INTO `sys_menu` VALUES (785924904339681281, 1, 782582623704494082, '用户详情', '1', '/basic/user/detail/:id', 782649986701373442, NULL, '@/views/basic/user/details/DetailsPage.vue', NULL, NULL, 20, 0, 1, 0, 1, '0', '2022-12-27 17:31:00', '2022-12-27 20:46:59', NULL);
INSERT INTO `sys_menu` VALUES (785927747020828673, 1, 782583497235410945, '绑定部门', '0', '/basic/role/binddept/:id', 782650095858135041, NULL, '@/views/basic/role/binddept/IndexPage.vue', NULL, NULL, 20, 0, 1, 0, 1, '0', '2022-12-27 17:42:18', '2022-12-27 20:47:29', NULL);
INSERT INTO `sys_menu` VALUES (785927849261182978, 1, 782583497235410945, '绑定权限', '0', '/basic/role/bindauthority/:id', 782650095858135041, NULL, '@/views/basic/role/bindauthority/IndexPage.vue', NULL, NULL, 30, 0, 1, 0, 1, '0', '2022-12-27 17:42:42', '2022-12-27 20:48:21', NULL);
INSERT INTO `sys_menu` VALUES (785927957314842626, 1, 782583497235410945, '绑定客户端', '0', '/basic/role/bindclient/:id', 782650095858135041, NULL, '@/views/basic/role/bindclient/IndexPage.vue', NULL, NULL, 40, 0, 1, 0, 1, '0', '2022-12-27 17:43:08', '2022-12-27 20:48:31', NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'ID',
  `tenant_id` bigint(20) unsigned NOT NULL COMMENT '租户',
  `name` varchar(50) CHARACTER SET utf8 NOT NULL DEFAULT '' COMMENT '角色名称',
  `code` varchar(50) CHARACTER SET utf8 NOT NULL DEFAULT '' COMMENT '角色编码',
  `type` char(1) NOT NULL COMMENT '角色类型',
  `status` char(1) CHARACTER SET utf8 NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `remark` varchar(300) CHARACTER SET utf8 DEFAULT '' COMMENT '备注',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tenant-code` (`tenant_id`,`code`) USING BTREE COMMENT '编码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
BEGIN;
INSERT INTO `sys_role` VALUES (1, 1, '超级管理员', 'role_admin', '0', '0', '超级管理员', '2021-01-03 11:07:59', '2022-12-17 18:57:32', NULL);
INSERT INTO `sys_role` VALUES (2, 1, '管理员', 'role_manager', '0', '0', '管理员', '2021-06-23 09:28:19', '2021-07-06 09:57:39', NULL);
INSERT INTO `sys_role` VALUES (3, 1, '用户', 'role_user', '0', '0', '用户', '2021-06-23 09:28:33', '2021-06-23 14:34:07', NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_role_authority
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_authority`;
CREATE TABLE `sys_role_authority` (
  `role_id` bigint(20) unsigned NOT NULL COMMENT '角色ID',
  `authority_id` bigint(20) unsigned NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`role_id`,`authority_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_role_authority
-- ----------------------------
BEGIN;
INSERT INTO `sys_role_authority` VALUES (1, 782647310861250562);
COMMIT;

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept` (
  `role_id` bigint(20) unsigned NOT NULL COMMENT '角色ID',
  `dept_id` bigint(20) unsigned NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for sys_role_oauth_client
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_oauth_client`;
CREATE TABLE `sys_role_oauth_client` (
  `role_id` bigint(20) unsigned NOT NULL COMMENT '角色ID',
  `client_id` varchar(100) NOT NULL COMMENT '客户端ID',
  PRIMARY KEY (`role_id`,`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_role_oauth_client
-- ----------------------------
BEGIN;
INSERT INTO `sys_role_oauth_client` VALUES (1, 'ingot');
COMMIT;

-- ----------------------------
-- Table structure for sys_role_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_user`;
CREATE TABLE `sys_role_user` (
  `role_id` bigint(20) unsigned NOT NULL COMMENT '角色ID',
  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户ID',
  PRIMARY KEY (`role_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_role_user
-- ----------------------------
BEGIN;
INSERT INTO `sys_role_user` VALUES (1, 1);
COMMIT;

-- ----------------------------
-- Table structure for sys_social_details
-- ----------------------------
DROP TABLE IF EXISTS `sys_social_details`;
CREATE TABLE `sys_social_details` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'ID',
  `tenant_id` bigint(20) unsigned NOT NULL COMMENT '租户ID',
  `app_id` varchar(64) NOT NULL COMMENT 'App ID',
  `app_secret` varchar(64) DEFAULT NULL COMMENT 'App Secret',
  `redirect_url` varchar(128) DEFAULT NULL COMMENT '重定向地址',
  `name` varchar(20) DEFAULT NULL COMMENT '社交名称',
  `type` varchar(20) DEFAULT NULL COMMENT '类型',
  `status` char(1) CHARACTER SET utf8 DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for sys_tenant
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'ID',
  `name` varchar(255) NOT NULL COMMENT '租户名称',
  `code` varchar(64) NOT NULL COMMENT '租户编号',
  `start_at` datetime DEFAULT NULL COMMENT '开始日期',
  `end_at` datetime DEFAULT NULL COMMENT '结束日期',
  `status` char(1) CHARACTER SET utf8 NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_tenant
-- ----------------------------
BEGIN;
INSERT INTO `sys_tenant` VALUES (1, 'Ingot Cloud', 'ingot', NULL, NULL, '0', '2021-01-06 13:48:26', '2022-12-19 21:18:26', NULL);
INSERT INTO `sys_tenant` VALUES (2, '测试租户', 'test', '2021-06-01 00:00:00', '2024-07-01 00:00:00', '0', '2021-06-21 10:45:53', '2022-12-16 17:05:26', '2022-12-16 10:01:49');
COMMIT;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'ID',
  `tenant_id` bigint(20) unsigned NOT NULL COMMENT '所属租户',
  `dept_id` bigint(20) unsigned NOT NULL COMMENT '部门ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '用户名',
  `password` varchar(300) CHARACTER SET utf8 DEFAULT NULL COMMENT '密码',
  `real_name` varchar(50) CHARACTER SET utf8 DEFAULT NULL COMMENT '姓名',
  `phone` varchar(30) CHARACTER SET utf8 DEFAULT NULL COMMENT '手机号',
  `email` varchar(50) CHARACTER SET utf8 DEFAULT NULL COMMENT '邮件地址',
  `status` char(1) CHARACTER SET utf8 NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `tenant-username` (`tenant_id`,`username`) USING BTREE COMMENT '用户名',
  KEY `tenant-phone` (`tenant_id`,`phone`) USING BTREE COMMENT '手机号',
  KEY `tenant-email` (`tenant_id`,`email`) USING BTREE COMMENT '邮箱'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
BEGIN;
INSERT INTO `sys_user` VALUES (1, 1, 1, 'admin', '{bcrypt}$2a$10$un6vyGjHLthh007s5qtHFe56doYCkA8BeEAkJZxQG67pPHjN75B76', '超级管理员', '18603243837', 'admin@ingot.com', '0', '2021-01-03 11:02:46', '2022-12-21 17:35:38', NULL);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
