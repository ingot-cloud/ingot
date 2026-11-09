-- ingot-security-event-store-mysql MySQL 建表脚本（MySQL 8.0.16+）。
-- 在消费服务DataSource对应的数据库执行；不选库、不删除表、不写入业务数据。
SET time_zone = '+00:00';

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
