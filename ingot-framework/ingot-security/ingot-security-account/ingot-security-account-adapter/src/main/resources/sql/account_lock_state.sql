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
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '安全框架账号锁定状态记录标识；数据库自增',
  user_id         BIGINT       NOT NULL COMMENT '被锁定的安全用户ID，与user_type共同唯一标识用户',
  user_type       VARCHAR(20)  NOT NULL DEFAULT '0' COMMENT '安全用户类型，对应UserTypeEnum：0系统账号、1会员账号',

  -- 锁定状态
  locked          TINYINT(1)   NOT NULL DEFAULT 0     COMMENT '是否锁定（0-否 1-是）',
  lock_type       VARCHAR(20)  DEFAULT NULL            COMMENT '锁定类型（MANUAL-手动 AUTO-自动）',
  lock_reason_code   VARCHAR(50)  DEFAULT NULL         COMMENT '锁定原因代码',
  lock_reason_detail VARCHAR(500) DEFAULT NULL         COMMENT '锁定原因详情',

  -- 锁定时间信息
  locked_at       DATETIME     DEFAULT NULL COMMENT '锁定时间；时间点按UTC保存',
  locked_until    DATETIME     DEFAULT NULL COMMENT '锁定到期时间（NULL=永久锁定）；时间点按UTC保存',

  -- 操作信息（手动锁定时填写）
  operator_id     BIGINT       DEFAULT NULL            COMMENT '操作人ID',
  operator_name   VARCHAR(64)  DEFAULT NULL            COMMENT '操作人姓名',

  -- 登录失败计数
  failed_login_count INT        NOT NULL DEFAULT 0    COMMENT '连续登录失败次数',
  last_failed_at  DATETIME     DEFAULT NULL COMMENT '最后一次失败时间；时间点按UTC保存',

  -- 时间戳
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间点，UTC',
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间点，UTC',

  PRIMARY KEY (id),
  UNIQUE KEY uk_user_id_type (user_id, user_type)       COMMENT '用户ID + 用户类型联合唯一',
  KEY idx_locked (locked, locked_until) USING BTREE     COMMENT '锁定状态 + 到期时间索引（自动解锁任务使用）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账号锁定状态表';
