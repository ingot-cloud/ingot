/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80044 (8.0.44)
 Source Host           : localhost:3306
 Source Schema         : ingot_iam

 Target Server Type    : MySQL
 Target Server Version : 80044 (8.0.44)
 File Encoding         : 65001

 Date: 20/09/2026 13:47:06
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
  `user_type` varchar(20) NOT NULL DEFAULT '0' COMMENT '用户类型（同 UserTypeEnum.value：0-系统用户 1-C端用户）',
  `locked` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否锁定（0-否 1-是）',
  `lock_type` varchar(20) DEFAULT NULL COMMENT '锁定类型（MANUAL-手动 AUTO-自动）',
  `lock_reason_code` varchar(50) DEFAULT NULL COMMENT '锁定原因代码',
  `lock_reason_detail` varchar(500) DEFAULT NULL COMMENT '锁定原因详情',
  `locked_at` datetime DEFAULT NULL COMMENT '锁定时间',
  `locked_until` datetime DEFAULT NULL COMMENT '锁定到期时间（NULL=永久锁定）',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人姓名',
  `failed_login_count` int NOT NULL DEFAULT '0' COMMENT '连续登录失败次数',
  `last_failed_at` datetime DEFAULT NULL COMMENT '最后一次失败时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id_type` (`user_id`,`user_type`) COMMENT '用户ID + 用户类型联合唯一',
  KEY `idx_locked` (`locked`,`locked_until`) USING BTREE COMMENT '锁定状态 + 到期时间索引（自动解锁任务使用）'
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号锁定状态表';

-- ----------------------------
-- Records of account_lock_state
-- ----------------------------
BEGIN;
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (1, 900001, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-09-16 13:50:11', '2026-09-20 13:39:00');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (2, 900002, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-09-16 13:50:28', '2026-09-18 14:43:42');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (7, 1000022, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 3, '2026-09-16 15:34:16', '2026-09-16 15:30:56', '2026-09-16 15:34:16');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (11, 1000032, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-09-16 15:34:38', '2026-09-16 16:10:44');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (12, 1000063, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-09-16 16:11:41', '2026-09-16 16:12:11');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (18, 1000200, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-09-20 13:39:01', '2026-09-20 13:39:01');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (19, 1000203, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-09-20 13:39:03', '2026-09-20 13:39:30');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (20, 1000206, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-09-20 13:39:04', '2026-09-20 13:39:31');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (21, 1000209, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-09-20 13:39:06', '2026-09-20 13:39:06');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (22, 1000212, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-09-20 13:39:08', '2026-09-20 13:39:08');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (23, 1000215, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-09-20 13:39:10', '2026-09-20 13:39:10');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (24, 1000218, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-09-20 13:39:12', '2026-09-20 13:39:12');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (25, 1000221, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-09-20 13:39:14', '2026-09-20 13:39:14');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (26, 1000224, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-09-20 13:39:16', '2026-09-20 13:39:16');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (27, 1000227, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-09-20 13:39:18', '2026-09-20 13:39:18');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (28, 1000230, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-09-20 13:39:19', '2026-09-20 13:39:19');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (29, 1000233, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-09-20 13:39:21', '2026-09-20 13:39:21');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (30, 1000236, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-09-20 13:39:23', '2026-09-20 13:39:23');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (31, 1000239, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-09-20 13:39:25', '2026-09-20 13:39:25');
INSERT INTO `account_lock_state` (`id`, `user_id`, `user_type`, `locked`, `lock_type`, `lock_reason_code`, `lock_reason_detail`, `locked_at`, `locked_until`, `operator_id`, `operator_name`, `failed_login_count`, `last_failed_at`, `created_at`, `updated_at`) VALUES (32, 1000242, '0', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, '2026-09-20 13:39:27', '2026-09-20 13:39:27');
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
INSERT INTO `biz_leaf_alloc` (`biz_tag`, `max_id`, `step`, `description`, `update_time`) VALUES ('iam', 1000400, 100, 'IAM 冷启动发号', '2026-09-20 05:39:07');
COMMIT;

-- ----------------------------
-- Table structure for iam_account
-- ----------------------------
DROP TABLE IF EXISTS `iam_account`;
CREATE TABLE `iam_account` (
  `id` bigint unsigned NOT NULL,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `password_hash` varchar(300) NOT NULL,
  `phone` varchar(32) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `must_change_password` tinyint(1) NOT NULL DEFAULT '0',
  `password_changed_at` datetime(6) DEFAULT NULL,
  `last_login_at` datetime(6) DEFAULT NULL,
  `version` bigint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `deleted_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_account_username` (`username`),
  KEY `idx_iam_account_phone` (`phone`),
  KEY `idx_iam_account_email` (`email`),
  CONSTRAINT `ck_iam_account_flags` CHECK (((`enabled` in (0,1)) and (`must_change_password` in (0,1)))),
  CONSTRAINT `ck_iam_account_id` CHECK ((`id` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_account
-- ----------------------------
BEGIN;
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (900001, 'platform', '{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '13800000001', NULL, 1, 0, NULL, '2026-09-20 13:39:00.009270', 0, '2026-09-16 02:35:42.234226', '2026-09-20 05:39:00.009463', NULL);
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (900002, 'owner', '{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '13800000002', NULL, 1, 0, NULL, '2026-09-18 14:43:42.211196', 0, '2026-09-16 02:35:42.234226', '2026-09-18 06:43:42.211374', NULL);
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000022, 'a01b-x-mu3ra4ey', '{bcrypt}$2a$10$wa3WeBiPxK5oNDIAjmx0ZO1LYhydPlBrZtexXmsymp2xS6TSDhLSK', NULL, NULL, 0, 1, '2026-09-16 15:31:22.469465', '2026-09-16 15:32:48.413816', 1, '2026-09-16 07:30:55.524758', '2026-09-16 07:34:02.215934', NULL);
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000032, 'a02-y-mu3ra4ey', '{bcrypt}$2a$10$a5Jy89vkCTnJommZ9hWvgudyQ3NwDbMymzYlZOndmKcuvSy/7.P5O', NULL, NULL, 1, 1, '2026-09-16 15:34:50.583591', '2026-09-16 16:10:43.835294', 1, '2026-09-16 07:34:37.539276', '2026-09-16 08:10:43.835357', NULL);
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000063, 'a26-z-mu3ra4ey', '{bcrypt}$2a$10$m0r874nwHdSk.sb7Lh6inubtTmJf3TuvmQJQYMiXBQqAh/pk7PIjS', NULL, NULL, 1, 1, '2026-09-16 16:11:44.065992', '2026-09-16 16:12:11.278566', 1, '2026-09-16 08:11:40.803949', '2026-09-16 08:12:11.278624', NULL);
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000200, 'iam-test-platform-reader', '{bcrypt}$2a$10$kx9c4v9toUNlfxtr9PIWruiXX34yF3NXE9wDRE7o/3BBcBPG0h4xS', NULL, NULL, 1, 1, '2026-09-20 13:39:02.001989', NULL, 1, '2026-09-20 05:39:00.701641', '2026-09-20 05:39:02.005070', NULL);
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000203, 'iam-test-owner-a', '{bcrypt}$2a$10$o49CF5S3vyh/HfJFd4bZmOazFMcDoKFVmOqVPVASrQefXLid4dh1K', NULL, NULL, 1, 1, '2026-09-20 13:39:03.871202', '2026-09-20 13:39:30.228156', 1, '2026-09-20 05:39:02.673403', '2026-09-20 05:39:30.228244', NULL);
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000206, 'iam-test-owner-b', '{bcrypt}$2a$10$IePc37Olic3S4WeEsrzzBexPJSoqQpKxvHGkrn4friEcj3CqMLmIi', NULL, NULL, 1, 1, '2026-09-20 13:39:05.634812', '2026-09-20 13:39:30.773368', 1, '2026-09-20 05:39:04.472113', '2026-09-20 05:39:30.773434', NULL);
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000209, 'iam-test-dual', '{bcrypt}$2a$10$7UwCD1D0atyIeYPRhVHHsO2zW29tseTUZWN4IVrl3sgIIQxwi79P2', NULL, NULL, 1, 1, '2026-09-20 13:39:07.424226', NULL, 1, '2026-09-20 05:39:06.253107', '2026-09-20 05:39:07.424426', NULL);
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000212, 'iam-test-reader-a', '{bcrypt}$2a$10$lbNkOehj9kzKjFITpvJu7.9KOKKYIOXIZXAikj7NqTGYGS802B2W2', NULL, NULL, 1, 1, '2026-09-20 13:39:09.261960', NULL, 1, '2026-09-20 05:39:08.059972', '2026-09-20 05:39:09.262153', NULL);
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000215, 'iam-test-editor-a', '{bcrypt}$2a$10$nbuSC4hlOYJv8SMkul/cNOOEg0QMRib/krJ/X27wSzsuaDqdTZslq', NULL, NULL, 1, 1, '2026-09-20 13:39:11.145721', NULL, 1, '2026-09-20 05:39:09.918133', '2026-09-20 05:39:11.145885', NULL);
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000218, 'iam-test-grantor-a', '{bcrypt}$2a$10$bJWm0GfuNEEK.aTu0v4OdO0xHhqHu7kIIWEOwOfF5wSNlLLM0fnRq', NULL, NULL, 1, 1, '2026-09-20 13:39:12.993750', NULL, 1, '2026-09-20 05:39:11.736554', '2026-09-20 05:39:12.993924', NULL);
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000221, 'iam-test-ordinary-a', '{bcrypt}$2a$10$SPhSiAbFieAjhs3OesaO8.3NDR/5doUy7vDHvc/VyIQFUmOh6qqYC', NULL, NULL, 1, 1, '2026-09-20 13:39:15.016782', NULL, 1, '2026-09-20 05:39:13.658623', '2026-09-20 05:39:15.016933', NULL);
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000224, 'iam-test-multi-dept-a', '{bcrypt}$2a$10$slCRsyOryTJm9rw9mQUUMecaNy91kHqOdKmvX6oOWmQP/OPtbWVL2', NULL, NULL, 1, 1, '2026-09-20 13:39:16.886412', NULL, 1, '2026-09-20 05:39:15.692825', '2026-09-20 05:39:16.886550', NULL);
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000227, 'iam-test-no-dept-a', '{bcrypt}$2a$10$ouiZKTb/GKL.ZwgDa8LKRu30KeVWfKsH8qObIuziWB/rLTJDqH8ia', NULL, NULL, 1, 1, '2026-09-20 13:39:18.712930', NULL, 1, '2026-09-20 05:39:17.497896', '2026-09-20 05:39:18.713100', NULL);
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000230, 'iam-test-no-access', '{bcrypt}$2a$10$mqCR8As5CvC2DFiw3FESuuN84S4jYS5QX3VGT7lHk4J.fBIz.PZgS', NULL, NULL, 1, 1, '2026-09-20 13:39:20.663566', NULL, 1, '2026-09-20 05:39:19.415401', '2026-09-20 05:39:20.663770', NULL);
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000233, 'iam-test-suspended', '{bcrypt}$2a$10$lbSkZAsqXpe3LjRZITESduU40O4.0Hfzl1qRgVKIufdP5mf9T8wKW', NULL, NULL, 1, 1, '2026-09-20 13:39:22.488121', NULL, 1, '2026-09-20 05:39:21.312490', '2026-09-20 05:39:22.488259', NULL);
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000236, 'iam-test-disabled', '{bcrypt}$2a$10$r2iBWIqLNp4JNPTiUfNna.3jTA3FEps33a.C2YzPgblDtOAd1BdQ2', NULL, NULL, 1, 1, '2026-09-20 13:39:24.324171', NULL, 1, '2026-09-20 05:39:23.107699', '2026-09-20 05:39:24.324344', NULL);
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000239, 'iam-test-owner-transfer-src', '{bcrypt}$2a$10$JPvHUf1cr1md1MJa5P976.Hv2.qOZLr.7OtXmsQPtCnVQb3vsRguS', NULL, NULL, 1, 1, '2026-09-20 13:39:26.212015', NULL, 1, '2026-09-20 05:39:24.962655', '2026-09-20 05:39:26.212198', NULL);
INSERT INTO `iam_account` (`id`, `username`, `password_hash`, `phone`, `email`, `enabled`, `must_change_password`, `password_changed_at`, `last_login_at`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000242, 'iam-test-owner-transfer-dst', '{bcrypt}$2a$10$GJXsGKwa7hE6tAPSfKjxK.8Doerr2.Of4geGnih4Nj2/U72Jyx/dm', NULL, NULL, 1, 1, '2026-09-20 13:39:28.127532', NULL, 1, '2026-09-20 05:39:26.892972', '2026-09-20 05:39:28.127651', NULL);
COMMIT;

