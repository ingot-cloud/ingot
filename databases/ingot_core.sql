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

 Date: 22/06/2026 17:28:29
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
) ENGINE=InnoDB AUTO_INCREMENT=134 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号安全事件表';

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
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (88, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.103', NULL, NULL, NULL, '2026-04-25 13:18:10');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (89, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.103', NULL, NULL, NULL, '2026-04-25 15:39:25');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (90, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.124', NULL, NULL, NULL, '2026-04-28 17:05:36');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (91, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.124', NULL, NULL, NULL, '2026-04-29 09:22:41');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (92, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.124', NULL, NULL, NULL, '2026-04-29 16:18:15');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (93, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.124', NULL, NULL, NULL, '2026-04-30 09:09:23');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (94, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.124', NULL, NULL, NULL, '2026-04-30 13:44:38');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (95, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.124', NULL, NULL, NULL, '2026-04-30 16:23:18');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (96, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.101', NULL, NULL, NULL, '2026-05-06 15:28:38');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (97, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.119', NULL, NULL, NULL, '2026-05-14 10:40:52');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (98, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.119', NULL, NULL, NULL, '2026-05-16 15:02:09');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (99, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.119', NULL, NULL, NULL, '2026-05-19 09:21:12');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (100, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.119', NULL, NULL, NULL, '2026-05-19 09:21:29');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (101, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.107', NULL, NULL, NULL, '2026-05-19 11:30:28');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (102, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.107', NULL, NULL, NULL, '2026-05-19 11:34:28');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (103, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.107', NULL, NULL, NULL, '2026-05-19 11:35:34');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (104, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.107', NULL, NULL, NULL, '2026-05-19 11:36:35');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (105, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.107', NULL, NULL, NULL, '2026-05-19 11:41:41');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (106, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.107', NULL, NULL, NULL, '2026-05-19 11:48:44');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (107, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.139', NULL, NULL, NULL, '2026-06-09 13:25:03');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (108, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.139', NULL, NULL, NULL, '2026-06-12 15:41:42');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (109, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.139', NULL, NULL, NULL, '2026-06-15 09:52:20');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (110, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.139', NULL, NULL, NULL, '2026-06-15 11:54:39');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (111, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.139', NULL, NULL, NULL, '2026-06-15 14:32:46');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (112, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.139', NULL, NULL, NULL, '2026-06-15 16:33:43');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (113, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.139', NULL, NULL, NULL, '2026-06-16 08:28:00');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (114, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.139', NULL, NULL, NULL, '2026-06-16 11:14:02');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (115, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.139', NULL, NULL, NULL, '2026-06-16 14:39:34');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (116, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.139', NULL, NULL, NULL, '2026-06-16 16:40:22');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (117, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.139', NULL, NULL, NULL, '2026-06-18 11:13:40');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (118, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.139', NULL, NULL, NULL, '2026-06-18 13:20:14');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (119, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.139', NULL, NULL, NULL, '2026-06-18 15:23:39');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (120, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.139', NULL, NULL, NULL, '2026-06-18 17:25:46');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (121, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.31.117', NULL, NULL, NULL, '2026-06-20 14:26:47');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (122, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.31.117', NULL, NULL, NULL, '2026-06-20 16:33:28');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (123, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.31.117', NULL, NULL, NULL, '2026-06-20 18:35:57');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (124, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.31.117', NULL, NULL, NULL, '2026-06-21 15:11:05');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (125, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.31.117', NULL, NULL, NULL, '2026-06-21 17:20:09');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (126, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.31.117', NULL, NULL, NULL, '2026-06-21 17:24:12');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (127, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.31.117', NULL, NULL, NULL, '2026-06-21 18:15:06');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (128, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.31.117', NULL, NULL, NULL, '2026-06-21 18:43:02');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (129, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.31.117', NULL, NULL, NULL, '2026-06-21 18:44:23');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (130, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-06-22 08:33:22');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (131, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-06-22 10:33:33');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (132, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-06-22 13:37:59');
INSERT INTO `account_security_event` (`id`, `user_id`, `user_type`, `event_type`, `event_category`, `reason_code`, `reason_detail`, `result`, `source`, `operator_id`, `operator_name`, `client_ip`, `user_agent`, `tenant_id`, `extra_data`, `created_at`) VALUES (133, 1, 'ADMIN', 'LOGIN_SUCCESS', 'AUTH', NULL, NULL, 'SUCCESS', 'AUTH', NULL, NULL, '192.168.1.100', NULL, NULL, NULL, '2026-06-22 15:39:18');
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
  `force_change` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否强制修改（0-否 1-是）',
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
  `code` varchar(64) DEFAULT NULL COMMENT '应用编码',
  `app_type` char(1) DEFAULT NULL COMMENT '应用类型,0:平台,1:租户',
  `menu_id` bigint NOT NULL DEFAULT '0' COMMENT '菜单ID',
  `permission_id` bigint NOT NULL DEFAULT '0' COMMENT '权限ID',
  `name` varchar(32) NOT NULL COMMENT '应用名称',
  `icon` varchar(255) DEFAULT NULL COMMENT '应用图标',
  `intro` varchar(255) NOT NULL COMMENT '应用介绍',
  `sort` int DEFAULT '999' COMMENT '排序',
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
INSERT INTO `platform_app` (`id`, `code`, `app_type`, `menu_id`, `permission_id`, `name`, `icon`, `intro`, `sort`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246850914065547265, 'platform:config', '0', 0, 1246850914036187138, '平台配置', 'mynaui:config', '平台配置应用', 10, '0', '2026-06-21 15:26:24', '2026-06-21 15:26:24', NULL);
INSERT INTO `platform_app` (`id`, `code`, `app_type`, `menu_id`, `permission_id`, `name`, `icon`, `intro`, `sort`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246912981145452545, 'platform:admin', '0', 0, 1246912981111898114, '管理身份', 'eos-icons:admin', '平台管理员', 20, '0', '2026-06-21 19:33:02', '2026-06-21 19:33:02', NULL);
INSERT INTO `platform_app` (`id`, `code`, `app_type`, `menu_id`, `permission_id`, `name`, `icon`, `intro`, `sort`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246915131086962690, 'platform:member', '0', 0, 1246915131061796866, '用户身份', 'carbon:app', 'App用户管理', 30, '0', '2026-06-21 19:41:35', '2026-06-21 19:41:35', NULL);
INSERT INTO `platform_app` (`id`, `code`, `app_type`, `menu_id`, `permission_id`, `name`, `icon`, `intro`, `sort`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246918461083320321, 'platform:org', '0', 0, 1246918461028794369, '组织管理', 'clarity:organization-line', '组织管理', 40, '0', '2026-06-21 19:54:49', '2026-06-21 19:54:49', NULL);
INSERT INTO `platform_app` (`id`, `code`, `app_type`, `menu_id`, `permission_id`, `name`, `icon`, `intro`, `sort`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247135989409800193, 'platform:security', '1', 0, 1247135989300748290, '安全中心', 'ic:sharp-security', '安全中心', 100, '0', '2026-06-22 10:19:12', '2026-06-22 10:19:12', '2026-06-22 02:21:57');
INSERT INTO `platform_app` (`id`, `code`, `app_type`, `menu_id`, `permission_id`, `name`, `icon`, `intro`, `sort`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247136775485284354, 'platform:security', '0', 0, 1247136775460118530, '安全中心', 'ic:sharp-security', '安全中心', 100, '0', '2026-06-22 10:22:19', '2026-06-22 10:22:19', NULL);
INSERT INTO `platform_app` (`id`, `code`, `app_type`, `menu_id`, `permission_id`, `name`, `icon`, `intro`, `sort`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247137699024887810, 'platform:develop', '0', 0, 1247137698999721985, '开发者平台', 'material-symbols:developer-mode-tv-outline', '开发者平台', 110, '0', '2026-06-22 10:25:59', '2026-06-22 10:25:59', NULL);
INSERT INTO `platform_app` (`id`, `code`, `app_type`, `menu_id`, `permission_id`, `name`, `icon`, `intro`, `sort`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247138809458483202, 'org:contacts', '1', 0, 1247138809424928770, '通讯录', 'typcn:contacts', '通讯录', 10, '0', '2026-06-22 10:30:24', '2026-06-22 10:30:24', NULL);
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
  `value` varchar(128) DEFAULT NULL COMMENT '字典项值（仅字典项有效）',
  `label` varchar(128) DEFAULT NULL COMMENT '字典项展示文本（仅字典项有效）',
  `type` char(1) NOT NULL COMMENT '字典类型',
  `scope_type` char(1) NOT NULL DEFAULT '0' COMMENT '作用域, 0:平台,1:租户,2:应用',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID（scope_type=1时必填）',
  `app_id` bigint DEFAULT NULL COMMENT '应用ID（scope_type=2时必填）',
  `org_type` char(1) NOT NULL DEFAULT '0' COMMENT '组织类型',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序权重',
  `system_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否内置字典',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `extra` json DEFAULT NULL COMMENT '扩展属性',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`),
  KEY `idx_dict_pid` (`pid`) USING BTREE,
  KEY `idx_dict_code` (`code`) USING BTREE,
  KEY `idx_dict_type_status` (`type`,`status`) USING BTREE,
  KEY `idx_dict_scope` (`scope_type`,`tenant_id`,`app_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of platform_dict
-- ----------------------------
BEGIN;
INSERT INTO `platform_dict` (`id`, `pid`, `code`, `name`, `value`, `label`, `type`, `scope_type`, `tenant_id`, `app_id`, `org_type`, `sort`, `system_flag`, `status`, `remark`, `extra`, `created_by`, `updated_by`, `created_at`, `updated_at`, `deleted_at`) VALUES (1227552905319469058, 0, 'sys', '系统配置', NULL, NULL, '0', '0', NULL, NULL, '0', 0, b'0', '0', NULL, NULL, NULL, NULL, '2026-04-29 09:23:01', '2026-04-29 16:32:02', NULL);
COMMIT;

-- ----------------------------
-- Table structure for platform_menu
-- ----------------------------
DROP TABLE IF EXISTS `platform_menu`;
CREATE TABLE `platform_menu` (
  `id` bigint unsigned NOT NULL COMMENT 'ID',
  `pid` bigint NOT NULL DEFAULT '0' COMMENT '父ID',
  `app_id` bigint DEFAULT NULL COMMENT '所属应用',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单名称',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单类型',
  `path` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单url',
  `access_mode` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '访问模式,0:开放,1:权限',
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
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_menu_app_pid_status_sort` (`app_id`,`pid`,`status`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of platform_menu
-- ----------------------------
BEGIN;
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246864778740072450, 0, 1246850914065547265, '平台配置', '0', '/platform/config', '1', 1246850914036187138, 1, '@/layouts/InAppLayout.vue', NULL, '/platform/config/app/home', 'mynaui:config', 10, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-21 16:21:30', '2026-06-21 16:21:30', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246865537795850242, 1246864778740072450, 1246850914065547265, '应用管理', '1', '/platform/config/app/home', '1', 1246865537716158465, 0, '@/pages/platform/config/app/home/IndexPage.vue', NULL, NULL, NULL, 10, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-21 16:24:31', '2026-06-21 16:24:31', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246865700434182146, 1246864778740072450, 1246850914065547265, '应用详情', '1', '/platform/config/app/detail/:appId', '1', 1246865700333518849, 0, '@/pages/platform/config/app/detail/IndexPage.vue', NULL, NULL, NULL, 20, 0, 1, 0, 1, '0', '0', NULL, '0', '2026-06-21 16:25:10', '2026-06-22 14:15:41', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246865931452252161, 1246864778740072450, 1246850914065547265, '角色管理', '1', '/platform/config/role', '1', 1246865931389337602, 0, '@/pages/platform/config/role/IndexPage.vue', NULL, NULL, NULL, 20, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-21 16:26:05', '2026-06-21 19:19:53', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246895217429151746, 1246864778740072450, 1246850914065547265, '菜单管理', '1', '/platform/config/menu', '1', 1246895217303322625, 0, '@/pages/platform/config/menu/IndexPage.vue', NULL, NULL, NULL, 30, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-21 18:22:27', '2026-06-21 18:22:38', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246895376481353730, 1246864778740072450, 1246850914065547265, '权限管理', '1', '/platform/config/permission', '1', 1246895376431022081, 0, '@/pages/platform/config/permission/IndexPage.vue', NULL, NULL, NULL, 40, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-21 18:23:05', '2026-06-21 18:23:05', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246908429054738433, 1246864778740072450, 1246850914065547265, '字典管理', '1', '/platform/config/dict', '1', 1246908428983435266, 0, '@/pages/platform/config/dict/IndexPage.vue', NULL, NULL, NULL, 50, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-21 19:14:57', '2026-06-21 19:15:08', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246913102880931841, 0, 1246912981145452545, '管理身份', '0', '/platform/admin', '1', 1246912981111898114, 1, '@/layouts/InAppLayout.vue', NULL, '/platform/admin/user', 'eos-icons:admin', 10, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-21 19:33:31', '2026-06-21 19:33:31', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246913272867684353, 1246913102880931841, 1246912981145452545, '用户管理', '1', '/platform/admin/user', '1', 1246913272758632449, 0, '@/pages/platform/admin/user/IndexPage.vue', NULL, NULL, NULL, 10, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-21 19:34:12', '2026-06-21 19:34:12', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246915250167447553, 0, 1246915131086962690, '用户身份', '0', '/platform/member', '1', 1246915131061796866, 1, '@/layouts/InAppLayout.vue', NULL, '/platform/member/user', 'carbon:app', 10, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-21 19:42:03', '2026-06-21 19:42:03', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246915377313579010, 1246915250167447553, 1246915131086962690, '用户管理', '1', '/platform/member/user', '1', 1246915377242275841, 0, '@/pages/platform/member/user/IndexPage.vue', NULL, NULL, NULL, 10, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-21 19:42:34', '2026-06-21 19:42:34', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246915435408883713, 1246915250167447553, 1246915131086962690, '角色管理', '1', '/platform/member/role', '1', 1246915435329191938, 0, '@/pages/platform/member/role/IndexPage.vue', NULL, NULL, NULL, 20, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-21 19:42:47', '2026-06-21 19:42:47', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246918584295194625, 0, 1246918461083320321, '组织管理', '0', '/platform/org', '1', 1246918461028794369, 1, '@/layouts/InAppLayout.vue', NULL, '/platform/org/tenant', 'clarity:organization-line', 10, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-21 19:55:18', '2026-06-21 19:55:18', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246918643413909506, 1246918584295194625, 1246918461083320321, '组织管理', '1', '/platform/org/tenant', '1', 1246918643338412034, 0, '@/pages/platform/org/tenant/IndexPage.vue', NULL, NULL, NULL, 10, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-21 19:55:32', '2026-06-21 19:55:32', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247136877864050690, 0, 1247136775485284354, '安全中心', '0', '/platform/security', '1', 1247136775460118530, 1, '@/layouts/InAppLayout.vue', NULL, '/platform/security/credential', 'ic:sharp-security', 10, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-22 10:22:43', '2026-06-22 10:22:43', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247136994348261378, 1247136877864050690, 1247136775485284354, '凭证策略', '1', '/platform/security/credential', '1', 1247136994251792386, 0, '@/pages/platform/security/credential/IndexPage.vue', NULL, NULL, NULL, 10, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-22 10:23:11', '2026-06-22 10:23:11', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247137066855194626, 1247136877864050690, 1247136775485284354, '在线用户', '1', '/platform/security/onlinetoken', '1', 1247137066804862978, 0, '@/pages/platform/security/onlinetoken/IndexPage.vue', NULL, NULL, NULL, 10, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-22 10:23:29', '2026-06-22 10:23:29', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247137808659800065, 0, 1247137699024887810, '开发者平台', '0', '/platform/develop', '1', 1247137698999721985, 1, '@/layouts/InAppLayout.vue', NULL, '/platform/develop/qrcode', 'material-symbols:developer-mode-tv-outline', 10, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-22 10:26:25', '2026-06-22 10:26:25', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247137885637861378, 1247137808659800065, 1247137699024887810, '生成二维码', '1', '/platform/develop/qrcode', '1', 1247137885583335425, 0, '@/pages/platform/develop/qrcode/IndexPage.vue', NULL, NULL, NULL, 10, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-22 10:26:44', '2026-06-22 10:26:44', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247137958216097793, 1247137808659800065, 1247137699024887810, '客户端管理', '1', '/platform/develop/client', '1', 1247137958161571842, 0, '@/pages/platform/develop/client/IndexPage.vue', NULL, NULL, NULL, 20, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-22 10:27:01', '2026-06-22 10:27:01', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247138012599443457, 1247137808659800065, 1247137699024887810, '社交管理', '1', '/platform/develop/social', '1', 1247138012544917505, 0, '@/pages/platform/develop/social/IndexPage.vue', NULL, NULL, NULL, 30, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-22 10:27:14', '2026-06-22 10:27:14', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247138076856180738, 1247137808659800065, 1247137699024887810, '业务ID管理', '1', '/platform/develop/id', '1', 1247138076789071873, 0, '@/pages/platform/develop/id/IndexPage.vue', NULL, NULL, NULL, 40, 0, 0, 0, 0, '0', '0', NULL, '0', '2026-06-22 10:27:29', '2026-06-22 10:27:29', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247138906179133441, 0, 1247138809458483202, '通讯录', '0', '/org/contacts', '1', 1247138809424928770, 1, '@/layouts/InAppLayout.vue', NULL, '/org/contacts/user', 'typcn:contacts', 10, 0, 0, 0, 0, '1', '0', NULL, '0', '2026-06-22 10:30:47', '2026-06-22 10:30:47', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247138952148705281, 1247138906179133441, 1247138809458483202, '成员管理', '1', '/org/contacts/user', '1', 1247138952089985025, 0, '@/pages/org/contacts/user/IndexPage.vue', NULL, NULL, NULL, 10, 0, 0, 0, 0, '1', '0', NULL, '0', '2026-06-22 10:30:58', '2026-06-22 10:30:58', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247138998588039169, 1247138906179133441, 1247138809458483202, '部门管理', '1', '/org/contacts/dept', '1', 1247138998533513218, 0, '@/pages/org/contacts/dept/IndexPage.vue', NULL, NULL, NULL, 20, 0, 0, 0, 0, '1', '0', NULL, '0', '2026-06-22 10:31:09', '2026-06-22 10:31:09', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247139048244404225, 1247138906179133441, 1247138809458483202, '角色管理', '1', '/org/contacts/role', '1', 1247139048185683970, 0, '@/pages/org/contacts/role/IndexPage.vue', NULL, NULL, NULL, 30, 0, 0, 0, 0, '1', '0', NULL, '0', '2026-06-22 10:31:21', '2026-06-22 10:31:21', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247139193258270721, 1247138906179133441, 1247138809458483202, '权限管理', '1', '/org/contacts/auth', '1', 1247139193186967553, 0, '@/pages/org/contacts/auth/IndexPage.vue', NULL, NULL, NULL, 40, 0, 0, 0, 0, '1', '0', NULL, '0', '2026-06-22 10:31:56', '2026-06-22 10:31:56', NULL);
INSERT INTO `platform_menu` (`id`, `pid`, `app_id`, `name`, `menu_type`, `path`, `access_mode`, `permission_id`, `custom_view_path`, `view_path`, `route_name`, `redirect`, `icon`, `sort`, `is_cache`, `hidden`, `hide_breadcrumb`, `props`, `org_type`, `link_type`, `link_url`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247139242579091457, 1247138906179133441, 1247138809458483202, '组织架构', '1', '/org/contacts/structure', '1', 1247139242520371202, 0, '@/pages/org/contacts/structure/IndexPage.vue', NULL, NULL, NULL, 50, 0, 0, 0, 0, '1', '0', NULL, '0', '2026-06-22 10:32:07', '2026-06-22 10:32:07', NULL);
COMMIT;

-- ----------------------------
-- Table structure for platform_permission
-- ----------------------------
DROP TABLE IF EXISTS `platform_permission`;
CREATE TABLE `platform_permission` (
  `id` bigint unsigned NOT NULL COMMENT 'ID',
  `app_id` bigint DEFAULT NULL COMMENT '所属应用',
  `pid` bigint unsigned NOT NULL DEFAULT '0' COMMENT '父ID',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限名称',
  `code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限编码',
  `type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型',
  `node_type` char(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '节点类型,0:分组,1:导航,2:操作',
  `source_type` char(1) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '来源类型,0:系统,1:菜单,2:手工',
  `source_id` bigint DEFAULT NULL COMMENT '来源资源ID',
  `managed` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否托管',
  `org_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '组织类型',
  `status` char(1) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '0' COMMENT '状态, 0:正常，9:禁用',
  `remark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '' COMMENT '备注',
  `created_at` datetime DEFAULT NULL COMMENT '创建日期',
  `updated_at` datetime DEFAULT NULL COMMENT '更新日期',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除日期',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_code` (`code`) USING BTREE COMMENT '编码',
  KEY `idx_permission_app_pid_status` (`app_id`,`pid`,`status`),
  KEY `idx_permission_source` (`source_type`,`source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of platform_permission
-- ----------------------------
BEGIN;
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246850914036187138, 1246850914065547265, 0, '平台配置', 'platform:config:**', '1', '0', '0', NULL, b'0', '0', '0', '', '2026-06-21 15:26:24', '2026-06-21 16:16:24', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246865537716158465, 1246850914065547265, 1246850914036187138, '应用管理', 'platform:config:app:home', '0', '1', '1', 1246865537795850242, b'1', '0', '0', '', '2026-06-21 16:24:31', '2026-06-21 16:24:31', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246865700333518849, 1246850914065547265, 1246850914036187138, '应用详情', 'platform:config:app:detail', '0', '1', '1', 1246865700434182146, b'1', '0', '0', '', '2026-06-21 16:25:10', '2026-06-21 16:25:10', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246865931389337602, 1246850914065547265, 1246850914036187138, '角色管理', 'platform:config:role', '0', '1', '1', 1246865931452252161, b'1', '0', '0', '', '2026-06-21 16:26:05', '2026-06-21 19:19:53', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246895217303322625, 1246850914065547265, 1246850914036187138, '菜单管理', 'platform:config:menu', '0', '1', '1', 1246895217429151746, b'1', '0', '0', '', '2026-06-21 18:22:27', '2026-06-21 18:22:27', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246895376431022081, 1246850914065547265, 1246850914036187138, '权限管理', 'platform:config:permission', '0', '1', '1', 1246895376481353730, b'1', '0', '0', '', '2026-06-21 18:23:05', '2026-06-21 18:23:05', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246908428983435266, 1246850914065547265, 1246850914036187138, '字典管理', 'platform:config:dict', '0', '1', '1', 1246908429054738433, b'1', '0', '0', '', '2026-06-21 19:14:57', '2026-06-21 19:14:57', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246912981111898114, 1246912981145452545, 0, '管理身份', 'platform:admin:**', '1', '0', '0', NULL, b'0', '0', '0', '', '2026-06-21 19:33:02', '2026-06-21 19:33:02', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246913272758632449, 1246912981145452545, 1246912981111898114, '用户管理', 'platform:admin:user', '0', '1', '1', 1246913272867684353, b'1', '0', '0', '', '2026-06-21 19:34:12', '2026-06-21 19:34:12', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246915131061796866, 1246915131086962690, 0, '用户身份', 'platform:member:**', '1', '0', '0', NULL, b'0', '0', '0', '', '2026-06-21 19:41:35', '2026-06-21 19:41:35', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246915377242275841, 1246915131086962690, 1246915131061796866, '用户管理', 'platform:member:user', '0', '1', '1', 1246915377313579010, b'1', '0', '0', '', '2026-06-21 19:42:34', '2026-06-21 19:42:34', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246915435329191938, 1246915131086962690, 1246915131061796866, '角色管理', 'platform:member:role', '0', '1', '1', 1246915435408883713, b'1', '0', '0', '', '2026-06-21 19:42:47', '2026-06-21 19:42:48', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246918461028794369, 1246918461083320321, 0, '组织管理', 'platform:org:**', '1', '0', '0', NULL, b'0', '0', '0', '', '2026-06-21 19:54:49', '2026-06-21 19:54:49', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1246918643338412034, 1246918461083320321, 1246918461028794369, '组织管理', 'platform:org:tenant', '0', '1', '1', 1246918643413909506, b'1', '0', '0', '', '2026-06-21 19:55:32', '2026-06-21 19:55:32', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247136775460118530, 1247136775485284354, 0, '安全中心', 'platform:security:**', '1', '0', '0', NULL, b'0', '0', '0', '', '2026-06-22 10:22:19', '2026-06-22 10:22:19', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247136994251792386, 1247136775485284354, 1247136775460118530, '凭证策略', 'platform:security:credential', '0', '1', '1', 1247136994348261378, b'1', '0', '0', '', '2026-06-22 10:23:11', '2026-06-22 10:23:11', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247137066804862978, 1247136775485284354, 1247136775460118530, '在线用户', 'platform:security:onlinetoken', '0', '1', '1', 1247137066855194626, b'1', '0', '0', '', '2026-06-22 10:23:29', '2026-06-22 10:23:29', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247137698999721985, 1247137699024887810, 0, '开发者平台', 'platform:develop:**', '1', '0', '0', NULL, b'0', '0', '0', '', '2026-06-22 10:25:59', '2026-06-22 10:25:59', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247137885583335425, 1247137699024887810, 1247137698999721985, '生成二维码', 'platform:develop:qrcode', '0', '1', '1', 1247137885637861378, b'1', '0', '0', '', '2026-06-22 10:26:44', '2026-06-22 10:26:44', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247137958161571842, 1247137699024887810, 1247137698999721985, '客户端管理', 'platform:develop:client', '0', '1', '1', 1247137958216097793, b'1', '0', '0', '', '2026-06-22 10:27:01', '2026-06-22 10:27:01', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247138012544917505, 1247137699024887810, 1247137698999721985, '社交管理', 'platform:develop:social', '0', '1', '1', 1247138012599443457, b'1', '0', '0', '', '2026-06-22 10:27:14', '2026-06-22 10:27:14', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247138076789071873, 1247137699024887810, 1247137698999721985, '业务ID管理', 'platform:develop:id', '0', '1', '1', 1247138076856180738, b'1', '0', '0', '', '2026-06-22 10:27:29', '2026-06-22 10:27:29', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247138809424928770, 1247138809458483202, 0, '通讯录', 'org:contacts:**', '1', '0', '0', NULL, b'0', '1', '0', '', '2026-06-22 10:30:24', '2026-06-22 10:30:24', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247138952089985025, 1247138809458483202, 1247138809424928770, '成员管理', 'org:contacts:user', '0', '1', '1', 1247138952148705281, b'1', '1', '0', '', '2026-06-22 10:30:58', '2026-06-22 10:30:58', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247138998533513218, 1247138809458483202, 1247138809424928770, '部门管理', 'org:contacts:dept', '0', '1', '1', 1247138998588039169, b'1', '1', '0', '', '2026-06-22 10:31:09', '2026-06-22 10:31:09', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247139048185683970, 1247138809458483202, 1247138809424928770, '角色管理', 'org:contacts:role', '0', '1', '1', 1247139048244404225, b'1', '1', '0', '', '2026-06-22 10:31:21', '2026-06-22 10:31:21', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247139193186967553, 1247138809458483202, 1247138809424928770, '权限管理', 'org:contacts:auth', '0', '1', '1', 1247139193258270721, b'1', '1', '0', '', '2026-06-22 10:31:55', '2026-06-22 10:31:56', NULL);
INSERT INTO `platform_permission` (`id`, `app_id`, `pid`, `name`, `code`, `type`, `node_type`, `source_type`, `source_id`, `managed`, `org_type`, `status`, `remark`, `created_at`, `updated_at`, `deleted_at`) VALUES (1247139242520371202, 1247138809458483202, 1247138809424928770, '组织架构', 'org:contacts:structure', '0', '1', '1', 1247139242579091457, b'1', '1', '0', '', '2026-06-22 10:32:07', '2026-06-22 10:32:07', NULL);
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
INSERT INTO `platform_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1247138256640827394, 1, 1246850914036187138);
INSERT INTO `platform_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1247138256649216002, 1, 1246912981111898114);
INSERT INTO `platform_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1247138256649216003, 1, 1246915131061796866);
INSERT INTO `platform_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1247138256649216004, 1, 1246918461028794369);
INSERT INTO `platform_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1247138256649216005, 1, 1247136775460118530);
INSERT INTO `platform_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1247138256649216006, 1, 1247137698999721985);
INSERT INTO `platform_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1247138809525592065, 2, 1247138809424928770);
INSERT INTO `platform_role_permission` (`id`, `role_id`, `permission_id`) VALUES (1247213590409142274, 3, 1247139242520371202);
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
INSERT INTO `sys_social_details` (`id`, `tenant_id`, `app_id`, `app_secret`, `redirect_url`, `name`, `type`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (925397365717008385, 1, 'wx123123123', '1231231231', NULL, 'ingot开源小程序', 'wechat_miniprogram', '0', '2024-01-16 14:24:46', '2026-05-14 11:56:47', NULL);
INSERT INTO `sys_social_details` (`id`, `tenant_id`, `app_id`, `app_secret`, `redirect_url`, `name`, `type`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1233008419032526850, 1, 'wx111', '123123', NULL, 'test', 'wechat_miniprogram', '0', '2026-05-14 10:41:16', '2026-05-14 10:41:16', '2026-05-14 02:41:34');
INSERT INTO `sys_social_details` (`id`, `tenant_id`, `app_id`, `app_secret`, `redirect_url`, `name`, `type`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES (1233027555393531905, 1, '1', '1', NULL, '1', 'wechat_miniprogram', '0', '2026-05-14 11:57:19', '2026-05-14 11:57:19', '2026-05-14 03:57:45');
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
INSERT INTO `sys_user` (`id`, `username`, `password`, `must_change_pwd`, `password_changed_at`, `nickname`, `phone`, `email`, `avatar`, `enabled`, `locked`, `last_login_at`, `last_login_ip`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1, 'admin', '{bcrypt}$2a$10$frVeEWrN5PRLZTheYoMw1uHNiylDcS3uvByYiYACJIvCkAMaoEfr2', 0, '2026-03-10 16:40:30', '超级管理员', '18888888888', 'admin@ingot.com', 'ingot/user/avatar/1/logo.png', 1, 0, '2026-06-22 15:39:18', '192.168.1.100', 0, '2021-01-03 11:02:46', '2026-05-19 09:22:01', NULL);
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
  `source` varchar(32) DEFAULT NULL COMMENT '授权来源',
  `valid_from` datetime DEFAULT NULL COMMENT '生效时间',
  `valid_until` datetime DEFAULT NULL COMMENT '失效时间',
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
INSERT INTO `tenant_role_permission_private` (`id`, `tenant_id`, `role_id`, `platform_role`, `permission_id`) VALUES (1247232449467932674, 1, 3, b'1', 1247139193186967553);
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
INSERT INTO `tenant_user_dept_private` (`id`, `tenant_id`, `user_id`, `dept_id`) VALUES (1234800411538354178, 1, 1, 1171517787697836033);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
