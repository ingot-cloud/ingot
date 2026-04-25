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

 Date: 25/04/2026 11:03:21
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号锁定状态表';

-- ----------------------------
-- Records of account_lock_state
-- ----------------------------
BEGIN;
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (25, 1221851150714556417, '0', 0, 'AUTO', 'LOGIN_FAIL', NULL, '2026-04-13 15:54:35', '2026-04-13 16:24:35', 1, 'admin', 0, NULL, '2026-04-13 15:46:37', '2026-04-13 16:51:12');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (30, 1221872417630998529, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-04-13 17:10:47', '2026-04-13 17:10:47');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (33, 1221837466541903874, '0', 0, 'MANUAL', 'MANUAL', NULL, '2026-04-22 17:19:06', '2026-04-23 00:00:00', 1, 'admin', 0, NULL, '2026-04-21 14:43:35', '2026-04-22 17:19:08');
COMMIT;

-- ----------------------------
-- Table structure for account_security_event
-- ----------------------------
DROP TABLE IF EXISTS `account_security_event`;
CREATE TABLE `account_security_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_type` varchar(20) NOT NULL COMMENT '用户类型（PLATFORM-平台用户 APP-应用用户）',
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
) ENGINE=InnoDB AUTO_INCREMENT=88 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号安全事件表';

