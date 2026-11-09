-- L4: 访问防护补全 — 网关策略种子 + 登录失败保护策略表
-- 目标库：ingot_security

USE ingot_security;

-- ============================================================
-- 1. login_failure_protection_policy : 登录失败保护（按维度）
-- ============================================================
CREATE TABLE IF NOT EXISTS `login_failure_protection_policy` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dimension`      VARCHAR(16)  NOT NULL                COMMENT 'IP/DEVICE/CLIENT/ACCOUNT_IP',
  `enabled`        TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '是否启用',
  `max_attempts`   INT          NOT NULL DEFAULT 50     COMMENT '窗口内最大失败次数',
  `window_minutes` INT          NOT NULL DEFAULT 1      COMMENT '滑动窗口（分钟）',
  `block_ttl_sec`  INT          NOT NULL DEFAULT 3600   COMMENT '临时封禁 TTL（秒）',
  `block_key_type` CHAR(2)      NOT NULL DEFAULT 'IP'   COMMENT '封禁 keyType: IP/DV/CL',
  `remark`         VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
  `created_at`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_login_failure_dimension` (`dimension`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录失败保护策略（按维度）';

INSERT INTO `login_failure_protection_policy`
  (`dimension`, `enabled`, `max_attempts`, `window_minutes`, `block_ttl_sec`, `block_key_type`, `remark`)
VALUES
  ('IP',         1, 50,  1, 3600, 'IP', '同一 IP 登录失败达阈值临时封禁'),
  ('DEVICE',     1, 30,  5, 1800, 'DV', '同一设备指纹登录失败达阈值临时封禁'),
  ('CLIENT',     1, 100, 5, 3600, 'CL', '同一 OAuth2 Client 登录失败达阈值临时封禁'),
  ('ACCOUNT_IP', 1, 10,  5, 3600, 'IP', '同一账号+IP 组合登录失败达阈值封禁 IP')
ON DUPLICATE KEY UPDATE `updated_at` = CURRENT_TIMESTAMP;

-- ============================================================
-- 2. 网关 API 分组与限流种子（若不存在则插入）
-- ============================================================
INSERT INTO `gateway_endpoint_group` (`code`, `name`, `pattern_list`, `enabled`, `remark`)
SELECT 'login-auth', '登录认证入口', JSON_ARRAY(
         JSON_OBJECT('path', '/auth/token/**', 'method', 'POST'),
         JSON_OBJECT('path', '/bff/auth/login', 'method', 'POST')
       ), 1, 'L4 登录路径分组'
WHERE NOT EXISTS (SELECT 1 FROM `gateway_endpoint_group` WHERE `code` = 'login-auth');

INSERT INTO `gateway_endpoint_group` (`code`, `name`, `pattern_list`, `enabled`, `remark`)
SELECT 'api-business', '业务 API 基线', JSON_ARRAY(
         JSON_OBJECT('path', '/pms/**', 'method', 'ANY'),
         JSON_OBJECT('path', '/member/**', 'method', 'ANY'),
         JSON_OBJECT('path', '/security/**', 'method', 'ANY')
       ), 1, '替代旧 RequestRateLimiter 路由前缀'
WHERE NOT EXISTS (SELECT 1 FROM `gateway_endpoint_group` WHERE `code` = 'api-business');

INSERT INTO `gateway_rate_limit_rule`
  (`code`, `group_code`, `dimension`, `qps`, `burst`, `interval_sec`, `control_behavior`, `enabled`, `priority`, `remark`)
SELECT 'login-ip', 'login-auth', 'IP', 1, 2, 60, 'F', 1, 0, '登录路径 IP 限流基线'
WHERE NOT EXISTS (SELECT 1 FROM `gateway_rate_limit_rule` WHERE `code` = 'login-ip');

INSERT INTO `gateway_rate_limit_rule`
  (`code`, `group_code`, `dimension`, `qps`, `burst`, `interval_sec`, `control_behavior`, `enabled`, `priority`, `remark`)
SELECT 'pms-ip', 'api-business', 'IP', 200, 300, 1, 'F', 1, 10, 'PMS 路由 SDK 限流'
WHERE NOT EXISTS (SELECT 1 FROM `gateway_rate_limit_rule` WHERE `code` = 'pms-ip');

INSERT INTO `gateway_rate_limit_rule`
  (`code`, `group_code`, `dimension`, `qps`, `burst`, `interval_sec`, `control_behavior`, `enabled`, `priority`, `remark`)
SELECT 'member-ip', 'api-business', 'IP', 200, 300, 1, 'F', 1, 11, 'Member 路由 SDK 限流'
WHERE NOT EXISTS (SELECT 1 FROM `gateway_rate_limit_rule` WHERE `code` = 'member-ip');

INSERT INTO `gateway_rate_limit_rule`
  (`code`, `group_code`, `dimension`, `qps`, `burst`, `interval_sec`, `control_behavior`, `enabled`, `priority`, `remark`)
SELECT 'security-ip', 'api-business', 'IP', 200, 300, 1, 'F', 1, 12, 'Security 路由 SDK 限流'
WHERE NOT EXISTS (SELECT 1 FROM `gateway_rate_limit_rule` WHERE `code` = 'security-ip');
