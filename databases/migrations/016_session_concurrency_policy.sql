-- L5: 会话安全 — 并发会话策略表 + GLOBAL 兜底种子
-- 目标库：ingot_security
--
-- 背景：并发会话不再由 OAuth2 client tokenSettings 的 STANDARD/UNIQUE 二值决定，
-- 改为中心化策略（最多 N 个会话 + 超限行为），Auth 侧经 Inner API 与分层缓存读取。
-- GLOBAL 种子 max_sessions=0 表示不限制，保持升级后行为与升级前一致（client
-- UNIQUE 仍在策略无限时作为缺省 N=1 生效），需要收紧时再改策略或新增窄 scope 记录。

USE ingot_security;

CREATE TABLE IF NOT EXISTS `session_concurrency_policy` (
  `id`                      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scope`                   VARCHAR(16)  NOT NULL                COMMENT '生效范围: GLOBAL/CLIENT/USER_TYPE',
  `client_id`               VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT 'scope=CLIENT 时的 clientId，其余为空串',
  `user_type`               VARCHAR(8)   NOT NULL DEFAULT ''     COMMENT 'scope=USER_TYPE 时的用户类型，其余为空串',
  `max_sessions`            INT          NOT NULL DEFAULT 0      COMMENT '同一维度最大会话数，0=不限制',
  `dimension`               VARCHAR(16)  NOT NULL DEFAULT 'USER_CLIENT' COMMENT '并发计数维度: USER_CLIENT',
  `overflow`                VARCHAR(16)  NOT NULL DEFAULT 'KICK_OLDEST' COMMENT '超限行为: REJECT/KICK_OLDEST/KICK_ALL',
  `admin_forbid_concurrent` TINYINT(1)   NOT NULL DEFAULT 0      COMMENT 'ADMIN 用户强制单会话',
  `enabled`                 TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '是否启用',
  `remark`                  VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
  `created_at`              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  -- client_id / user_type 存空串而非 NULL：MySQL 唯一索引不比较 NULL，
  -- 用 NULL 会让同一 scope 出现重复行
  UNIQUE KEY `uq_session_concurrency_scope` (`scope`, `client_id`, `user_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='并发会话策略';

INSERT INTO `session_concurrency_policy`
  (`scope`, `client_id`, `user_type`, `max_sessions`, `dimension`, `overflow`,
   `admin_forbid_concurrent`, `enabled`, `remark`)
SELECT 'GLOBAL', '', '', 0, 'USER_CLIENT', 'KICK_OLDEST', 0, 1, '全局兜底：不限制并发会话'
WHERE NOT EXISTS (
  SELECT 1 FROM `session_concurrency_policy`
  WHERE `scope` = 'GLOBAL' AND `client_id` = '' AND `user_type` = ''
);
