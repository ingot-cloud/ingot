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

 Date: 23/08/2023 15:25:46
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for biz_leaf_alloc
-- ----------------------------
DROP TABLE IF EXISTS `biz_leaf_alloc`;
CREATE TABLE `biz_leaf_alloc` (
  `biz_tag` varchar(128) NOT NULL DEFAULT '',
  `max_id` bigint(20) NOT NULL DEFAULT '1',
  `step` int(11) NOT NULL,
  `description` varchar(256) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`biz_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
  `attributes` blob,
  `state` varchar(500) DEFAULT NULL,
  `authorization_code_value` blob,
  `authorization_code_issued_at` datetime DEFAULT NULL,
  `authorization_code_expires_at` datetime DEFAULT NULL,
  `authorization_code_metadata` blob,
  `access_token_value` blob,
  `access_token_issued_at` datetime DEFAULT NULL,
  `access_token_expires_at` datetime DEFAULT NULL,
  `access_token_metadata` blob,
  `access_token_type` varchar(100) DEFAULT NULL,
  `access_token_scopes` varchar(1000) DEFAULT NULL,
  `oidc_id_token_value` blob,
  `oidc_id_token_issued_at` datetime DEFAULT NULL,
  `oidc_id_token_expires_at` datetime DEFAULT NULL,
  `oidc_id_token_metadata` blob,
  `refresh_token_value` blob,
  `refresh_token_issued_at` datetime DEFAULT NULL,
  `refresh_token_expires_at` datetime DEFAULT NULL,
  `refresh_token_metadata` blob,
  `user_code_value` blob,
  `user_code_issued_at` datetime DEFAULT NULL,
  `user_code_expires_at` datetime DEFAULT NULL,
  `user_code_metadata` blob,
  `device_code_value` blob,
  `device_code_issued_at` datetime DEFAULT NULL,
  `device_code_expires_at` datetime DEFAULT NULL,
  `device_code_metadata` blob,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of oauth2_authorization
-- ----------------------------
BEGIN;
INSERT INTO `oauth2_authorization` VALUES ('144e29fa-8880-429e-ba7f-be179ab2a677', 'ingot', 'admin', 'password', 'message.read,message.write', 0x7B2240636C617373223A226A6176612E7574696C2E436F6C6C656374696F6E7324556E6D6F6469666961626C654D6170222C226A6176612E73656375726974792E5072696E636970616C223A7B2240636C617373223A22636F6D2E696E676F742E6672616D65776F726B2E73656375726974792E6F61757468322E7365727665722E617574686F72697A6174696F6E2E61757468656E7469636174696F6E2E4F41757468325573657244657461696C7341757468656E7469636174696F6E546F6B656E222C22617574686F726974696573223A5B226A6176612E7574696C2E436F6C6C656374696F6E7324556E6D6F6469666961626C6552616E646F6D4163636573734C697374222C5B7B2240636C617373223A22636F6D2E696E676F742E6672616D65776F726B2E73656375726974792E636F72652E617574686F726974792E436C69656E744772616E746564417574686F72697479222C22617574686F72697479223A22434C49454E545F696E676F74227D2C7B2240636C617373223A226F72672E737072696E676672616D65776F726B2E73656375726974792E636F72652E617574686F726974792E53696D706C654772616E746564417574686F72697479222C22617574686F72697479223A226261736963227D2C7B2240636C617373223A226F72672E737072696E676672616D65776F726B2E73656375726974792E636F72652E617574686F726974792E53696D706C654772616E746564417574686F72697479222C22617574686F72697479223A22646576656C6F70227D2C7B2240636C617373223A226F72672E737072696E676672616D65776F726B2E73656375726974792E636F72652E617574686F726974792E53696D706C654772616E746564417574686F72697479222C22617574686F72697479223A22726F6C655F61646D696E227D5D5D2C2264657461696C73223A6E756C6C2C2261757468656E74696361746564223A747275652C227072696E636970616C223A7B2240636C617373223A22636F6D2E696E676F742E6672616D65776F726B2E73656375726974792E636F72652E7573657264657461696C732E496E676F7455736572222C22757365726E616D65223A2261646D696E222C22617574686F726974696573223A5B226A6176612E7574696C2E436F6C6C656374696F6E7324556E6D6F6469666961626C65536574222C5B7B2240636C617373223A22636F6D2E696E676F742E6672616D65776F726B2E73656375726974792E636F72652E617574686F726974792E436C69656E744772616E746564417574686F72697479222C22617574686F72697479223A22434C49454E545F696E676F74227D2C7B2240636C617373223A226F72672E737072696E676672616D65776F726B2E73656375726974792E636F72652E617574686F726974792E53696D706C654772616E746564417574686F72697479222C22617574686F72697479223A226261736963227D2C7B2240636C617373223A226F72672E737072696E676672616D65776F726B2E73656375726974792E636F72652E617574686F726974792E53696D706C654772616E746564417574686F72697479222C22617574686F72697479223A22646576656C6F70227D2C7B2240636C617373223A226F72672E737072696E676672616D65776F726B2E73656375726974792E636F72652E617574686F726974792E53696D706C654772616E746564417574686F72697479222C22617574686F72697479223A22726F6C655F61646D696E227D5D5D2C226964223A312C22646570744964223A312C2274656E616E744964223A312C22636C69656E744964223A22696E676F74222C22746F6B656E4175746854797065223A2230227D2C2263726564656E7469616C73223A6E756C6C2C226E616D65223A2261646D696E227D7D, NULL, NULL, NULL, NULL, NULL, 0x65794A72615751694F6949794D6D46694E47517A4F4331694F47526D4C5451344F546B74595745334D793169597A55334F4755784D6D566C5A6A41694C434A68624763694F694A53557A49314E694A392E65794A7A645749694F694A685A47317062694973496D46315A434936496D6C755A32393049697769626D4A6D496A6F784E6A6B794E6A6B7A4D44677A4C434A30595851694F6949774969776963324E76634755694F6C7369636D39735A5639685A47317062694973496D316C63334E685A325575636D56685A434973496D526C646D5673623341694C434A6959584E7059794973496D316C63334E685A32557564334A70644755695853776961584E7A496A6F696148523063446F764C326C755A3239304C5746316447677463325679646D56794F6A55784D4441694C434A70496A6F784C434A6B5A584230496A6F784C434A6C654841694F6A45324F5449334D4441794F444D73496D6C68644349364D5459354D6A59354D7A41344D7977696447567559573530496A6F7866512E527647515565395A49417832714261454B346469714548733057517A4B7154467047784C73563732676C4678375A736E5F6E5F6F2D5A516257307A3476483666664166664A4863334D70692D5F6A6D72347A4C77464A3465314D7034634D4E4F4C5A455475377666714F416B7437724A696F744A3057435A4A6F365A7273333339506C68676F58776956733571475F386E4B74484546794255704C335F7072477241776E576436315841446B7348564D65514C324130363748544D4C324352635973644C58397A6279537363656D61587835655966586B466777686167455A4C5572516D4A41794F476459374C4A486F78313731474B75733941426255415530317057626E6C464D784647306B666273484B314C34796B6779736648716B714632446B6C3345716548585859674469776F65436859786347464A794C71494B52364B784F58744641434D6C4B6466426A4F7576784877, '2023-08-22 16:31:23', '2023-08-22 18:31:23', 0x7B2240636C617373223A226A6176612E7574696C2E436F6C6C656374696F6E7324556E6D6F6469666961626C654D6170222C226D657461646174612E746F6B656E2E636C61696D73223A7B2240636C617373223A226A6176612E7574696C2E436F6C6C656374696F6E7324556E6D6F6469666961626C654D6170222C22737562223A2261646D696E222C22617564223A5B226A6176612E7574696C2E436F6C6C656374696F6E732453696E676C65746F6E4C697374222C5B22696E676F74225D5D2C226E6266223A5B226A6176612E74696D652E496E7374616E74222C313639323639333038332E3033393232393030305D2C22746174223A2230222C2273636F7065223A5B226A6176612E7574696C2E48617368536574222C5B22726F6C655F61646D696E222C226D6573736167652E72656164222C22646576656C6F70222C226261736963222C226D6573736167652E7772697465225D5D2C22697373223A5B226A6176612E6E65742E55524C222C22687474703A2F2F696E676F742D617574682D7365727665723A35313030225D2C2269223A5B226A6176612E6C616E672E4C6F6E67222C315D2C2264657074223A5B226A6176612E6C616E672E4C6F6E67222C315D2C22657870223A5B226A6176612E74696D652E496E7374616E74222C313639323730303238332E3033393232393030305D2C22696174223A5B226A6176612E74696D652E496E7374616E74222C313639323639333038332E3033393232393030305D2C2274656E616E74223A5B226A6176612E6C616E672E4C6F6E67222C315D7D2C226D657461646174612E746F6B656E2E696E76616C696461746564223A66616C73657D, 'Bearer', 'message.read,message.write', NULL, NULL, NULL, NULL, 0x65745258626A48614555356642693861496F6841527734664839696B4D78316456526F4D45567945773035474239574B4150344E516D57653548476545734B792D694754396F616564437578684E2D4A6E467A48496F4E31713569475255397463745F63324B4849505A466D4350704238646D667734526B575F5952615F6474, '2023-08-22 16:31:23', '2023-08-29 16:31:23', 0x7B2240636C617373223A226A6176612E7574696C2E436F6C6C656374696F6E7324556E6D6F6469666961626C654D6170222C226D657461646174612E746F6B656E2E696E76616C696461746564223A66616C73657D, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for oauth2_authorization_consent
-- ----------------------------
DROP TABLE IF EXISTS `oauth2_authorization_consent`;
CREATE TABLE `oauth2_authorization_consent` (
  `registered_client_id` varchar(100) NOT NULL,
  `principal_name` varchar(200) NOT NULL,
  `authorities` varchar(1000) NOT NULL,
  PRIMARY KEY (`registered_client_id`,`principal_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of oauth2_authorization_consent
-- ----------------------------
BEGIN;
INSERT INTO `oauth2_authorization_consent` VALUES ('ingot', 'admin', 'SCOPE_message.read');
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
  `post_logout_redirect_uris` varchar(1000) DEFAULT NULL COMMENT 'logout重定向url',
  `scopes` varchar(1000) CHARACTER SET utf8 NOT NULL COMMENT '客户端的访问范围',
  `client_settings` varchar(2000) CHARACTER SET utf8 NOT NULL COMMENT '客户端设置',
  `token_settings` varchar(2000) CHARACTER SET utf8 NOT NULL COMMENT 'token设置',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`),
  KEY `clientId` (`client_id`) USING BTREE COMMENT '客户端ID',
  KEY `clientName` (`client_name`) USING BTREE COMMENT '客户端名称',
  KEY `clientIdIssuedAt` (`client_id_issued_at`) USING BTREE COMMENT '发布时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of oauth2_registered_client
-- ----------------------------
BEGIN;
INSERT INTO `oauth2_registered_client` VALUES ('ingot', 'ingot', '2020-11-20 15:57:29', '{bcrypt}$2a$10$ngVRBWfVV1AK1/Kst3ndLez5vM54FFgIQcvv8azMUyo4aJtLybnOC', NULL, 'Ingot管理平台', 'client_secret_basic', 'refresh_token,client_credentials,password,authorization_code,confirm_code', 'https://ingotcloud.com', NULL, 'message.read,message.write', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.client.require-authorization-consent\":true,\"ingot.settings.client.status\":\"0\",\"settings.client.require-proof-key\":false}', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.token.reuse-refresh-tokens\":false,\"settings.token.id-token-signature-algorithm\":[\"org.springframework.security.oauth2.jose.jws.SignatureAlgorithm\",\"RS256\"],\"settings.token.access-token-time-to-live\":[\"java.time.Duration\",7200.000000000],\"ingot.settings.token.auth-type\":\"0\",\"settings.token.access-token-format\":{\"@class\":\"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat\",\"value\":\"self-contained\"},\"settings.token.refresh-token-time-to-live\":[\"java.time.Duration\",604800.000000000],\"settings.token.authorization-code-time-to-live\":[\"java.time.Duration\",300.000000000]}', '2023-02-16 15:13:40', NULL);
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
INSERT INTO `sys_authority` VALUES (782649986701373442, 1, 782647310861250562, '账户管理', 'basic.user', '0', '', '2022-12-18 16:37:39', '2023-02-14 09:25:39', NULL);
INSERT INTO `sys_authority` VALUES (782650056679141377, 1, 782647310861250562, '部门管理', 'basic.dept', '0', '', '2022-12-18 16:37:56', NULL, NULL);
INSERT INTO `sys_authority` VALUES (782650095858135041, 1, 782647310861250562, '角色管理', 'basic.role', '0', '', '2022-12-18 16:38:05', NULL, NULL);
INSERT INTO `sys_authority` VALUES (782650139818635265, 1, 782647310861250562, '租户管理', 'basic.tenant', '0', '', '2022-12-18 16:38:15', NULL, NULL);
INSERT INTO `sys_authority` VALUES (782650179794546690, 1, 782647310861250562, '菜单管理', 'basic.menu', '0', '', '2022-12-18 16:38:25', '2023-02-16 17:38:10', NULL);
INSERT INTO `sys_authority` VALUES (782650237604638722, 1, 782647310861250562, '权限管理', 'basic.authority', '0', '', '2022-12-18 16:38:39', '2022-12-26 21:10:14', NULL);
INSERT INTO `sys_authority` VALUES (782650307704041474, 1, 782647310861250562, '客户端管理', 'basic.client', '0', '', '2022-12-18 16:38:55', '2022-12-27 12:19:35', NULL);
INSERT INTO `sys_authority` VALUES (805471516535599106, 1, 782647310861250562, '社交管理', 'basic.social', '0', '', '2023-02-19 16:02:16', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807933676733902849, 1, 782649986701373442, '读', 'basic.user.read', '0', '', '2023-02-26 11:06:00', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807933724267950081, 1, 782649986701373442, '写', 'basic.user.write', '0', '', '2023-02-26 11:06:12', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807933768714989570, 1, 782650056679141377, '读', 'basic.dept.read', '0', '', '2023-02-26 11:06:22', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807933824746696705, 1, 782650056679141377, '写', 'basic.dept.write', '0', '', '2023-02-26 11:06:36', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807933868778500097, 1, 782650095858135041, '读', 'basic.role.read', '0', '', '2023-02-26 11:06:46', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807933911300354049, 1, 782650095858135041, '写', 'basic.role.write', '0', '', '2023-02-26 11:06:56', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807933967919263745, 1, 782650139818635265, '读', 'basic.tenant.read', '0', '', '2023-02-26 11:07:10', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807934009086357505, 1, 782650139818635265, '写', 'basic.tenant.write', '0', '', '2023-02-26 11:07:20', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807934048638644225, 1, 782650179794546690, '读', 'basic.menu.read', '0', '', '2023-02-26 11:07:29', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807934078883770370, 1, 782650179794546690, '写', 'basic.menu.write', '0', '', '2023-02-26 11:07:36', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807934112199127042, 1, 782650307704041474, '读', 'basic.client.read', '0', '', '2023-02-26 11:07:44', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807934139076227073, 1, 782650307704041474, '写', 'basic.client.write', '0', '', '2023-02-26 11:07:51', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807934166997708802, 1, 805471516535599106, '读', 'basic.social.read', '0', '', '2023-02-26 11:07:57', NULL, NULL);
INSERT INTO `sys_authority` VALUES (807934191819599874, 1, 805471516535599106, '写', 'basic.social.write', '0', '', '2023-02-26 11:08:03', NULL, NULL);
INSERT INTO `sys_authority` VALUES (868163807997636610, 1, 0, '开发平台', 'develop', '0', '', '2023-08-11 15:59:03', NULL, NULL);
INSERT INTO `sys_authority` VALUES (868164119483428866, 1, 868163807997636610, '生成二维码', 'develop.qrcode', '0', '', '2023-08-11 16:00:17', NULL, NULL);
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
  `custom_view_path` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否自定义视图路径',
  `view_path` varchar(128) DEFAULT NULL COMMENT '视图路径',
  `route_name` varchar(32) DEFAULT NULL COMMENT '命名路由',
  `redirect` varchar(128) DEFAULT NULL COMMENT '重定向',
  `icon` varchar(64) DEFAULT NULL COMMENT '图标',
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
INSERT INTO `sys_menu` VALUES (782579756306313217, 1, 0, '基础管理', '0', '/basic', 782647310861250562, 1, '@/layouts/InAppLayout.vue', NULL, '/basic/user', 'ingot:basic', 10, 0, 0, 0, 0, '0', '2022-12-18 11:58:35', '2022-12-27 20:46:33', NULL);
INSERT INTO `sys_menu` VALUES (782582623704494082, 1, 782579756306313217, '账户管理', '0', '/basic/user', 807933676733902849, 1, '@/layouts/InSimpleLayout.vue', NULL, '/basic/user/home', NULL, 10, 0, 0, 0, 0, '0', '2022-12-18 12:09:58', '2023-02-26 11:34:54', NULL);
INSERT INTO `sys_menu` VALUES (782583360945696769, 1, 782579756306313217, '部门管理', '1', '/basic/dept', 807933768714989570, 0, '@/pages/basic/dept/IndexPage.vue', NULL, NULL, NULL, 12, 0, 0, 0, 0, '0', '2022-12-18 12:12:54', '2023-02-26 11:34:47', NULL);
INSERT INTO `sys_menu` VALUES (782583497235410945, 1, 782579756306313217, '角色管理', '0', '/basic/role', 807933868778500097, 1, '@/layouts/InSimpleLayout.vue', NULL, '/basic/role/home', NULL, 14, 0, 0, 0, 0, '0', '2022-12-18 12:13:27', '2023-02-26 11:33:43', NULL);
INSERT INTO `sys_menu` VALUES (782583633701285889, 1, 782583497235410945, '角色管理', '1', '/basic/role/home', 807933868778500097, 0, '@/pages/basic/role/home/IndexPage.vue', NULL, NULL, NULL, 10, 0, 0, 0, 0, '0', '2022-12-18 12:13:59', '2023-02-26 11:33:51', NULL);
INSERT INTO `sys_menu` VALUES (782584241699205121, 1, 782579756306313217, '租户管理', '1', '/basic/tenant', 807933967919263745, 0, '@/pages/basic/tenant/IndexPage.vue', NULL, NULL, NULL, 16, 0, 0, 0, 0, '0', '2022-12-18 12:16:24', '2023-02-26 11:34:38', NULL);
INSERT INTO `sys_menu` VALUES (782584370229456897, 1, 782579756306313217, '菜单管理', '1', '/basic/menu', 807934048638644225, 0, '@/pages/basic/menu/IndexPage.vue', NULL, NULL, NULL, 18, 0, 0, 0, 0, '0', '2022-12-18 12:16:55', '2023-02-26 11:34:24', NULL);
INSERT INTO `sys_menu` VALUES (782586637955411969, 1, 782579756306313217, '权限管理', '1', '/basic/authority', 782650237604638722, 0, '@/pages/basic/authority/IndexPage.vue', NULL, NULL, NULL, 20, 0, 0, 0, 0, '0', '2022-12-18 12:25:55', '2022-12-27 20:48:59', NULL);
INSERT INTO `sys_menu` VALUES (782587038901514241, 1, 782579756306313217, '客户端管理', '0', '/basic/client', 807934112199127042, 1, '@/layouts/InSimpleLayout.vue', NULL, '/basic/client/home', NULL, 22, 0, 0, 0, 0, '0', '2022-12-18 12:27:31', '2023-02-26 11:34:06', NULL);
INSERT INTO `sys_menu` VALUES (782587211480346625, 1, 782587038901514241, '客户端列表', '1', '/basic/client/home', 807933676733902849, 0, '@/pages/basic/client/home/IndexPage.vue', NULL, NULL, NULL, 10, 0, 0, 0, 0, '0', '2022-12-18 12:28:12', '2023-02-26 11:26:39', NULL);
INSERT INTO `sys_menu` VALUES (782661688801144834, 1, 782587038901514241, '编辑客户端', '1', '/basic/client/details/:id', 807934139076227073, 0, '@/pages/basic/client/details/IndexPage.vue', NULL, NULL, NULL, 20, 0, 1, 0, 1, '0', '2022-12-18 17:24:09', '2023-02-26 11:25:36', NULL);
INSERT INTO `sys_menu` VALUES (784784987182116866, 1, 782582623704494082, '账户列表', '1', '/basic/user/home', 807933676733902849, 0, '@/pages/basic/user/home/IndexPage.vue', NULL, NULL, NULL, 10, 0, 0, 0, 0, '0', '2022-12-24 14:01:23', '2023-02-26 11:26:21', NULL);
INSERT INTO `sys_menu` VALUES (785924904339681281, 1, 782582623704494082, '账户详情', '1', '/basic/user/details/:id', 807933724267950081, 0, '@/pages/basic/user/details/IndexPage.vue', NULL, NULL, NULL, 20, 0, 1, 0, 1, '0', '2022-12-27 17:31:00', '2023-02-26 11:24:42', NULL);
INSERT INTO `sys_menu` VALUES (785927747020828673, 1, 782583497235410945, '绑定部门', '0', '/basic/role/binddept/:id', 807933911300354049, 0, '@/pages/basic/role/binddept/IndexPage.vue', NULL, NULL, NULL, 20, 0, 1, 0, 1, '0', '2022-12-27 17:42:18', '2023-02-26 11:25:02', NULL);
INSERT INTO `sys_menu` VALUES (785927849261182978, 1, 782583497235410945, '绑定权限', '0', '/basic/role/bindauthority/:id', 807933911300354049, 0, '@/pages/basic/role/bindauthority/IndexPage.vue', NULL, NULL, NULL, 30, 0, 1, 0, 1, '0', '2022-12-27 17:42:42', '2023-02-26 11:25:10', NULL);
INSERT INTO `sys_menu` VALUES (785927957314842626, 1, 782583497235410945, '绑定客户端', '0', '/basic/role/bindclient/:id', 807933911300354049, 0, '@/pages/basic/role/bindclient/IndexPage.vue', NULL, NULL, NULL, 40, 0, 1, 0, 1, '0', '2022-12-27 17:43:08', '2023-02-26 11:25:15', NULL);
INSERT INTO `sys_menu` VALUES (805472038554480642, 1, 782579756306313217, '社交管理', '1', '/basic/social', 807934166997708802, 0, '@/pages/basic/social/IndexPage.vue', NULL, NULL, NULL, 30, 0, 0, 0, 0, '0', '2023-02-19 16:04:20', '2023-02-26 11:34:13', NULL);
INSERT INTO `sys_menu` VALUES (868164540688019458, 1, 0, '开发平台', '0', '/develop', 868164119483428866, 1, '@/layouts/InAppLayout.vue', NULL, '/develop/qrcode', 'material-symbols:developer-mode-tv-outline', 10, 0, 0, 0, 0, '0', '2023-08-11 16:01:57', '2023-08-11 16:17:18', NULL);
INSERT INTO `sys_menu` VALUES (868164933065158657, 1, 868164540688019458, '生成二维码', '1', '/develop/qrcode', 868164119483428866, 0, '@/pages/develop/qrcode/IndexPage.vue', NULL, NULL, NULL, 10, 0, 0, 0, 0, '0', '2023-08-11 16:03:31', NULL, NULL);
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
  `type` char(1) NOT NULL DEFAULT '0' COMMENT '角色类型',
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
INSERT INTO `sys_role` VALUES (2, 1, '管理员', 'role_manager', '0', '0', '管理员', '2021-06-23 09:28:19', '2023-02-16 18:10:59', NULL);
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
INSERT INTO `sys_role_authority` VALUES (1, 868163807997636610);
INSERT INTO `sys_role_authority` VALUES (2, 807933676733902849);
INSERT INTO `sys_role_authority` VALUES (2, 807933768714989570);
INSERT INTO `sys_role_authority` VALUES (2, 807933967919263745);
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
INSERT INTO `sys_role_oauth_client` VALUES (2, 'ingot');
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
  `name` varchar(128) NOT NULL COMMENT '租户名称',
  `code` varchar(64) NOT NULL COMMENT '租户编号',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
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
INSERT INTO `sys_tenant` VALUES (1, 'Ingot管理平台', 'ingot', 'http://ingot-cloud:5000/ingot/public/tenant/logo.png?t=1691634467617', NULL, NULL, '0', '2021-01-06 13:48:26', '2023-08-10 10:27:51', NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint(20) unsigned NOT NULL COMMENT 'ID',
  `tenant_id` bigint(20) unsigned NOT NULL COMMENT '所属租户',
  `dept_id` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT '部门ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '用户名',
  `password` varchar(300) CHARACTER SET utf8 NOT NULL COMMENT '密码',
  `init_pwd` tinyint(1) NOT NULL DEFAULT '1' COMMENT '初始化密码标识',
  `nickname` varchar(32) CHARACTER SET utf8 DEFAULT NULL COMMENT '昵称',
  `phone` varchar(32) CHARACTER SET utf8 DEFAULT NULL COMMENT '手机号',
  `email` varchar(64) CHARACTER SET utf8 DEFAULT NULL COMMENT '邮件地址',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
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
INSERT INTO `sys_user` VALUES (1, 1, 1, 'admin', '{bcrypt}$2a$10$un6vyGjHLthh007s5qtHFe56doYCkA8BeEAkJZxQG67pPHjN75B76', 0, '超级管理员', '88888888888', 'admin@ingot.com', 'http://ingot-cloud:5000/ingot/public/user/avatar/logo.png?t=1691650921922', '0', '2021-01-03 11:02:46', '2023-08-10 15:02:03', NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_user_social
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_social`;
CREATE TABLE `sys_user_social` (
  `id` bigint(20) NOT NULL COMMENT 'ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `type` varchar(20) NOT NULL COMMENT '渠道类型',
  `unique_id` varchar(32) NOT NULL COMMENT '渠道唯一ID',
  `bind_at` datetime NOT NULL COMMENT '绑定时间',
  PRIMARY KEY (`id`),
  KEY `unique-type-user` (`unique_id`,`type`,`user_id`) USING BTREE COMMENT '渠道用户索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
