-- ============================================================
-- 回滚 013: 重建空表 account_security_event（不恢复历史数据）
-- 库: ingot_core / ingot_member
-- ============================================================

USE ingot_core;

CREATE TABLE IF NOT EXISTS `account_security_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_type` varchar(20) NOT NULL COMMENT '用户类型',
  `event_type` varchar(50) NOT NULL COMMENT '事件类型',
  `event_category` varchar(20) NOT NULL COMMENT '事件分类（AUTH / ACCOUNT / CREDENTIAL）',
  `reason_code` varchar(50) DEFAULT NULL COMMENT '原因代码',
  `reason_detail` varchar(500) DEFAULT NULL COMMENT '详细描述',
  `result` varchar(20) DEFAULT NULL COMMENT '结果（SUCCESS / FAILURE）',
  `source` varchar(50) DEFAULT NULL COMMENT '来源（AUTH / PMS / MEMBER / SYSTEM）',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人姓名',
  `client_ip` varchar(64) DEFAULT NULL COMMENT '客户端IP',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '客户端 User-Agent',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `extra_data` json DEFAULT NULL COMMENT '扩展数据',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_event` (`user_id`,`user_type`,`event_type`,`created_at`) USING BTREE,
  KEY `idx_created_at` (`created_at`) USING BTREE,
  KEY `idx_event_type` (`event_type`,`created_at`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`,`created_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号安全事件表（rollback 空表，勿再写入）';

USE ingot_member;

CREATE TABLE IF NOT EXISTS `account_security_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_type` varchar(20) NOT NULL COMMENT '用户类型（同 UserTypeEnum.value）',
  `event_type` varchar(50) NOT NULL COMMENT '事件类型',
  `event_category` varchar(20) NOT NULL COMMENT '事件分类（AUTH / ACCOUNT / CREDENTIAL）',
  `reason_code` varchar(50) DEFAULT NULL COMMENT '原因代码',
  `reason_detail` varchar(500) DEFAULT NULL COMMENT '详细描述',
  `result` varchar(20) DEFAULT NULL COMMENT '结果（SUCCESS / FAILURE）',
  `source` varchar(50) DEFAULT NULL COMMENT '来源（AUTH / PMS / MEMBER / SYSTEM）',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人姓名',
  `client_ip` varchar(64) DEFAULT NULL COMMENT '客户端IP',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '客户端 User-Agent',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `extra_data` json DEFAULT NULL COMMENT '扩展数据',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_event` (`user_id`,`user_type`,`event_type`,`created_at`) USING BTREE,
  KEY `idx_created_at` (`created_at`) USING BTREE,
  KEY `idx_event_type` (`event_type`,`created_at`) USING BTREE,
  KEY `idx_tenant` (`tenant_id`,`created_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号安全事件表（rollback 空表，勿再写入）';

SELECT 'rollback_013_drop_account_security_event.sql 执行完成（空表）' AS message;
