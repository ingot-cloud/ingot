/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : localhost:3306
 Source Schema         : ingot_security

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 07/02/2026 15:26:15
*/

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ingot_security
CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;

USE ingot_security;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for credential_policy_config
-- ----------------------------
DROP TABLE IF EXISTS `credential_policy_config`;
CREATE TABLE `credential_policy_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `policy_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '策略类型: STRENGTH(强度), EXPIRATION(过期), HISTORY(历史)3',
  `policy_config` json NOT NULL COMMENT '策略配置JSON',
  `priority` int DEFAULT '0' COMMENT '优先级，数字越小优先级越高',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `active_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci GENERATED ALWAYS AS ((case when (`enabled` = 1) then `policy_type` else NULL end)) STORED COMMENT '激活类型',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_active_type` (`active_type`),
  KEY `idx_type_enabled` (`policy_type`,`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=2020031712521412611 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='凭证策略配置表';

-- ----------------------------
-- Records of credential_policy_config
-- ----------------------------
BEGIN;
INSERT INTO `credential_policy_config` (`id`, `policy_type`, `policy_config`, `priority`, `enabled`, `created_at`, `updated_at`) VALUES (1, '1', '{\"maxLength\": 32, \"minLength\": \"8\", \"requireDigit\": true, \"specialChars\": \"!@#$%^&*()_+-=[]{}|;:,.<>?\", \"requireLowercase\": true, \"requireUppercase\": true, \"forbiddenPatterns\": [\"password\", \"123456\", \"admin\", \"qwerty\", \"abc123\"], \"requireSpecialChar\": false, \"forbidUserAttributes\": true}', 10, 1, '2026-01-23 07:28:02', '2026-02-02 08:09:57');
INSERT INTO `credential_policy_config` (`id`, `policy_type`, `policy_config`, `priority`, `enabled`, `created_at`, `updated_at`) VALUES (2, '3', '{\"enabled\": true, \"maxDays\": 90, \"graceLoginCount\": 3, \"warningDaysBefore\": \"7\", \"forceChangeAfterReset\": true}', 20, 1, '2026-01-23 07:28:02', '2026-02-02 08:10:03');
INSERT INTO `credential_policy_config` (`id`, `policy_type`, `policy_config`, `priority`, `enabled`, `created_at`, `updated_at`) VALUES (3, '2', '{\"enabled\": true, \"checkCount\": 5, \"keepRecentCount\": 5}', 30, 1, '2026-01-23 07:28:02', '2026-02-02 08:10:10');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
