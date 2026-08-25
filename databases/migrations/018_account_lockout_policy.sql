-- 账号锁定策略中心化 — 按用户类型分行
-- 目标库：ingot_security
--
-- B 端（user_type=0 ADMIN）允许永久锁（lock_duration_minutes=0）；
-- C 端（user_type=1 APP）种子为 15 分钟，管理面禁止改为 0。

USE ingot_security;

CREATE TABLE IF NOT EXISTS `account_lockout_policy_config` (
  `id`                      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_type`               VARCHAR(8)   NOT NULL                COMMENT '用户类型: 0=ADMIN, 1=APP',
  `enabled`                 TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '是否启用自动锁定',
  `max_attempts`            INT          NOT NULL DEFAULT 5      COMMENT '失败次数阈值',
  `lock_duration_minutes`   INT          NOT NULL DEFAULT 30     COMMENT '锁定时长（分钟），0=永久，仅 ADMIN 允许',
  `attempt_window_minutes`  INT          NOT NULL DEFAULT 15     COMMENT '失败计数窗口（分钟）',
  `hint_after_attempts`     INT          NOT NULL DEFAULT 3      COMMENT '从第几次失败开始给出剩余次数提示',
  `remark`                  VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
  `created_at`              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_account_lockout_user_type` (`user_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号登录失败锁定策略（按用户类型）';

INSERT INTO `account_lockout_policy_config`
  (`user_type`, `enabled`, `max_attempts`, `lock_duration_minutes`, `attempt_window_minutes`,
   `hint_after_attempts`, `remark`)
SELECT '0', 1, 5, 30, 15, 3, 'B端管理员登录失败锁定'
WHERE NOT EXISTS (
  SELECT 1 FROM `account_lockout_policy_config` WHERE `user_type` = '0'
);

INSERT INTO `account_lockout_policy_config`
  (`user_type`, `enabled`, `max_attempts`, `lock_duration_minutes`, `attempt_window_minutes`,
   `hint_after_attempts`, `remark`)
SELECT '1', 1, 5, 15, 15, 3, 'C端用户登录失败锁定'
WHERE NOT EXISTS (
  SELECT 1 FROM `account_lockout_policy_config` WHERE `user_type` = '1'
);
