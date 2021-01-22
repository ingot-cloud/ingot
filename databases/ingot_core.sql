/*
 Navicat Premium Data Transfer

 Source Server         : mysql-localhost
 Source Server Type    : MySQL
 Source Server Version : 50732
 Source Host           : localhost:3306
 Source Schema         : ingot_core

 Target Server Type    : MySQL
 Target Server Version : 50732
 File Encoding         : 65001

 Date: 22/01/2021 08:58:39
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_authority
-- ----------------------------
DROP TABLE IF EXISTS `sys_authority`;
CREATE TABLE `sys_authority` (
  `id` bigint(20) NOT NULL COMMENT 'ID',
  `version` bigint(20) DEFAULT NULL COMMENT '版本号',
  `tenant_id` int(11) NOT NULL COMMENT '租户ID',
  `pid` bigint(20) DEFAULT NULL COMMENT '父ID',
  `name` varchar(100) NOT NULL COMMENT '权限名称',
  `code` varchar(100) NOT NULL COMMENT '权限编码',
  `path` varchar(128) DEFAULT NULL COMMENT 'URL',
  `status` char(1) CHARACTER SET utf8 DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `remark` varchar(255) CHARACTER SET utf8 DEFAULT '' COMMENT '备注',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `code-tenant` (`code`,`tenant_id`) USING BTREE COMMENT '租户编码唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id` bigint(20) NOT NULL COMMENT 'ID',
  `version` bigint(20) DEFAULT NULL COMMENT '版本号',
  `tenant_id` int(11) NOT NULL COMMENT '租户ID',
  `pid` bigint(20) DEFAULT NULL COMMENT '父ID',
  `name` varchar(50) DEFAULT NULL COMMENT '部门名称',
  `scope` char(1) NOT NULL DEFAULT '0' COMMENT '部门角色范围, 0:当前部门，1:当前部门和直接子部门',
  `sort` int(11) DEFAULT '0' COMMENT '排序',
  `status` char(1) CHARACTER SET utf8 DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
BEGIN;
INSERT INTO `sys_dept` VALUES (1, NULL, 1, NULL, 'ingot', '0', 0, '0', '2021-01-03 18:51:01', NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` bigint(20) NOT NULL COMMENT 'ID',
  `version` bigint(20) DEFAULT NULL COMMENT '版本号',
  `tenant_id` int(11) NOT NULL COMMENT '租户ID',
  `pid` bigint(20) DEFAULT NULL COMMENT '父ID',
  `name` varchar(32) DEFAULT NULL COMMENT '菜单名称',
  `path` varchar(128) DEFAULT NULL COMMENT '菜单url',
  `view_path` varchar(128) DEFAULT NULL COMMENT '视图路径',
  `icon` varchar(32) DEFAULT NULL COMMENT '图标',
  `sort` int(11) DEFAULT '0' COMMENT '排序',
  `cache` tinyint(1) DEFAULT '0' COMMENT '是否缓存',
  `hidden` tinyint(1) DEFAULT '0' COMMENT '是否隐藏',
  `params` varchar(255) DEFAULT NULL COMMENT '参数',
  `status` char(1) CHARACTER SET utf8 DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `remark` varchar(255) CHARACTER SET utf8 DEFAULT '' COMMENT '备注',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for sys_oauth_client_details
-- ----------------------------
DROP TABLE IF EXISTS `sys_oauth_client_details`;
CREATE TABLE `sys_oauth_client_details` (
  `id` bigint(20) NOT NULL COMMENT 'ID',
  `version` bigint(20) DEFAULT '0' COMMENT '版本号',
  `tenant_id` int(11) DEFAULT NULL COMMENT '租户ID',
  `client_id` varchar(32) NOT NULL COMMENT '客户端ID',
  `client_secret` varchar(256) CHARACTER SET utf8 NOT NULL COMMENT '客户端秘钥',
  `resource_id` varchar(32) CHARACTER SET utf8 NOT NULL COMMENT '资源ID',
  `resource_ids` varchar(256) CHARACTER SET utf8 DEFAULT ',' COMMENT '授权的资源ID',
  `scope` varchar(256) CHARACTER SET utf8 DEFAULT NULL COMMENT '客户端的访问范围，如果为空（默认）的话，那么客户端拥有全部的访问范围',
  `authorized_grant_types` varchar(256) CHARACTER SET utf8 DEFAULT NULL COMMENT '客户端可以使用的授权类型',
  `web_server_redirect_uri` varchar(256) CHARACTER SET utf8 DEFAULT NULL COMMENT '重定向URL',
  `authorities` varchar(256) CHARACTER SET utf8 DEFAULT NULL COMMENT '客户端可以使用的权限',
  `access_token_validity` int(11) DEFAULT NULL COMMENT '令牌有效时间/秒',
  `refresh_token_validity` int(11) DEFAULT NULL COMMENT '刷新令牌有效时间/秒',
  `additional_information` varchar(4096) CHARACTER SET utf8 DEFAULT NULL COMMENT '额外参数',
  `autoapprove` varchar(256) CHARACTER SET utf8 DEFAULT NULL COMMENT '授权码模式是否跳过授权',
  `auth_type` varchar(20) CHARACTER SET utf8 DEFAULT 'standard' COMMENT '授权类型，默认standard',
  `type` varchar(20) CHARACTER SET utf8 DEFAULT NULL COMMENT 'client类型',
  `status` char(1) CHARACTER SET utf8 DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `remark` varchar(255) CHARACTER SET utf8 DEFAULT '' COMMENT '备注',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `client-tenant` (`client_id`,`tenant_id`) USING BTREE COMMENT '同租户client唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_oauth_client_details
-- ----------------------------
BEGIN;
INSERT INTO `sys_oauth_client_details` VALUES (1, 0, 1, 'ingot-service-acs', '{noop}GwCwru42UrxQTUFLWOAsJqa3/+WSTlRMwiQDBq8KOgI=', 'ingot-resource-acs', '', NULL, 'password,refresh_token,client_credentials', 'http://localhost:9527/#/', NULL, 7200, 2592000, '{}', 'true', 'standard', 'Service', '0', '授权中心服务', '2020-11-20 15:56:24', '2020-11-20 15:56:26', NULL);
INSERT INTO `sys_oauth_client_details` VALUES (2, 0, 1, 'ingot-service-pms', '{noop}GwCwru42UrxQTUFLWOAsJqa3/+WSTlRMwiQDBq8KOgI=', 'ingot-resource-pms', '', NULL, 'password,refresh_token,client_credentials', 'http://localhost:9527/#/', NULL, 7200, 2592000, '{}', 'true', 'standard', 'Service', '0', '权限管理系统', '2020-11-20 15:56:24', '2020-11-20 15:56:26', NULL);
INSERT INTO `sys_oauth_client_details` VALUES (3, 0, 1, 'web-cloud', '{noop}web-cloud', 'web-cloud-resource', 'ingot-resource-pms', 'web', 'password,refresh_token,client_credentials', NULL, 'role_web', 10, 30, '{}', 'false', 'standard', 'Service', '0', '云后管', '2020-11-20 15:57:29', '2020-11-20 15:57:31', NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint(20) NOT NULL COMMENT 'ID',
  `version` bigint(20) DEFAULT '0' COMMENT '版本号',
  `tenant_id` int(11) NOT NULL COMMENT '租户',
  `name` varchar(50) CHARACTER SET utf8 NOT NULL DEFAULT '' COMMENT '角色名称',
  `code` varchar(50) CHARACTER SET utf8 NOT NULL DEFAULT '' COMMENT '角色编码',
  `type` varchar(20) DEFAULT NULL COMMENT '角色类型',
  `status` char(1) CHARACTER SET utf8 DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `remark` varchar(300) CHARACTER SET utf8 DEFAULT '' COMMENT '备注',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `code-tenant` (`code`,`tenant_id`) USING BTREE COMMENT '租户编码唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
BEGIN;
INSERT INTO `sys_role` VALUES (1, 0, 1, '超级管理员', 'role_admin', NULL, '0', '超级管理员', '2021-01-03 11:07:59', NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_role_authority
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_authority`;
CREATE TABLE `sys_role_authority` (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `authority` bigint(20) NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`role_id`,`authority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept` (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `dept_id` bigint(20) NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for sys_role_oauth_client
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_oauth_client`;
CREATE TABLE `sys_role_oauth_client` (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `client_id` bigint(20) NOT NULL COMMENT '客户端ID',
  PRIMARY KEY (`role_id`,`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_role_oauth_client
-- ----------------------------
BEGIN;
INSERT INTO `sys_role_oauth_client` VALUES (1, 3);
COMMIT;

-- ----------------------------
-- Table structure for sys_role_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_user`;
CREATE TABLE `sys_role_user` (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
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
  `id` bigint(20) NOT NULL COMMENT 'ID',
  `version` bigint(20) DEFAULT '0' COMMENT '版本号',
  `tenant_id` int(11) NOT NULL COMMENT '租户ID',
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
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `version` bigint(20) DEFAULT '0' COMMENT '版本号',
  `name` varchar(255) NOT NULL COMMENT '租户名称',
  `code` varchar(64) NOT NULL COMMENT '租户编号',
  `start_at` datetime DEFAULT NULL COMMENT '开始日期',
  `end_at` datetime DEFAULT NULL COMMENT '结束日期',
  `status` char(1) CHARACTER SET utf8 DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `code-tenant` (`code`) USING BTREE COMMENT '编码唯一'
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_tenant
-- ----------------------------
BEGIN;
INSERT INTO `sys_tenant` VALUES (1, 0, 'ingot', 'ingot', NULL, NULL, '0', '2021-01-06 13:48:26', NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL COMMENT 'ID',
  `version` bigint(20) DEFAULT '0' COMMENT '版本号',
  `tenant_id` int(11) NOT NULL DEFAULT '0' COMMENT '所属租户',
  `dept_id` bigint(20) NOT NULL COMMENT '部门ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL COMMENT '用户名',
  `password` varchar(300) CHARACTER SET utf8 NOT NULL DEFAULT '' COMMENT '密码',
  `real_name` varchar(50) CHARACTER SET utf8 DEFAULT '' COMMENT '姓名',
  `phone` varchar(30) CHARACTER SET utf8 DEFAULT '' COMMENT '手机号',
  `email` varchar(50) CHARACTER SET utf8 DEFAULT '' COMMENT '邮件地址',
  `status` char(1) CHARACTER SET utf8 DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `username-tenant` (`username`,`tenant_id`) USING BTREE COMMENT '租户用户名唯一',
  UNIQUE KEY `phone-tenant` (`phone`,`tenant_id`) USING BTREE COMMENT '租户手机号唯一',
  UNIQUE KEY `email-tenant` (`email`,`tenant_id`) USING BTREE COMMENT '租户邮箱唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
BEGIN;
INSERT INTO `sys_user` VALUES (1, 0, 1, 1, 'admin', '{noop}admin', '超级管理员', '18603243837', '', '0', '2021-01-03 11:02:46', NULL, NULL);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
