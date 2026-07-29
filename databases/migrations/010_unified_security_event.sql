-- L3: 统一安全事件中心 — security_event 表
-- 目标库：ingot_security

USE ingot_security;

CREATE TABLE IF NOT EXISTS `security_event` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `event_type`      VARCHAR(64)  NOT NULL COMMENT '事件类型',
  `event_category`  VARCHAR(20)  NOT NULL COMMENT 'AUTH/ACCOUNT/CREDENTIAL/ACCESS',
  `occurred_at`     DATETIME              DEFAULT NULL COMMENT '业务发生时间',
  `received_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '中心接收时间',
  `tenant_id`       BIGINT                DEFAULT NULL,
  `user_id`         BIGINT                DEFAULT NULL,
  `user_type`       VARCHAR(20)           DEFAULT NULL COMMENT 'ADMIN/APP',
  `account`         VARCHAR(128)          DEFAULT NULL,
  `client_id`       VARCHAR(64)           DEFAULT NULL,
  `app_id`          VARCHAR(64)           DEFAULT NULL,
  `session_id`      VARCHAR(64)           DEFAULT NULL,
  `device_id`       VARCHAR(128)          DEFAULT NULL,
  `client_ip`       VARCHAR(64)           DEFAULT NULL,
  `request_uri`     VARCHAR(512)          DEFAULT NULL,
  `user_agent`      VARCHAR(512)          DEFAULT NULL,
  `result`          VARCHAR(20)           DEFAULT NULL COMMENT 'SUCCESS/FAILURE',
  `reason_code`     VARCHAR(50)           DEFAULT NULL,
  `reason_detail`   VARCHAR(500)          DEFAULT NULL,
  `source_module`   VARCHAR(64)  NOT NULL COMMENT '上报模块',
  `source`          VARCHAR(50)           DEFAULT NULL,
  `operator_id`     BIGINT                DEFAULT NULL,
  `operator_name`   VARCHAR(64)           DEFAULT NULL,
  `trace_id`        VARCHAR(64)           DEFAULT NULL,
  `extension`       JSON                  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_event_time` (`received_at`),
  KEY `idx_event_type` (`event_type`, `received_at`),
  KEY `idx_tenant_user` (`tenant_id`, `user_id`),
  KEY `idx_trace` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一安全事件';
