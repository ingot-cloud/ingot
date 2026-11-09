-- ============================================================
-- 安全策略中心 - 限流规则 / 黑白名单 / API 分组 / 封禁审计
-- 版本: V5.0
-- 日期: 2026-05-26
-- 说明: 在 ingot_security 库新增 4 张表，承载网关层限流规则、
--       IP/设备/用户黑白名单、API 路径分组及自动封禁/解封审计。
--       托管由 ingot-service-security 提供页面化 CRUD，下游
--       通过 ingot-gateway-rule-client SDK + InvalidationBus
--       订阅刷新。
-- ============================================================

USE ingot_security;

SET NAMES utf8mb4;

-- ============================================================
-- 1. gateway_endpoint_group : API 路径分组定义
--    解决多条规则/策略复用同一组 path 的维护问题；
--    规则也允许内联 pattern_list 不引用本表。
-- ============================================================
DROP TABLE IF EXISTS `gateway_endpoint_group`;
CREATE TABLE `gateway_endpoint_group` (
  `id`           bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code`         varchar(64)   NOT NULL                COMMENT '分组编码（唯一）',
  `name`         varchar(128)  NOT NULL                COMMENT '分组名称',
  `pattern_list` json          NOT NULL                COMMENT 'API 路径列表 [{"path":"/x/**","method":"POST"}]',
  `enabled`      tinyint(1)    NOT NULL DEFAULT 1      COMMENT '是否启用',
  `remark`       varchar(255)           DEFAULT NULL   COMMENT '备注',
  `created_at`   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_endpoint_group_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='API 路径分组定义';

-- ============================================================
-- 2. gateway_rate_limit_rule : 限流规则
--    维度: IP / DV(device) / UI(user)
--    可引用 endpoint_group，也可内联 pattern_list
-- ============================================================
DROP TABLE IF EXISTS `gateway_rate_limit_rule`;
CREATE TABLE `gateway_rate_limit_rule` (
  `id`               bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code`             varchar(64)   NOT NULL                COMMENT '规则编码（唯一）',
  `group_code`       varchar(64)            DEFAULT NULL   COMMENT '关联 endpoint_group.code，空则使用 pattern_list',
  `pattern_list`     json                   DEFAULT NULL   COMMENT '内联路径列表 [{"path":"/x/**","method":"POST"}]',
  `dimension`        char(2)       NOT NULL DEFAULT 'IP'   COMMENT '限流维度: IP/DV/UI',
  `qps`              int           NOT NULL DEFAULT 50     COMMENT '平均速率（次/秒）',
  `burst`            int           NOT NULL DEFAULT 100    COMMENT '突发容量',
  `interval_sec`     int           NOT NULL DEFAULT 1      COMMENT '统计窗口（秒）',
  `control_behavior` char(1)       NOT NULL DEFAULT 'F'    COMMENT '控制行为: F=快速失败, Q=排队等待',
  `enabled`          tinyint(1)    NOT NULL DEFAULT 1      COMMENT '是否启用',
  `priority`         int           NOT NULL DEFAULT 0      COMMENT '优先级，数字越小越优先',
  `remark`           varchar(255)           DEFAULT NULL   COMMENT '备注',
  `created_at`       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_rate_limit_rule_code` (`code`),
  KEY `idx_rate_limit_rule_group_enabled` (`group_code`, `enabled`),
  KEY `idx_rate_limit_rule_enabled_priority` (`enabled`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='网关限流规则';

-- ============================================================
-- 3. gateway_ip_list : 黑白名单（合表）
--    list_type: B=黑名单 / W=白名单
--    key_type:  IP / DV / UI / CIDR / UA / RF (Referer)
-- ============================================================
DROP TABLE IF EXISTS `gateway_ip_list`;
CREATE TABLE `gateway_ip_list` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `list_type`     char(1)       NOT NULL                COMMENT '名单类型: B=黑名单, W=白名单',
  `key_type`      char(2)       NOT NULL                COMMENT '维度: IP/DV/UI/CD(CIDR)/UA/RF',
  `key_value`     varchar(256)  NOT NULL                COMMENT '匹配值（IP/CIDR/设备指纹/userId/UA正则）',
  `reason`        varchar(255)           DEFAULT NULL   COMMENT '原因',
  `source`        char(1)       NOT NULL DEFAULT 'M'    COMMENT '来源: M=手工, A=自动',
  `effective_at`  timestamp              DEFAULT NULL   COMMENT '生效时间，NULL=立即',
  `expires_at`    timestamp              DEFAULT NULL   COMMENT '失效时间，NULL=永久',
  `enabled`       tinyint(1)    NOT NULL DEFAULT 1      COMMENT '是否启用',
  `operator_id`   bigint                 DEFAULT NULL   COMMENT '操作人ID',
  `operator_name` varchar(64)            DEFAULT NULL   COMMENT '操作人名称',
  `created_at`    timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ip_list_lookup` (`list_type`, `key_type`, `enabled`),
  KEY `idx_ip_list_key_value` (`key_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='IP/设备/用户 黑白名单';

-- ============================================================
-- 4. gateway_blacklist_event : 封禁/解封/续期审计
-- ============================================================
DROP TABLE IF EXISTS `gateway_blacklist_event`;
CREATE TABLE `gateway_blacklist_event` (
  `id`              bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `key_type`        char(2)       NOT NULL                COMMENT '维度: IP/DV/UI 等',
  `key_value`       varchar(256)  NOT NULL                COMMENT '主体值',
  `action`          char(1)       NOT NULL                COMMENT '动作: B=封禁, U=解封, R=续期',
  `trigger_source`  char(1)       NOT NULL                COMMENT '触发: A=自动, M=手工',
  `rule_code`       varchar(64)            DEFAULT NULL   COMMENT '触发的规则编码',
  `count_in_window` int                    DEFAULT NULL   COMMENT '触发窗口内命中次数',
  `ttl_sec`         int                    DEFAULT NULL   COMMENT '本次封禁时长（秒）',
  `trace_id`        varchar(64)            DEFAULT NULL   COMMENT '链路追踪ID',
  `request_path`    varchar(512)           DEFAULT NULL   COMMENT '请求路径',
  `user_agent`      varchar(512)           DEFAULT NULL   COMMENT 'User-Agent',
  `real_ip`         varchar(64)            DEFAULT NULL   COMMENT '客户端真实 IP',
  `remark`          varchar(255)           DEFAULT NULL   COMMENT '备注',
  `created_at`      timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_blacklist_event_key` (`key_type`, `key_value`),
  KEY `idx_blacklist_event_action_time` (`action`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='封禁/解封审计';

-- ============================================================
-- 5. security_challenge_policy : 挑战策略（Phase 4）
--    维度 + 触发条件 + 验证码类型 + PassToken 配置 + 拉黑阈值
-- ============================================================
DROP TABLE IF EXISTS `security_challenge_policy`;
CREATE TABLE `security_challenge_policy` (
  `id`                       bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code`                     varchar(64)   NOT NULL                COMMENT '策略编码（唯一）',
  `group_code`               varchar(64)            DEFAULT NULL   COMMENT '关联 endpoint_group.code',
  `pattern_list`             json                   DEFAULT NULL   COMMENT '内联路径列表',
  `trigger`                  varchar(32)   NOT NULL DEFAULT 'on_rate_limit' COMMENT 'always / on_rate_limit（登录失败锁定见 account-domain）',
  `challenge_type`           varchar(16)   NOT NULL DEFAULT 'SLIDER' COMMENT 'SLIDER/IMAGE/SMS/EMAIL',
  `failure_dimension`        char(2)                DEFAULT NULL   COMMENT '已废弃，保留列兼容',
  `failure_threshold`        int                    DEFAULT NULL   COMMENT '已废弃，保留列兼容',
  `failure_window_sec`       int                    DEFAULT NULL   COMMENT '已废弃，保留列兼容',
  `pass_token_ttl_sec`       int           NOT NULL DEFAULT 60     COMMENT 'PassToken 有效期（秒）',
  `pass_token_remaining`     int           NOT NULL DEFAULT 1      COMMENT 'PassToken 可用次数',
  `challenge_failure_limit`  int           NOT NULL DEFAULT 5      COMMENT '管理面保留；网关 Phase1 未实现验码失败拉黑',
  `block_ttl_sec`            int           NOT NULL DEFAULT 900    COMMENT '管理面保留；网关限流违规封禁见 SentinelBlockHandler',
  `scope`                    varchar(64)            DEFAULT NULL   COMMENT '策略作用域（与 PassToken scope 对齐）',
  `enabled`                  tinyint(1)    NOT NULL DEFAULT 1      COMMENT '是否启用',
  `priority`                 int           NOT NULL DEFAULT 0      COMMENT '优先级',
  `remark`                   varchar(255)           DEFAULT NULL   COMMENT '备注',
  `created_at`               timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`               timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_challenge_policy_code` (`code`),
  KEY `idx_challenge_policy_trigger_enabled` (`trigger`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='挑战策略';

-- ============================================================
-- 完成
-- ============================================================
SELECT 'Migration 005: security policy center (7 tables) completed.' AS result;
