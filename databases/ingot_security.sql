-- ============================================================================
-- Ingot Credential Security Database Schema
-- 凭证安全模块数据库初始化脚本
-- ============================================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ingot_security
CHARACTER SET utf8mb4 
COLLATE utf8mb4_0900_ai_ci;

USE ingot_security;

-- ============================================================================
-- 策略配置表
-- ============================================================================

CREATE TABLE IF NOT EXISTS credential_policy_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT COMMENT '租户ID，NULL表示全局默认策略',
    policy_type VARCHAR(50) NOT NULL COMMENT '策略类型: STRENGTH(强度), EXPIRATION(过期), HISTORY(历史)',
    policy_config JSON NOT NULL COMMENT '策略配置JSON',
    priority INT DEFAULT 0 COMMENT '优先级，数字越小优先级越高',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_tenant_type (tenant_id, policy_type),
    INDEX idx_type_enabled (policy_type, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='凭证策略配置表';

-- ============================================================================
-- 初始化全局默认策略
-- ============================================================================

-- 全局默认：密码强度策略（标准企业配置）
INSERT INTO credential_policy_config 
(tenant_id, policy_type, policy_config, priority, enabled, created_at, updated_at) 
VALUES
(NULL, 'STRENGTH', '{
  "minLength": 8,
  "maxLength": 32,
  "requireUppercase": true,
  "requireLowercase": true,
  "requireDigit": true,
  "requireSpecialChar": false,
  "specialChars": "!@#$%^&*()_+-=[]{}|;:,.<>?",
  "forbiddenPatterns": ["password", "123456", "admin", "qwerty", "abc123"],
  "forbidUserAttributes": true
}', 10, true, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
  policy_config = VALUES(policy_config),
  updated_at = NOW();

-- 全局默认：密码过期策略（90天）
INSERT INTO credential_policy_config 
(tenant_id, policy_type, policy_config, priority, enabled, created_at, updated_at) 
VALUES
(NULL, 'EXPIRATION', '{
  "enabled": true,
  "maxDays": 90,
  "warningDaysBefore": 7,
  "graceLoginCount": 3,
  "forceChangeAfterReset": true
}', 20, true, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
  policy_config = VALUES(policy_config),
  updated_at = NOW();

-- 全局默认：密码历史策略（保留最近5次）
INSERT INTO credential_policy_config 
(tenant_id, policy_type, policy_config, priority, enabled, created_at, updated_at) 
VALUES
(NULL, 'HISTORY', '{
  "enabled": true,
  "keepRecentCount": 5,
  "checkCount": 5
}', 30, true, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
  policy_config = VALUES(policy_config),
  updated_at = NOW();

-- ============================================================================
-- 示例：高安全要求租户（租户ID=1）
-- ============================================================================

INSERT INTO credential_policy_config 
(tenant_id, policy_type, policy_config, priority, enabled, created_at, updated_at) 
VALUES
(1, 'STRENGTH', '{
  "minLength": 12,
  "maxLength": 64,
  "requireUppercase": true,
  "requireLowercase": true,
  "requireDigit": true,
  "requireSpecialChar": true,
  "specialChars": "!@#$%^&*()_+-=[]{}|;:,.<>?",
  "forbiddenPatterns": ["password", "123456", "admin", "qwerty", "abc123", "welcome", "company"],
  "forbidUserAttributes": true
}', 10, true, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
  policy_config = VALUES(policy_config),
  updated_at = NOW();

INSERT INTO credential_policy_config 
(tenant_id, policy_type, policy_config, priority, enabled, created_at, updated_at) 
VALUES
(1, 'EXPIRATION', '{
  "enabled": true,
  "maxDays": 60,
  "warningDaysBefore": 14,
  "graceLoginCount": 1,
  "forceChangeAfterReset": true
}', 20, true, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
  policy_config = VALUES(policy_config),
  updated_at = NOW();

INSERT INTO credential_policy_config 
(tenant_id, policy_type, policy_config, priority, enabled, created_at, updated_at) 
VALUES
(1, 'HISTORY', '{
  "enabled": true,
  "keepRecentCount": 10,
  "checkCount": 10
}', 30, true, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
  policy_config = VALUES(policy_config),
  updated_at = NOW();

-- ============================================================================
-- 完成
-- ============================================================================
