-- 006: 限流违规升级全局配置（单行表）
-- 依赖 005_security_policy_center.sql

DROP TABLE IF EXISTS `gateway_violation_escalation`;
CREATE TABLE `gateway_violation_escalation` (
  `id`                  bigint        NOT NULL                COMMENT '主键，固定 1',
  `window_sec`          int           NOT NULL DEFAULT 60     COMMENT '违规计数滑动窗口(秒)',
  `block_threshold`     int           NOT NULL DEFAULT 30     COMMENT '窗口内限流拒绝次数阈值',
  `temp_block_ttl_sec`  int           NOT NULL DEFAULT 900    COMMENT '临时封禁TTL(秒)',
  `enabled`             tinyint(1)    NOT NULL DEFAULT 1      COMMENT '是否启用违规升级',
  `created_at`          datetime      NOT NULL                COMMENT '创建时间',
  `updated_at`          datetime      NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='限流违规升级全局配置';

INSERT INTO `gateway_violation_escalation`
  (`id`, `window_sec`, `block_threshold`, `temp_block_ttl_sec`, `enabled`, `created_at`, `updated_at`)
VALUES
  (1, 60, 30, 900, 1, NOW(), NOW());
