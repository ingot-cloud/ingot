-- ============================================================
-- 为 PMS Service 添加密码管理相关表
-- ============================================================

USE ingot_core;

-- ------------------------------------------------------------
-- 表结构：password_history (密码历史记录 - 环形缓冲)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `password_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希值',
  `sequence_number` INT NOT NULL COMMENT '序号（用于环形缓冲，从1开始）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_sequence` (`user_id`, `sequence_number`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密码历史记录（环形缓冲）';

-- ------------------------------------------------------------
-- 表结构：password_expiration (密码过期信息)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `password_expiration` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `last_changed_at` DATETIME NOT NULL COMMENT '最后修改密码时间',
  `expires_at` DATETIME NOT NULL COMMENT '密码过期时间',
  `force_change` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否强制修改（0-否 1-是）',
  `grace_login_remaining` INT NOT NULL DEFAULT 0 COMMENT '剩余宽限登录次数',
  `next_warning_at` DATETIME NULL COMMENT '下次提醒时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_expires_at` (`expires_at`),
  KEY `idx_next_warning_at` (`next_warning_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='密码过期信息';

-- ============================================================================
-- 为现有用户初始化密码过期信息（可选）
-- ============================================================================

-- 注意：仅在首次部署时执行，为现有用户初始化密码过期数据
-- 如果是新系统，可以跳过这一步

/*
INSERT INTO password_expiration (user_id, password_set_at, expire_at)
SELECT
    id,
    COALESCE(updated_at, created_at) as password_set_at,
    DATE_ADD(COALESCE(updated_at, created_at), INTERVAL 90 DAY) as expire_at
FROM member_user
WHERE NOT EXISTS (
    SELECT 1 FROM password_expiration
    WHERE password_expiration.user_id = member_user.id
);
*/

-- ============================================================================
-- 完成
-- ============================================================================