-- ----------------------------
-- Table structure for iam_action
-- ----------------------------
DROP TABLE IF EXISTS `iam_action`;
CREATE TABLE `iam_action` (
  `id` bigint unsigned NOT NULL,
  `application_id` bigint unsigned NOT NULL,
  `resource_id` bigint unsigned NOT NULL,
  `code` varchar(192) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `name` varchar(128) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_action_code` (`code`),
  UNIQUE KEY `uk_iam_action_application` (`application_id`,`id`),
  KEY `idx_iam_action_resource` (`application_id`,`resource_id`,`id`),
  CONSTRAINT `fk_iam_action_resource` FOREIGN KEY (`application_id`, `resource_id`) REFERENCES `iam_resource` (`application_id`, `id`),
  CONSTRAINT `ck_iam_action_enabled` CHECK ((`enabled` in (0,1))),
  CONSTRAINT `ck_iam_action_exact` CHECK (((locate(_utf8mb4'*',`code`) = 0) and (char_length(`code`) > 0)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_action
-- ----------------------------
BEGIN;
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120001, 100001, 110001, 'iam-platform:account:create', '创建全局账号', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120002, 100001, 110001, 'iam-platform:account:delete', '删除全局账号', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120003, 100001, 110001, 'iam-platform:account:disable', '停用全局账号', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120004, 100001, 110001, 'iam-platform:account:enable', '启用全局账号', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120005, 100001, 110001, 'iam-platform:account:lock', '锁定全局账号', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120006, 100001, 110001, 'iam-platform:account:lookup', '查找全局账号', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120007, 100001, 110001, 'iam-platform:account:read', '查看全局账号', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120008, 100001, 110001, 'iam-platform:account:reset-password', '重置密码', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120009, 100001, 110001, 'iam-platform:account:unlock', '解锁全局账号', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120010, 100001, 110001, 'iam-platform:account:update', '编辑全局账号', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120011, 100001, 110002, 'iam-platform:action:create', '创建操作', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120012, 100001, 110002, 'iam-platform:action:delete', '删除操作', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120013, 100001, 110002, 'iam-platform:action:read', '查看操作', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120014, 100001, 110002, 'iam-platform:action:status', '启停操作', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120015, 100001, 110002, 'iam-platform:action:update', '编辑操作', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120016, 100001, 110003, 'iam-platform:application:create', '创建应用', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120017, 100001, 110003, 'iam-platform:application:delete', '删除应用', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120018, 100001, 110003, 'iam-platform:application:read', '查看应用', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120019, 100001, 110003, 'iam-platform:application:status', '启停应用', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120020, 100001, 110003, 'iam-platform:application:update', '编辑应用', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120021, 100001, 110004, 'iam-platform:assignment:create', '创建角色授权', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120022, 100001, 110004, 'iam-platform:assignment:delete', '删除角色授权', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120023, 100001, 110004, 'iam-platform:assignment:read', '查看角色授权', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120024, 100001, 110004, 'iam-platform:assignment:update', '编辑角色授权', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120025, 100001, 110005, 'iam-platform:audit:read', '查看审计记录', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120026, 100001, 110006, 'iam-platform:authorization:diagnose', '诊断', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120027, 100001, 110007, 'iam-platform:credential-policy:create', '创建凭证策略', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120028, 100001, 110007, 'iam-platform:credential-policy:delete', '删除凭证策略', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120029, 100001, 110007, 'iam-platform:credential-policy:read', '查看凭证策略', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120030, 100001, 110007, 'iam-platform:credential-policy:update', '编辑凭证策略', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120031, 100001, 110008, 'iam-platform:delegation:create', '创建授权委派', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120032, 100001, 110008, 'iam-platform:delegation:delete', '删除授权委派', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120033, 100001, 110008, 'iam-platform:delegation:preview', '预览授权委派', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120034, 100001, 110008, 'iam-platform:delegation:read', '查看授权委派', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120035, 100001, 110008, 'iam-platform:delegation:update', '编辑授权委派', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120036, 100001, 110009, 'iam-platform:dictionary:create', '创建字典', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120037, 100001, 110009, 'iam-platform:dictionary:delete', '删除字典', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120038, 100001, 110009, 'iam-platform:dictionary:read', '查看字典', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120039, 100001, 110009, 'iam-platform:dictionary:update', '编辑字典', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120040, 100001, 110010, 'iam-platform:entitlement:preview', '预览应用开通', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120041, 100001, 110010, 'iam-platform:entitlement:read', '查看应用开通', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120042, 100001, 110010, 'iam-platform:entitlement:update', '编辑应用开通', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120043, 100001, 110011, 'iam-platform:group:create', '创建用户组', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120044, 100001, 110011, 'iam-platform:group:delete', '删除用户组', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120045, 100001, 110011, 'iam-platform:group:preview', '预览用户组', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120046, 100001, 110011, 'iam-platform:group:read', '查看用户组', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120047, 100001, 110011, 'iam-platform:group:update', '编辑用户组', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120048, 100001, 110012, 'iam-platform:id-allocation:create', '创建发号', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120049, 100001, 110012, 'iam-platform:id-allocation:delete', '删除发号', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120050, 100001, 110012, 'iam-platform:id-allocation:read', '查看发号', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120051, 100001, 110012, 'iam-platform:id-allocation:update', '编辑发号', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120052, 100001, 110013, 'iam-platform:lockout-policy:read', '查看账号锁定策略', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120053, 100001, 110013, 'iam-platform:lockout-policy:update', '编辑账号锁定策略', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120054, 100001, 110014, 'iam-platform:login-failure-policy:read', '查看登录失败防护', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120055, 100001, 110014, 'iam-platform:login-failure-policy:update', '编辑登录失败防护', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120056, 100001, 110015, 'iam-platform:member:create', '创建成员', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120057, 100001, 110015, 'iam-platform:member:read', '查看成员', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120058, 100001, 110015, 'iam-platform:member:remove', '移出成员', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120059, 100001, 110015, 'iam-platform:member:status', '启停成员', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120060, 100001, 110015, 'iam-platform:member:update', '编辑成员', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120061, 100001, 110016, 'iam-platform:menu:create', '创建菜单', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120062, 100001, 110016, 'iam-platform:menu:delete', '删除菜单', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120063, 100001, 110016, 'iam-platform:menu:read', '查看菜单', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120064, 100001, 110016, 'iam-platform:menu:update', '编辑菜单', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120065, 100001, 110017, 'iam-platform:plan:create', '创建套餐', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120066, 100001, 110017, 'iam-platform:plan:read', '查看套餐', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120067, 100001, 110017, 'iam-platform:plan:update', '编辑套餐', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120068, 100001, 110018, 'iam-platform:resource:create', '创建资源', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120069, 100001, 110018, 'iam-platform:resource:delete', '删除资源', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120070, 100001, 110018, 'iam-platform:resource:read', '查看资源', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120071, 100001, 110018, 'iam-platform:resource:update', '编辑资源', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120072, 100001, 110019, 'iam-platform:role:create', '创建角色', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120073, 100001, 110019, 'iam-platform:role:delete', '删除角色', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120074, 100001, 110019, 'iam-platform:role:preview', '预览角色', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120075, 100001, 110019, 'iam-platform:role:publish', '发布版本角色', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120076, 100001, 110019, 'iam-platform:role:read', '查看角色', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120077, 100001, 110019, 'iam-platform:role:status', '启停角色', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120078, 100001, 110020, 'iam-platform:security-policy:create', '创建安全策略', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120079, 100001, 110020, 'iam-platform:security-policy:delete', '删除安全策略', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120080, 100001, 110020, 'iam-platform:security-policy:read', '查看安全策略', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120081, 100001, 110020, 'iam-platform:security-policy:update', '编辑安全策略', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120082, 100001, 110021, 'iam-platform:session:read', '查看在线会话', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120083, 100001, 110021, 'iam-platform:session:revoke', '撤销', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120084, 100001, 110022, 'iam-platform:session-policy:read', '查看会话并发策略', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120085, 100001, 110022, 'iam-platform:session-policy:update', '编辑会话并发策略', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120086, 100001, 110023, 'iam-platform:shared-role:create', '创建共享角色', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120087, 100001, 110023, 'iam-platform:shared-role:delete', '删除共享角色', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120088, 100001, 110023, 'iam-platform:shared-role:preview', '预览共享角色', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120089, 100001, 110023, 'iam-platform:shared-role:publish', '发布版本共享角色', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120090, 100001, 110023, 'iam-platform:shared-role:read', '查看共享角色', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120091, 100001, 110023, 'iam-platform:shared-role:status', '启停共享角色', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120092, 100001, 110024, 'iam-platform:social-config:create', '创建社会化登录配置', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120093, 100001, 110024, 'iam-platform:social-config:delete', '删除社会化登录配置', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120094, 100001, 110024, 'iam-platform:social-config:read', '查看社会化登录配置', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120095, 100001, 110024, 'iam-platform:social-config:update', '编辑社会化登录配置', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120096, 100001, 110025, 'iam-platform:tenant:create', '创建组织', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120097, 100001, 110025, 'iam-platform:tenant:preview', '预览组织', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120098, 100001, 110025, 'iam-platform:tenant:read', '查看组织', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120099, 100001, 110025, 'iam-platform:tenant:update', '编辑组织', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120100, 100002, 110026, 'iam-tenant:application:read', '查看应用', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120101, 100002, 110027, 'iam-tenant:assignment:create', '创建角色授权', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120102, 100002, 110027, 'iam-tenant:assignment:delete', '删除角色授权', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120103, 100002, 110027, 'iam-tenant:assignment:read', '查看角色授权', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120104, 100002, 110027, 'iam-tenant:assignment:update', '编辑角色授权', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120105, 100002, 110028, 'iam-tenant:audience:read', '查看应用可用人群', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120106, 100002, 110028, 'iam-tenant:audience:update', '编辑应用可用人群', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120107, 100002, 110029, 'iam-tenant:audit:read', '查看审计记录', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120108, 100002, 110030, 'iam-tenant:authorization:diagnose', '诊断', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120109, 100002, 110031, 'iam-tenant:delegation:create', '创建授权委派', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120110, 100002, 110031, 'iam-tenant:delegation:delete', '删除授权委派', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120111, 100002, 110031, 'iam-tenant:delegation:preview', '预览授权委派', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120112, 100002, 110031, 'iam-tenant:delegation:read', '查看授权委派', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120113, 100002, 110031, 'iam-tenant:delegation:update', '编辑授权委派', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120114, 100002, 110032, 'iam-tenant:department:create', '创建部门', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120115, 100002, 110032, 'iam-tenant:department:delete', '删除部门', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120116, 100002, 110032, 'iam-tenant:department:read', '查看部门', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120117, 100002, 110032, 'iam-tenant:department:update', '编辑部门', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120118, 100002, 110033, 'iam-tenant:directory:read', '查看通讯录', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120119, 100002, 110034, 'iam-tenant:directory-policy:read', '查看通讯录可见范围策略', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120120, 100002, 110034, 'iam-tenant:directory-policy:update', '编辑通讯录可见范围策略', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120121, 100002, 110035, 'iam-tenant:field-policy:read', '查看成员字段策略', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120122, 100002, 110035, 'iam-tenant:field-policy:update', '编辑成员字段策略', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120123, 100002, 110036, 'iam-tenant:group:create', '创建用户组', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120124, 100002, 110036, 'iam-tenant:group:delete', '删除用户组', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120125, 100002, 110036, 'iam-tenant:group:preview', '预览用户组', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120126, 100002, 110036, 'iam-tenant:group:read', '查看用户组', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120127, 100002, 110036, 'iam-tenant:group:update', '编辑用户组', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120128, 100002, 110037, 'iam-tenant:member:create', '创建成员', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120129, 100002, 110037, 'iam-tenant:member:departments', '调整任职', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120130, 100002, 110037, 'iam-tenant:member:export', '导出成员', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120131, 100002, 110037, 'iam-tenant:member:read', '查看成员', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120132, 100002, 110037, 'iam-tenant:member:remove', '移出成员', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120133, 100002, 110037, 'iam-tenant:member:status', '启停成员', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120134, 100002, 110037, 'iam-tenant:member:update', '编辑成员', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120135, 100002, 110038, 'iam-tenant:policy:preview', '预览策略预览', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120136, 100002, 110039, 'iam-tenant:role:create', '创建角色', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120137, 100002, 110039, 'iam-tenant:role:delete', '删除角色', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120138, 100002, 110039, 'iam-tenant:role:preview', '预览角色', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120139, 100002, 110039, 'iam-tenant:role:publish', '发布版本角色', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120140, 100002, 110039, 'iam-tenant:role:read', '查看角色', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120141, 100002, 110039, 'iam-tenant:role:status', '启停角色', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120142, 100002, 110039, 'iam-tenant:role:upgrade', '升级角色', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120143, 100002, 110040, 'iam-tenant:settings:owner-transfer', '转交所有者', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120144, 100002, 110040, 'iam-tenant:settings:read', '查看组织设置', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (120145, 100002, 110040, 'iam-tenant:settings:update', '编辑组织设置', 1, 0);
INSERT INTO `iam_action` (`id`, `application_id`, `resource_id`, `code`, `name`, `enabled`, `version`) VALUES (1000041, 1000037, 1000039, 'a02-mu3ra4ey:item:read', '读取业务对象', 1, 0);
COMMIT;

-- ----------------------------
-- Table structure for iam_app_audience
-- ----------------------------
DROP TABLE IF EXISTS `iam_app_audience`;
CREATE TABLE `iam_app_audience` (
  `tenant_id` bigint unsigned NOT NULL,
  `application_id` bigint unsigned NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `audience_kind` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `version` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`tenant_id`,`application_id`),
  CONSTRAINT `fk_iam_audience_entitlement` FOREIGN KEY (`tenant_id`, `application_id`) REFERENCES `iam_tenant_app_entitlement` (`tenant_id`, `application_id`),
  CONSTRAINT `ck_iam_audience_enabled` CHECK ((`enabled` in (0,1))),
  CONSTRAINT `ck_iam_audience_kind` CHECK ((`audience_kind` in (_utf8mb4'ALL',_utf8mb4'SELECTED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_app_audience
-- ----------------------------
BEGIN;
INSERT INTO `iam_app_audience` (`tenant_id`, `application_id`, `enabled`, `audience_kind`, `version`) VALUES (1000001, 100002, 1, 'ALL', 0);
INSERT INTO `iam_app_audience` (`tenant_id`, `application_id`, `enabled`, `audience_kind`, `version`) VALUES (1000001, 1000037, 1, 'ALL', 0);
INSERT INTO `iam_app_audience` (`tenant_id`, `application_id`, `enabled`, `audience_kind`, `version`) VALUES (1000007, 100002, 1, 'ALL', 0);
INSERT INTO `iam_app_audience` (`tenant_id`, `application_id`, `enabled`, `audience_kind`, `version`) VALUES (1000246, 100002, 1, 'ALL', 0);
INSERT INTO `iam_app_audience` (`tenant_id`, `application_id`, `enabled`, `audience_kind`, `version`) VALUES (1000252, 100002, 1, 'ALL', 0);
COMMIT;

-- ----------------------------
-- Table structure for iam_application
-- ----------------------------
DROP TABLE IF EXISTS `iam_application`;
CREATE TABLE `iam_application` (
  `id` bigint unsigned NOT NULL,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `domain` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `name` varchar(128) NOT NULL,
  `description` varchar(512) DEFAULT NULL,
  `icon` varchar(512) DEFAULT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `baseline` tinyint(1) NOT NULL DEFAULT '0',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_application_code` (`code`),
  CONSTRAINT `ck_iam_application_baseline` CHECK ((`baseline` in (0,1))),
  CONSTRAINT `ck_iam_application_baseline_domain` CHECK (((`baseline` = false) or (`domain` = _utf8mb4'TENANT'))),
  CONSTRAINT `ck_iam_application_domain` CHECK ((`domain` in (_utf8mb4'PLATFORM',_utf8mb4'TENANT'))),
  CONSTRAINT `ck_iam_application_enabled` CHECK ((`enabled` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_application
-- ----------------------------
BEGIN;
INSERT INTO `iam_application` (`id`, `code`, `domain`, `name`, `description`, `icon`, `sort_order`, `baseline`, `enabled`, `version`) VALUES (100001, 'iam-platform', 'PLATFORM', '平台治理', '平台域身份、目录与授权治理', NULL, 1, 0, 1, 0);
INSERT INTO `iam_application` (`id`, `code`, `domain`, `name`, `description`, `icon`, `sort_order`, `baseline`, `enabled`, `version`) VALUES (100002, 'iam-tenant', 'TENANT', '组织治理', '组织域成员、部门与授权治理', NULL, 1, 1, 1, 0);
INSERT INTO `iam_application` (`id`, `code`, `domain`, `name`, `description`, `icon`, `sort_order`, `baseline`, `enabled`, `version`) VALUES (1000037, 'a02-biz-mu3ra4ey', 'TENANT', 'A02 业务应用', '开通不等于授权', NULL, 90, 0, 1, 0);
COMMIT;

-- ----------------------------
-- Table structure for iam_audience_department
-- ----------------------------
DROP TABLE IF EXISTS `iam_audience_department`;
CREATE TABLE `iam_audience_department` (
  `tenant_id` bigint unsigned NOT NULL,
  `application_id` bigint unsigned NOT NULL,
  `department_id` bigint unsigned NOT NULL,
  `include_descendants` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`tenant_id`,`application_id`,`department_id`),
  KEY `fk_iam_audience_dept` (`tenant_id`,`department_id`),
  CONSTRAINT `fk_iam_audience_dept` FOREIGN KEY (`tenant_id`, `department_id`) REFERENCES `iam_department` (`tenant_id`, `id`),
  CONSTRAINT `fk_iam_audience_dept_config` FOREIGN KEY (`tenant_id`, `application_id`) REFERENCES `iam_app_audience` (`tenant_id`, `application_id`),
  CONSTRAINT `ck_iam_audience_descendants` CHECK ((`include_descendants` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_audience_department
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_audience_group
-- ----------------------------
DROP TABLE IF EXISTS `iam_audience_group`;
CREATE TABLE `iam_audience_group` (
  `tenant_id` bigint unsigned NOT NULL,
  `application_id` bigint unsigned NOT NULL,
  `group_id` bigint unsigned NOT NULL,
  PRIMARY KEY (`tenant_id`,`application_id`,`group_id`),
  KEY `fk_iam_audience_group` (`tenant_id`,`group_id`),
  CONSTRAINT `fk_iam_audience_group` FOREIGN KEY (`tenant_id`, `group_id`) REFERENCES `iam_tenant_group` (`tenant_id`, `id`),
  CONSTRAINT `fk_iam_audience_group_config` FOREIGN KEY (`tenant_id`, `application_id`) REFERENCES `iam_app_audience` (`tenant_id`, `application_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_audience_group
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_audience_member
-- ----------------------------
DROP TABLE IF EXISTS `iam_audience_member`;
CREATE TABLE `iam_audience_member` (
  `tenant_id` bigint unsigned NOT NULL,
  `application_id` bigint unsigned NOT NULL,
  `member_id` bigint unsigned NOT NULL,
  PRIMARY KEY (`tenant_id`,`application_id`,`member_id`),
  KEY `fk_iam_audience_member` (`tenant_id`,`member_id`),
  CONSTRAINT `fk_iam_audience_member` FOREIGN KEY (`tenant_id`, `member_id`) REFERENCES `iam_tenant_member` (`tenant_id`, `id`),
  CONSTRAINT `fk_iam_audience_member_config` FOREIGN KEY (`tenant_id`, `application_id`) REFERENCES `iam_app_audience` (`tenant_id`, `application_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_audience_member
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_authorization_audit
-- ----------------------------
DROP TABLE IF EXISTS `iam_authorization_audit`;
CREATE TABLE `iam_authorization_audit` (
  `id` bigint unsigned NOT NULL,
  `event_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `actor_account_id` bigint unsigned NOT NULL,
  `actor_member_id` bigint unsigned NOT NULL,
  `domain` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `tenant_id` bigint unsigned DEFAULT NULL,
  `target_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `target_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `change_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `safe_before` json NOT NULL,
  `safe_after` json NOT NULL,
  `revisions` json NOT NULL,
  `delegation_id` bigint unsigned DEFAULT NULL,
  `assignment_id` bigint unsigned DEFAULT NULL,
  `trace_id` varchar(128) DEFAULT NULL,
  `occurred_at` datetime(6) NOT NULL,
  `delivered_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_audit_event` (`event_id`),
  KEY `idx_iam_audit_tenant_time` (`tenant_id`,`occurred_at`,`id`),
  KEY `idx_iam_audit_actor` (`actor_account_id`,`occurred_at`,`id`),
  KEY `idx_iam_audit_delivery` (`delivered_at`,`id`),
  CONSTRAINT `ck_iam_audit_context` CHECK ((((`domain` = _utf8mb4'PLATFORM') and (`tenant_id` is null)) or ((`domain` = _utf8mb4'TENANT') and (`tenant_id` is not null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_authorization_audit
-- ----------------------------
BEGIN;
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000005, '107e6b15-9f6c-4b83-9065-8a030b7efb10', 900001, 910001, 'PLATFORM', NULL, 'tenant', '1000001', 'CREATE', '{}', '{\"NAME\": \"test\", \"OWNER_MEMBER\": \"1000002\"}', '{\"FIELD\": \"150002\", \"DIRECTORY\": \"150001\", \"GOVERNANCE\": \"141002\"}', NULL, NULL, NULL, '2026-09-16 05:54:05.750520', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000011, '316bf46c-7ffa-4468-b6c4-80a5cf90c155', 900001, 910001, 'PLATFORM', NULL, 'tenant', '1000007', 'CREATE', '{}', '{\"NAME\": \"test1\", \"OWNER_MEMBER\": \"1000008\"}', '{\"FIELD\": \"150002\", \"DIRECTORY\": \"150001\", \"GOVERNANCE\": \"141002\"}', NULL, NULL, NULL, '2026-09-16 05:54:12.481851', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000013, '66993652-770c-44a9-86c0-2efc59f2bfd7', 900002, 1000002, 'TENANT', 1000001, 'member', '1000012', 'CREATE', '{}', '{\"NAME\": \"平台账号在A\"}', '{\"member\": \"0\"}', NULL, NULL, NULL, '2026-09-16 06:08:11.616039', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000015, '17a6f9a4-88b8-4246-bf22-e160d0e63903', 900002, 1000002, 'TENANT', 1000001, 'member', '1000012', 'DISABLE', '{\"STATUS\": \"ACTIVE\"}', '{\"STATUS\": \"SUSPENDED\"}', '{\"member\": \"1\"}', NULL, NULL, NULL, '2026-09-16 06:13:03.954296', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000017, 'e0904473-854c-4c14-811c-2b90da60a0ad', 900002, 1000002, 'TENANT', 1000001, 'member', '1000012', 'REMOVE', '{\"STATUS\": \"SUSPENDED\"}', '{\"STATUS\": \"REMOVED\"}', '{\"member\": \"2\"}', NULL, NULL, NULL, '2026-09-16 06:16:19.415985', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000023, '3faf95b6-480c-4490-9093-e9e436c4716d', 900001, 910001, 'PLATFORM', NULL, 'account', '1000022', 'CREATE', '{}', '{\"NAME\": \"a01b-x-mu3ra4ey\", \"STATUS\": \"ENABLED\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-16 07:30:55.671234', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000024, '2bcbf2a4-867d-4dee-94bb-d58b9eac3e50', 900001, 910001, 'PLATFORM', NULL, 'account', '1000022', 'UPDATE', '{\"NAME\": \"a01b-x-mu3ra4ey\"}', '{\"NAME\": \"a01b-x-mu3ra4ey\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-16 07:31:22.494316', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000026, '2fac81a9-1dc9-4067-80b6-c28accaccbe2', 900001, 910001, 'PLATFORM', NULL, 'member', '1000025', 'CREATE', '{}', '{\"NAME\": \"A01b 平台成员\"}', '{\"member\": \"0\"}', NULL, NULL, NULL, '2026-09-16 07:31:49.060008', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000028, '981ab78a-3843-48d6-a239-7f7342f8f4d4', 900002, 1000002, 'TENANT', 1000001, 'member', '1000027', 'CREATE', '{}', '{\"NAME\": \"A01b 租户成员\"}', '{\"member\": \"0\"}', NULL, NULL, NULL, '2026-09-16 07:32:04.422924', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000029, '895b1829-6b36-41e5-814d-307af35aef87', 900001, 910001, 'PLATFORM', NULL, 'member', '1000025', 'DISABLE', '{\"STATUS\": \"ACTIVE\"}', '{\"STATUS\": \"SUSPENDED\"}', '{\"member\": \"1\"}', NULL, NULL, NULL, '2026-09-16 07:32:39.937709', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000030, 'c2584839-eb26-4b82-81e4-97caa714c175', 900002, 1000002, 'TENANT', 1000001, 'member', '1000027', 'REMOVE', '{\"STATUS\": \"ACTIVE\"}', '{\"STATUS\": \"REMOVED\"}', '{\"member\": \"1\"}', NULL, NULL, NULL, '2026-09-16 07:33:35.939446', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000031, '8d32b041-2f4b-4e79-a81c-40f6d639c139', 900001, 910001, 'PLATFORM', NULL, 'account', '1000022', 'DISABLE', '{\"STATUS\": \"ENABLED\"}', '{\"STATUS\": \"DISABLED\"}', '{\"account\": \"1\"}', NULL, NULL, NULL, '2026-09-16 07:34:02.230959', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000033, '15016c00-3375-47e5-b2d1-541b7c2a7a81', 900001, 910001, 'PLATFORM', NULL, 'account', '1000032', 'CREATE', '{}', '{\"NAME\": \"a02-y-mu3ra4ey\", \"STATUS\": \"ENABLED\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-16 07:34:37.660245', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000034, 'c8680193-c71f-4d5e-a8bf-82d3a0cc6786', 900001, 910001, 'PLATFORM', NULL, 'account', '1000032', 'UPDATE', '{\"NAME\": \"a02-y-mu3ra4ey\"}', '{\"NAME\": \"a02-y-mu3ra4ey\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-16 07:34:50.594587', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000036, '45d4a4b9-99ff-4f61-82ac-802dc1310e32', 900002, 1000002, 'TENANT', 1000001, 'member', '1000035', 'CREATE', '{}', '{\"NAME\": \"A02 业务成员\"}', '{\"member\": \"0\"}', NULL, NULL, NULL, '2026-09-16 07:34:57.798759', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000038, 'f6d0f7f4-bf72-4680-82a4-2aef795beab3', 900001, 910001, 'PLATFORM', NULL, 'application', '1000037', 'CREATE', '{}', '{\"NAME\": \"A02 业务应用\"}', '{\"application\": \"0\"}', NULL, NULL, NULL, '2026-09-16 07:35:11.287780', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000040, '98f0f311-c394-4ca7-a5ba-c7008f68509f', 900001, 910001, 'PLATFORM', NULL, 'resource', '1000039', 'CREATE', '{}', '{\"NAME\": \"业务对象\"}', '{\"resource\": \"0\"}', NULL, NULL, NULL, '2026-09-16 07:35:14.557826', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000042, '30cad871-ca5a-4a92-ab87-28d8d79cfcb9', 900001, 910001, 'PLATFORM', NULL, 'action', '1000041', 'CREATE', '{}', '{\"NAME\": \"读取业务对象\"}', '{\"action\": \"0\"}', NULL, NULL, NULL, '2026-09-16 07:35:17.805632', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000045, '3899b22e-35ca-4284-9361-7d3a6c314ec2', 900001, 910001, 'PLATFORM', NULL, 'entitlement', '1000001', 'UPDATE', '{}', '{\"ENTITLEMENT\": \"2\"}', '{\"entitlement\": \"100002:0:1|1000037:0:1\"}', NULL, NULL, NULL, '2026-09-16 08:07:53.967165', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000048, 'fd2b9fd9-1780-461a-bfae-2b3657c043e7', 900001, 910001, 'PLATFORM', NULL, 'role', '1000046', 'CREATE', '{}', '{\"NAME\": \"A02 业务读取\"}', '{\"role\": \"0\"}', NULL, NULL, NULL, '2026-09-16 08:09:05.404825', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000050, '595b5343-bed2-45e5-ae93-9604d7358c2b', 900002, 1000002, 'TENANT', 1000001, 'assignment', '1000049', 'CREATE', '{}', '{\"ROLE_REVISION\": \"1000047\"}', '{\"assignment\": \"0\"}', NULL, 1000049, NULL, '2026-09-16 08:10:24.768831', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000053, '54b9bef1-7b37-4c33-b353-2771e383635b', 900001, 910001, 'PLATFORM', NULL, 'role', '1000051', 'CREATE', '{}', '{\"NAME\": \"A04 成员读写\"}', '{\"role\": \"0\"}', NULL, NULL, NULL, '2026-09-16 08:11:04.368016', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000055, 'fd3aeae1-00de-48b8-872b-32da76250077', 900002, 1000002, 'TENANT', 1000001, 'assignment', '1000054', 'CREATE', '{}', '{\"ROLE_REVISION\": \"1000052\"}', '{\"assignment\": \"0\"}', NULL, 1000054, NULL, '2026-09-16 08:11:12.590864', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000058, '3142117e-c3bb-4409-a0bb-4ad231d3e023', 900002, 1000002, 'TENANT', 1000001, 'role', '1000056', 'CREATE', '{}', '{\"NAME\": \"A04 只读定制\"}', '{\"role\": \"0\"}', NULL, NULL, NULL, '2026-09-16 08:11:15.632301', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000060, 'eb38fd0f-6993-4f57-ae48-47842259e178', 900002, 1000002, 'TENANT', 1000001, 'role', '1000056', 'UPDATE', '{\"ROLE_REVISION\": \"1\"}', '{\"ROLE_REVISION\": \"1000059\"}', '{\"role\": \"1\"}', NULL, NULL, NULL, '2026-09-16 08:11:27.845888', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000062, 'd58bcbf5-a028-4be6-9119-78823dc75391', 900002, 1000002, 'TENANT', 1000001, 'role', '1000056', 'UPDATE', '{\"ROLE_REVISION\": \"2\"}', '{\"ROLE_REVISION\": \"1000061\"}', '{\"role\": \"2\"}', NULL, NULL, NULL, '2026-09-16 08:11:33.459540', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000064, 'c13c15eb-8286-4cc8-99f4-c2074b33dea9', 900001, 910001, 'PLATFORM', NULL, 'account', '1000063', 'CREATE', '{}', '{\"NAME\": \"a26-z-mu3ra4ey\", \"STATUS\": \"ENABLED\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-16 08:11:40.922896', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000065, 'b48626d7-d7c2-4950-9b06-75af2e9d3ee0', 900001, 910001, 'PLATFORM', NULL, 'account', '1000063', 'UPDATE', '{\"NAME\": \"a26-z-mu3ra4ey\"}', '{\"NAME\": \"a26-z-mu3ra4ey\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-16 08:11:44.078514', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000067, 'e58f829b-2d8e-4072-99f5-5a875c8f73bf', 900002, 1000002, 'TENANT', 1000001, 'member', '1000066', 'CREATE', '{}', '{\"NAME\": \"A26 新所有者\"}', '{\"member\": \"0\"}', NULL, NULL, NULL, '2026-09-16 08:11:47.433086', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000069, 'a4082385-d68d-4d27-bf2c-0c1ef5a59728', 900002, 1000002, 'TENANT', 1000001, 'assignment', '1000068', 'CREATE', '{}', '{\"ROLE_REVISION\": \"1000052\"}', '{\"assignment\": \"0\"}', NULL, 1000068, NULL, '2026-09-16 08:11:49.669901', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000070, 'c1917719-ac71-4fd8-b4ab-55ca9c95aad8', 900002, 1000002, 'TENANT', 1000001, 'assignment', '1000004', 'REVOKE', '{\"OWNER_MEMBER\": \"1000002\"}', '{\"STATUS\": \"REVOKED\"}', '{\"assignment\": \"1\"}', NULL, 1000004, NULL, '2026-09-16 08:11:54.835676', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000072, 'd7ad5048-b66b-47d1-a83c-c0179fe84471', 900002, 1000002, 'TENANT', 1000001, 'assignment', '1000071', 'CREATE', '{}', '{\"OWNER_MEMBER\": \"1000066\", \"ROLE_REVISION\": \"141002\"}', '{\"assignment\": \"0\"}', NULL, 1000071, NULL, '2026-09-16 08:11:54.843593', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000073, '30472fc2-b001-45c6-aab1-a146a724418c', 900002, 1000002, 'TENANT', 1000001, 'tenant', '1000001', 'OWNER_TRANSFER', '{\"OWNER_MEMBER\": \"1000002\"}', '{\"OWNER_MEMBER\": \"1000066\"}', '{\"tenant\": \"1\"}', NULL, NULL, NULL, '2026-09-16 08:11:54.852649', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000201, '096706e6-f66d-418a-b6b3-eb95184c06ab', 900001, 910001, 'PLATFORM', NULL, 'account', '1000200', 'CREATE', '{}', '{\"NAME\": \"iam-test-platform-reader\", \"STATUS\": \"ENABLED\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:00.857809', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000202, 'bc71998c-7cdb-4227-87f5-184df58e13aa', 900001, 910001, 'PLATFORM', NULL, 'account', '1000200', 'UPDATE', '{\"NAME\": \"iam-test-platform-reader\"}', '{\"NAME\": \"iam-test-platform-reader\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:02.025888', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000204, '0eeb7ef7-dd6a-4ddc-9a0e-ce4c8b74b928', 900001, 910001, 'PLATFORM', NULL, 'account', '1000203', 'CREATE', '{}', '{\"NAME\": \"iam-test-owner-a\", \"STATUS\": \"ENABLED\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:02.792342', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000205, '7282018a-bd9d-4745-849b-d6fbca962a85', 900001, 910001, 'PLATFORM', NULL, 'account', '1000203', 'UPDATE', '{\"NAME\": \"iam-test-owner-a\"}', '{\"NAME\": \"iam-test-owner-a\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:03.884303', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000207, '863f248a-aa5d-4569-a879-322fb17cedeb', 900001, 910001, 'PLATFORM', NULL, 'account', '1000206', 'CREATE', '{}', '{\"NAME\": \"iam-test-owner-b\", \"STATUS\": \"ENABLED\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:04.592200', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000208, '6241d061-171d-4d8c-8734-5fa6c752e401', 900001, 910001, 'PLATFORM', NULL, 'account', '1000206', 'UPDATE', '{\"NAME\": \"iam-test-owner-b\"}', '{\"NAME\": \"iam-test-owner-b\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:05.648486', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000210, '08d23e92-ca88-4b69-85fc-0089a8aafda1', 900001, 910001, 'PLATFORM', NULL, 'account', '1000209', 'CREATE', '{}', '{\"NAME\": \"iam-test-dual\", \"STATUS\": \"ENABLED\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:06.377307', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000211, 'e34dd33e-0b17-481e-b9f7-16f820e01546', 900001, 910001, 'PLATFORM', NULL, 'account', '1000209', 'UPDATE', '{\"NAME\": \"iam-test-dual\"}', '{\"NAME\": \"iam-test-dual\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:07.437980', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000213, 'ab07510d-d0eb-43c3-97a8-a6bb6596886d', 900001, 910001, 'PLATFORM', NULL, 'account', '1000212', 'CREATE', '{}', '{\"NAME\": \"iam-test-reader-a\", \"STATUS\": \"ENABLED\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:08.184585', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000214, '4f0a9185-e442-40de-aca1-f6170d1491b1', 900001, 910001, 'PLATFORM', NULL, 'account', '1000212', 'UPDATE', '{\"NAME\": \"iam-test-reader-a\"}', '{\"NAME\": \"iam-test-reader-a\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:09.279082', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000216, '9669a2b9-94ee-4afc-aef8-c5ebdd5db259', 900001, 910001, 'PLATFORM', NULL, 'account', '1000215', 'CREATE', '{}', '{\"NAME\": \"iam-test-editor-a\", \"STATUS\": \"ENABLED\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:10.060781', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000217, 'ab821d60-a777-4cc8-9c4b-959e968329e7', 900001, 910001, 'PLATFORM', NULL, 'account', '1000215', 'UPDATE', '{\"NAME\": \"iam-test-editor-a\"}', '{\"NAME\": \"iam-test-editor-a\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:11.161956', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000219, 'f087edfb-8903-41db-ad9a-7e149838f0af', 900001, 910001, 'PLATFORM', NULL, 'account', '1000218', 'CREATE', '{}', '{\"NAME\": \"iam-test-grantor-a\", \"STATUS\": \"ENABLED\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:11.857701', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000220, '2b2906f2-141a-47c1-bf92-96d3bbb47a35', 900001, 910001, 'PLATFORM', NULL, 'account', '1000218', 'UPDATE', '{\"NAME\": \"iam-test-grantor-a\"}', '{\"NAME\": \"iam-test-grantor-a\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:13.031451', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000222, '78056899-8933-4a21-8372-dfa5b24a1397', 900001, 910001, 'PLATFORM', NULL, 'account', '1000221', 'CREATE', '{}', '{\"NAME\": \"iam-test-ordinary-a\", \"STATUS\": \"ENABLED\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:13.817145', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000223, '651ab3dd-4e20-4116-b772-e3d6c071e351', 900001, 910001, 'PLATFORM', NULL, 'account', '1000221', 'UPDATE', '{\"NAME\": \"iam-test-ordinary-a\"}', '{\"NAME\": \"iam-test-ordinary-a\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:15.028834', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000225, 'e91d16c9-0d6b-43bd-946b-1640c879b4c6', 900001, 910001, 'PLATFORM', NULL, 'account', '1000224', 'CREATE', '{}', '{\"NAME\": \"iam-test-multi-dept-a\", \"STATUS\": \"ENABLED\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:15.819493', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000226, '85a3f026-3073-4b52-9bda-e24ad4d8709b', 900001, 910001, 'PLATFORM', NULL, 'account', '1000224', 'UPDATE', '{\"NAME\": \"iam-test-multi-dept-a\"}', '{\"NAME\": \"iam-test-multi-dept-a\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:16.900744', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000228, '15597aca-6556-4d6c-99ca-d57b09d56e10', 900001, 910001, 'PLATFORM', NULL, 'account', '1000227', 'CREATE', '{}', '{\"NAME\": \"iam-test-no-dept-a\", \"STATUS\": \"ENABLED\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:17.645026', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000229, 'd5f7913b-8772-440e-88df-3ba560b828b9', 900001, 910001, 'PLATFORM', NULL, 'account', '1000227', 'UPDATE', '{\"NAME\": \"iam-test-no-dept-a\"}', '{\"NAME\": \"iam-test-no-dept-a\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:18.763808', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000231, '5557e74d-1471-4658-bce3-1d9e2284d9aa', 900001, 910001, 'PLATFORM', NULL, 'account', '1000230', 'CREATE', '{}', '{\"NAME\": \"iam-test-no-access\", \"STATUS\": \"ENABLED\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:19.538493', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000232, '3322aa5d-7efb-4849-b647-c654237f9814', 900001, 910001, 'PLATFORM', NULL, 'account', '1000230', 'UPDATE', '{\"NAME\": \"iam-test-no-access\"}', '{\"NAME\": \"iam-test-no-access\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:20.679818', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000234, '6077090c-656c-4ec4-8cdf-f7717d40cf10', 900001, 910001, 'PLATFORM', NULL, 'account', '1000233', 'CREATE', '{}', '{\"NAME\": \"iam-test-suspended\", \"STATUS\": \"ENABLED\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:21.433948', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000235, '8cff726a-4254-4287-9788-c088aac29841', 900001, 910001, 'PLATFORM', NULL, 'account', '1000233', 'UPDATE', '{\"NAME\": \"iam-test-suspended\"}', '{\"NAME\": \"iam-test-suspended\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:22.500663', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000237, '257c7026-622a-49fc-bfcd-ebb5e336bca8', 900001, 910001, 'PLATFORM', NULL, 'account', '1000236', 'CREATE', '{}', '{\"NAME\": \"iam-test-disabled\", \"STATUS\": \"ENABLED\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:23.252449', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000238, 'e87b16d2-c96a-459a-8b05-dc14879ca1e7', 900001, 910001, 'PLATFORM', NULL, 'account', '1000236', 'UPDATE', '{\"NAME\": \"iam-test-disabled\"}', '{\"NAME\": \"iam-test-disabled\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:24.337181', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000240, '2604bf93-00ff-495d-996e-8ca0b4f5efd2', 900001, 910001, 'PLATFORM', NULL, 'account', '1000239', 'CREATE', '{}', '{\"NAME\": \"iam-test-owner-transfer-src\", \"STATUS\": \"ENABLED\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:25.090641', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000241, 'bf538c9a-dd01-4373-b1a0-26112304e6c5', 900001, 910001, 'PLATFORM', NULL, 'account', '1000239', 'UPDATE', '{\"NAME\": \"iam-test-owner-transfer-src\"}', '{\"NAME\": \"iam-test-owner-transfer-src\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:26.226913', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000243, 'd4896baf-ceb2-4a01-9969-dba78da9fc9c', 900001, 910001, 'PLATFORM', NULL, 'account', '1000242', 'CREATE', '{}', '{\"NAME\": \"iam-test-owner-transfer-dst\", \"STATUS\": \"ENABLED\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:27.015466', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000244, 'ab3aeef1-c5d7-4b8f-94a4-e2e0eb44c001', 900001, 910001, 'PLATFORM', NULL, 'account', '1000242', 'UPDATE', '{\"NAME\": \"iam-test-owner-transfer-dst\"}', '{\"NAME\": \"iam-test-owner-transfer-dst\"}', '{\"account\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:28.140467', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000250, '9accc94d-ed46-4313-8ff4-924777f9b646', 900001, 910001, 'PLATFORM', NULL, 'tenant', '1000246', 'CREATE', '{}', '{\"NAME\": \"测试组织A\", \"OWNER_MEMBER\": \"1000247\"}', '{\"FIELD\": \"150002\", \"DIRECTORY\": \"150001\", \"GOVERNANCE\": \"141002\"}', NULL, NULL, NULL, '2026-09-20 05:39:29.033946', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000256, 'fb647e96-d8f4-4ca6-9425-c123156368a6', 900001, 910001, 'PLATFORM', NULL, 'tenant', '1000252', 'CREATE', '{}', '{\"NAME\": \"测试组织B\", \"OWNER_MEMBER\": \"1000253\"}', '{\"FIELD\": \"150002\", \"DIRECTORY\": \"150001\", \"GOVERNANCE\": \"141002\"}', NULL, NULL, NULL, '2026-09-20 05:39:29.625952', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000258, 'c95bd7ff-240a-4510-ae8b-9b088969091e', 1000203, 1000247, 'TENANT', 1000246, 'department', '1000257', 'CREATE', '{}', '{\"NAME\": \"研发\"}', '{\"department\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:31.481416', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000260, '625a67bc-202f-4ce2-bdcd-bc90d0fb3316', 1000203, 1000247, 'TENANT', 1000246, 'department', '1000259', 'CREATE', '{}', '{\"NAME\": \"销售\"}', '{\"department\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:32.110962', NULL);
INSERT INTO `iam_authorization_audit` (`id`, `event_id`, `actor_account_id`, `actor_member_id`, `domain`, `tenant_id`, `target_type`, `target_id`, `change_type`, `safe_before`, `safe_after`, `revisions`, `delegation_id`, `assignment_id`, `trace_id`, `occurred_at`, `delivered_at`) VALUES (1000262, '3b60bba4-0997-42be-9151-b4a0deb2fce7', 1000203, 1000247, 'TENANT', 1000246, 'department', '1000261', 'CREATE', '{}', '{\"NAME\": \"空部门\"}', '{\"department\": \"0\"}', NULL, NULL, NULL, '2026-09-20 05:39:32.645713', NULL);
COMMIT;

-- ----------------------------
-- Table structure for iam_default_policy_revision
-- ----------------------------
DROP TABLE IF EXISTS `iam_default_policy_revision`;
CREATE TABLE `iam_default_policy_revision` (
  `id` bigint unsigned NOT NULL,
  `kind` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `revision` bigint unsigned NOT NULL,
  `definition` json NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_default_policy_kind` (`id`,`kind`),
  UNIQUE KEY `uk_iam_default_policy_revision` (`kind`,`revision`),
  CONSTRAINT `ck_iam_default_policy_definition` CHECK ((json_type(`definition`) = _utf8mb4'OBJECT')),
  CONSTRAINT `ck_iam_default_policy_kind` CHECK ((`kind` in (_utf8mb4'DIRECTORY',_utf8mb4'FIELD'))),
  CONSTRAINT `ck_iam_default_policy_revision` CHECK ((`revision` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_default_policy_revision
-- ----------------------------
BEGIN;
INSERT INTO `iam_default_policy_revision` (`id`, `kind`, `revision`, `definition`) VALUES (150001, 'DIRECTORY', 1, '{\"scope\": \"ALL\"}');
INSERT INTO `iam_default_policy_revision` (`id`, `kind`, `revision`, `definition`) VALUES (150002, 'FIELD', 1, '{\"fields\": {\"email\": {\"editable\": false, \"visibility\": \"MASKED\"}, \"phone\": {\"editable\": false, \"visibility\": \"MASKED\"}, \"avatar\": {\"editable\": true, \"visibility\": \"FULL\"}, \"displayName\": {\"editable\": true, \"visibility\": \"FULL\"}}}');
COMMIT;

-- ----------------------------
-- Table structure for iam_delegation_action_ceiling
-- ----------------------------
DROP TABLE IF EXISTS `iam_delegation_action_ceiling`;
CREATE TABLE `iam_delegation_action_ceiling` (
  `delegation_id` bigint unsigned NOT NULL,
  `action_id` bigint unsigned NOT NULL,
  `scopes` json NOT NULL,
  `scope_bindings` json NOT NULL,
  PRIMARY KEY (`delegation_id`,`action_id`),
  KEY `fk_iam_ceiling_action` (`action_id`),
  CONSTRAINT `fk_iam_ceiling_action` FOREIGN KEY (`action_id`) REFERENCES `iam_action` (`id`),
  CONSTRAINT `fk_iam_ceiling_delegation` FOREIGN KEY (`delegation_id`) REFERENCES `iam_delegation_grant` (`id`),
  CONSTRAINT `ck_iam_ceiling_bindings` CHECK ((json_type(`scope_bindings`) = _utf8mb4'OBJECT')),
  CONSTRAINT `ck_iam_ceiling_scopes` CHECK ((json_type(`scopes`) = _utf8mb4'ARRAY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_delegation_action_ceiling
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_delegation_grant
-- ----------------------------
DROP TABLE IF EXISTS `iam_delegation_grant`;
CREATE TABLE `iam_delegation_grant` (
  `id` bigint unsigned NOT NULL,
  `domain` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `tenant_id` bigint unsigned DEFAULT NULL,
  `tenant_key` bigint unsigned GENERATED ALWAYS AS (coalesce(`tenant_id`,0)) STORED,
  `platform_administrator_id` bigint unsigned DEFAULT NULL,
  `tenant_administrator_id` bigint unsigned DEFAULT NULL,
  `valid_from` datetime(6) DEFAULT NULL,
  `valid_until` datetime(6) DEFAULT NULL,
  `max_assignment_duration_seconds` bigint unsigned NOT NULL,
  `max_assignment_duration_nanos` int unsigned NOT NULL DEFAULT '0',
  `status` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE',
  `version` bigint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_delegation_domain` (`domain`,`tenant_key`,`id`),
  UNIQUE KEY `uk_iam_delegation_tenant` (`tenant_id`,`id`),
  KEY `idx_iam_delegation_platform_admin` (`platform_administrator_id`,`status`,`valid_until`),
  KEY `idx_iam_delegation_tenant_admin` (`tenant_id`,`tenant_administrator_id`,`status`,`valid_until`),
  CONSTRAINT `fk_iam_delegation_platform_admin` FOREIGN KEY (`platform_administrator_id`) REFERENCES `iam_platform_member` (`id`),
  CONSTRAINT `fk_iam_delegation_tenant_admin` FOREIGN KEY (`tenant_id`, `tenant_administrator_id`) REFERENCES `iam_tenant_member` (`tenant_id`, `id`),
  CONSTRAINT `ck_iam_delegation_duration` CHECK (((`max_assignment_duration_nanos` < 1000000000) and ((`max_assignment_duration_seconds` > 0) or (`max_assignment_duration_nanos` > 0)))),
  CONSTRAINT `ck_iam_delegation_identity` CHECK ((((`domain` = _utf8mb4'PLATFORM') and (`tenant_id` is null) and (`platform_administrator_id` is not null) and (`tenant_administrator_id` is null)) or ((`domain` = _utf8mb4'TENANT') and (`tenant_id` is not null) and (`tenant_administrator_id` is not null) and (`platform_administrator_id` is null)))),
  CONSTRAINT `ck_iam_delegation_interval` CHECK (((`valid_from` is null) or (`valid_until` is null) or (`valid_from` < `valid_until`))),
  CONSTRAINT `ck_iam_delegation_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'REVOKED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_delegation_grant
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_delegation_recipient_department
-- ----------------------------
DROP TABLE IF EXISTS `iam_delegation_recipient_department`;
CREATE TABLE `iam_delegation_recipient_department` (
  `delegation_id` bigint unsigned NOT NULL,
  `tenant_id` bigint unsigned NOT NULL,
  `department_id` bigint unsigned NOT NULL,
  `include_descendants` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`delegation_id`,`department_id`),
  KEY `fk_iam_recipient_dept_delegation` (`tenant_id`,`delegation_id`),
  KEY `fk_iam_recipient_dept_department` (`tenant_id`,`department_id`),
  CONSTRAINT `fk_iam_recipient_dept_delegation` FOREIGN KEY (`tenant_id`, `delegation_id`) REFERENCES `iam_delegation_grant` (`tenant_id`, `id`),
  CONSTRAINT `fk_iam_recipient_dept_department` FOREIGN KEY (`tenant_id`, `department_id`) REFERENCES `iam_department` (`tenant_id`, `id`),
  CONSTRAINT `ck_iam_recipient_descendants` CHECK ((`include_descendants` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_delegation_recipient_department
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_delegation_recipient_member
-- ----------------------------
DROP TABLE IF EXISTS `iam_delegation_recipient_member`;
CREATE TABLE `iam_delegation_recipient_member` (
  `delegation_id` bigint unsigned NOT NULL,
  `domain` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `tenant_id` bigint unsigned DEFAULT NULL,
  `tenant_key` bigint unsigned GENERATED ALWAYS AS (coalesce(`tenant_id`,0)) STORED,
  `platform_member_id` bigint unsigned DEFAULT NULL,
  `tenant_member_id` bigint unsigned DEFAULT NULL,
  `member_id` bigint unsigned GENERATED ALWAYS AS (coalesce(`platform_member_id`,`tenant_member_id`)) STORED,
  UNIQUE KEY `uk_iam_delegation_recipient` (`delegation_id`,`member_id`),
  KEY `fk_iam_recipient_delegation` (`domain`,`tenant_key`,`delegation_id`),
  KEY `fk_iam_recipient_platform` (`platform_member_id`),
  KEY `fk_iam_recipient_tenant` (`tenant_id`,`tenant_member_id`),
  CONSTRAINT `fk_iam_recipient_delegation` FOREIGN KEY (`domain`, `tenant_key`, `delegation_id`) REFERENCES `iam_delegation_grant` (`domain`, `tenant_key`, `id`),
  CONSTRAINT `fk_iam_recipient_platform` FOREIGN KEY (`platform_member_id`) REFERENCES `iam_platform_member` (`id`),
  CONSTRAINT `fk_iam_recipient_tenant` FOREIGN KEY (`tenant_id`, `tenant_member_id`) REFERENCES `iam_tenant_member` (`tenant_id`, `id`),
  CONSTRAINT `ck_iam_recipient_identity` CHECK ((((`domain` = _utf8mb4'PLATFORM') and (`tenant_id` is null) and (`platform_member_id` is not null) and (`tenant_member_id` is null)) or ((`domain` = _utf8mb4'TENANT') and (`tenant_id` is not null) and (`tenant_member_id` is not null) and (`platform_member_id` is null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_delegation_recipient_member
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_delegation_role_revision
-- ----------------------------
DROP TABLE IF EXISTS `iam_delegation_role_revision`;
CREATE TABLE `iam_delegation_role_revision` (
  `delegation_id` bigint unsigned NOT NULL,
  `revision_id` bigint unsigned NOT NULL,
  `revision_kind` varchar(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  PRIMARY KEY (`delegation_id`,`revision_id`),
  KEY `fk_iam_delegation_role_revision` (`revision_id`,`revision_kind`),
  CONSTRAINT `fk_iam_delegation_role_revision` FOREIGN KEY (`revision_id`, `revision_kind`) REFERENCES `iam_role_revision` (`id`, `kind`),
  CONSTRAINT `fk_iam_delegation_role_source` FOREIGN KEY (`delegation_id`) REFERENCES `iam_delegation_grant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_delegation_role_revision
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_department
-- ----------------------------
DROP TABLE IF EXISTS `iam_department`;
CREATE TABLE `iam_department` (
  `id` bigint unsigned NOT NULL,
  `tenant_id` bigint unsigned NOT NULL,
  `parent_id` bigint unsigned DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `version` bigint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_department_domain` (`tenant_id`,`id`),
  KEY `idx_iam_department_parent` (`tenant_id`,`parent_id`,`id`),
  CONSTRAINT `fk_iam_department_parent` FOREIGN KEY (`tenant_id`, `parent_id`) REFERENCES `iam_department` (`tenant_id`, `id`),
  CONSTRAINT `fk_iam_department_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `iam_tenant` (`id`),
  CONSTRAINT `ck_iam_department_id` CHECK ((`id` > 0)),
  CONSTRAINT `ck_iam_department_parent` CHECK (((`parent_id` is null) or (`parent_id` <> `id`)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_department
-- ----------------------------
BEGIN;
INSERT INTO `iam_department` (`id`, `tenant_id`, `parent_id`, `name`, `sort_order`, `version`, `created_at`, `updated_at`) VALUES (1000003, 1000001, NULL, 'test', 0, 0, '2026-09-16 05:54:05.606442', '2026-09-16 05:54:05.606442');
INSERT INTO `iam_department` (`id`, `tenant_id`, `parent_id`, `name`, `sort_order`, `version`, `created_at`, `updated_at`) VALUES (1000009, 1000007, NULL, 'test1', 0, 0, '2026-09-16 05:54:12.426574', '2026-09-16 05:54:12.426574');
INSERT INTO `iam_department` (`id`, `tenant_id`, `parent_id`, `name`, `sort_order`, `version`, `created_at`, `updated_at`) VALUES (1000248, 1000246, NULL, '根部门', 0, 0, '2026-09-20 05:39:28.896311', '2026-09-20 05:39:28.896311');
INSERT INTO `iam_department` (`id`, `tenant_id`, `parent_id`, `name`, `sort_order`, `version`, `created_at`, `updated_at`) VALUES (1000254, 1000252, NULL, '根部门', 0, 0, '2026-09-20 05:39:29.578645', '2026-09-20 05:39:29.578645');
INSERT INTO `iam_department` (`id`, `tenant_id`, `parent_id`, `name`, `sort_order`, `version`, `created_at`, `updated_at`) VALUES (1000257, 1000246, 1000248, '研发', 10, 0, '2026-09-20 05:39:31.478231', '2026-09-20 05:39:31.478231');
INSERT INTO `iam_department` (`id`, `tenant_id`, `parent_id`, `name`, `sort_order`, `version`, `created_at`, `updated_at`) VALUES (1000259, 1000246, 1000248, '销售', 10, 0, '2026-09-20 05:39:32.108271', '2026-09-20 05:39:32.108271');
INSERT INTO `iam_department` (`id`, `tenant_id`, `parent_id`, `name`, `sort_order`, `version`, `created_at`, `updated_at`) VALUES (1000261, 1000246, 1000248, '空部门', 10, 0, '2026-09-20 05:39:32.642908', '2026-09-20 05:39:32.642908');
COMMIT;

-- ----------------------------
-- Table structure for iam_directory_policy
-- ----------------------------
DROP TABLE IF EXISTS `iam_directory_policy`;
CREATE TABLE `iam_directory_policy` (
  `tenant_id` bigint unsigned NOT NULL,
  `default_revision_id` bigint unsigned NOT NULL,
  `default_kind` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'DIRECTORY',
  `default_scope` varchar(16) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT '空表示继承固定默认版本',
  `default_selector_id` bigint unsigned DEFAULT NULL,
  `version` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`tenant_id`),
  KEY `fk_iam_directory_default` (`default_revision_id`,`default_kind`),
  KEY `fk_iam_directory_default_selector` (`tenant_id`,`default_selector_id`),
  CONSTRAINT `fk_iam_directory_default` FOREIGN KEY (`default_revision_id`, `default_kind`) REFERENCES `iam_default_policy_revision` (`id`, `kind`),
  CONSTRAINT `fk_iam_directory_default_selector` FOREIGN KEY (`tenant_id`, `default_selector_id`) REFERENCES `iam_policy_selector` (`tenant_id`, `id`),
  CONSTRAINT `fk_iam_directory_policy_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `iam_tenant` (`id`),
  CONSTRAINT `ck_iam_directory_default_kind` CHECK ((`default_kind` = _utf8mb4'DIRECTORY')),
  CONSTRAINT `ck_iam_directory_default_scope` CHECK ((((`default_scope` is null) and (`default_selector_id` is null)) or ((`default_scope` is not null) and (((`default_scope` in (_utf8mb4'ALL',_utf8mb4'SELF')) and (`default_selector_id` is null)) or ((`default_scope` = _utf8mb4'SELECTED') and (`default_selector_id` is not null))))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_directory_policy
-- ----------------------------
BEGIN;
INSERT INTO `iam_directory_policy` (`tenant_id`, `default_revision_id`, `default_kind`, `default_scope`, `default_selector_id`, `version`) VALUES (1000001, 150001, 'DIRECTORY', NULL, NULL, 0);
INSERT INTO `iam_directory_policy` (`tenant_id`, `default_revision_id`, `default_kind`, `default_scope`, `default_selector_id`, `version`) VALUES (1000007, 150001, 'DIRECTORY', NULL, NULL, 0);
INSERT INTO `iam_directory_policy` (`tenant_id`, `default_revision_id`, `default_kind`, `default_scope`, `default_selector_id`, `version`) VALUES (1000246, 150001, 'DIRECTORY', NULL, NULL, 0);
INSERT INTO `iam_directory_policy` (`tenant_id`, `default_revision_id`, `default_kind`, `default_scope`, `default_selector_id`, `version`) VALUES (1000252, 150001, 'DIRECTORY', NULL, NULL, 0);
COMMIT;

-- ----------------------------
-- Table structure for iam_directory_rule
-- ----------------------------
DROP TABLE IF EXISTS `iam_directory_rule`;
CREATE TABLE `iam_directory_rule` (
  `id` bigint unsigned NOT NULL,
  `tenant_id` bigint unsigned NOT NULL,
  `effect` varchar(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `viewer_selector_id` bigint unsigned NOT NULL,
  `target_selector_id` bigint unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_iam_directory_rule` (`tenant_id`,`effect`,`id`),
  KEY `fk_iam_directory_rule_viewer` (`tenant_id`,`viewer_selector_id`),
  KEY `fk_iam_directory_rule_target` (`tenant_id`,`target_selector_id`),
  CONSTRAINT `fk_iam_directory_rule_policy` FOREIGN KEY (`tenant_id`) REFERENCES `iam_directory_policy` (`tenant_id`),
  CONSTRAINT `fk_iam_directory_rule_target` FOREIGN KEY (`tenant_id`, `target_selector_id`) REFERENCES `iam_policy_selector` (`tenant_id`, `id`),
  CONSTRAINT `fk_iam_directory_rule_viewer` FOREIGN KEY (`tenant_id`, `viewer_selector_id`) REFERENCES `iam_policy_selector` (`tenant_id`, `id`),
  CONSTRAINT `ck_iam_directory_effect` CHECK ((`effect` in (_utf8mb4'ALLOW',_utf8mb4'DENY')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_directory_rule
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_field_policy
-- ----------------------------
DROP TABLE IF EXISTS `iam_field_policy`;
CREATE TABLE `iam_field_policy` (
  `tenant_id` bigint unsigned NOT NULL,
  `default_revision_id` bigint unsigned NOT NULL,
  `default_kind` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'FIELD',
  `version` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`tenant_id`),
  KEY `fk_iam_field_default` (`default_revision_id`,`default_kind`),
  CONSTRAINT `fk_iam_field_default` FOREIGN KEY (`default_revision_id`, `default_kind`) REFERENCES `iam_default_policy_revision` (`id`, `kind`),
  CONSTRAINT `fk_iam_field_policy_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `iam_tenant` (`id`),
  CONSTRAINT `ck_iam_field_default_kind` CHECK ((`default_kind` = _utf8mb4'FIELD'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_field_policy
-- ----------------------------
BEGIN;
INSERT INTO `iam_field_policy` (`tenant_id`, `default_revision_id`, `default_kind`, `version`) VALUES (1000001, 150002, 'FIELD', 0);
INSERT INTO `iam_field_policy` (`tenant_id`, `default_revision_id`, `default_kind`, `version`) VALUES (1000007, 150002, 'FIELD', 0);
INSERT INTO `iam_field_policy` (`tenant_id`, `default_revision_id`, `default_kind`, `version`) VALUES (1000246, 150002, 'FIELD', 0);
INSERT INTO `iam_field_policy` (`tenant_id`, `default_revision_id`, `default_kind`, `version`) VALUES (1000252, 150002, 'FIELD', 0);
COMMIT;

-- ----------------------------
-- Table structure for iam_field_rule
-- ----------------------------
DROP TABLE IF EXISTS `iam_field_rule`;
CREATE TABLE `iam_field_rule` (
  `id` bigint unsigned NOT NULL,
  `tenant_id` bigint unsigned NOT NULL,
  `scenario` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `field_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `viewer_selector_id` bigint unsigned NOT NULL,
  `target_scope` json NOT NULL,
  `scope_bindings` json NOT NULL,
  `visibility` varchar(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `editable` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_iam_field_rule_match` (`tenant_id`,`scenario`,`field_key`,`id`),
  KEY `fk_iam_field_rule_viewer` (`tenant_id`,`viewer_selector_id`),
  CONSTRAINT `fk_iam_field_rule_policy` FOREIGN KEY (`tenant_id`) REFERENCES `iam_field_policy` (`tenant_id`),
  CONSTRAINT `fk_iam_field_rule_viewer` FOREIGN KEY (`tenant_id`, `viewer_selector_id`) REFERENCES `iam_policy_selector` (`tenant_id`, `id`),
  CONSTRAINT `ck_iam_field_rule_bindings` CHECK ((json_type(`scope_bindings`) = _utf8mb4'OBJECT')),
  CONSTRAINT `ck_iam_field_rule_editable` CHECK (((`editable` in (0,1)) and ((`editable` = 0) or (`visibility` = _utf8mb4'FULL')))),
  CONSTRAINT `ck_iam_field_rule_scenario` CHECK ((`scenario` in (_utf8mb4'MANAGEMENT',_utf8mb4'DIRECTORY'))),
  CONSTRAINT `ck_iam_field_rule_scope` CHECK ((json_type(`target_scope`) = _utf8mb4'ARRAY')),
  CONSTRAINT `ck_iam_field_rule_visibility` CHECK ((`visibility` in (_utf8mb4'HIDDEN',_utf8mb4'MASKED',_utf8mb4'FULL')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_field_rule
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_member_department
-- ----------------------------
DROP TABLE IF EXISTS `iam_member_department`;
CREATE TABLE `iam_member_department` (
  `tenant_id` bigint unsigned NOT NULL,
  `member_id` bigint unsigned NOT NULL,
  `department_id` bigint unsigned NOT NULL,
  `is_primary` tinyint(1) NOT NULL DEFAULT '0',
  `primary_member_id` bigint unsigned GENERATED ALWAYS AS ((case when (`is_primary` = 1) then `member_id` else NULL end)) STORED,
  PRIMARY KEY (`tenant_id`,`member_id`,`department_id`),
  UNIQUE KEY `uk_iam_member_primary_department` (`tenant_id`,`primary_member_id`),
  KEY `idx_iam_department_members` (`tenant_id`,`department_id`,`member_id`),
  CONSTRAINT `fk_iam_member_department_department` FOREIGN KEY (`tenant_id`, `department_id`) REFERENCES `iam_department` (`tenant_id`, `id`),
  CONSTRAINT `fk_iam_member_department_member` FOREIGN KEY (`tenant_id`, `member_id`) REFERENCES `iam_tenant_member` (`tenant_id`, `id`),
  CONSTRAINT `ck_iam_member_department_primary` CHECK ((`is_primary` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_member_department
-- ----------------------------
BEGIN;
INSERT INTO `iam_member_department` (`tenant_id`, `member_id`, `department_id`, `is_primary`) VALUES (1000001, 1000002, 1000003, 1);
INSERT INTO `iam_member_department` (`tenant_id`, `member_id`, `department_id`, `is_primary`) VALUES (1000001, 1000012, 1000003, 1);
INSERT INTO `iam_member_department` (`tenant_id`, `member_id`, `department_id`, `is_primary`) VALUES (1000001, 1000027, 1000003, 1);
INSERT INTO `iam_member_department` (`tenant_id`, `member_id`, `department_id`, `is_primary`) VALUES (1000001, 1000035, 1000003, 1);
INSERT INTO `iam_member_department` (`tenant_id`, `member_id`, `department_id`, `is_primary`) VALUES (1000001, 1000066, 1000003, 1);
INSERT INTO `iam_member_department` (`tenant_id`, `member_id`, `department_id`, `is_primary`) VALUES (1000007, 1000008, 1000009, 1);
INSERT INTO `iam_member_department` (`tenant_id`, `member_id`, `department_id`, `is_primary`) VALUES (1000246, 1000247, 1000248, 1);
INSERT INTO `iam_member_department` (`tenant_id`, `member_id`, `department_id`, `is_primary`) VALUES (1000252, 1000253, 1000254, 1);
COMMIT;

-- ----------------------------
-- Table structure for iam_member_export
-- ----------------------------
DROP TABLE IF EXISTS `iam_member_export`;
CREATE TABLE `iam_member_export` (
  `id` bigint unsigned NOT NULL,
  `tenant_id` bigint unsigned NOT NULL,
  `actor_member_id` bigint unsigned NOT NULL,
  `tenant_version` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `status` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `member_ids` json DEFAULT NULL,
  `failure_reason` varchar(64) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `expires_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_iam_member_export_tenant_status` (`tenant_id`,`status`,`expires_at`),
  CONSTRAINT `ck_iam_member_export_ids` CHECK (((`member_ids` is null) or (json_type(`member_ids`) = _utf8mb4'ARRAY'))),
  CONSTRAINT `ck_iam_member_export_status` CHECK ((`status` in (_utf8mb4'PENDING',_utf8mb4'RUNNING',_utf8mb4'SUCCEEDED',_utf8mb4'FAILED',_utf8mb4'EXPIRED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_member_export
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_menu
-- ----------------------------
DROP TABLE IF EXISTS `iam_menu`;
CREATE TABLE `iam_menu` (
  `id` bigint unsigned NOT NULL,
  `application_id` bigint unsigned NOT NULL,
  `parent_id` bigint unsigned DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `path` varchar(512) DEFAULT NULL,
  `view_path` varchar(256) DEFAULT NULL,
  `route_name` varchar(128) DEFAULT NULL,
  `icon` varchar(512) DEFAULT NULL,
  `kind` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `match_mode` varchar(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ANY',
  `access_mode` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTION',
  `sort_order` int NOT NULL DEFAULT '0',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_menu_application` (`application_id`,`id`),
  KEY `idx_iam_menu_parent` (`application_id`,`parent_id`,`sort_order`,`id`),
  CONSTRAINT `fk_iam_menu_application` FOREIGN KEY (`application_id`) REFERENCES `iam_application` (`id`),
  CONSTRAINT `fk_iam_menu_parent` FOREIGN KEY (`application_id`, `parent_id`) REFERENCES `iam_menu` (`application_id`, `id`),
  CONSTRAINT `ck_iam_menu_access` CHECK ((`access_mode` in (_utf8mb4'OPEN',_utf8mb4'ACTION'))),
  CONSTRAINT `ck_iam_menu_enabled` CHECK ((`enabled` in (0,1))),
  CONSTRAINT `ck_iam_menu_kind` CHECK ((`kind` in (_utf8mb4'DIRECTORY',_utf8mb4'PAGE'))),
  CONSTRAINT `ck_iam_menu_match` CHECK ((`match_mode` in (_utf8mb4'ANY',_utf8mb4'ALL'))),
  CONSTRAINT `ck_iam_menu_parent` CHECK (((`parent_id` is null) or (`parent_id` <> `id`)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_menu
-- ----------------------------
BEGIN;
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130001, 100001, NULL, '组织与租户', '/platform/iam/tenant', 'layout.main', 'platform.iam.tenant', NULL, 'DIRECTORY', 'ANY', 'ACTION', 1, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130002, 100001, 130001, '租户管理', '/platform/iam/tenants', 'platform.iam.tenants', 'platform.iam.tenants', NULL, 'PAGE', 'ANY', 'ACTION', 1, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130003, 100001, NULL, '应用与配置', '/platform/iam/config', 'layout.main', 'platform.iam.config', NULL, 'DIRECTORY', 'ANY', 'ACTION', 2, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130004, 100001, 130003, '应用目录', '/platform/iam/applications', 'platform.iam.applications', 'platform.iam.applications', NULL, 'PAGE', 'ANY', 'ACTION', 1, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130005, 100001, 130003, '套餐', '/platform/iam/plans', 'platform.iam.plans', 'platform.iam.plans', NULL, 'PAGE', 'ANY', 'ACTION', 2, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130006, 100001, 130003, '共享角色', '/platform/iam/shared/roles', 'platform.iam.shared.roles', 'platform.iam.shared.roles', NULL, 'PAGE', 'ANY', 'ACTION', 3, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130007, 100001, NULL, '平台管理', '/platform/iam/manage', 'layout.main', 'platform.iam.manage', NULL, 'DIRECTORY', 'ANY', 'ACTION', 3, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130008, 100001, 130007, '平台人员', '/platform/iam/personnel', 'platform.iam.personnel', 'platform.iam.personnel', NULL, 'PAGE', 'ANY', 'ACTION', 2, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130009, 100001, 130007, '角色与授权', '/platform/iam/authorization', 'platform.iam.authorization', 'platform.iam.authorization', NULL, 'PAGE', 'ANY', 'ACTION', 3, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130025, 100001, 130007, '全局账号', '/platform/iam/accounts', 'platform.iam.accounts', 'platform.iam.accounts', NULL, 'PAGE', 'ANY', 'ACTION', 1, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130010, 100001, NULL, '安全与运维', '/platform/iam/security', 'layout.main', 'platform.iam.security', NULL, 'DIRECTORY', 'ANY', 'ACTION', 4, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130011, 100001, 130010, '事件与审计', '/security/iam/authorization/audit', 'security.iam.authorization.audit', 'security.iam.authorization.audit', NULL, 'PAGE', 'ANY', 'ACTION', 1, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130012, 100002, NULL, '组织管理', '/org/iam/organization', 'layout.main', 'org.iam.organization', NULL, 'DIRECTORY', 'ANY', 'ACTION', 1, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130013, 100002, 130012, '成员与部门', '/org/iam/members', 'org.iam.members', 'org.iam.members', NULL, 'PAGE', 'ANY', 'ACTION', 1, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130014, 100002, 130012, '用户组', '/org/iam/groups', 'org.iam.groups', 'org.iam.groups', NULL, 'PAGE', 'ANY', 'ACTION', 2, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130015, 100002, 130012, '组织设置', '/org/iam/settings', 'org.iam.settings', 'org.iam.settings', NULL, 'PAGE', 'ANY', 'ACTION', 3, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130016, 100002, NULL, '权限与应用', '/org/iam/authorization/root', 'layout.main', 'org.iam.authorization.root', NULL, 'DIRECTORY', 'ANY', 'ACTION', 2, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130017, 100002, 130016, '角色与授权', '/org/iam/authorization', 'org.iam.authorization', 'org.iam.authorization', NULL, 'PAGE', 'ANY', 'ACTION', 1, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130018, 100002, 130016, '应用管理', '/org/iam/applications', 'org.iam.applications', 'org.iam.applications', NULL, 'PAGE', 'ANY', 'ACTION', 2, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130019, 100002, NULL, '安全与合规', '/org/iam/compliance', 'layout.main', 'org.iam.compliance', NULL, 'DIRECTORY', 'ANY', 'ACTION', 3, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130020, 100002, 130019, '成员权限', '/security/iam/member/permissions', 'security.iam.member.permissions', 'security.iam.member.permissions', NULL, 'PAGE', 'ANY', 'ACTION', 1, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130021, 100002, 130019, '审计', '/security/iam/authorization/audit', 'security.iam.authorization.audit', 'security.iam.authorization.audit', NULL, 'PAGE', 'ANY', 'ACTION', 2, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130022, 100002, NULL, '工作台', '/org/iam/workspace', 'layout.main', 'org.iam.workspace', NULL, 'DIRECTORY', 'ANY', 'ACTION', 4, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130023, 100002, 130022, '工作台', '/org/iam/workbench', 'org.iam.workbench', 'org.iam.workbench', NULL, 'PAGE', 'ANY', 'ACTION', 1, 1, 0);
INSERT INTO `iam_menu` (`id`, `application_id`, `parent_id`, `name`, `path`, `view_path`, `route_name`, `icon`, `kind`, `match_mode`, `access_mode`, `sort_order`, `enabled`, `version`) VALUES (130024, 100002, 130022, '通讯录', '/org/iam/directory', 'org.iam.directory', 'org.iam.directory', NULL, 'PAGE', 'ANY', 'ACTION', 2, 1, 0);
COMMIT;

-- ----------------------------
-- Table structure for iam_menu_action
-- ----------------------------
DROP TABLE IF EXISTS `iam_menu_action`;
CREATE TABLE `iam_menu_action` (
  `application_id` bigint unsigned NOT NULL,
  `menu_id` bigint unsigned NOT NULL,
  `action_id` bigint unsigned NOT NULL,
  PRIMARY KEY (`application_id`,`menu_id`,`action_id`),
  KEY `idx_iam_action_menus` (`application_id`,`action_id`,`menu_id`),
  CONSTRAINT `fk_iam_menu_action_action` FOREIGN KEY (`application_id`, `action_id`) REFERENCES `iam_action` (`application_id`, `id`),
  CONSTRAINT `fk_iam_menu_action_menu` FOREIGN KEY (`application_id`, `menu_id`) REFERENCES `iam_menu` (`application_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_menu_action
-- ----------------------------
BEGIN;
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130001, 120098);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130002, 120098);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130003, 120018);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130003, 120066);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130003, 120090);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130004, 120018);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130005, 120066);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130006, 120090);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130007, 120007);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130007, 120023);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130007, 120034);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130007, 120057);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130007, 120076);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130008, 120057);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130025, 120007);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130009, 120023);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130009, 120034);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130009, 120076);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130010, 120025);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100001, 130011, 120025);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130012, 120116);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130012, 120126);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130012, 120131);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130012, 120144);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130013, 120116);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130013, 120131);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130014, 120126);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130015, 120144);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130016, 120100);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130016, 120103);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130016, 120112);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130016, 120140);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130017, 120103);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130017, 120112);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130017, 120140);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130018, 120100);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130019, 120107);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130019, 120119);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130019, 120121);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130020, 120119);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130020, 120121);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130021, 120107);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130022, 120100);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130022, 120118);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130023, 120100);
INSERT INTO `iam_menu_action` (`application_id`, `menu_id`, `action_id`) VALUES (100002, 130024, 120118);
COMMIT;

-- ----------------------------
-- Table structure for iam_migration_batch
-- ----------------------------
DROP TABLE IF EXISTS `iam_migration_batch`;
CREATE TABLE `iam_migration_batch` (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `source_fingerprint` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `target_identifier` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `rules_version` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `status` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'NEW',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `verified_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_iam_batch_status` CHECK ((`status` in (_utf8mb4'NEW',_utf8mb4'PREFLIGHTED',_utf8mb4'IMPORTED',_utf8mb4'VERIFIED',_utf8mb4'FAILED'))),
  CONSTRAINT `ck_iam_batch_verified` CHECK ((((`status` = _utf8mb4'VERIFIED') and (`verified_at` is not null)) or ((`status` <> _utf8mb4'VERIFIED') and (`verified_at` is null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_migration_batch
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_migration_issue
-- ----------------------------
DROP TABLE IF EXISTS `iam_migration_issue`;
CREATE TABLE `iam_migration_issue` (
  `id` bigint unsigned NOT NULL,
  `batch_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `issue_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `source_table` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `source_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `safe_description` varchar(1024) NOT NULL,
  `disposition` varchar(16) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL,
  `resolved_by` varchar(128) DEFAULT NULL,
  `resolution_reason` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_iam_issue_unresolved` (`batch_id`,`disposition`,`id`),
  CONSTRAINT `fk_iam_issue_batch` FOREIGN KEY (`batch_id`) REFERENCES `iam_migration_batch` (`id`),
  CONSTRAINT `ck_iam_issue_disposition` CHECK (((`disposition` is null) or (`disposition` in (_utf8mb4'KEEP_MAPPED',_utf8mb4'NARROW',_utf8mb4'RECONFIGURE',_utf8mb4'ARCHIVE',_utf8mb4'SKIP')))),
  CONSTRAINT `ck_iam_issue_resolution` CHECK ((((`disposition` is null) and (`resolved_by` is null) and (`resolution_reason` is null)) or ((`disposition` is not null) and (`resolved_by` is not null) and (`resolution_reason` is not null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_migration_issue
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_migration_mapping
-- ----------------------------
DROP TABLE IF EXISTS `iam_migration_mapping`;
CREATE TABLE `iam_migration_mapping` (
  `batch_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `source_table` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `source_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `target_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `target_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `disposition` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `actor` varchar(128) NOT NULL,
  `reason` varchar(1024) NOT NULL,
  PRIMARY KEY (`batch_id`,`source_table`,`source_id`,`target_type`,`target_id`),
  CONSTRAINT `fk_iam_mapping_batch` FOREIGN KEY (`batch_id`) REFERENCES `iam_migration_batch` (`id`),
  CONSTRAINT `ck_iam_mapping_disposition` CHECK ((`disposition` in (_utf8mb4'KEEP_MAPPED',_utf8mb4'NARROW',_utf8mb4'RECONFIGURE',_utf8mb4'ARCHIVE',_utf8mb4'SKIP')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_migration_mapping
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_plan
-- ----------------------------
DROP TABLE IF EXISTS `iam_plan`;
CREATE TABLE `iam_plan` (
  `id` bigint unsigned NOT NULL,
  `name` varchar(128) NOT NULL,
  `description` varchar(512) DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_iam_plan_enabled` CHECK ((`enabled` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_plan
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_plan_application
-- ----------------------------
DROP TABLE IF EXISTS `iam_plan_application`;
CREATE TABLE `iam_plan_application` (
  `plan_id` bigint unsigned NOT NULL,
  `application_id` bigint unsigned NOT NULL,
  PRIMARY KEY (`plan_id`,`application_id`),
  KEY `fk_iam_plan_application_app` (`application_id`),
  CONSTRAINT `fk_iam_plan_application_app` FOREIGN KEY (`application_id`) REFERENCES `iam_application` (`id`),
  CONSTRAINT `fk_iam_plan_application_plan` FOREIGN KEY (`plan_id`) REFERENCES `iam_plan` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_plan_application
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_platform_group
-- ----------------------------
DROP TABLE IF EXISTS `iam_platform_group`;
CREATE TABLE `iam_platform_group` (
  `id` bigint unsigned NOT NULL,
  `name` varchar(128) NOT NULL,
  `description` varchar(512) DEFAULT NULL,
  `version` bigint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_iam_platform_group_id` CHECK ((`id` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_platform_group
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_platform_group_member
-- ----------------------------
DROP TABLE IF EXISTS `iam_platform_group_member`;
CREATE TABLE `iam_platform_group_member` (
  `group_id` bigint unsigned NOT NULL,
  `member_id` bigint unsigned NOT NULL,
  PRIMARY KEY (`group_id`,`member_id`),
  KEY `idx_iam_platform_member_groups` (`member_id`,`group_id`),
  CONSTRAINT `fk_iam_platform_group_entry_group` FOREIGN KEY (`group_id`) REFERENCES `iam_platform_group` (`id`),
  CONSTRAINT `fk_iam_platform_group_entry_member` FOREIGN KEY (`member_id`) REFERENCES `iam_platform_member` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_platform_group_member
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_platform_member
-- ----------------------------
DROP TABLE IF EXISTS `iam_platform_member`;
CREATE TABLE `iam_platform_member` (
  `id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `display_name` varchar(128) NOT NULL,
  `avatar` varchar(512) DEFAULT NULL,
  `status` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE',
  `version` bigint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_platform_member_account` (`account_id`),
  KEY `idx_iam_platform_member_status` (`status`,`id`),
  CONSTRAINT `fk_iam_platform_member_account` FOREIGN KEY (`account_id`) REFERENCES `iam_account` (`id`),
  CONSTRAINT `ck_iam_platform_member_id` CHECK ((`id` > 0)),
  CONSTRAINT `ck_iam_platform_member_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'SUSPENDED',_utf8mb4'REMOVED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_platform_member
-- ----------------------------
BEGIN;
INSERT INTO `iam_platform_member` (`id`, `account_id`, `display_name`, `avatar`, `status`, `version`, `created_at`, `updated_at`) VALUES (910001, 900001, '平台治理', NULL, 'ACTIVE', 0, '2026-09-16 02:35:42.239533', '2026-09-16 02:35:42.239533');
INSERT INTO `iam_platform_member` (`id`, `account_id`, `display_name`, `avatar`, `status`, `version`, `created_at`, `updated_at`) VALUES (1000025, 1000022, 'A01b 平台成员', NULL, 'SUSPENDED', 1, '2026-09-16 07:31:49.056101', '2026-09-16 07:32:39.932078');
COMMIT;

-- ----------------------------
-- Table structure for iam_policy_selector
-- ----------------------------
DROP TABLE IF EXISTS `iam_policy_selector`;
CREATE TABLE `iam_policy_selector` (
  `id` bigint unsigned NOT NULL,
  `tenant_id` bigint unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_selector_tenant` (`tenant_id`,`id`),
  CONSTRAINT `fk_iam_selector_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `iam_tenant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_policy_selector
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_policy_selector_department
-- ----------------------------
DROP TABLE IF EXISTS `iam_policy_selector_department`;
CREATE TABLE `iam_policy_selector_department` (
  `tenant_id` bigint unsigned NOT NULL,
  `selector_id` bigint unsigned NOT NULL,
  `department_id` bigint unsigned NOT NULL,
  `include_descendants` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`tenant_id`,`selector_id`,`department_id`),
  KEY `fk_iam_selector_dept_dept` (`tenant_id`,`department_id`),
  CONSTRAINT `fk_iam_selector_dept_dept` FOREIGN KEY (`tenant_id`, `department_id`) REFERENCES `iam_department` (`tenant_id`, `id`),
  CONSTRAINT `fk_iam_selector_dept_selector` FOREIGN KEY (`tenant_id`, `selector_id`) REFERENCES `iam_policy_selector` (`tenant_id`, `id`),
  CONSTRAINT `ck_iam_selector_descendants` CHECK ((`include_descendants` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_policy_selector_department
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_policy_selector_member
-- ----------------------------
DROP TABLE IF EXISTS `iam_policy_selector_member`;
CREATE TABLE `iam_policy_selector_member` (
  `tenant_id` bigint unsigned NOT NULL,
  `selector_id` bigint unsigned NOT NULL,
  `member_id` bigint unsigned NOT NULL,
  PRIMARY KEY (`tenant_id`,`selector_id`,`member_id`),
  KEY `fk_iam_selector_member_member` (`tenant_id`,`member_id`),
  CONSTRAINT `fk_iam_selector_member_member` FOREIGN KEY (`tenant_id`, `member_id`) REFERENCES `iam_tenant_member` (`tenant_id`, `id`),
  CONSTRAINT `fk_iam_selector_member_selector` FOREIGN KEY (`tenant_id`, `selector_id`) REFERENCES `iam_policy_selector` (`tenant_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_policy_selector_member
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_resource
-- ----------------------------
DROP TABLE IF EXISTS `iam_resource`;
CREATE TABLE `iam_resource` (
  `id` bigint unsigned NOT NULL,
  `application_id` bigint unsigned NOT NULL,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `name` varchar(128) NOT NULL,
  `scope_capabilities` json NOT NULL,
  `field_capabilities` json NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_resource_code` (`application_id`,`code`),
  UNIQUE KEY `uk_iam_resource_application` (`application_id`,`id`),
  CONSTRAINT `fk_iam_resource_application` FOREIGN KEY (`application_id`) REFERENCES `iam_application` (`id`),
  CONSTRAINT `ck_iam_resource_enabled` CHECK ((`enabled` in (0,1))),
  CONSTRAINT `ck_iam_resource_fields` CHECK ((json_type(`field_capabilities`) = _utf8mb4'ARRAY')),
  CONSTRAINT `ck_iam_resource_scopes` CHECK ((json_type(`scope_capabilities`) = _utf8mb4'ARRAY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_resource
-- ----------------------------
BEGIN;
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110001, 100001, 'account', '全局账号', '[\"ALL\", \"OBJECT_SET\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110002, 100001, 'action', '操作', '[\"ALL\", \"OBJECT_SET\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110003, 100001, 'application', '应用', '[\"ALL\", \"OBJECT_SET\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110004, 100001, 'assignment', '角色授权', '[\"ALL\", \"OBJECT_SET\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110005, 100001, 'audit', '审计记录', '[\"ALL\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110006, 100001, 'authorization', '授权诊断', '[\"ALL\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110007, 100001, 'credential-policy', '凭证策略', '[\"ALL\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110008, 100001, 'delegation', '授权委派', '[\"ALL\", \"OBJECT_SET\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110009, 100001, 'dictionary', '字典', '[\"ALL\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110010, 100001, 'entitlement', '应用开通', '[\"ALL\", \"OBJECT_SET\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110011, 100001, 'group', '用户组', '[\"ALL\", \"OBJECT_SET\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110012, 100001, 'id-allocation', '发号', '[\"ALL\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110013, 100001, 'lockout-policy', '账号锁定策略', '[\"ALL\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110014, 100001, 'login-failure-policy', '登录失败防护', '[\"ALL\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110015, 100001, 'member', '成员', '[\"ALL\", \"SELF\", \"MEMBER_DEPARTMENTS\", \"MANAGED_DEPARTMENTS\", \"OBJECT_SET\"]', '[{\"key\": \"displayName\", \"label\": \"显示名\", \"editable\": true, \"sortable\": true, \"filterable\": true, \"visibilities\": [\"HIDDEN\", \"MASKED\", \"FULL\"]}, {\"key\": \"avatar\", \"label\": \"头像\", \"editable\": true, \"sortable\": false, \"filterable\": false, \"visibilities\": [\"HIDDEN\", \"FULL\"]}, {\"key\": \"phone\", \"label\": \"手机号\", \"editable\": true, \"sortable\": false, \"filterable\": true, \"visibilities\": [\"HIDDEN\", \"MASKED\", \"FULL\"]}, {\"key\": \"email\", \"label\": \"邮箱\", \"editable\": true, \"sortable\": false, \"filterable\": true, \"visibilities\": [\"HIDDEN\", \"MASKED\", \"FULL\"]}]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110016, 100001, 'menu', '菜单', '[\"ALL\", \"OBJECT_SET\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110017, 100001, 'plan', '套餐', '[\"ALL\", \"OBJECT_SET\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110018, 100001, 'resource', '资源', '[\"ALL\", \"OBJECT_SET\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110019, 100001, 'role', '角色', '[\"ALL\", \"OBJECT_SET\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110020, 100001, 'security-policy', '安全策略', '[\"ALL\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110021, 100001, 'session', '在线会话', '[\"ALL\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110022, 100001, 'session-policy', '会话并发策略', '[\"ALL\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110023, 100001, 'shared-role', '共享角色', '[\"ALL\", \"OBJECT_SET\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110024, 100001, 'social-config', '社会化登录配置', '[\"ALL\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110025, 100001, 'tenant', '组织', '[\"ALL\", \"OBJECT_SET\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110026, 100002, 'application', '应用', '[\"ALL\", \"OBJECT_SET\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110027, 100002, 'assignment', '角色授权', '[\"ALL\", \"OBJECT_SET\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110028, 100002, 'audience', '应用可用人群', '[\"ALL\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110029, 100002, 'audit', '审计记录', '[\"ALL\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110030, 100002, 'authorization', '授权诊断', '[\"ALL\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110031, 100002, 'delegation', '授权委派', '[\"ALL\", \"OBJECT_SET\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110032, 100002, 'department', '部门', '[\"ALL\", \"MANAGED_DEPARTMENTS\", \"OBJECT_SET\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110033, 100002, 'directory', '通讯录', '[\"ALL\", \"SELF\", \"MEMBER_DEPARTMENTS\", \"MANAGED_DEPARTMENTS\", \"OBJECT_SET\"]', '[{\"key\": \"displayName\", \"label\": \"显示名\", \"editable\": true, \"sortable\": true, \"filterable\": true, \"visibilities\": [\"HIDDEN\", \"MASKED\", \"FULL\"]}, {\"key\": \"avatar\", \"label\": \"头像\", \"editable\": true, \"sortable\": false, \"filterable\": false, \"visibilities\": [\"HIDDEN\", \"FULL\"]}, {\"key\": \"phone\", \"label\": \"手机号\", \"editable\": true, \"sortable\": false, \"filterable\": true, \"visibilities\": [\"HIDDEN\", \"MASKED\", \"FULL\"]}, {\"key\": \"email\", \"label\": \"邮箱\", \"editable\": true, \"sortable\": false, \"filterable\": true, \"visibilities\": [\"HIDDEN\", \"MASKED\", \"FULL\"]}]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110034, 100002, 'directory-policy', '通讯录可见范围策略', '[\"ALL\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110035, 100002, 'field-policy', '成员字段策略', '[\"ALL\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110036, 100002, 'group', '用户组', '[\"ALL\", \"OBJECT_SET\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110037, 100002, 'member', '成员', '[\"ALL\", \"SELF\", \"MEMBER_DEPARTMENTS\", \"MANAGED_DEPARTMENTS\", \"OBJECT_SET\"]', '[{\"key\": \"displayName\", \"label\": \"显示名\", \"editable\": true, \"sortable\": true, \"filterable\": true, \"visibilities\": [\"HIDDEN\", \"MASKED\", \"FULL\"]}, {\"key\": \"avatar\", \"label\": \"头像\", \"editable\": true, \"sortable\": false, \"filterable\": false, \"visibilities\": [\"HIDDEN\", \"FULL\"]}, {\"key\": \"phone\", \"label\": \"手机号\", \"editable\": true, \"sortable\": false, \"filterable\": true, \"visibilities\": [\"HIDDEN\", \"MASKED\", \"FULL\"]}, {\"key\": \"email\", \"label\": \"邮箱\", \"editable\": true, \"sortable\": false, \"filterable\": true, \"visibilities\": [\"HIDDEN\", \"MASKED\", \"FULL\"]}]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110038, 100002, 'policy', '策略预览', '[\"ALL\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110039, 100002, 'role', '角色', '[\"ALL\", \"OBJECT_SET\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (110040, 100002, 'settings', '组织设置', '[\"ALL\"]', '[]', 1, 0);
INSERT INTO `iam_resource` (`id`, `application_id`, `code`, `name`, `scope_capabilities`, `field_capabilities`, `enabled`, `version`) VALUES (1000039, 1000037, 'item', '业务对象', '[\"ALL\"]', '[]', 1, 0);
COMMIT;

-- ----------------------------
-- Table structure for iam_role_assignment
-- ----------------------------
DROP TABLE IF EXISTS `iam_role_assignment`;
CREATE TABLE `iam_role_assignment` (
  `id` bigint unsigned NOT NULL,
  `domain` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `tenant_id` bigint unsigned DEFAULT NULL,
  `tenant_key` bigint unsigned GENERATED ALWAYS AS (coalesce(`tenant_id`,0)) STORED,
  `subject_type` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `platform_member_id` bigint unsigned DEFAULT NULL,
  `platform_group_id` bigint unsigned DEFAULT NULL,
  `tenant_member_id` bigint unsigned DEFAULT NULL,
  `tenant_group_id` bigint unsigned DEFAULT NULL,
  `revision_id` bigint unsigned NOT NULL,
  `revision_kind` varchar(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `scope_bindings` json NOT NULL,
  `delegation_grant_id` bigint unsigned DEFAULT NULL,
  `valid_from` datetime(6) NOT NULL,
  `valid_until` datetime(6) DEFAULT NULL,
  `status` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE',
  `source` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `version` bigint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_iam_assignment_platform_member` (`platform_member_id`,`status`,`valid_until`),
  KEY `idx_iam_assignment_platform_group` (`platform_group_id`,`status`,`valid_until`),
  KEY `idx_iam_assignment_tenant_member` (`tenant_id`,`tenant_member_id`,`status`,`valid_until`),
  KEY `idx_iam_assignment_tenant_group` (`tenant_id`,`tenant_group_id`,`status`,`valid_until`),
  KEY `idx_iam_assignment_delegation` (`delegation_grant_id`,`status`),
  KEY `fk_iam_assignment_revision` (`revision_id`,`revision_kind`),
  KEY `fk_iam_assignment_delegation` (`domain`,`tenant_key`,`delegation_grant_id`),
  CONSTRAINT `fk_iam_assignment_delegation` FOREIGN KEY (`domain`, `tenant_key`, `delegation_grant_id`) REFERENCES `iam_delegation_grant` (`domain`, `tenant_key`, `id`),
  CONSTRAINT `fk_iam_assignment_platform_group` FOREIGN KEY (`platform_group_id`) REFERENCES `iam_platform_group` (`id`),
  CONSTRAINT `fk_iam_assignment_platform_member` FOREIGN KEY (`platform_member_id`) REFERENCES `iam_platform_member` (`id`),
  CONSTRAINT `fk_iam_assignment_revision` FOREIGN KEY (`revision_id`, `revision_kind`) REFERENCES `iam_role_revision` (`id`, `kind`),
  CONSTRAINT `fk_iam_assignment_tenant_group` FOREIGN KEY (`tenant_id`, `tenant_group_id`) REFERENCES `iam_tenant_group` (`tenant_id`, `id`),
  CONSTRAINT `fk_iam_assignment_tenant_member` FOREIGN KEY (`tenant_id`, `tenant_member_id`) REFERENCES `iam_tenant_member` (`tenant_id`, `id`),
  CONSTRAINT `ck_iam_assignment_bindings` CHECK ((json_type(`scope_bindings`) = _utf8mb4'OBJECT')),
  CONSTRAINT `ck_iam_assignment_interval` CHECK (((`valid_until` is null) or (`valid_from` < `valid_until`))),
  CONSTRAINT `ck_iam_assignment_source` CHECK ((`source` in (_utf8mb4'MANUAL',_utf8mb4'INITIALIZATION',_utf8mb4'MIGRATION'))),
  CONSTRAINT `ck_iam_assignment_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'REVOKED'))),
  CONSTRAINT `ck_iam_assignment_subject` CHECK ((((`domain` = _utf8mb4'PLATFORM') and (`tenant_id` is null) and (`tenant_member_id` is null) and (`tenant_group_id` is null) and (((`subject_type` = _utf8mb4'MEMBER') and (`platform_member_id` is not null) and (`platform_group_id` is null)) or ((`subject_type` = _utf8mb4'GROUP') and (`platform_group_id` is not null) and (`platform_member_id` is null)))) or ((`domain` = _utf8mb4'TENANT') and (`tenant_id` is not null) and (`platform_member_id` is null) and (`platform_group_id` is null) and (((`subject_type` = _utf8mb4'MEMBER') and (`tenant_member_id` is not null) and (`tenant_group_id` is null)) or ((`subject_type` = _utf8mb4'GROUP') and (`tenant_group_id` is not null) and (`tenant_member_id` is null))))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_role_assignment
-- ----------------------------
BEGIN;
INSERT INTO `iam_role_assignment` (`id`, `domain`, `tenant_id`, `subject_type`, `platform_member_id`, `platform_group_id`, `tenant_member_id`, `tenant_group_id`, `revision_id`, `revision_kind`, `scope_bindings`, `delegation_grant_id`, `valid_from`, `valid_until`, `status`, `source`, `version`, `created_at`) VALUES (940001, 'PLATFORM', NULL, 'MEMBER', 910001, NULL, NULL, NULL, 141001, 'SYSTEM', '{}', NULL, '2026-09-16 02:35:42.242087', NULL, 'ACTIVE', 'MANUAL', 0, '2026-09-16 02:35:42.242087');
INSERT INTO `iam_role_assignment` (`id`, `domain`, `tenant_id`, `subject_type`, `platform_member_id`, `platform_group_id`, `tenant_member_id`, `tenant_group_id`, `revision_id`, `revision_kind`, `scope_bindings`, `delegation_grant_id`, `valid_from`, `valid_until`, `status`, `source`, `version`, `created_at`) VALUES (1000004, 'TENANT', 1000001, 'MEMBER', NULL, NULL, 1000002, NULL, 141002, 'SYSTEM', '{}', NULL, '2026-09-16 05:54:05.586664', NULL, 'REVOKED', 'INITIALIZATION', 1, '2026-09-16 05:54:05.701587');
INSERT INTO `iam_role_assignment` (`id`, `domain`, `tenant_id`, `subject_type`, `platform_member_id`, `platform_group_id`, `tenant_member_id`, `tenant_group_id`, `revision_id`, `revision_kind`, `scope_bindings`, `delegation_grant_id`, `valid_from`, `valid_until`, `status`, `source`, `version`, `created_at`) VALUES (1000010, 'TENANT', 1000007, 'MEMBER', NULL, NULL, 1000008, NULL, 141002, 'SYSTEM', '{}', NULL, '2026-09-16 05:54:12.412468', NULL, 'ACTIVE', 'INITIALIZATION', 0, '2026-09-16 05:54:12.455984');
INSERT INTO `iam_role_assignment` (`id`, `domain`, `tenant_id`, `subject_type`, `platform_member_id`, `platform_group_id`, `tenant_member_id`, `tenant_group_id`, `revision_id`, `revision_kind`, `scope_bindings`, `delegation_grant_id`, `valid_from`, `valid_until`, `status`, `source`, `version`, `created_at`) VALUES (1000049, 'TENANT', 1000001, 'MEMBER', NULL, NULL, 1000035, NULL, 1000047, 'SHARED', '{}', NULL, '2026-09-16 08:10:24.761687', NULL, 'ACTIVE', 'MANUAL', 0, '2026-09-16 08:10:24.766815');
INSERT INTO `iam_role_assignment` (`id`, `domain`, `tenant_id`, `subject_type`, `platform_member_id`, `platform_group_id`, `tenant_member_id`, `tenant_group_id`, `revision_id`, `revision_kind`, `scope_bindings`, `delegation_grant_id`, `valid_from`, `valid_until`, `status`, `source`, `version`, `created_at`) VALUES (1000054, 'TENANT', 1000001, 'MEMBER', NULL, NULL, 1000035, NULL, 1000052, 'SHARED', '{}', NULL, '2026-09-16 08:11:12.582009', NULL, 'ACTIVE', 'MANUAL', 0, '2026-09-16 08:11:12.587783');
INSERT INTO `iam_role_assignment` (`id`, `domain`, `tenant_id`, `subject_type`, `platform_member_id`, `platform_group_id`, `tenant_member_id`, `tenant_group_id`, `revision_id`, `revision_kind`, `scope_bindings`, `delegation_grant_id`, `valid_from`, `valid_until`, `status`, `source`, `version`, `created_at`) VALUES (1000068, 'TENANT', 1000001, 'MEMBER', NULL, NULL, 1000002, NULL, 1000052, 'SHARED', '{}', NULL, '2026-09-16 08:11:49.661872', NULL, 'ACTIVE', 'MANUAL', 0, '2026-09-16 08:11:49.663496');
INSERT INTO `iam_role_assignment` (`id`, `domain`, `tenant_id`, `subject_type`, `platform_member_id`, `platform_group_id`, `tenant_member_id`, `tenant_group_id`, `revision_id`, `revision_kind`, `scope_bindings`, `delegation_grant_id`, `valid_from`, `valid_until`, `status`, `source`, `version`, `created_at`) VALUES (1000071, 'TENANT', 1000001, 'MEMBER', NULL, NULL, 1000066, NULL, 141002, 'SYSTEM', '{}', NULL, '2026-09-16 05:54:05.586664', NULL, 'ACTIVE', 'INITIALIZATION', 0, '2026-09-16 08:11:54.841247');
INSERT INTO `iam_role_assignment` (`id`, `domain`, `tenant_id`, `subject_type`, `platform_member_id`, `platform_group_id`, `tenant_member_id`, `tenant_group_id`, `revision_id`, `revision_kind`, `scope_bindings`, `delegation_grant_id`, `valid_from`, `valid_until`, `status`, `source`, `version`, `created_at`) VALUES (1000249, 'TENANT', 1000246, 'MEMBER', NULL, NULL, 1000247, NULL, 141002, 'SYSTEM', '{}', NULL, '2026-09-20 05:39:28.876479', NULL, 'ACTIVE', 'INITIALIZATION', 0, '2026-09-20 05:39:28.922222');
INSERT INTO `iam_role_assignment` (`id`, `domain`, `tenant_id`, `subject_type`, `platform_member_id`, `platform_group_id`, `tenant_member_id`, `tenant_group_id`, `revision_id`, `revision_kind`, `scope_bindings`, `delegation_grant_id`, `valid_from`, `valid_until`, `status`, `source`, `version`, `created_at`) VALUES (1000255, 'TENANT', 1000252, 'MEMBER', NULL, NULL, 1000253, NULL, 141002, 'SYSTEM', '{}', NULL, '2026-09-20 05:39:29.565967', NULL, 'ACTIVE', 'INITIALIZATION', 0, '2026-09-20 05:39:29.592237');
COMMIT;

-- ----------------------------
-- Table structure for iam_role_definition
-- ----------------------------
DROP TABLE IF EXISTS `iam_role_definition`;
CREATE TABLE `iam_role_definition` (
  `id` bigint unsigned NOT NULL,
  `domain` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `tenant_id` bigint unsigned DEFAULT NULL,
  `tenant_key` bigint unsigned GENERATED ALWAYS AS (coalesce(`tenant_id`,0)) STORED,
  `kind` varchar(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `name` varchar(128) NOT NULL,
  `description` varchar(512) DEFAULT NULL,
  `group_name` varchar(128) DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_role_kind` (`id`,`kind`),
  UNIQUE KEY `uk_iam_role_code` (`domain`,`tenant_key`,`code`),
  KEY `fk_iam_role_tenant` (`tenant_id`),
  CONSTRAINT `fk_iam_role_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `iam_tenant` (`id`),
  CONSTRAINT `ck_iam_role_domain` CHECK ((`domain` in (_utf8mb4'PLATFORM',_utf8mb4'TENANT'))),
  CONSTRAINT `ck_iam_role_enabled` CHECK ((`enabled` in (0,1))),
  CONSTRAINT `ck_iam_role_kind` CHECK ((((`kind` = _utf8mb4'SYSTEM') and (`tenant_id` is null)) or ((`kind` = _utf8mb4'SHARED') and (`domain` = _utf8mb4'TENANT') and (`tenant_id` is null)) or ((`kind` = _utf8mb4'PLATFORM_CUSTOM') and (`domain` = _utf8mb4'PLATFORM') and (`tenant_id` is null)) or ((`kind` = _utf8mb4'TENANT_CUSTOM') and (`domain` = _utf8mb4'TENANT') and (`tenant_id` is not null))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_role_definition
-- ----------------------------
BEGIN;
INSERT INTO `iam_role_definition` (`id`, `domain`, `tenant_id`, `kind`, `code`, `name`, `description`, `group_name`, `enabled`, `version`) VALUES (140001, 'PLATFORM', NULL, 'SYSTEM', 'platform-governance', '平台治理', '平台域全部治理操作', NULL, 1, 0);
INSERT INTO `iam_role_definition` (`id`, `domain`, `tenant_id`, `kind`, `code`, `name`, `description`, `group_name`, `enabled`, `version`) VALUES (140002, 'TENANT', NULL, 'SYSTEM', 'tenant-governance', '组织治理', '组织所有者的全部治理操作', NULL, 1, 0);
INSERT INTO `iam_role_definition` (`id`, `domain`, `tenant_id`, `kind`, `code`, `name`, `description`, `group_name`, `enabled`, `version`) VALUES (1000046, 'TENANT', NULL, 'SHARED', 'a02-shared-mu3ra4ey', 'A02 业务读取', '只含业务操作', '验收', 1, 0);
INSERT INTO `iam_role_definition` (`id`, `domain`, `tenant_id`, `kind`, `code`, `name`, `description`, `group_name`, `enabled`, `version`) VALUES (1000051, 'TENANT', NULL, 'SHARED', 'a04-shared-mu3ra4ey', 'A04 成员读写', 'read+update ALL', '验收', 1, 0);
INSERT INTO `iam_role_definition` (`id`, `domain`, `tenant_id`, `kind`, `code`, `name`, `description`, `group_name`, `enabled`, `version`) VALUES (1000056, 'TENANT', 1000001, 'TENANT_CUSTOM', 'a04-custom-mu3ra4ey', 'A04 只读定制', 'REMOVE member:update', '验收', 1, 2);
COMMIT;

-- ----------------------------
-- Table structure for iam_role_delta
-- ----------------------------
DROP TABLE IF EXISTS `iam_role_delta`;
CREATE TABLE `iam_role_delta` (
  `revision_id` bigint unsigned NOT NULL,
  `action_id` bigint unsigned NOT NULL,
  `operation` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `scopes` json NOT NULL,
  PRIMARY KEY (`revision_id`,`action_id`),
  KEY `fk_iam_delta_action` (`action_id`),
  CONSTRAINT `fk_iam_delta_action` FOREIGN KEY (`action_id`) REFERENCES `iam_action` (`id`),
  CONSTRAINT `fk_iam_delta_revision` FOREIGN KEY (`revision_id`) REFERENCES `iam_role_revision` (`id`),
  CONSTRAINT `ck_iam_delta_operation` CHECK ((`operation` in (_utf8mb4'ADD',_utf8mb4'REMOVE',_utf8mb4'REPLACE_SCOPE'))),
  CONSTRAINT `ck_iam_delta_scopes` CHECK (((json_type(`scopes`) = _utf8mb4'ARRAY') and ((`operation` <> _utf8mb4'REMOVE') or (json_length(`scopes`) = 0))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_role_delta
-- ----------------------------
BEGIN;
INSERT INTO `iam_role_delta` (`revision_id`, `action_id`, `operation`, `scopes`) VALUES (1000057, 120134, 'REMOVE', '[]');
INSERT INTO `iam_role_delta` (`revision_id`, `action_id`, `operation`, `scopes`) VALUES (1000059, 120130, 'ADD', '[{\"kind\": \"ALL\", \"parameterKey\": null, \"includeDescendants\": null}]');
INSERT INTO `iam_role_delta` (`revision_id`, `action_id`, `operation`, `scopes`) VALUES (1000059, 120131, 'REPLACE_SCOPE', '[{\"kind\": \"SELF\", \"parameterKey\": null, \"includeDescendants\": null}]');
INSERT INTO `iam_role_delta` (`revision_id`, `action_id`, `operation`, `scopes`) VALUES (1000059, 120134, 'REMOVE', '[]');
COMMIT;

-- ----------------------------
-- Table structure for iam_role_grant
-- ----------------------------
DROP TABLE IF EXISTS `iam_role_grant`;
CREATE TABLE `iam_role_grant` (
  `revision_id` bigint unsigned NOT NULL,
  `action_id` bigint unsigned NOT NULL,
  `scopes` json NOT NULL,
  PRIMARY KEY (`revision_id`,`action_id`),
  KEY `fk_iam_grant_action` (`action_id`),
  CONSTRAINT `fk_iam_grant_action` FOREIGN KEY (`action_id`) REFERENCES `iam_action` (`id`),
  CONSTRAINT `fk_iam_grant_revision` FOREIGN KEY (`revision_id`) REFERENCES `iam_role_revision` (`id`),
  CONSTRAINT `ck_iam_grant_scopes` CHECK ((json_type(`scopes`) = _utf8mb4'ARRAY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_role_grant
-- ----------------------------
BEGIN;
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120001, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120002, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120003, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120004, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120005, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120006, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120007, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120008, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120009, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120010, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120011, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120012, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120013, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120014, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120015, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120016, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120017, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120018, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120019, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120020, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120021, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120022, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120023, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120024, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120025, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120026, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120027, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120028, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120029, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120030, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120031, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120032, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120033, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120034, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120035, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120036, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120037, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120038, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120039, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120040, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120041, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120042, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120043, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120044, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120045, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120046, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120047, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120048, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120049, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120050, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120051, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120052, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120053, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120054, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120055, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120056, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120057, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120058, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120059, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120060, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120061, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120062, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120063, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120064, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120065, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120066, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120067, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120068, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120069, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120070, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120071, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120072, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120073, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120074, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120075, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120076, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120077, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120078, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120079, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120080, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120081, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120082, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120083, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120084, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120085, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120086, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120087, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120088, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120089, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120090, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120091, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120092, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120093, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120094, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120095, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120096, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120097, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120098, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141001, 120099, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120100, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120101, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120102, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120103, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120104, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120105, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120106, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120107, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120108, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120109, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120110, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120111, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120112, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120113, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120114, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120115, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120116, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120117, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120118, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120119, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120120, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120121, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120122, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120123, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120124, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120125, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120126, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120127, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120128, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120129, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120130, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120131, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120132, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120133, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120134, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120135, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120136, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120137, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120138, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120139, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120140, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120141, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120142, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120143, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120144, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (141002, 120145, '[{\"kind\": \"ALL\"}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (1000047, 1000041, '[{\"kind\": \"ALL\", \"parameterKey\": null, \"includeDescendants\": null}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (1000052, 120131, '[{\"kind\": \"ALL\", \"parameterKey\": null, \"includeDescendants\": null}]');
INSERT INTO `iam_role_grant` (`revision_id`, `action_id`, `scopes`) VALUES (1000052, 120134, '[{\"kind\": \"ALL\", \"parameterKey\": null, \"includeDescendants\": null}]');
COMMIT;

-- ----------------------------
-- Table structure for iam_role_parameter
-- ----------------------------
DROP TABLE IF EXISTS `iam_role_parameter`;
CREATE TABLE `iam_role_parameter` (
  `revision_id` bigint unsigned NOT NULL,
  `parameter_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `binding_kind` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  PRIMARY KEY (`revision_id`,`parameter_key`),
  CONSTRAINT `fk_iam_parameter_revision` FOREIGN KEY (`revision_id`) REFERENCES `iam_role_revision` (`id`),
  CONSTRAINT `ck_iam_parameter_kind` CHECK ((`binding_kind` in (_utf8mb4'DEPARTMENTS',_utf8mb4'OBJECTS')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_role_parameter
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_role_revision
-- ----------------------------
DROP TABLE IF EXISTS `iam_role_revision`;
CREATE TABLE `iam_role_revision` (
  `id` bigint unsigned NOT NULL,
  `role_id` bigint unsigned NOT NULL,
  `kind` varchar(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `revision` bigint unsigned NOT NULL,
  `base_revision_id` bigint unsigned DEFAULT NULL,
  `base_kind` varchar(24) CHARACTER SET ascii COLLATE ascii_bin GENERATED ALWAYS AS ((case when (`base_revision_id` is null) then NULL else _utf8mb4'SHARED' end)) STORED,
  `metadata_overrides` json NOT NULL,
  `published_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_revision_number` (`role_id`,`revision`),
  UNIQUE KEY `uk_iam_revision_kind` (`id`,`kind`),
  KEY `fk_iam_revision_role` (`role_id`,`kind`),
  KEY `fk_iam_revision_base` (`base_revision_id`,`base_kind`),
  CONSTRAINT `fk_iam_revision_base` FOREIGN KEY (`base_revision_id`, `base_kind`) REFERENCES `iam_role_revision` (`id`, `kind`),
  CONSTRAINT `fk_iam_revision_role` FOREIGN KEY (`role_id`, `kind`) REFERENCES `iam_role_definition` (`id`, `kind`),
  CONSTRAINT `ck_iam_revision_base` CHECK (((`base_revision_id` is null) or ((`kind` = _utf8mb4'TENANT_CUSTOM') and (`base_revision_id` <> `id`)))),
  CONSTRAINT `ck_iam_revision_metadata` CHECK ((json_type(`metadata_overrides`) = _utf8mb4'OBJECT')),
  CONSTRAINT `ck_iam_revision_number` CHECK ((`revision` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_role_revision
-- ----------------------------
BEGIN;
INSERT INTO `iam_role_revision` (`id`, `role_id`, `kind`, `revision`, `base_revision_id`, `metadata_overrides`, `published_at`) VALUES (141001, 140001, 'SYSTEM', 1, NULL, '{}', '2026-09-16 02:35:00.352140');
INSERT INTO `iam_role_revision` (`id`, `role_id`, `kind`, `revision`, `base_revision_id`, `metadata_overrides`, `published_at`) VALUES (141002, 140002, 'SYSTEM', 1, NULL, '{}', '2026-09-16 02:35:00.353844');
INSERT INTO `iam_role_revision` (`id`, `role_id`, `kind`, `revision`, `base_revision_id`, `metadata_overrides`, `published_at`) VALUES (1000047, 1000046, 'SHARED', 1, NULL, '{}', '2026-09-16 08:09:05.389195');
INSERT INTO `iam_role_revision` (`id`, `role_id`, `kind`, `revision`, `base_revision_id`, `metadata_overrides`, `published_at`) VALUES (1000052, 1000051, 'SHARED', 1, NULL, '{}', '2026-09-16 08:11:04.355029');
INSERT INTO `iam_role_revision` (`id`, `role_id`, `kind`, `revision`, `base_revision_id`, `metadata_overrides`, `published_at`) VALUES (1000057, 1000056, 'TENANT_CUSTOM', 1, 1000052, '{\"name\": \"只读定制\", \"groupName\": null, \"description\": null}', '2026-09-16 08:11:15.613957');
INSERT INTO `iam_role_revision` (`id`, `role_id`, `kind`, `revision`, `base_revision_id`, `metadata_overrides`, `published_at`) VALUES (1000059, 1000056, 'TENANT_CUSTOM', 2, 1000052, '{\"name\": \"差异合成\", \"groupName\": null, \"description\": null}', '2026-09-16 08:11:27.824662');
INSERT INTO `iam_role_revision` (`id`, `role_id`, `kind`, `revision`, `base_revision_id`, `metadata_overrides`, `published_at`) VALUES (1000061, 1000056, 'TENANT_CUSTOM', 3, 1000052, '{}', '2026-09-16 08:11:33.450746');
COMMIT;

-- ----------------------------
-- Table structure for iam_tenant
-- ----------------------------
DROP TABLE IF EXISTS `iam_tenant`;
CREATE TABLE `iam_tenant` (
  `id` bigint unsigned NOT NULL,
  `name` varchar(128) NOT NULL,
  `avatar` varchar(512) DEFAULT NULL,
  `owner_member_id` bigint unsigned DEFAULT NULL COMMENT '创建事务内允许暂空；提交前必须是本租户有效成员',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `version` bigint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `deleted_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_iam_tenant_owner` (`id`,`owner_member_id`),
  CONSTRAINT `fk_iam_tenant_owner` FOREIGN KEY (`id`, `owner_member_id`) REFERENCES `iam_tenant_member` (`tenant_id`, `id`),
  CONSTRAINT `ck_iam_tenant_enabled` CHECK ((`enabled` in (0,1))),
  CONSTRAINT `ck_iam_tenant_id` CHECK ((`id` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_tenant
-- ----------------------------
BEGIN;
INSERT INTO `iam_tenant` (`id`, `name`, `avatar`, `owner_member_id`, `enabled`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000001, 'test', NULL, 1000066, 1, 1, '2026-09-16 05:54:05.591221', '2026-09-16 05:54:05.591221', NULL);
INSERT INTO `iam_tenant` (`id`, `name`, `avatar`, `owner_member_id`, `enabled`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000007, 'test1', NULL, 1000008, 1, 0, '2026-09-16 05:54:12.418135', '2026-09-16 05:54:12.418135', NULL);
INSERT INTO `iam_tenant` (`id`, `name`, `avatar`, `owner_member_id`, `enabled`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000246, '测试组织A', NULL, 1000247, 1, 0, '2026-09-20 05:39:28.880874', '2026-09-20 05:39:28.880874', NULL);
INSERT INTO `iam_tenant` (`id`, `name`, `avatar`, `owner_member_id`, `enabled`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1000252, '测试组织B', NULL, 1000253, 1, 0, '2026-09-20 05:39:29.566758', '2026-09-20 05:39:29.566758', NULL);
COMMIT;

-- ----------------------------
-- Table structure for iam_tenant_app_entitlement
-- ----------------------------
DROP TABLE IF EXISTS `iam_tenant_app_entitlement`;
CREATE TABLE `iam_tenant_app_entitlement` (
  `id` bigint unsigned NOT NULL,
  `tenant_id` bigint unsigned NOT NULL,
  `application_id` bigint unsigned NOT NULL,
  `enabled` tinyint(1) NOT NULL,
  `source` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  `source_id` bigint unsigned DEFAULT NULL,
  `valid_from` datetime(6) DEFAULT NULL,
  `valid_until` datetime(6) DEFAULT NULL,
  `version` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_entitlement` (`tenant_id`,`application_id`),
  KEY `fk_iam_entitlement_app` (`application_id`),
  CONSTRAINT `fk_iam_entitlement_app` FOREIGN KEY (`application_id`) REFERENCES `iam_application` (`id`),
  CONSTRAINT `fk_iam_entitlement_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `iam_tenant` (`id`),
  CONSTRAINT `ck_iam_entitlement_enabled` CHECK ((`enabled` in (0,1))),
  CONSTRAINT `ck_iam_entitlement_interval` CHECK (((`valid_from` is null) or (`valid_until` is null) or (`valid_from` < `valid_until`))),
  CONSTRAINT `ck_iam_entitlement_source` CHECK ((`source` in (_utf8mb4'INITIALIZATION',_utf8mb4'MANUAL',_utf8mb4'PLAN',_utf8mb4'MIGRATION')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_tenant_app_entitlement
-- ----------------------------
BEGIN;
INSERT INTO `iam_tenant_app_entitlement` (`id`, `tenant_id`, `application_id`, `enabled`, `source`, `source_id`, `valid_from`, `valid_until`, `version`) VALUES (1000006, 1000007, 100002, 1, 'INITIALIZATION', NULL, '2026-09-16 05:54:12.412468', NULL, 0);
INSERT INTO `iam_tenant_app_entitlement` (`id`, `tenant_id`, `application_id`, `enabled`, `source`, `source_id`, `valid_from`, `valid_until`, `version`) VALUES (1000043, 1000001, 100002, 1, 'MANUAL', NULL, '2026-09-16 05:54:05.586664', NULL, 0);
INSERT INTO `iam_tenant_app_entitlement` (`id`, `tenant_id`, `application_id`, `enabled`, `source`, `source_id`, `valid_from`, `valid_until`, `version`) VALUES (1000044, 1000001, 1000037, 1, 'MANUAL', NULL, '2026-09-16 08:07:53.948714', NULL, 0);
INSERT INTO `iam_tenant_app_entitlement` (`id`, `tenant_id`, `application_id`, `enabled`, `source`, `source_id`, `valid_from`, `valid_until`, `version`) VALUES (1000245, 1000246, 100002, 1, 'INITIALIZATION', NULL, '2026-09-20 05:39:28.876479', NULL, 0);
INSERT INTO `iam_tenant_app_entitlement` (`id`, `tenant_id`, `application_id`, `enabled`, `source`, `source_id`, `valid_from`, `valid_until`, `version`) VALUES (1000251, 1000252, 100002, 1, 'INITIALIZATION', NULL, '2026-09-20 05:39:29.565967', NULL, 0);
COMMIT;

-- ----------------------------
-- Table structure for iam_tenant_group
-- ----------------------------
DROP TABLE IF EXISTS `iam_tenant_group`;
CREATE TABLE `iam_tenant_group` (
  `id` bigint unsigned NOT NULL,
  `tenant_id` bigint unsigned NOT NULL,
  `name` varchar(128) NOT NULL,
  `description` varchar(512) DEFAULT NULL,
  `version` bigint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_tenant_group_domain` (`tenant_id`,`id`),
  CONSTRAINT `fk_iam_tenant_group_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `iam_tenant` (`id`),
  CONSTRAINT `ck_iam_tenant_group_id` CHECK ((`id` > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_tenant_group
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_tenant_group_department
-- ----------------------------
DROP TABLE IF EXISTS `iam_tenant_group_department`;
CREATE TABLE `iam_tenant_group_department` (
  `tenant_id` bigint unsigned NOT NULL,
  `group_id` bigint unsigned NOT NULL,
  `department_id` bigint unsigned NOT NULL,
  `include_descendants` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`tenant_id`,`group_id`,`department_id`),
  KEY `idx_iam_tenant_department_groups` (`tenant_id`,`department_id`,`group_id`),
  CONSTRAINT `fk_iam_tenant_group_dept_department` FOREIGN KEY (`tenant_id`, `department_id`) REFERENCES `iam_department` (`tenant_id`, `id`),
  CONSTRAINT `fk_iam_tenant_group_dept_group` FOREIGN KEY (`tenant_id`, `group_id`) REFERENCES `iam_tenant_group` (`tenant_id`, `id`),
  CONSTRAINT `ck_iam_group_include_descendants` CHECK ((`include_descendants` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_tenant_group_department
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_tenant_group_member
-- ----------------------------
DROP TABLE IF EXISTS `iam_tenant_group_member`;
CREATE TABLE `iam_tenant_group_member` (
  `tenant_id` bigint unsigned NOT NULL,
  `group_id` bigint unsigned NOT NULL,
  `member_id` bigint unsigned NOT NULL,
  PRIMARY KEY (`tenant_id`,`group_id`,`member_id`),
  KEY `idx_iam_tenant_member_groups` (`tenant_id`,`member_id`,`group_id`),
  CONSTRAINT `fk_iam_tenant_group_entry_group` FOREIGN KEY (`tenant_id`, `group_id`) REFERENCES `iam_tenant_group` (`tenant_id`, `id`),
  CONSTRAINT `fk_iam_tenant_group_entry_member` FOREIGN KEY (`tenant_id`, `member_id`) REFERENCES `iam_tenant_member` (`tenant_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_tenant_group_member
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for iam_tenant_member
-- ----------------------------
DROP TABLE IF EXISTS `iam_tenant_member`;
CREATE TABLE `iam_tenant_member` (
  `id` bigint unsigned NOT NULL,
  `tenant_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `display_name` varchar(128) NOT NULL,
  `avatar` varchar(512) DEFAULT NULL,
  `phone` varchar(32) DEFAULT NULL COMMENT '组织通讯录资料，不修改全局登录手机号',
  `email` varchar(128) DEFAULT NULL COMMENT '组织通讯录资料，不修改全局登录邮箱',
  `status` varchar(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'ACTIVE',
  `version` bigint unsigned NOT NULL DEFAULT '0',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_tenant_member_account` (`tenant_id`,`account_id`),
  UNIQUE KEY `uk_iam_tenant_member_domain` (`tenant_id`,`id`),
  KEY `idx_iam_tenant_member_status` (`tenant_id`,`status`,`id`),
  KEY `idx_iam_tenant_member_account` (`account_id`,`status`),
  CONSTRAINT `fk_iam_tenant_member_account` FOREIGN KEY (`account_id`) REFERENCES `iam_account` (`id`),
  CONSTRAINT `fk_iam_tenant_member_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `iam_tenant` (`id`),
  CONSTRAINT `ck_iam_tenant_member_id` CHECK ((`id` > 0)),
  CONSTRAINT `ck_iam_tenant_member_status` CHECK ((`status` in (_utf8mb4'ACTIVE',_utf8mb4'SUSPENDED',_utf8mb4'REMOVED')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of iam_tenant_member
-- ----------------------------
BEGIN;
INSERT INTO `iam_tenant_member` (`id`, `tenant_id`, `account_id`, `display_name`, `avatar`, `phone`, `email`, `status`, `version`, `created_at`, `updated_at`) VALUES (1000002, 1000001, 900002, '组织所有者', NULL, NULL, NULL, 'ACTIVE', 0, '2026-09-16 05:54:05.597498', '2026-09-16 05:54:05.597498');
INSERT INTO `iam_tenant_member` (`id`, `tenant_id`, `account_id`, `display_name`, `avatar`, `phone`, `email`, `status`, `version`, `created_at`, `updated_at`) VALUES (1000008, 1000007, 900002, '组织所有者', NULL, NULL, NULL, 'ACTIVE', 0, '2026-09-16 05:54:12.422803', '2026-09-16 05:54:12.422803');
INSERT INTO `iam_tenant_member` (`id`, `tenant_id`, `account_id`, `display_name`, `avatar`, `phone`, `email`, `status`, `version`, `created_at`, `updated_at`) VALUES (1000012, 1000001, 900001, '平台账号在A', NULL, NULL, NULL, 'REMOVED', 2, '2026-09-16 06:08:11.551720', '2026-09-16 06:16:19.408101');
INSERT INTO `iam_tenant_member` (`id`, `tenant_id`, `account_id`, `display_name`, `avatar`, `phone`, `email`, `status`, `version`, `created_at`, `updated_at`) VALUES (1000027, 1000001, 1000022, 'A01b 租户成员', NULL, NULL, NULL, 'REMOVED', 1, '2026-09-16 07:32:04.405963', '2026-09-16 07:33:35.936134');
INSERT INTO `iam_tenant_member` (`id`, `tenant_id`, `account_id`, `display_name`, `avatar`, `phone`, `email`, `status`, `version`, `created_at`, `updated_at`) VALUES (1000035, 1000001, 1000032, 'A02 业务成员', NULL, NULL, NULL, 'ACTIVE', 0, '2026-09-16 07:34:57.782606', '2026-09-16 07:34:57.782606');
INSERT INTO `iam_tenant_member` (`id`, `tenant_id`, `account_id`, `display_name`, `avatar`, `phone`, `email`, `status`, `version`, `created_at`, `updated_at`) VALUES (1000066, 1000001, 1000063, 'A26 新所有者', NULL, NULL, NULL, 'ACTIVE', 0, '2026-09-16 08:11:47.416830', '2026-09-16 08:11:47.416830');
INSERT INTO `iam_tenant_member` (`id`, `tenant_id`, `account_id`, `display_name`, `avatar`, `phone`, `email`, `status`, `version`, `created_at`, `updated_at`) VALUES (1000247, 1000246, 1000203, 'owner-a', NULL, NULL, NULL, 'ACTIVE', 0, '2026-09-20 05:39:28.888534', '2026-09-20 05:39:28.888534');
INSERT INTO `iam_tenant_member` (`id`, `tenant_id`, `account_id`, `display_name`, `avatar`, `phone`, `email`, `status`, `version`, `created_at`, `updated_at`) VALUES (1000253, 1000252, 1000206, 'owner-b', NULL, NULL, NULL, 'ACTIVE', 0, '2026-09-20 05:39:29.574978', '2026-09-20 05:39:29.574978');
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密码过期信息';

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
) ENGINE=InnoDB AUTO_INCREMENT=1279801293280407555 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密码历史记录（环形缓冲）';

-- ----------------------------
-- Records of password_history
-- ----------------------------
BEGIN;
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1278379796044746754, 1000022, '{bcrypt}$2a$10$trQ06BrDVECdFrsQQt8Ek.pktF29V.Y3WTmdjp5Ph3ZA/jVJ9.e5C', 1, 1, '2026-09-16 15:30:56', '2026-09-16 15:30:56');
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1278380727155068930, 1000032, '{bcrypt}$2a$10$0vyeDQCx9YYKEaxMOPzgi.abTA1LU2wRk3wYtS1PtK..GbFzz4aM.', 1, 1, '2026-09-16 15:34:38', '2026-09-16 15:34:38');
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1278390052204351489, 1000063, '{bcrypt}$2a$10$cBA.0FFEVBroi/TLQQRfWuVhgokfFAPSR1xYQrOqURJpBkYgqXKFC', 1, 1, '2026-09-16 16:11:41', '2026-09-16 16:11:41');
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1279801183498694657, 1000200, '{bcrypt}$2a$10$wEppxHL2LrmifnbUMGOQpewlARwq3FDi1c0i3i0cufK3DZLWN54/i', 1, 1, '2026-09-20 13:39:01', '2026-09-20 13:39:01');
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1279801191690170370, 1000203, '{bcrypt}$2a$10$YJ.LBxr.cHIGZBfMeD/uXu02ESWbXb.qlGp9j8pJSiCQJAc9.OFha', 1, 1, '2026-09-20 13:39:03', '2026-09-20 13:39:03');
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1279801199227334658, 1000206, '{bcrypt}$2a$10$/6A4EPmzWH6SBTVuuhIPtOQ4kaL17vRFv594U3JR3baVs6aRUhzNq', 1, 1, '2026-09-20 13:39:05', '2026-09-20 13:39:05');
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1279801206714167297, 1000209, '{bcrypt}$2a$10$VPpOgjB7LoSMfuuooJBcKOH8NiHleBB7xZdxW1v.04s5CGTVMGnmy', 1, 1, '2026-09-20 13:39:06', '2026-09-20 13:39:06');
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1279801214297468929, 1000212, '{bcrypt}$2a$10$EiIFiz83m3BsASPoNfm.wOY8yyRvIKAL7vlQz9IjDBjA2Xsu/P0zy', 1, 1, '2026-09-20 13:39:08', '2026-09-20 13:39:08');
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1279801222136623106, 1000215, '{bcrypt}$2a$10$df6M8k2siA.CxRR9zHXTfeoyQ0Baq5Nx3w8OsCDRFfyMIMg.wICVm', 1, 1, '2026-09-20 13:39:10', '2026-09-20 13:39:10');
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1279801229711536129, 1000218, '{bcrypt}$2a$10$SIOrW9BoSxibIVAq0EYJo.4E3lwAjyxF3SZZYpRNvFHtO95mVdljW', 1, 1, '2026-09-20 13:39:12', '2026-09-20 13:39:12');
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1279801237760405506, 1000221, '{bcrypt}$2a$10$rc9Tko3ZnO/ID2HZs/nudOy/QlFjJs5/Rz2Ad68h.DIAgbyfuZQfG', 1, 1, '2026-09-20 13:39:14', '2026-09-20 13:39:14');
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1279801246312591362, 1000224, '{bcrypt}$2a$10$nNoupegtgh8sKzyT7u04pOWwdaXRk5qTnIA.EGsHsDszg7qGftc1m', 1, 1, '2026-09-20 13:39:16', '2026-09-20 13:39:16');
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1279801253992361985, 1000227, '{bcrypt}$2a$10$jHnKq0fTOSHhXJB8AoeVyOWfq9hmSPlZ0xxvWMZr3kyl7AjuGt2AC', 1, 1, '2026-09-20 13:39:18', '2026-09-20 13:39:18');
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1279801261923790850, 1000230, '{bcrypt}$2a$10$uao7dUC8iugItz.S61/aq.vR5QCWBZJGxEWzY9d9R4CjAsTzSaggK', 1, 1, '2026-09-20 13:39:20', '2026-09-20 13:39:20');
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1279801269876191234, 1000233, '{bcrypt}$2a$10$XIRNgXb1BNOTfsq6Gr4E1u9UKfd.CV6smI8XXd212.kF8cGtk2cYW', 1, 1, '2026-09-20 13:39:21', '2026-09-20 13:39:21');
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1279801277526601730, 1000236, '{bcrypt}$2a$10$u.r9jktoFE7Zgd5uoLpwYOLTz.Q/BXzdtqK6EVIRdYJzs6r2HQeuG', 1, 1, '2026-09-20 13:39:23', '2026-09-20 13:39:23');
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1279801285181206529, 1000239, '{bcrypt}$2a$10$T9CW1L2.B2kS9NLPxmyjl.yYCV0EU2scOMpykaH5WWowC5mOFmyHi', 1, 1, '2026-09-20 13:39:25', '2026-09-20 13:39:25');
INSERT INTO `password_history` (`id`, `user_id`, `password_hash`, `sequence_number`, `version`, `created_at`, `updated_at`) VALUES (1279801293280407554, 1000242, '{bcrypt}$2a$10$GiYc//p..rEWgJMuk2gXv.ie2DVjgN1EZiK9LXEH.AiJreAhMkJJK', 1, 1, '2026-09-20 13:39:27', '2026-09-20 13:39:27');
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
COMMIT;

-- ----------------------------
-- Table structure for security_event
-- ----------------------------
DROP TABLE IF EXISTS `security_event`;
CREATE TABLE `security_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_id` varchar(32) DEFAULT NULL COMMENT 'producer 幂等 ID',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `event_category` varchar(20) NOT NULL COMMENT 'AUTH/ACCOUNT/CREDENTIAL/ACCESS',
  `priority` varchar(16) NOT NULL DEFAULT 'BEST_EFFORT' COMMENT 'BEST_EFFORT/DURABLE',
  `occurred_at` datetime DEFAULT NULL COMMENT '业务发生时间',
  `received_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '接收时间',
  `tenant_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `user_type` varchar(20) DEFAULT NULL,
  `account` varchar(128) DEFAULT NULL,
  `client_id` varchar(64) DEFAULT NULL,
  `app_id` varchar(64) DEFAULT NULL,
  `session_id` varchar(64) DEFAULT NULL,
  `device_id` varchar(128) DEFAULT NULL,
  `client_ip` varchar(64) DEFAULT NULL,
  `request_uri` varchar(512) DEFAULT NULL,
  `user_agent` varchar(512) DEFAULT NULL,
  `result` varchar(20) DEFAULT NULL,
  `reason_code` varchar(50) DEFAULT NULL,
  `reason_detail` varchar(500) DEFAULT NULL,
  `source_module` varchar(64) NOT NULL COMMENT '上报模块',
  `source` varchar(50) DEFAULT NULL,
  `operator_id` bigint DEFAULT NULL,
  `operator_name` varchar(64) DEFAULT NULL,
  `trace_id` varchar(64) DEFAULT NULL,
  `extension` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_received_id` (`received_at`,`id`),
  KEY `idx_event_type` (`event_type`,`received_at`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_trace` (`trace_id`)
) ENGINE=InnoDB AUTO_INCREMENT=93349 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一安全事件（canonical）';

-- ----------------------------
-- Records of security_event
-- ----------------------------
BEGIN;
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93243, 'b2936cf8ddaa40b8952abf43ad9aa3dc', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 11:23:54', '2026-09-16 11:23:54', NULL, 900001, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93244, '55c5f26320074210ab7ae8a29477aad4', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 11:24:00', '2026-09-16 11:24:00', NULL, 900001, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93245, 'bac382d0676e4cbaa88187fd054a2789', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 11:24:03', '2026-09-16 11:24:03', NULL, 900001, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93246, 'ad95904282b4444fad98a43ef2e26382', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 11:25:20', '2026-09-16 11:25:20', NULL, 900001, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93247, 'c3b12399e5be42f2a0a89a53db90585f', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 11:55:40', '2026-09-16 11:55:40', NULL, 900001, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93248, '2d4115682a8a4dfebf97aa4034b81d8d', 'LOGIN_FAILURE', 'AUTH', 'BEST_EFFORT', '2026-09-16 13:50:11', '2026-09-16 13:50:11', NULL, 900001, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'FAILURE', NULL, 'InvalidArgument', 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93249, '52ed9af3f53d49f58049b282ac8fe226', 'LOGIN_FAILURE', 'AUTH', 'BEST_EFFORT', '2026-09-16 13:50:28', '2026-09-16 13:50:28', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'FAILURE', NULL, 'IdentityInvalid', 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93250, '4e06f271df3f44019e201cc50f90bec0', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 13:50:39', '2026-09-16 13:50:39', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93251, '903d63b981484c978a1d33c8d3b8997b', 'LOGIN_FAILURE', 'AUTH', 'BEST_EFFORT', '2026-09-16 13:51:11', '2026-09-16 13:51:11', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'FAILURE', NULL, 'IdentityInvalid', 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93252, 'cc9a05fd8dda40e8ba27eb3bf2fde59c', 'LOGIN_FAILURE', 'AUTH', 'BEST_EFFORT', '2026-09-16 13:51:21', '2026-09-16 13:51:21', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'FAILURE', NULL, 'IdentityInvalid', 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93253, '5f4fd7da006346d88862c421d3975c63', 'LOGIN_FAILURE', 'AUTH', 'BEST_EFFORT', '2026-09-16 13:51:33', '2026-09-16 13:51:33', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'FAILURE', NULL, 'IdentityInvalid', 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93254, '72d4412d1e6044908aa2d4b9ed5b6605', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 13:54:51', '2026-09-16 13:54:51', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93255, '6e5eab7efcfd46b28bcd794e93ec40ba', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 14:09:45', '2026-09-16 14:09:45', NULL, 900001, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93256, 'b041c4fe90004380a8a489ad8972e213', 'LOGIN_FAILURE', 'AUTH', 'BEST_EFFORT', '2026-09-16 14:11:51', '2026-09-16 14:11:51', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'FAILURE', NULL, 'IdentityInvalid', 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93257, '899d1d7e014646f1b65fb6dc34152da9', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 14:12:01', '2026-09-16 14:12:01', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93258, '5eae8ccbac6d48d7a1faa76e388dbb44', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 14:12:54', '2026-09-16 14:12:54', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93259, 'b0787194eb3c43c7b68927f8cbf06ca8', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 14:28:22', '2026-09-16 14:28:22', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93260, 'cb226cf1c9fd4890b91764d7e30b2da6', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 15:10:13', '2026-09-16 15:10:13', NULL, 900001, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93261, '78ea3fc6cb6f43c1bb9dd0bcba88dc22', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 15:26:07', '2026-09-16 15:26:07', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93262, '78c54054050146a698f0c07f2990197d', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 15:27:52', '2026-09-16 15:27:52', NULL, 900001, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93263, 'cae85b4ac694449aadb548109fe6da74', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 15:28:05', '2026-09-16 15:28:05', NULL, 900001, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93264, '628832194b494dd4958514c85a6d8bc4', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 15:28:18', '2026-09-16 15:28:18', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93265, '0aa6d82c67a445f0a4da806aea049565', 'ACCOUNT_CREATED', 'ACCOUNT', 'DURABLE', '2026-09-16 15:30:56', '2026-09-16 15:30:56', NULL, 1000022, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93266, '3f55f060af0c4f09bf1a486df702b422', 'PASSWORD_RESET', 'CREDENTIAL', 'DURABLE', '2026-09-16 15:31:22', '2026-09-16 15:31:23', NULL, 1000022, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, '910001', NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93267, 'e3412abc2bfe44d99a58e0690923efa0', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 15:32:27', '2026-09-16 15:32:27', NULL, 1000022, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93268, 'f3ba89bfce0a42b4a264cc70a32b802e', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 15:32:33', '2026-09-16 15:32:33', NULL, 1000022, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93269, 'f4e31155cd7745e0b4b9a018632e61ac', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 15:32:48', '2026-09-16 15:32:48', NULL, 1000022, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93270, '22f5b5acf33444128a8e86afe1c1fbe3', 'LOGIN_FAILURE', 'AUTH', 'BEST_EFFORT', '2026-09-16 15:33:29', '2026-09-16 15:33:29', NULL, 1000022, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'FAILURE', NULL, 'IdentityInvalid', 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93271, '4c581dac31d84594970a3db5c758cd01', 'LOGIN_FAILURE', 'AUTH', 'BEST_EFFORT', '2026-09-16 15:33:55', '2026-09-16 15:33:55', NULL, 1000022, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'FAILURE', NULL, 'IdentityInvalid', 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93272, '140f057eb52448b99424579084752777', 'ACCOUNT_DISABLED', 'ACCOUNT', 'DURABLE', '2026-09-16 15:34:02', '2026-09-16 15:34:02', NULL, 1000022, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'IAM', 'IAM', 900001, '910001', NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93273, '71922053fe4e4a9ba096ae7b0bdb6992', 'LOGIN_FAILURE', 'AUTH', 'BEST_EFFORT', '2026-09-16 15:34:16', '2026-09-16 15:34:16', NULL, 1000022, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'FAILURE', NULL, 'S0400', 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93274, '3871fb6c229e411da38401db29e0f368', 'ACCOUNT_CREATED', 'ACCOUNT', 'DURABLE', '2026-09-16 15:34:38', '2026-09-16 15:34:38', NULL, 1000032, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93275, '961ef65da4b64e989361ebe0d144b76f', 'PASSWORD_RESET', 'CREDENTIAL', 'DURABLE', '2026-09-16 15:34:51', '2026-09-16 15:34:51', NULL, 1000032, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, '910001', NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93276, 'bbab4afde8874267bb3902e69798a1b0', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 16:08:02', '2026-09-16 16:08:02', NULL, 1000032, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93277, '316d7b3ee5dc4cb1a947a089e0521353', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 16:08:24', '2026-09-16 16:08:24', NULL, 1000032, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93278, 'e743bf850aec49bfbbb5abdda28ceec2', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 16:10:36', '2026-09-16 16:10:36', NULL, 1000032, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93279, '3ebf58f363be4ddf9fde199a9f3a32c9', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 16:10:44', '2026-09-16 16:10:44', NULL, 1000032, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93280, 'a67a326b75e748c2bf3a6934b6d88223', 'ACCOUNT_CREATED', 'ACCOUNT', 'DURABLE', '2026-09-16 16:11:41', '2026-09-16 16:11:41', NULL, 1000063, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93281, '679e6b9ed3fb46639d1308296a3d22b3', 'PASSWORD_RESET', 'CREDENTIAL', 'DURABLE', '2026-09-16 16:11:44', '2026-09-16 16:11:44', NULL, 1000063, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, '910001', NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93282, 'bd281cf54d8e4ea4bb4aec61a5586fa2', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-16 16:12:11', '2026-09-16 16:12:11', NULL, 1000063, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93283, '3d273231902b4bf9a670a84b0a0e7033', 'LOGIN_FAILURE', 'AUTH', 'BEST_EFFORT', '2026-09-17 14:27:14', '2026-09-17 14:27:14', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'FAILURE', NULL, 'unauthorized_client', 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93284, '1c2b5b8c87f34a17be6c89ea34d407e6', 'LOGIN_FAILURE', 'AUTH', 'BEST_EFFORT', '2026-09-17 15:34:23', '2026-09-17 15:34:23', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'FAILURE', NULL, 'unauthorized_client', 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93285, '2783bc41267a4b089e244313ef78db83', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-17 15:34:54', '2026-09-17 15:34:54', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93286, '68f975df0b71435fae7dc8e72ba9a233', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-17 16:05:49', '2026-09-17 16:05:49', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93287, '4afd3a3d0c704bccb69014b0b6e04f48', 'LOGIN_FAILURE', 'AUTH', 'BEST_EFFORT', '2026-09-17 16:31:04', '2026-09-17 16:31:04', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'FAILURE', NULL, 'S0400', 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93288, '84d220b7f76540159f647bd6cdd0bf69', 'LOGIN_FAILURE', 'AUTH', 'BEST_EFFORT', '2026-09-17 16:37:41', '2026-09-17 16:37:41', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'FAILURE', NULL, 'S0400', 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93289, 'a45087611642432f86155456ff4082c5', 'LOGIN_FAILURE', 'AUTH', 'BEST_EFFORT', '2026-09-17 16:38:10', '2026-09-17 16:38:10', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'FAILURE', NULL, 'S0400', 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93290, '00ecbae474f14663948053ca65e9976d', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-17 16:39:29', '2026-09-17 16:39:29', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93291, 'd839c32ffe7c4b7ca1ed220d1d4d4616', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-17 16:40:45', '2026-09-17 16:40:45', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93292, '05c423d82a4d4084977c1ce5147a65ac', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-17 16:46:22', '2026-09-17 16:46:22', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93293, '3928a5acf989437aae801e283c7cea9f', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-17 16:55:10', '2026-09-17 16:55:10', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93294, '7439c1da10f0485c96f7958240864f3b', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-17 16:59:22', '2026-09-17 16:59:22', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93295, 'd6a6dd6c256644ec85623e8f21bca558', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-18 09:07:08', '2026-09-18 09:07:08', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93296, '6fc55443ce7d41cd93a2782c073aba3b', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-18 09:31:53', '2026-09-18 09:31:53', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93297, 'd1e26962d6614699bae0e8bc66053bd1', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-18 10:22:34', '2026-09-18 10:22:34', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93298, 'b43d3a8f5ecd42d889e371a6858f7cb9', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-18 10:24:15', '2026-09-18 10:24:15', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93299, 'fb2bc5a7dbad445fabb6e1b39261c3a4', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-18 10:25:27', '2026-09-18 10:25:27', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93300, 'd905f559ea5541eb8dd75bd016a91d49', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-18 10:26:01', '2026-09-18 10:26:01', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93301, '90dbcecbaab844c39a8d01ff48336a6c', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-18 10:26:47', '2026-09-18 10:26:47', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93302, '47d656a055754024a7d91f78c8f50885', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-18 10:27:57', '2026-09-18 10:27:57', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93303, 'a1d0a6e994464811af3149ec901255b1', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-18 10:34:52', '2026-09-18 10:34:52', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93304, '17c9bcd6eb5e48e4bae1e7eb581b26fa', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-18 10:36:31', '2026-09-18 10:36:31', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93305, '9299de08ba804b29a50492d3cc5fcd40', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-18 10:37:52', '2026-09-18 10:37:52', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93306, 'ead8de87e460431a9128fd3899c3b37c', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-18 10:44:18', '2026-09-18 10:44:18', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93307, 'fd1bddf32692485f96a7cfa0f823ecf0', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-18 10:56:35', '2026-09-18 10:56:35', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93308, '94f83969ff74467380687b14583feba4', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-18 11:13:50', '2026-09-18 11:13:50', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93309, '7753f7acd498472e81c9154122aa3609', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-18 14:37:02', '2026-09-18 14:37:02', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93310, '6300c00d364c4d4fbb1cad0c9aa62346', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-18 14:37:38', '2026-09-18 14:37:38', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93311, '0e7b84b16e174e02beabb9c328d164ab', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-18 14:43:42', '2026-09-18 14:43:42', NULL, 900002, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93312, 'fa14e6ad4a4a4b1bbe69bc6d8a965638', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-18 15:42:33', '2026-09-18 15:42:33', NULL, 900001, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93313, '5f54d66369124bd0bb4e5ef1e7128aa1', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-19 09:32:30', '2026-09-19 09:32:30', NULL, 900001, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93314, 'a17862e5f640448b8893b36a062aa0cb', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-19 15:09:25', '2026-09-19 15:09:25', NULL, 900001, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93315, '13a30162f2814c37b763360b7a7ffcf7', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-20 10:09:08', '2026-09-20 10:09:08', NULL, 900001, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93316, '6cf49897851d4bc0899b78d0d29d0046', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-20 13:39:00', '2026-09-20 13:39:00', NULL, 900001, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93317, 'add6a4c455784f26ab7122a86b6a575e', 'ACCOUNT_CREATED', 'ACCOUNT', 'DURABLE', '2026-09-20 13:39:01', '2026-09-20 13:39:01', NULL, 1000200, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93318, '0354fc528eda47a8b18a20ee03329b72', 'PASSWORD_RESET', 'CREDENTIAL', 'DURABLE', '2026-09-20 13:39:02', '2026-09-20 13:39:02', NULL, 1000200, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, '910001', NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93319, '017a6b863ce34a659d5fb6831479e80e', 'ACCOUNT_CREATED', 'ACCOUNT', 'DURABLE', '2026-09-20 13:39:03', '2026-09-20 13:39:03', NULL, 1000203, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93320, 'a2ad87e62f974c81a88591db55a10601', 'PASSWORD_RESET', 'CREDENTIAL', 'DURABLE', '2026-09-20 13:39:04', '2026-09-20 13:39:04', NULL, 1000203, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, '910001', NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93321, 'f9bab12ad0f24d9e96567b425aaddbba', 'ACCOUNT_CREATED', 'ACCOUNT', 'DURABLE', '2026-09-20 13:39:04', '2026-09-20 13:39:05', NULL, 1000206, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93322, '3b42a50bd24d42c6b926630f04e8d08d', 'PASSWORD_RESET', 'CREDENTIAL', 'DURABLE', '2026-09-20 13:39:06', '2026-09-20 13:39:06', NULL, 1000206, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, '910001', NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93323, 'e353dbe441934d9287d7b6f6b63da0c0', 'ACCOUNT_CREATED', 'ACCOUNT', 'DURABLE', '2026-09-20 13:39:06', '2026-09-20 13:39:06', NULL, 1000209, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93324, '8984d81008be492982326ce58115d24a', 'PASSWORD_RESET', 'CREDENTIAL', 'DURABLE', '2026-09-20 13:39:07', '2026-09-20 13:39:07', NULL, 1000209, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, '910001', NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93325, '37986ef16c6e4055af0a3c60ad430d17', 'ACCOUNT_CREATED', 'ACCOUNT', 'DURABLE', '2026-09-20 13:39:08', '2026-09-20 13:39:08', NULL, 1000212, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93326, '4eba141f79484a3cb951cdb990884f09', 'PASSWORD_RESET', 'CREDENTIAL', 'DURABLE', '2026-09-20 13:39:09', '2026-09-20 13:39:09', NULL, 1000212, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, '910001', NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93327, 'e4dadaab1ed7465195f52e97e6d6d3df', 'ACCOUNT_CREATED', 'ACCOUNT', 'DURABLE', '2026-09-20 13:39:10', '2026-09-20 13:39:10', NULL, 1000215, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93328, 'f724d8b97fd04840b9ed4a8703ed9338', 'PASSWORD_RESET', 'CREDENTIAL', 'DURABLE', '2026-09-20 13:39:11', '2026-09-20 13:39:11', NULL, 1000215, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, '910001', NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93329, 'fe43663b6f6c408ea1e9abd367d5bb70', 'ACCOUNT_CREATED', 'ACCOUNT', 'DURABLE', '2026-09-20 13:39:12', '2026-09-20 13:39:12', NULL, 1000218, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93330, 'f1cedb1889444440b7126253cdc5692d', 'PASSWORD_RESET', 'CREDENTIAL', 'DURABLE', '2026-09-20 13:39:13', '2026-09-20 13:39:13', NULL, 1000218, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, '910001', NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93331, '0c30d69dbc2f464daf6cab372e919bf6', 'ACCOUNT_CREATED', 'ACCOUNT', 'DURABLE', '2026-09-20 13:39:14', '2026-09-20 13:39:14', NULL, 1000221, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93332, '8cef6f782ae346c7ac24b176916f8d7f', 'PASSWORD_RESET', 'CREDENTIAL', 'DURABLE', '2026-09-20 13:39:15', '2026-09-20 13:39:15', NULL, 1000221, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, '910001', NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93333, 'be5c49adaf0c4c018e8cc115d86f4f36', 'ACCOUNT_CREATED', 'ACCOUNT', 'DURABLE', '2026-09-20 13:39:16', '2026-09-20 13:39:16', NULL, 1000224, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93334, '9e1a570d74ad453183a7a745d48a50a6', 'PASSWORD_RESET', 'CREDENTIAL', 'DURABLE', '2026-09-20 13:39:17', '2026-09-20 13:39:17', NULL, 1000224, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, '910001', NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93335, 'bceca40863a641fdbf28487d498eda7c', 'ACCOUNT_CREATED', 'ACCOUNT', 'DURABLE', '2026-09-20 13:39:17', '2026-09-20 13:39:18', NULL, 1000227, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93336, 'dd6630a89c7d4c77a4ec9e2c5fe80e91', 'PASSWORD_RESET', 'CREDENTIAL', 'DURABLE', '2026-09-20 13:39:19', '2026-09-20 13:39:19', NULL, 1000227, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, '910001', NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93337, 'f405e87310594161bf57011ec591fbf2', 'ACCOUNT_CREATED', 'ACCOUNT', 'DURABLE', '2026-09-20 13:39:19', '2026-09-20 13:39:20', NULL, 1000230, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93338, '845048168b1e482987d65aed04974804', 'PASSWORD_RESET', 'CREDENTIAL', 'DURABLE', '2026-09-20 13:39:21', '2026-09-20 13:39:21', NULL, 1000230, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, '910001', NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93339, '6ae53699a228450fb7008f6018b82a6a', 'ACCOUNT_CREATED', 'ACCOUNT', 'DURABLE', '2026-09-20 13:39:21', '2026-09-20 13:39:21', NULL, 1000233, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93340, '51ffdecaf32b445fae2c70fcba3bda39', 'PASSWORD_RESET', 'CREDENTIAL', 'DURABLE', '2026-09-20 13:39:23', '2026-09-20 13:39:23', NULL, 1000233, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, '910001', NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93341, '93b73f6331b84669850a939ea56fb797', 'ACCOUNT_CREATED', 'ACCOUNT', 'DURABLE', '2026-09-20 13:39:23', '2026-09-20 13:39:23', NULL, 1000236, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93342, '75df749936ea4d8f9de326bd94e4c1fa', 'PASSWORD_RESET', 'CREDENTIAL', 'DURABLE', '2026-09-20 13:39:24', '2026-09-20 13:39:24', NULL, 1000236, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, '910001', NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93343, 'f250327f08504cfd9c7c2bbe55a42278', 'ACCOUNT_CREATED', 'ACCOUNT', 'DURABLE', '2026-09-20 13:39:25', '2026-09-20 13:39:25', NULL, 1000239, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93344, 'dba1c0b66728470fb2839162ccde280e', 'PASSWORD_RESET', 'CREDENTIAL', 'DURABLE', '2026-09-20 13:39:26', '2026-09-20 13:39:26', NULL, 1000239, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, '910001', NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93345, '4f7fce6bbebd402290207f273f81cca2', 'ACCOUNT_CREATED', 'ACCOUNT', 'DURABLE', '2026-09-20 13:39:27', '2026-09-20 13:39:27', NULL, 1000242, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93346, '28cc9185d08744e493b8f14250f82e30', 'PASSWORD_RESET', 'CREDENTIAL', 'DURABLE', '2026-09-20 13:39:28', '2026-09-20 13:39:28', NULL, 1000242, 'ADMIN', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'IAM', 900001, '910001', NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93347, '772ed68db5b242258e0d49beec46e89f', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-20 13:39:30', '2026-09-20 13:39:30', NULL, 1000203, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
INSERT INTO `security_event` (`id`, `event_id`, `event_type`, `event_category`, `priority`, `occurred_at`, `received_at`, `tenant_id`, `user_id`, `user_type`, `account`, `client_id`, `app_id`, `session_id`, `device_id`, `client_ip`, `request_uri`, `user_agent`, `result`, `reason_code`, `reason_detail`, `source_module`, `source`, `operator_id`, `operator_name`, `trace_id`, `extension`) VALUES (93348, 'e09985caaa724215accc8fb513c4999e', 'LOGIN_SUCCESS', 'AUTH', 'BEST_EFFORT', '2026-09-20 13:39:31', '2026-09-20 13:39:31', NULL, 1000206, 'ADMIN', NULL, NULL, NULL, NULL, NULL, '127.0.0.1', NULL, NULL, 'SUCCESS', NULL, NULL, 'IAM', 'AUTH', NULL, NULL, NULL, NULL);
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

SET FOREIGN_KEY_CHECKS = 1;