-- ----------------------------
-- Records of account_security_event
-- ----------------------------
BEGIN;
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (27, 1221851150714556417, 'ADMIN', 'LOGIN_FAILURE', 'AUTH', NULL, 'S0400', 'FAILURE', 'AUTH', NULL, NULL, '192.168.1.148', NULL, NULL, NULL, '2026-04-13 15:46:37');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (28, 1221851150714556417, 'ADMIN', 'LOGIN_FAILURE', 'AUTH', NULL, 'S0400', 'FAILURE', 'AUTH', NULL, NULL, '192.168.1.148', NULL, NULL, NULL, '2026-04-13 15:47:30');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (29, 1221851150714556417, 'ADMIN', 'LOGIN_FAILURE', 'AUTH', NULL, 'S0400', 'FAILURE', 'AUTH', NULL, NULL, '192.168.1.148', NULL, NULL, NULL, '2026-04-13 15:47:40');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (30, 1221851150714556417, 'ADMIN', 'LOGIN_FAILURE', 'AUTH', NULL, 'S0400', 'FAILURE', 'AUTH', NULL, NULL, '192.168.1.148', NULL, NULL, NULL, '2026-04-13 15:53:20');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (31, 1221851150714556417, 'ADMIN', 'LOGIN_FAILURE', 'AUTH', NULL, 'S0400', 'FAILURE', 'AUTH', NULL, NULL, '192.168.1.148', NULL, NULL, NULL, '2026-04-13 15:54:35');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (32, 1221851150714556417, 'ADMIN', 'ACCOUNT_LOCKED', 'ACCOUNT', 'LOGIN_FAIL', '登录失败次数超限', NULL, 'SYSTEM', NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-13 15:54:35');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (33, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.148', NULL, NULL, NULL, '2026-04-13 15:55:09');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (34, 1221851150714556417, 'ADMIN', 'ACCOUNT_UNLOCKED', 'ACCOUNT', NULL, '1', NULL, 'PMS', 1, 'admin', NULL, NULL, NULL, NULL, '2026-04-13 15:57:09');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (35, 1221851150714556417, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.148', NULL, NULL, NULL, '2026-04-13 15:58:02');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (36, 1221851150714556417, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.148', NULL, NULL, NULL, '2026-04-13 16:00:29');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (37, 1221851150714556417, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.148', NULL, NULL, NULL, '2026-04-13 16:07:43');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (38, 1221851150714556417, 'ADMIN', 'PASSWORD_RESET', 'CREDENTIAL', NULL, NULL, 'SUCCESS', 'PMS', 1221851150714556417, 'test1', NULL, NULL, NULL, NULL, '2026-04-13 16:31:21');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (39, 1221851150714556417, 'ADMIN', 'PASSWORD_RESET', 'CREDENTIAL', NULL, NULL, 'SUCCESS', 'PMS', 1221851150714556417, 'test1', NULL, NULL, NULL, NULL, '2026-04-13 16:39:14');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (40, 1221851150714556417, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.148', NULL, NULL, NULL, '2026-04-13 16:39:29');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (41, 1221851150714556417, 'ADMIN', 'FORCE_CHANGE_PASSWORD', 'CREDENTIAL', NULL, NULL, 'SUCCESS', 'PMS', NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-13 16:51:01');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (42, 1221851150714556417, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.148', NULL, NULL, NULL, '2026-04-13 16:51:12');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (43, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.148', NULL, NULL, NULL, '2026-04-13 17:01:50');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (44, 1221872417630998529, 'ADMIN', 'ACCOUNT_CREATED', 'ACCOUNT', NULL, NULL, 'SUCCESS', 'SYSTEM', 1, NULL, NULL, NULL, NULL, NULL, '2026-04-13 17:10:47');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (45, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.148', NULL, NULL, NULL, '2026-04-14 08:16:38');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (46, 1222105570597556226, 'ADMIN', 'ACCOUNT_CREATED', 'ACCOUNT', NULL, NULL, 'SUCCESS', 'PMS', 1, NULL, NULL, NULL, NULL, NULL, '2026-04-14 08:37:15');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (47, 1222105570597556226, 'ADMIN', 'ACCOUNT_DELETED', 'ACCOUNT', NULL, NULL, NULL, 'PMS', 1, 'admin', NULL, NULL, NULL, NULL, '2026-04-14 08:37:51');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (48, 1222119790152142849, 'ADMIN', 'ACCOUNT_DELETED', 'ACCOUNT', NULL, NULL, NULL, 'PMS', 1, 'admin', NULL, NULL, NULL, NULL, '2026-04-14 10:12:40');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (49, 1222129624926801921, 'ADMIN', 'ACCOUNT_CREATED', 'ACCOUNT', NULL, NULL, 'SUCCESS', 'PMS', 1, NULL, NULL, NULL, NULL, NULL, '2026-04-14 10:12:50');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (50, 1222129624926801921, 'ADMIN', 'ACCOUNT_DELETED', 'ACCOUNT', NULL, NULL, NULL, 'PMS', 1, 'admin', NULL, NULL, NULL, NULL, '2026-04-14 10:13:24');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (51, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.148', NULL, NULL, NULL, '2026-04-14 11:17:00');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (52, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.148', NULL, NULL, NULL, '2026-04-14 11:17:23');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (53, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-04-21 14:40:01');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (54, 1221837466541903874, 'ADMIN', 'LOGIN_FAILURE', 'AUTH', NULL, 'S0400', 'FAILURE', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-04-21 14:43:35');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (55, 1221837466541903874, 'ADMIN', 'LOGIN_FAILURE', 'AUTH', NULL, 'S0400', 'FAILURE', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-04-21 14:44:24');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (56, 1221837466541903874, 'ADMIN', 'LOGIN_FAILURE', 'AUTH', NULL, 'S0400', 'FAILURE', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-04-21 14:44:29');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (57, 1221837466541903874, 'ADMIN', 'LOGIN_FAILURE', 'AUTH', NULL, 'S0400', 'FAILURE', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-04-21 14:44:35');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (58, 1221837466541903874, 'ADMIN', 'LOGIN_FAILURE', 'AUTH', NULL, 'S0400', 'FAILURE', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-04-21 14:44:41');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (59, 1221837466541903874, 'ADMIN', 'ACCOUNT_LOCKED', 'ACCOUNT', 'LOGIN_FAIL', '登录失败次数超限', NULL, 'SYSTEM', NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-21 14:44:41');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (60, 1221837466541903874, 'ADMIN', 'LOGIN_FAILURE', 'AUTH', NULL, 'S0400', 'FAILURE', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-04-21 14:44:59');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (61, 1221837466541903874, 'ADMIN', 'ACCOUNT_LOCKED', 'ACCOUNT', 'LOGIN_FAIL', '登录失败次数超限', NULL, 'SYSTEM', NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-21 14:44:59');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (62, 1221837466541903874, 'ADMIN', 'LOGIN_FAILURE', 'AUTH', NULL, 'S0400', 'FAILURE', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-04-21 14:46:40');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (63, 1221837466541903874, 'ADMIN', 'ACCOUNT_LOCKED', 'ACCOUNT', 'LOGIN_FAIL', '登录失败次数超限', NULL, 'SYSTEM', NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-21 14:46:40');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (64, 1221837466541903874, 'ADMIN', 'LOGIN_FAILURE', 'AUTH', NULL, 'S0400', 'FAILURE', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-04-21 14:47:14');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (65, 1221837466541903874, 'ADMIN', 'ACCOUNT_LOCKED', 'ACCOUNT', 'LOGIN_FAIL', '登录失败次数超限', NULL, 'SYSTEM', NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-21 14:47:14');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (66, 1221837466541903874, 'ADMIN', 'LOGIN_FAILURE', 'AUTH', NULL, 'S0400', 'FAILURE', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-04-21 14:57:21');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (67, 1221837466541903874, 'ADMIN', 'ACCOUNT_LOCKED', 'ACCOUNT', 'LOGIN_FAIL', '登录失败次数超限', NULL, 'SYSTEM', NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-21 14:57:21');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (68, 1221837466541903874, 'ADMIN', 'LOGIN_FAILURE', 'AUTH', NULL, 'S0400', 'FAILURE', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-04-21 15:00:24');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (69, 1221837466541903874, 'ADMIN', 'ACCOUNT_LOCKED', 'ACCOUNT', 'LOGIN_FAIL', '登录失败次数超限', NULL, 'SYSTEM', NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-21 15:00:24');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (70, 1221837466541903874, 'ADMIN', 'LOGIN_FAILURE', 'AUTH', NULL, 'S0400', 'FAILURE', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-04-21 15:09:50');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (71, 1221837466541903874, 'ADMIN', 'ACCOUNT_LOCKED', 'ACCOUNT', 'LOGIN_FAIL', '登录失败次数超限', NULL, 'SYSTEM', NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-21 15:09:50');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (72, 1221837466541903874, 'ADMIN', 'LOGIN_FAILURE', 'AUTH', NULL, 'S0400', 'FAILURE', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-04-21 15:21:15');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (73, 1221837466541903874, 'ADMIN', 'ACCOUNT_LOCKED', 'ACCOUNT', 'LOGIN_FAIL', '登录失败次数超限', NULL, 'SYSTEM', NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-21 15:21:15');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (74, 1221837466541903874, 'ADMIN', 'LOGIN_FAILURE', 'AUTH', NULL, 'S0400', 'FAILURE', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-04-21 15:36:14');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (75, 1221837466541903874, 'ADMIN', 'ACCOUNT_LOCKED', 'ACCOUNT', 'LOGIN_FAIL', '登录失败次数超限', NULL, 'SYSTEM', NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-21 15:36:14');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (76, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-04-21 15:43:50');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (77, 1221837466541903874, 'ADMIN', 'ACCOUNT_UNLOCKED', 'ACCOUNT', NULL, '临时锁定已过期', NULL, 'SYSTEM', NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-21 16:07:00');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (78, 1221837466541903874, 'ADMIN', 'LOGIN_FAILURE', 'AUTH', NULL, 'S0400', 'FAILURE', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-04-21 16:08:11');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (79, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.101', NULL, NULL, NULL, '2026-04-22 15:36:58');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (80, 1221837466541903874, 'ADMIN', 'ACCOUNT_DISABLED', 'ACCOUNT', NULL, NULL, NULL, 'PMS', 1, 'admin', NULL, NULL, NULL, NULL, '2026-04-22 17:18:57');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (81, 1221837466541903874, 'ADMIN', 'ACCOUNT_ENABLED', 'ACCOUNT', NULL, NULL, NULL, 'PMS', 1, 'admin', NULL, NULL, NULL, NULL, '2026-04-22 17:19:00');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (82, 1221837466541903874, 'ADMIN', 'ACCOUNT_LOCKED', 'ACCOUNT', 'MANUAL', '123', NULL, 'PMS', 1, 'admin', NULL, NULL, NULL, NULL, '2026-04-22 17:19:06');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (83, 1221837466541903874, 'ADMIN', 'ACCOUNT_UNLOCKED', 'ACCOUNT', NULL, '123', NULL, 'PMS', 1, 'admin', NULL, NULL, NULL, NULL, '2026-04-22 17:19:08');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (84, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.101', NULL, NULL, NULL, '2026-04-23 09:59:28');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (85, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.103', NULL, NULL, NULL, '2026-04-24 10:44:13');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (86, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.103', NULL, NULL, NULL, '2026-04-24 17:13:08');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (87, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.103', NULL, NULL, NULL, '2026-04-25 10:16:18');
COMMIT;

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
INSERT INTO `biz_leaf_alloc` (`biz_tag`, `max_id`, `step`, `description`, `update_time`) VALUES ('org_code', 5001, 1000, '组织编码', '2026-04-13 06:54:12');
INSERT INTO `biz_leaf_alloc` (`biz_tag`, `max_id`, `step`, `description`, `update_time`) VALUES ('org_role_code', 38001, 1000, '组织角色编码', '2025-11-26 01:19:25');
COMMIT;

-- ----------------------------
-- Table structure for password_expiration
-- ----------------------------
DROP TABLE IF EXISTS `password_expiration`;
CREATE TABLE `password_expiration` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `last_changed_at` datetime NOT NULL COMMENT '最后修改密码时间',
  `expires_at` datetime NOT NULL COMMENT '密码过期时间',
  `grace_login_remaining` int NOT NULL DEFAULT '0' COMMENT '剩余宽限登录次数',
  `next_warning_at` datetime DEFAULT NULL COMMENT '下次提醒时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_expires_at` (`expires_at`),
  KEY `idx_next_warning_at` (`next_warning_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1215701794488721410 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密码过期信息';

-- ----------------------------
-- Records of password_expiration
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for password_history
-- ----------------------------
DROP TABLE IF EXISTS `password_history`;
CREATE TABLE `password_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `password_hash` varchar(255) NOT NULL COMMENT '密码哈希值',
  `sequence_number` int NOT NULL COMMENT '序号（用于环形缓冲，从1开始）',
  `version` bigint NOT NULL DEFAULT '1' COMMENT '版本',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_sequence` (`user_id`,`sequence_number`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=1222129625534976003 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密码历史记录（环形缓冲）';

-- ----------------------------
-- Records of password_history
-- ----------------------------
BEGIN;
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1221867441915580417, 1221851150714556417, '{bcrypt}$2a$10$Wwmp1ReZ2hQtS0u.4xajaOPRVYlcyi9gYyMeq221gMnDaOcuQYG1m', 1, 1, '2026-04-13 16:51:00', '2026-04-13 16:51:00');
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1221872418234978306, 1221872417630998529, '{bcrypt}$2a$10$z.9umISvZpx/Q/gshY70n.nlSF9r6u.NN/PQ9HjaAYeNqcm49cm3e', 1, 1, '2026-04-13 17:10:47', '2026-04-13 17:10:47');
COMMIT;

-- ----------------------------
-- Table structure for platform_app
-- ----------------------------
DROP TABLE IF EXISTS `platform_app`;
CREATE TABLE `platform_app` (
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
-- Records of platform_app
-- ----------------------------
BEGIN;
INSERT INTO `platform_app` (`id`, `menu_id`, `permission_id`, `name`, `icon`, `intro`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171529080861356034, 881187162883403778, 881187314109034498, '通讯录', 'typcn:contacts', '通讯录', '0', '2025-11-25 19:04:20', '2025-11-25 19:04:20', NULL);
INSERT INTO `platform_app` (`id`, `menu_id`, `permission_id`, `name`, `icon`, `intro`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1172811256819085314, 968448105293086721, 969898113037041666, '链接', 'tdesign:link', '12213', '0', '2025-11-29 07:59:15', '2025-11-29 07:59:15', NULL);
COMMIT;

-- ----------------------------
-- Table structure for platform_dict
-- ----------------------------
DROP TABLE IF EXISTS `platform_dict`;
CREATE TABLE `platform_dict` (
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
-- Records of platform_dict
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for platform_menu
-- ----------------------------
DROP TABLE IF EXISTS `platform_menu`;
CREATE TABLE `platform_menu` (
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
-- Records of platform_menu
-- ----------------------------
BEGIN;
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (782579756306313217, 0, '管理身份', '0', '/platform/admin', b'1', 782647310861250562, 1, '@/layouts/InAppLayout.vue', NULL, '/platform/admin/user', 'eos-icons:admin', 3, 0, 0, 0, 0, '0', '0', NULL, '0', '2022-12-18 11:58:35', '2026-04-25 10:32:32', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (868164540688019458, 0, '开发者平台', '0', '/platform/develop', b'1', 868163807997636610, 1, '@/layouts/InAppLayout.vue', NULL, '/platform/develop/qrcode', 'material-symbols:developer-mode-tv-outline', 5, 0, 0, 0, 0, '0', '0', NULL, '0', '2023-08-11 16:01:57', '2026-04-25 10:24:09', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (868164933065158657, 868164540688019458, '生成二维码', '1', '/platform/develop/qrcode', b'1', 868164119483428866, 0, '@/pages/platform/develop/qrcode/IndexPage.vue', NULL, NULL, NULL, 10, 0, 0, 0, 0, '0', '0', NULL, '0', '2023-08-11 16:03:31', '2025-11-25 17:39:20', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (881182442898894850, 0, '用户身份', '0', '/platform/member', b'1', 881181673671929858, 1, '@/layouts/InAppLayout.vue', NULL, '/platform/member/user', 'carbon:app', 4, 0, 0, 0, 0, '0', '0', NULL, '0', '2023-09-16 14:10:27', '2026-04-25 10:23:47', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (881182648029720577, 881182442898894850, '用户管理', '1', '/platform/member/user', b'1', 881181747638480898, 0, '@/pages/platform/member/user/IndexPage.vue', NULL, NULL, NULL, 1, 0, 0, 0, 0, '0', '0', NULL, '0', '2023-09-16 14:11:16', '2025-12-05 11:01:45', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (881182743173312514, 881182442898894850, '角色管理', '1', '/platform/member/role', b'1', 881181819205890049, 0, '@/pages/platform/member/role/IndexPage.vue', NULL, NULL, NULL, 1, 0, 0, 0, 0, '0', '0', NULL, '0', '2023-09-16 14:11:39', '2025-12-05 11:01:54', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (881187162883403778, 0, '通讯录', '0', '/org/contacts', b'1', 881187314109034498, 1, '@/layouts/InAppLayout.vue', NULL, '/org/contacts/user', 'typcn:contacts', 100, 0, 0, 0, 0, '1', '0', NULL, '0', '2023-09-16 14:29:12', '2025-11-25 18:09:03', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (881188246884495361, 881187162883403778, '成员管理', '1', '/org/contacts/user', b'1', 881187363425660929, 0, '@/pages/org/contacts/user/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '1', '0', NULL, '0', '2023-09-16 14:33:31', '2025-11-25 18:10:43', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (881188426534924289, 881187162883403778, '部门管理', '1', '/org/contacts/dept', b'1', 881187407549739010, 1, '@/pages/org/contacts/dept/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '1', '0', NULL, '0', '2023-09-16 14:34:14', '2024-05-19 10:23:30', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (881188564993093634, 881187162883403778, '角色管理', '1', '/org/contacts/role', b'1', 881187486947913730, 1, '@/pages/org/contacts/role/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '1', '0', NULL, '0', '2023-09-16 14:34:47', '2024-05-19 10:23:26', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (894887232728248321, 868164540688019458, '客户端管理', '1', '/platform/develop/client', b'1', 894886463954268162, 0, '@/pages/platform/develop/client/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2023-10-24 09:48:24', '2025-11-25 17:41:19', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (894887953284509698, 868164540688019458, '社交管理', '1', '/platform/develop/social', b'1', 894886696582950913, 0, '@/pages/platform/develop/social/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2023-10-24 09:51:15', '2025-11-25 17:39:28', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (894981446371684354, 868164540688019458, '业务ID管理', '1', '/platform/develop/id', b'1', 894979974569439234, 0, '@/pages/platform/develop/id/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2023-10-24 16:02:46', '2025-11-25 17:39:32', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (908759165842030594, 881187162883403778, '权限管理', '1', '/org/contacts/auth', b'1', 908756155371945986, 0, '@/pages/org/contacts/auth/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '1', '0', NULL, '0', '2023-12-01 16:30:30', '2025-05-08 10:05:50', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (968448105293086721, 0, '链接', '0', '/link', b'1', 969898113037041666, 1, '@/layouts/InAppLayout.vue', NULL, NULL, 'tdesign:link', 999, 0, 0, 0, 0, '1', '0', NULL, '0', '2024-05-14 09:33:02', '2025-02-01 19:31:13', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (968449951562792962, 968448105293086721, 'Vue', '1', '/link/ZAWG4gOT', b'1', 969898113037041666, 1, '@/layouts/InIFrameLayout.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '1', '1', 'https://cn.vuejs.org/', '0', '2024-05-14 09:40:22', '2024-05-19 10:23:55', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (968455189648703489, 968448105293086721, '阿里云', '1', '/link/ulahCLJw', b'1', 969898113037041666, 1, '@/layouts/InIFrameLayout.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '1', '1', 'http://www.aliyun.com', '0', '2024-05-14 10:01:11', '2024-05-19 10:23:59', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (968555619909173250, 968448105293086721, '百度', '1', '/link/CtA3FqHR', b'1', 969898113037041666, 1, '@/layouts/InExtLinkLayout.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '1', '2', 'https://www.baidu.com', '0', '2024-05-14 16:40:16', '2024-05-19 10:24:02', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (969900188395438082, 968448105293086721, '阿里云测试2', '1', '/link/NgEObclX', b'1', 970014721428488193, 1, '@/layouts/InIFrameLayout.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '1', '1', 'https://www.aliyun.com', '0', '2024-05-18 09:43:06', '2024-05-19 10:37:18', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1075465479671361538, 782579756306313217, '用户管理', '1', '/platform/admin/user', b'1', 1075465479608446977, 0, '@/pages/platform/admin/user/IndexPage.vue', NULL, NULL, NULL, 5, 0, 0, 0, 0, '0', '0', NULL, '0', '2025-03-05 17:01:53', '2026-04-25 10:34:40', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1098553864240046081, 881187162883403778, '组织架构', '1', '/org/contacts/structure', b'1', 1098553864210685954, 0, '@/pages/org/contacts/structure/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '1', '0', NULL, '0', '2025-05-08 10:06:52', '2025-11-25 17:15:45', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171383124115320834, 0, '平台配置', '0', '/platform/base', b'1', 1171383124023046146, 1, '@/layouts/InAppLayout.vue', NULL, '/platform/base/menu', 'mynaui:config', 1, 0, 0, 0, 0, '0', '0', NULL, '0', '2025-11-25 09:24:21', '2026-04-25 10:26:00', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171384799689437186, 1171383124115320834, '菜单管理', '1', '/platform/base/menu', b'1', 1171384799576190978, 0, '@/pages/platform/base/menu/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2025-11-25 09:31:01', '2025-11-25 17:19:32', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171385137007947777, 1171383124115320834, '角色管理', '1', '/platform/base/role', b'1', 1171385136957616129, 0, '@/pages/platform/base/role/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2025-11-25 09:32:21', '2025-11-25 17:19:11', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171385232201871361, 1171383124115320834, '权限管理', '1', '/platform/base/permission', b'1', 1171385232147345409, 0, '@/pages/platform/base/permission/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2025-11-25 09:32:44', '2025-11-25 17:19:15', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171385548041351169, 1171383124115320834, '应用管理', '1', '/platform/base/app', b'1', 1171385547961659394, 0, '@/pages/platform/base/app/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2025-11-25 09:33:59', '2025-11-25 17:19:26', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171506377827807234, 0, '组织管理', '0', '/platform/org', b'1', 1171506377735532545, 0, '@/layouts/InAppLayout.vue', NULL, '/platform/org/tenant', 'clarity:organization-line', 2, 0, 0, 0, 0, '0', '0', NULL, '0', '2025-11-25 17:34:07', '2026-04-25 10:22:16', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171506495809384449, 1171506377827807234, '组织管理', '1', '/platform/org/tenant', b'1', 1171506495763247105, 0, '@/pages/platform/org/tenant/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2025-11-25 17:34:35', '2025-11-25 17:34:35', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1175028897327607810, 881182442898894850, '权限管理', '1', '/platform/member/permission', b'1', 1175028897264693250, 0, '@/pages/platform/member/permission/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2025-12-05 10:51:21', '2025-12-05 11:01:57', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1197222719172567042, 0, '安全中心', '0', '/platform/security', b'1', 1197222719055126529, 1, '@/layouts/InAppLayout.vue', NULL, '/platform/security/credential', 'ic:sharp-security', 5, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-02-04 16:41:41', '2026-02-06 15:52:06', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1197223040129097729, 1197222719172567042, '凭证策略', '1', '/platform/security/credential', b'1', 1197223040045211649, 0, '@/pages/platform/security/credential/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-02-04 16:42:57', '2026-02-04 17:17:50', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1225772359370035202, 0, '审计与日志', '0', '/platform/aam', b'1', 1225772359281954818, 1, '@/layouts/InAppLayout.vue', NULL, '/platform/aam/onlinetoken', 'solar:monitor-broken', 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-04-24 11:27:45', '2026-04-24 11:27:45', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `name`, `menu_type`, `path`, `enable_permission`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1225772508536262658, 1225772359370035202, '在线用户', '1', '/platform/aam/onlinetoken', b'1', 1225772508464959490, 0, '@/pages/platform/aam/onlinetoken/IndexPage.vue', NULL, NULL, NULL, 999, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-04-24 11:28:21', '2026-04-24 11:28:21', NULL);
COMMIT;

-- ----------------------------
-- Table structure for platform_permission
-- ----------------------------
DROP TABLE IF EXISTS `platform_permission`;
CREATE TABLE `platform_permission` (
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
-- Records of platform_permission
-- ----------------------------
BEGIN;
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (782647310861250562, 0, '管理身份', 'platform:admin', '0', '0', '0', '', '2022-12-18 16:27:01', '2026-04-25 10:32:32', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (782650139818635265, 782647310861250562, '租户管理', 'platform:system:tenant', '0', '0', '0', '', '2022-12-18 16:38:15', NULL, NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (868163807997636610, 0, '开发者平台', 'platform:develop', '0', '0', '0', '', '2023-08-11 15:59:03', '2026-04-25 10:24:09', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (868164119483428866, 868163807997636610, '生成二维码', 'platform:develop:qrcode', '0', '0', '0', '', '2023-08-11 16:00:17', '2025-11-25 17:39:20', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (881181673671929858, 0, '用户身份', 'platform:member', '0', '0', '0', '', '2023-09-16 14:07:24', '2026-04-25 10:23:47', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (881181747638480898, 881181673671929858, '用户管理', 'platform:member:user', '0', '0', '0', '', '2023-09-16 14:07:41', '2025-12-05 11:01:45', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (881181819205890049, 881181673671929858, '角色管理', 'platform:member:role', '0', '0', '0', '', '2023-09-16 14:07:58', '2025-12-05 11:01:54', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (881187314109034498, 0, '通讯录', 'org:contacts', '0', '1', '0', '', '2023-09-16 14:29:48', '2025-11-25 18:09:03', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (881187363425660929, 881187314109034498, '成员管理', 'org:contacts:user', '0', '1', '0', '', '2023-09-16 14:30:00', '2025-11-25 18:10:43', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (881187407549739010, 881187314109034498, '部门管理', 'org:contacts:dept', '0', '1', '0', '', '2023-09-16 14:30:11', '2024-05-19 10:23:30', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (881187486947913730, 881187314109034498, '角色管理', 'org:contacts:role', '0', '1', '0', '', '2023-09-16 14:30:30', '2024-05-19 10:23:26', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (894886463954268162, 868163807997636610, '客户端管理', 'platform:develop:client', '0', '0', '0', '', '2023-10-24 09:45:20', '2025-11-25 17:41:19', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (894886696582950913, 868163807997636610, '社交管理', 'platform:develop:social', '0', '0', '0', '', '2023-10-24 09:46:16', '2025-11-25 17:39:28', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (894979974569439234, 868163807997636610, 'ID管理', 'platform:develop:id', '0', '0', '0', '', '2023-10-24 15:56:55', '2025-11-25 17:39:32', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (908756155371945986, 881187314109034498, '权限管理', 'org:contacts:auth', '0', '1', '0', '', '2023-12-01 16:18:32', '2025-05-08 10:05:50', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (969898113037041666, 0, '链接', 'link', '0', '1', '0', '', '2024-05-18 09:34:51', '2025-02-01 19:31:13', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (970014721428488193, 969898113037041666, '阿里云测试2', 'link:asdasd', '0', '1', '0', '', '2024-05-18 17:18:13', '2024-05-19 10:37:18', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1075465479608446977, 782647310861250562, '用户管理', 'platform:admin:user', '0', '0', '0', '', '2025-03-05 17:01:53', '2026-04-25 10:34:40', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1098553864210685954, 881187314109034498, '组织架构', 'org:contacts:structure', '0', '1', '0', '', '2025-05-08 10:06:52', '2025-11-25 17:15:45', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171383124023046146, 0, '平台配置', 'platform:base', '0', '0', '0', '', '2025-11-25 09:24:21', '2026-04-25 10:26:00', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171384799576190978, 1171383124023046146, '菜单管理', 'platform:base:menu', '0', '0', '0', '', '2025-11-25 09:31:01', '2025-11-25 17:19:32', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171385136957616129, 1171383124023046146, '角色管理', 'platform:base:role', '0', '0', '0', '', '2025-11-25 09:32:21', '2025-11-25 17:19:11', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171385232147345409, 1171383124023046146, '权限管理', 'platform:base:permission', '0', '0', '0', '', '2025-11-25 09:32:44', '2025-11-25 17:19:15', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171385547961659394, 1171383124023046146, '应用管理', 'platform:base:app', '0', '0', '0', '', '2025-11-25 09:33:59', '2025-11-25 17:19:26', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171506377735532545, 0, '组织管理', 'platform:org', '0', '0', '0', '', '2025-11-25 17:34:07', '2026-04-25 10:22:16', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171506495763247105, 1171506377735532545, '组织管理', 'platform:org:tenant', '0', '0', '0', '', '2025-11-25 17:34:35', '2025-11-25 17:34:35', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1175028897264693250, 881181673671929858, '权限管理', 'platform:member:permission', '0', '0', '0', '', '2025-12-05 10:51:21', '2025-12-05 11:01:57', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1197222719055126529, 0, '安全中心', 'platform:security', '0', '0', '0', '', '2026-02-04 16:41:41', '2026-02-06 15:52:06', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1197223040045211649, 1197222719055126529, '凭证策略', 'platform:security:credential', '0', '0', '0', '', '2026-02-04 16:42:57', '2026-02-04 17:17:50', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1225772359281954818, 0, '审计与日志', 'platform:aam', '0', '0', '0', '', '2026-04-24 11:27:45', '2026-04-24 11:27:45', NULL);
INSERT INTO `platform_permission` (`id`, `pid`, `name`, `code`, `type`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1225772508464959490, 1225772359281954818, '在线用户', 'platform:aam:onlinetoken', '0', '0', '0', '', '2026-04-24 11:28:21', '2026-04-24 11:28:21', NULL);
COMMIT;

-- ----------------------------
-- Table structure for platform_role
-- ----------------------------
DROP TABLE IF EXISTS `platform_role`;
CREATE TABLE `platform_role` (
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
-- Records of platform_role
-- ----------------------------
BEGIN;
INSERT INTO `platform_role` (`id`, `pid`, `name`, `code`, `subject`, `type`, `org_type`, `filter_dept`, `scope_type`, `scopes`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1, 0, '超级管理员', 'role_admin', '0', '0', '0', 0, 0, '[]', '0', '2021-01-03 11:07:59', '2025-04-03 16:46:14', NULL);
INSERT INTO `platform_role` (`id`, `pid`, `name`, `code`, `subject`, `type`, `org_type`, `filter_dept`, `scope_type`, `scopes`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (2, 1000, '管理员', 'role_org_admin', '0', '0', '1', 0, 0, '[]', '0', '2021-06-23 09:28:19', '2024-05-13 14:04:55', NULL);
INSERT INTO `platform_role` (`id`, `pid`, `name`, `code`, `subject`, `type`, `org_type`, `filter_dept`, `scope_type`, `scopes`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (3, 1000, '子管理员', 'role_org_sub_admin', '0', '0', '1', 0, 0, '[]', '0', '2021-06-23 09:28:33', '2025-11-27 14:15:06', NULL);
INSERT INTO `platform_role` (`id`, `pid`, `name`, `code`, `subject`, `type`, `org_type`, `filter_dept`, `scope_type`, `scopes`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000, 0, '默认', '', '0', '1', '1', 0, 0, '[]', '0', '2025-11-24 16:59:34', '2025-11-25 13:04:57', NULL);
INSERT INTO `platform_role` (`id`, `pid`, `name`, `code`, `subject`, `type`, `org_type`, `filter_dept`, `scope_type`, `scopes`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1075807902184353794, 1000, '主管', 'role_org_manager', '0', '0', '1', 1, 2, '[]', '0', '2025-03-06 15:42:33', '2025-04-29 17:29:02', NULL);
COMMIT;

-- ----------------------------
-- Table structure for platform_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `platform_role_permission`;
CREATE TABLE `platform_role_permission` (
  `id` bigint NOT NULL COMMENT 'ID',
  `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
  `permission_id` bigint unsigned NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of platform_role_permission
-- ----------------------------
BEGIN;
INSERT INTO `platform_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1172814842718318593, 2, 881187314109034498);
INSERT INTO `platform_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1197223218911305729, 1, 782647310861250562);
INSERT INTO `platform_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1197223218923888642, 1, 868163807997636610);
INSERT INTO `platform_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1197223218928082945, 1, 881181673671929858);
INSERT INTO `platform_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1197223218928082946, 1, 1171383124023046146);
INSERT INTO `platform_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1197223218928082947, 1, 1171506377735532545);
INSERT INTO `platform_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1197223218928082948, 1, 1197222719055126529);
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
INSERT INTO `sys_social_details` (`id`, `tenant_id`, `app_id`, `app_secret`, `redirect_url`, `name`, `type`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (925397365717008385, 1, 'wx123123123', '123123123', NULL, 'ingot开源小程序', 'wechat_miniprogram', '0', '2024-01-16 14:24:46', '2025-12-07 14:41:26', NULL);
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
INSERT INTO `sys_tenant` (`id`, `name`, `code`, `org_type`, `avatar`, `status`, `plan_id`, `end_at`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171766486562762754, '测试组织', 'org_%d192114', '1', 'ingot/user/avatar/ic_logo.png', '0', 0, NULL, '2025-11-26 10:47:42', NULL, '2026-04-13 06:53:45');
INSERT INTO `sys_tenant` (`id`, `name`, `code`, `org_type`, `avatar`, `status`, `plan_id`, `end_at`, `created_at`, `updated_at`, `deleted_at`) VALUES (1221838049504022529, '测试组织', 'org_%d256113', '1', 'ingot/user/avatar/jiujiuqiuzhi.jpg', '0', 0, NULL, '2026-04-13 14:54:13', NULL, NULL);
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
  `must_change_pwd` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否必须修改密码（0-否 1-是）',
  `password_changed_at` datetime DEFAULT NULL COMMENT '密码最后修改时间',
  `nickname` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '昵称',
  `phone` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '手机号',
  `email` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '邮件地址',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '头像',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用（0-禁用 1-启用）',
  `locked` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否锁定（冗余字段，详情见 account_lock_state）',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后登录IP',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_username` (`username`,(coalesce(`deleted_at`,0))) COMMENT '用户名全局唯一（软删除友好）',
  KEY `idx_phone` (`phone`) USING BTREE COMMENT '手机号',
  KEY `idx_email` (`email`) USING BTREE COMMENT '邮箱',
  KEY `idx_enabled` (`enabled`) USING BTREE COMMENT '启用状态索引',
  KEY `idx_locked` (`locked`) USING BTREE COMMENT '锁定状态索引',
  KEY `idx_last_login` (`last_login_at`) USING BTREE COMMENT '最后登录时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
