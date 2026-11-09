-- ingot-security-credential-data MySQL 建表脚本（MySQL 8.0.16+）。
-- 在消费服务DataSource对应的数据库执行；不选库、不删除表、不写入业务数据。
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS `fw_security_password_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '安全框架密码历史环形缓冲记录标识；数据库自增',
  `user_id` BIGINT NOT NULL COMMENT '安全框架全局用户ID；同用户按sequence_number维护环形历史',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '不可逆密码哈希，禁止作为响应明文返回',
  `sequence_number` INT NOT NULL COMMENT '序号（用于环形缓冲，从1开始）',
  `version` bigint NOT NULL DEFAULT '1' COMMENT '记录乐观锁版本；变更时按所属业务事务递增',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间点，UTC',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间点，UTC',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_sequence` (`user_id`, `sequence_number`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密码历史记录（环形缓冲）；所属模块：ingot-security-credential-data';

CREATE TABLE IF NOT EXISTS `fw_security_password_expiration` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '安全框架密码过期状态记录标识；数据库自增',
  `user_id` BIGINT NOT NULL COMMENT '安全框架全局用户ID，唯一对应一份密码过期状态',
  `last_changed_at` DATETIME NOT NULL COMMENT '最后修改密码时间；时间点按UTC保存',
  `expires_at` DATETIME NOT NULL COMMENT '到期时间点，UTC',
  `force_change` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否强制修改（0-否 1-是）',
  `grace_login_remaining` INT NOT NULL DEFAULT 0 COMMENT '剩余宽限登录次数',
  `next_warning_at` DATETIME NULL COMMENT '下次提醒时间；时间点按UTC保存',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间点，UTC',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间点，UTC',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_expires_at` (`expires_at`),
  KEY `idx_next_warning_at` (`next_warning_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密码过期信息；所属模块：ingot-security-credential-data';
