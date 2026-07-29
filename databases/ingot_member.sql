/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : localhost:3306
 Source Schema         : ingot_member

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 23/04/2026 16:37:37
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of biz_leaf_alloc
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for member_permission
-- ----------------------------
DROP TABLE IF EXISTS `member_permission`;
CREATE TABLE `member_permission` (
  `id` bigint unsigned NOT NULL COMMENT 'ID',
  `pid` bigint unsigned NOT NULL DEFAULT '0' COMMENT '父ID',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限名称',
  `code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限编码',
  `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `remark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '' COMMENT '备注',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`),
  KEY `idx_code` (`code`) USING BTREE COMMENT '编码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of member_permission
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for member_role
-- ----------------------------
DROP TABLE IF EXISTS `member_role`;
CREATE TABLE `member_role` (
  `id` bigint unsigned NOT NULL COMMENT 'ID',
  `pid` bigint NOT NULL DEFAULT '0' COMMENT '组ID',
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT '角色名称',
  `code` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT '角色编码',
  `built_in` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否内置',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`),
  KEY `idx_code` (`code`) USING BTREE COMMENT '编码'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of member_role
-- ----------------------------
BEGIN;
INSERT INTO `member_role` (`id`, `pid`, `name`, `code`, `built_in`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1175083532130185217, 0, '用户角色', 'role_member_user', 1, '0', '2025-12-05 14:28:27', '2025-12-05 14:28:27', NULL);
COMMIT;

-- ----------------------------
-- Table structure for member_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `member_role_permission`;
CREATE TABLE `member_role_permission` (
  `id` bigint NOT NULL COMMENT 'ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
  `permission_id` bigint unsigned NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_role` (`role_id`,`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of member_role_permission
-- ----------------------------
BEGIN;
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
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`) USING BTREE,
  KEY `idx_role` (`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of member_role_user
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for member_social_details
-- ----------------------------
DROP TABLE IF EXISTS `member_social_details`;
CREATE TABLE `member_social_details` (
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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of member_social_details
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
  `must_change_pwd` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否必须修改密码（0-否 1-是）',
  `password_changed_at` datetime DEFAULT NULL COMMENT '密码最后修改时间',
  `nickname` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '昵称',
  `phone` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '手机号',
  `email` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '邮件地址',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '头像',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用（0-禁用 1-启用）',
  `locked` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否锁定（冗余字段，详情见 account_lock_state）',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(64) DEFAULT NULL COMMENT '最后登录IP',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`,(coalesce(`deleted_at`,0))) COMMENT '用户名全局唯一（软删除友好）',
  KEY `idx_phone` (`phone`) USING BTREE COMMENT '手机号',
  KEY `idx_email` (`email`) USING BTREE COMMENT '邮箱',
  KEY `idx_enabled` (`enabled`) USING BTREE COMMENT '启用状态索引',
  KEY `idx_locked` (`locked`) USING BTREE COMMENT '锁定状态索引',
  KEY `idx_last_login` (`last_login_at`) USING BTREE COMMENT '最后登录时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of member_user
-- ----------------------------
BEGIN;
INSERT INTO `member_user` (`id`, `username`, `password`, `must_change_pwd`, `password_changed_at`, `nickname`, `phone`, `email`, `avatar`, `enabled`, `locked`, `last_login_at`, `last_login_ip`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1175075664995807234, '18612341234', '{bcrypt}$2a$10$viYtEaHNRKgIUBFMMYQgmOihZiVNDHbZ7179dHL.OfzKX0NtSry9u', 1, '2026-04-22 15:40:33', '测试用户', '18612341234', NULL, 'ingot/user/avatar/ic_logo.png', 1, 0, NULL, NULL, 0, '2025-12-05 13:57:11', '2025-12-05 14:28:05', NULL);
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`) USING BTREE,
  KEY `idx_tenant_user` (`tenant_id`,`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of member_user_tenant
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for account_lock_state
-- ----------------------------
DROP TABLE IF EXISTS `account_lock_state`;
CREATE TABLE `account_lock_state` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_type` varchar(20) NOT NULL DEFAULT '0' COMMENT '用户类型（0-系统用户 1-C端用户，同 UserTypeEnum.value）',
  `locked` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否锁定（0-否 1-是）',
  `lock_type` varchar(20) DEFAULT NULL COMMENT '锁定类型（MANUAL-手动 AUTO-自动）',
  `lock_reason_code` varchar(50) DEFAULT NULL COMMENT '锁定原因代码',
  `lock_reason_detail` varchar(500) DEFAULT NULL COMMENT '锁定原因详情',
  `locked_at` datetime DEFAULT NULL COMMENT '锁定时间',
  `locked_until` datetime DEFAULT NULL COMMENT '锁定到期时间（NULL=永久锁定）',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人姓名',
  `failed_login_count` int NOT NULL DEFAULT '0' COMMENT '连续失败次数',
  `last_failed_at` datetime DEFAULT NULL COMMENT '最后失败时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id_type` (`user_id`,`user_type`) COMMENT '用户ID + 用户类型联合唯一',
  KEY `idx_locked` (`locked`,`locked_until`) USING BTREE COMMENT '锁定状态和到期时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号锁定状态表';

-- ----------------------------
-- Records of account_lock_state
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for account_security_event
-- ----------------------------
DROP TABLE IF EXISTS `account_security_event`;
CREATE TABLE `account_security_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_type` varchar(20) NOT NULL COMMENT '用户类型（0-系统用户 1-C端用户，同 UserTypeEnum.value）',
  `event_type` varchar(50) NOT NULL COMMENT '事件类型',
  `event_category` varchar(20) NOT NULL COMMENT '事件分类（AUTH-认证 ACCOUNT-账号 CREDENTIAL-凭证）',
  `reason_code` varchar(50) DEFAULT NULL COMMENT '原因代码',
  `reason_detail` varchar(500) DEFAULT NULL COMMENT '详细描述',
  `result` varchar(20) DEFAULT NULL COMMENT '结果（SUCCESS-成功 FAILURE-失败）',
  `source` varchar(50) DEFAULT NULL COMMENT '来源（AUTH-认证服务 PMS-PMS服务 MEMBER-Member服务 SYSTEM-系统）',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人姓名',
  `client_ip` varchar(64) DEFAULT NULL COMMENT '客户端IP',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '客户端信息（User-Agent）',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID（来自上下文）',
  `extra_data` json DEFAULT NULL COMMENT '扩展数据',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_event` (`user_id`,`user_type`,`event_type`,`created_at`) USING BTREE COMMENT '用户事件查询索引',
  KEY `idx_created_at` (`created_at`) USING BTREE COMMENT '时间索引',
  KEY `idx_event_type` (`event_type`,`created_at`) USING BTREE COMMENT '事件类型索引',
  KEY `idx_tenant` (`tenant_id`,`created_at`) USING BTREE COMMENT '租户索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号安全事件表';

-- ----------------------------
-- Records of account_security_event
-- ----------------------------
BEGIN;
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
