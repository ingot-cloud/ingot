-- Phase 02: security_event recording schema（additive）
-- 012: 中心库增列 + PMS/Member canonical security_event 表

-- ============================================================
-- ingot_security：现有 security_event 增列
-- ============================================================
USE ingot_security;

ALTER TABLE `security_event`
    ADD COLUMN `event_id` VARCHAR(32) NULL COMMENT 'producer 生成的幂等 ID' AFTER `id`;

ALTER TABLE `security_event`
    ADD COLUMN `priority` VARCHAR(16) NOT NULL DEFAULT 'BEST_EFFORT' COMMENT 'BEST_EFFORT/DURABLE' AFTER `event_category`;

ALTER TABLE `security_event`
    ADD UNIQUE KEY `uk_event_id` (`event_id`);

ALTER TABLE `security_event`
    ADD KEY `idx_received_id` (`received_at`, `id`);

-- ============================================================
-- ingot_core：PMS 本地 canonical security_event
-- ============================================================
USE ingot_core;

CREATE TABLE IF NOT EXISTS `security_event` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_id`        VARCHAR(32)  NULL COMMENT 'producer 幂等 ID',
  `event_type`      VARCHAR(64)  NOT NULL COMMENT '事件类型',
  `event_category`  VARCHAR(20)  NOT NULL COMMENT 'AUTH/ACCOUNT/CREDENTIAL/ACCESS',
  `priority`        VARCHAR(16)  NOT NULL DEFAULT 'BEST_EFFORT' COMMENT 'BEST_EFFORT/DURABLE',
  `occurred_at`     DATETIME              DEFAULT NULL COMMENT '业务发生时间',
  `received_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '接收时间',
  `tenant_id`       BIGINT                DEFAULT NULL,
  `user_id`         BIGINT                DEFAULT NULL,
  `user_type`       VARCHAR(20)           DEFAULT NULL,
  `account`         VARCHAR(128)          DEFAULT NULL,
  `client_id`       VARCHAR(64)           DEFAULT NULL,
  `app_id`          VARCHAR(64)           DEFAULT NULL,
  `session_id`      VARCHAR(64)           DEFAULT NULL,
  `device_id`       VARCHAR(128)          DEFAULT NULL,
  `client_ip`       VARCHAR(64)           DEFAULT NULL,
  `request_uri`     VARCHAR(512)          DEFAULT NULL,
  `user_agent`      VARCHAR(512)          DEFAULT NULL,
  `result`          VARCHAR(20)           DEFAULT NULL,
  `reason_code`     VARCHAR(50)           DEFAULT NULL,
  `reason_detail`   VARCHAR(500)          DEFAULT NULL,
  `source_module`   VARCHAR(64)  NOT NULL COMMENT '上报模块',
  `source`          VARCHAR(50)           DEFAULT NULL,
  `operator_id`     BIGINT                DEFAULT NULL,
  `operator_name`   VARCHAR(64)           DEFAULT NULL,
  `trace_id`        VARCHAR(64)           DEFAULT NULL,
  `extension`       JSON                  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_received_id` (`received_at`, `id`),
  KEY `idx_event_type` (`event_type`, `received_at`),
  KEY `idx_tenant_user` (`tenant_id`, `user_id`),
  KEY `idx_trace` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一安全事件（canonical）';

-- ============================================================
-- ingot_member：Member 本地 canonical security_event
-- ============================================================
USE ingot_member;

CREATE TABLE IF NOT EXISTS `security_event` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_id`        VARCHAR(32)  NULL COMMENT 'producer 幂等 ID',
  `event_type`      VARCHAR(64)  NOT NULL COMMENT '事件类型',
  `event_category`  VARCHAR(20)  NOT NULL COMMENT 'AUTH/ACCOUNT/CREDENTIAL/ACCESS',
  `priority`        VARCHAR(16)  NOT NULL DEFAULT 'BEST_EFFORT' COMMENT 'BEST_EFFORT/DURABLE',
  `occurred_at`     DATETIME              DEFAULT NULL COMMENT '业务发生时间',
  `received_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '接收时间',
  `tenant_id`       BIGINT                DEFAULT NULL,
  `user_id`         BIGINT                DEFAULT NULL,
  `user_type`       VARCHAR(20)           DEFAULT NULL,
  `account`         VARCHAR(128)          DEFAULT NULL,
  `client_id`       VARCHAR(64)           DEFAULT NULL,
  `app_id`          VARCHAR(64)           DEFAULT NULL,
  `session_id`      VARCHAR(64)           DEFAULT NULL,
  `device_id`       VARCHAR(128)          DEFAULT NULL,
  `client_ip`       VARCHAR(64)           DEFAULT NULL,
  `request_uri`     VARCHAR(512)          DEFAULT NULL,
  `user_agent`      VARCHAR(512)          DEFAULT NULL,
  `result`          VARCHAR(20)           DEFAULT NULL,
  `reason_code`     VARCHAR(50)           DEFAULT NULL,
  `reason_detail`   VARCHAR(500)          DEFAULT NULL,
  `source_module`   VARCHAR(64)  NOT NULL COMMENT '上报模块',
  `source`          VARCHAR(50)           DEFAULT NULL,
  `operator_id`     BIGINT                DEFAULT NULL,
  `operator_name`   VARCHAR(64)           DEFAULT NULL,
  `trace_id`        VARCHAR(64)           DEFAULT NULL,
  `extension`       JSON                  DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_received_id` (`received_at`, `id`),
  KEY `idx_event_type` (`event_type`, `received_at`),
  KEY `idx_tenant_user` (`tenant_id`, `user_id`),
  KEY `idx_trace` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一安全事件（canonical）';
