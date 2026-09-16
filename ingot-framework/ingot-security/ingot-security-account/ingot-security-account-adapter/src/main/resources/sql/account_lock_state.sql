-- ============================================================
-- account_lock_state 表 DDL
-- 模块: ingot-security-account-adapter
-- 说明: 账号锁定状态表，记录账号的锁定/解锁状态及失败计数。
--       引入 ingot-security-account-adapter 模块时需执行此脚本。
--
-- 字段说明：
--   user_type 与 UserTypeEnum.value 对应：
--     0 = ADMIN（系统用户 / IAM）
--     1 = APP  （C端用户 / Member）
--   (user_id, user_type) 联合唯一，支持 IAM 和 Member 同 ID 的用户共存。
-- ============================================================

CREATE TABLE IF NOT EXISTS account_lock_state (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id         BIGINT       NOT NULL               COMMENT '用户ID',
  user_type       VARCHAR(20)  NOT NULL DEFAULT '0'   COMMENT '用户类型（同 UserTypeEnum.value：0-系统用户 1-C端用户）',

  -- 锁定状态
  locked          TINYINT(1)   NOT NULL DEFAULT 0     COMMENT '是否锁定（0-否 1-是）',
  lock_type       VARCHAR(20)  DEFAULT NULL            COMMENT '锁定类型（MANUAL-手动 AUTO-自动）',
  lock_reason_code   VARCHAR(50)  DEFAULT NULL         COMMENT '锁定原因代码',
  lock_reason_detail VARCHAR(500) DEFAULT NULL         COMMENT '锁定原因详情',

  -- 锁定时间信息
  locked_at       DATETIME     DEFAULT NULL            COMMENT '锁定时间',
  locked_until    DATETIME     DEFAULT NULL            COMMENT '锁定到期时间（NULL=永久锁定）',

  -- 操作信息（手动锁定时填写）
  operator_id     BIGINT       DEFAULT NULL            COMMENT '操作人ID',
  operator_name   VARCHAR(64)  DEFAULT NULL            COMMENT '操作人姓名',

  -- 登录失败计数
  failed_login_count INT        NOT NULL DEFAULT 0    COMMENT '连续登录失败次数',
  last_failed_at  DATETIME     DEFAULT NULL            COMMENT '最后一次失败时间',

  -- 时间戳
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                    COMMENT '创建时间',
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (id),
  UNIQUE KEY uk_user_id_type (user_id, user_type)       COMMENT '用户ID + 用户类型联合唯一',
  KEY idx_locked (locked, locked_until) USING BTREE     COMMENT '锁定状态 + 到期时间索引（自动解锁任务使用）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号锁定状态表';