BEGIN;
INSERT INTO `sys_user` (`id`, `username`, `password`, `must_change_pwd`, `password_changed_at`, `nickname`, `phone`, `email`, `avatar`, `enabled`, `locked`, `last_login_at`, `last_login_ip`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1, 'admin', '{bcrypt}$2a$10$frVeEWrN5PRLZTheYoMw1uHNiylDcS3uvByYiYACJIvCkAMaoEfr2', 0, '2026-03-10 16:40:30', '超级管理员', '18888888888', 'admin@ingot.com', 'ingot/user/avatar/1/logo.png', 1, 0, '2026-04-25 10:16:18', '192.168.1.103', 0, '2021-01-03 11:02:46', '2026-02-11 14:12:44', NULL);
INSERT INTO `sys_user` (`id`, `username`, `password`, `must_change_pwd`, `password_changed_at`, `nickname`, `phone`, `email`, `avatar`, `enabled`, `locked`, `last_login_at`, `last_login_ip`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1221837466541903874, 'test', '{bcrypt}$2a$10$ks9PAjjnT3Dtz509tY6Pv.Kqk2EsV1jRLU.hBNNTEX2tKn8erbRtK', 1, '2026-04-13 14:51:54', '测试1', '18600000001', NULL, NULL, 1, 0, NULL, NULL, 0, '2026-04-13 14:51:54', '2026-04-13 15:03:36', NULL);
INSERT INTO `sys_user` (`id`, `username`, `password`, `must_change_pwd`, `password_changed_at`, `nickname`, `phone`, `email`, `avatar`, `enabled`, `locked`, `last_login_at`, `last_login_ip`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1221851150714556417, 'test1', '{bcrypt}$2a$10$I4Ps6HCGmYTFFTOGLiq79ezifWrIPdAAjIq4.DNCU95syA00qqu6K', 0, '2026-04-13 16:51:01', '测试初始化密码', '123', NULL, NULL, 1, 0, '2026-04-13 16:51:12', '192.168.1.148', 0, '2026-04-13 15:46:16', NULL, NULL);
INSERT INTO `sys_user` (`id`, `username`, `password`, `must_change_pwd`, `password_changed_at`, `nickname`, `phone`, `email`, `avatar`, `enabled`, `locked`, `last_login_at`, `last_login_ip`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1222129624926801921, '4124124', '{bcrypt}$2a$10$O/cQ1FhmJzE3.KNXO2tDp.OwVRI8FwlkMvsOwrnsEfWzhLdle5YrG', 1, '2026-04-14 10:12:50', '123123', '4124124', NULL, NULL, 1, 0, NULL, NULL, 0, '2026-04-14 10:12:50', NULL, '2026-04-14 02:13:23');
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
INSERT INTO `sys_user_tenant` (`id`, `user_id`, `tenant_id`, `main`, `name`, `avatar`, `created_at`) VALUES (1221838049940230146, 1221837466541903874, 1221838049504022529, b'1', '测试组织', 'ingot/user/avatar/jiujiuqiuzhi.jpg', '2026-04-13 14:54:13');
INSERT INTO `sys_user_tenant` (`id`, `user_id`, `tenant_id`, `main`, `name`, `avatar`, `created_at`) VALUES (1221854021363322882, 1221851150714556417, 1, b'1', '英格特云', 'http://ingot-cloud:9090/ingot/public/tenant/logo.png?t=1710140674513', '2026-04-13 15:57:41');
COMMIT;

