-- ============================================================
-- Member 账号保护表（account_lock_state / account_security_event）
-- 版本: V2.0
-- 日期: 2026-07-24
-- 说明: 账号保护全用户闭环（L2）——Member 引入 ingot-account-adapter，
--       在 ingot_member 库建账号锁定状态表与安全事件表，与 ADMIN（ingot_core）对齐。
--       表结构与 ingot_core 一致，user_type 存 '1'（APP / C端用户）。
--       两表 DDL 随 ingot-account-adapter 模块管理（见其 resources/sql/）。
-- ============================================================

USE ingot_member;

-- ============================================================
-- account_lock_state：账号锁定状态表
-- ============================================================
CREATE TABLE IF NOT EXISTS `account_lock_state` (
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

-- ============================================================
-- account_security_event：账号安全事件表
-- ============================================================
CREATE TABLE IF NOT EXISTS `account_security_event` (
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

-- ============================================================
-- 完成
-- ============================================================
SELECT '009_member_account_protection.sql 执行完成' AS message;
