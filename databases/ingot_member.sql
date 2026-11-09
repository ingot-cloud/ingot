-- 业务时间点统一 UTC；初始化连接也必须明确会话时区。
SET time_zone = '+00:00';

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

 Date: 17/08/2026 16:09:03
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for fw_security_account_lock_state
-- ----------------------------
DROP TABLE IF EXISTS `fw_security_account_lock_state`;
CREATE TABLE IF NOT EXISTS fw_security_account_lock_state (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '安全框架账号锁定状态记录标识；数据库自增',
  user_id         BIGINT       NOT NULL COMMENT '被锁定的安全用户ID，与user_type共同唯一标识用户',
  user_type       VARCHAR(20)  NOT NULL DEFAULT '0' COMMENT '安全用户类型，对应UserTypeEnum：0系统账号、1会员账号',

  -- 锁定状态
  locked          TINYINT(1)   NOT NULL DEFAULT 0     COMMENT '是否锁定（0-否 1-是）',
  lock_type       VARCHAR(20)  DEFAULT NULL            COMMENT '锁定类型（MANUAL-手动 AUTO-自动）',
  lock_reason_code   VARCHAR(50)  DEFAULT NULL         COMMENT '锁定原因代码',
  lock_reason_detail VARCHAR(500) DEFAULT NULL         COMMENT '锁定原因详情',

  -- 锁定时间信息
  locked_at       DATETIME     DEFAULT NULL COMMENT '锁定时间；时间点按UTC保存',
  locked_until    DATETIME     DEFAULT NULL COMMENT '锁定到期时间（NULL=永久锁定）；时间点按UTC保存',

  -- 操作信息（手动锁定时填写）
  operator_id     BIGINT       DEFAULT NULL            COMMENT '操作人ID',
  operator_name   VARCHAR(64)  DEFAULT NULL            COMMENT '操作人姓名',

  -- 登录失败计数
  failed_login_count INT        NOT NULL DEFAULT 0    COMMENT '连续登录失败次数',
  last_failed_at  DATETIME     DEFAULT NULL COMMENT '最后一次失败时间；时间点按UTC保存',

  -- 时间戳
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间点，UTC',
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间点，UTC',

  PRIMARY KEY (id),
  UNIQUE KEY uk_user_id_type (user_id, user_type)       COMMENT '用户ID + 用户类型联合唯一',
  KEY idx_locked (locked, locked_until) USING BTREE     COMMENT '锁定状态 + 到期时间索引（自动解锁任务使用）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号锁定状态表；所属模块：ingot-security-account-adapter';

-- ----------------------------
-- Records of fw_security_account_lock_state
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for fw_id_leaf_alloc
-- ----------------------------
DROP TABLE IF EXISTS `fw_id_leaf_alloc`;
CREATE TABLE IF NOT EXISTS `fw_id_leaf_alloc` (
  `biz_tag` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT 'Leaf业务号段名称，不同业务独立分配',
  `max_id` bigint NOT NULL DEFAULT '1' COMMENT '已分配号段的最大ID高水位，更新必须保持发号原子性',
  `step` int NOT NULL COMMENT '每次申请的号段长度',
  `description` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '业务说明，可空',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '号段高水位最近更新时间点，UTC',
  PRIMARY KEY (`biz_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Leaf业务号段分配高水位；所属模块：ingot-id';

-- ----------------------------
-- Records of fw_id_leaf_alloc
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
  `locked` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否锁定（冗余字段，详情见 fw_security_account_lock_state）',
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
INSERT INTO `member_user` (`id`, `username`, `password`, `must_change_pwd`, `password_changed_at`, `nickname`, `phone`, `email`, `avatar`, `enabled`, `locked`, `last_login_at`, `last_login_ip`, `version`, `created_at`, `updated_at`, `deleted_at`) VALUES (1175075664995807234, '18612341234', '{bcrypt}$2a$10$hsIIWfLalkeAbxOTVbEfgus3v3CC0tXhi8usi7JeoArDldG/um4dK', 1, '2026-07-28 16:54:33', '测试用户', '18612341234', NULL, 'ingot/user/avatar/ic_logo.png', 1, 0, '2026-07-29 14:50:31', '127.0.0.1', 0, '2025-12-05 13:57:11', '2025-12-05 14:28:05', NULL);
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
-- Table structure for fw_security_event
-- ----------------------------
DROP TABLE IF EXISTS `fw_security_event`;
CREATE TABLE IF NOT EXISTS `fw_security_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '统一安全事件持久记录记录标识；数据库自增',
  `event_id` varchar(32) DEFAULT NULL COMMENT 'producer 幂等 ID',
  `event_type` varchar(64) NOT NULL COMMENT '事件类型',
  `event_category` varchar(20) NOT NULL COMMENT 'AUTH/ACCOUNT/CREDENTIAL/ACCESS',
  `priority` varchar(16) NOT NULL DEFAULT 'BEST_EFFORT' COMMENT 'BEST_EFFORT/DURABLE',
  `occurred_at` datetime DEFAULT NULL COMMENT '业务发生时间点，UTC',
  `received_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '服务端接收安全事件的时间点，UTC',
  `tenant_id` bigint DEFAULT NULL COMMENT '事件所属租户ID；NULL表示平台事件或未提供租户上下文',
  `user_id` bigint DEFAULT NULL COMMENT '安全事件关联的用户标识，结合user_type解释；可空',
  `user_type` varchar(20) DEFAULT NULL COMMENT '安全用户类型，对应UserTypeEnum：0系统账号、1会员账号',
  `account` varchar(128) DEFAULT NULL COMMENT '事件涉及的账号显示标识，可空',
  `client_id` varchar(64) DEFAULT NULL COMMENT '认证客户端标识，可空',
  `app_id` varchar(64) DEFAULT NULL COMMENT '上报方提供的应用标识，可空',
  `session_id` varchar(64) DEFAULT NULL COMMENT '认证会话标识，可空',
  `device_id` varchar(128) DEFAULT NULL COMMENT '设备标识，可空',
  `client_ip` varchar(64) DEFAULT NULL COMMENT '客户端IP地址，可空',
  `request_uri` varchar(512) DEFAULT NULL COMMENT '触发事件的请求路径，可空',
  `user_agent` varchar(512) DEFAULT NULL COMMENT '客户端User-Agent，可空',
  `result` varchar(20) DEFAULT NULL COMMENT '安全操作结果稳定标识，可空',
  `reason_code` varchar(50) DEFAULT NULL COMMENT '安全事件原因代码，可空',
  `reason_detail` varchar(500) DEFAULT NULL COMMENT '安全事件原因说明，不保存凭证原文',
  `source_module` varchar(64) NOT NULL COMMENT '上报模块',
  `source` varchar(50) DEFAULT NULL COMMENT '安全事件来源渠道或触发来源，可空',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人标识，可空',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人姓名快照，可空',
  `trace_id` varchar(64) DEFAULT NULL COMMENT '请求链路追踪标识，可空',
  `extension` json DEFAULT NULL COMMENT '安全事件扩展JSON，须遵守脱敏白名单',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_received_id` (`received_at`,`id`),
  KEY `idx_event_type` (`event_type`,`received_at`),
  KEY `idx_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_trace` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一安全事件（canonical）；所属模块：ingot-security-event-store-mysql';

-- ----------------------------
-- Records of fw_security_event
-- ----------------------------
BEGIN;
COMMIT;


CREATE TABLE IF NOT EXISTS fw_oss_asset_upload (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '一次性上传凭证 ID',
    owner_key VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '服务端恢复的域、组织和操作者',
    purpose VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '固定业务用途编码',
    target_key VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '既有目标 ID 或空字符串',
    bucket_name VARCHAR(63) NOT NULL COMMENT '服务端选定的私有 bucket',
    object_name VARCHAR(255) NOT NULL COMMENT '服务端生成的对象名',
    state VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '暂存状态，消费成功随业务事务移除',
    expires_at DATETIME(6) NOT NULL COMMENT '凭证到期时间，UTC',
    PRIMARY KEY (id),
    KEY idx_asset_upload_expires (expires_at, id),
    CONSTRAINT ck_asset_upload_state CHECK (state IN ('PENDING','READY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务图片暂存与一次性绑定凭证；所属模块：ingot-oss-common';

SET FOREIGN_KEY_CHECKS = 1;