-- ----------------------------
-- Table structure for tenant_app_config
-- ----------------------------
DROP TABLE IF EXISTS `tenant_app_config`;
CREATE TABLE `tenant_app_config` (
  `id` bigint NOT NULL COMMENT 'ID',
  `app_id` bigint NOT NULL DEFAULT '0' COMMENT '应用ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `enabled` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否启用',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_app` (`tenant_id`,`app_id`) USING BTREE
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
INSERT INTO `tenant_dept` (`id`, `tenant_id`, `pid`, `name`, `sort`, `main_flag`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171517787697836033, 1, 1, '测试部门', 999, b'0', '0', '2025-11-25 18:19:27', '2026-03-27 16:19:54', NULL);
INSERT INTO `tenant_dept` (`id`, `tenant_id`, `pid`, `name`, `sort`, `main_flag`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1171520634774614018, 1, 1171517787697836033, '测试小组', 999, b'0', '0', '2025-11-25 18:30:46', NULL, NULL);
INSERT INTO `tenant_dept` (`id`, `tenant_id`, `pid`, `name`, `sort`, `main_flag`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1221838049558548481, 1221838049504022529, 0, '测试组织', 0, b'1', '0', '2026-04-13 14:54:13', NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for tenant_role_permission_private
-- ----------------------------
DROP TABLE IF EXISTS `tenant_role_permission_private`;
CREATE TABLE `tenant_role_permission_private` (
  `id` bigint NOT NULL COMMENT 'ID',
  `tenant_id` bigint NOT NULL COMMENT '租户ID',
  `role_id` bigint unsigned NOT NULL COMMENT '角色ID',
  `platform_role` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否为平台角色',
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
  `platform_role` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否为平台角色',
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
INSERT INTO `tenant_role_user_private` (`id`, `tenant_id`, `role_id`, `platform_role`, `user_id`, `dept_id`) VALUES (1172813477321371650, 1, 1, b'1', 1, NULL);
INSERT INTO `tenant_role_user_private` (`id`, `tenant_id`, `role_id`, `platform_role`, `user_id`, `dept_id`) VALUES (1172813477325565954, 1, 2, b'1', 1, NULL);
INSERT INTO `tenant_role_user_private` (`id`, `tenant_id`, `role_id`, `platform_role`, `user_id`, `dept_id`) VALUES (1221838050204471297, 1221838049504022529, 2, b'1', 1221837466541903874, NULL);
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
INSERT INTO `tenant_user_dept_private` (`id`, `tenant_id`, `user_id`, `dept_id`) VALUES (1221854021522706434, 1, 1221851150714556417, 1171517787697836033);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
