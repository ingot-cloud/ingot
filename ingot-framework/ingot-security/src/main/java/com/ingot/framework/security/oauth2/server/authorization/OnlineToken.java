package com.ingot.framework.security.oauth2.server.authorization;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 在线Token信息
 * 存储当前有效的Token完整信息
 *
 * <p>Author: wangchao</p>
 * <p>Date: 2024/12/17</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineToken implements Serializable {

	@Serial
    private static final long serialVersionUID = 1L;

	// ========== 核心标识 ==========
	/**
	 * JWT ID（使用Snowflake生成）
	 */
	private String jti;

	// ========== 用户信息 ==========
	/**
	 * 用户ID
	 */
	private Long userId;

	/**
	 * 租户ID
	 */
	private Long tenantId;

	/**
	 * 用户名
	 */
	private String principalName;

	/**
	 * 客户端ID
	 */
	private String clientId;

	// ========== 扩展信息（JWT瘦身后的字段） ==========
	/**
	 * Token认证类型
	 */
	private String authType;

	/**
	 * 用户类型
	 */
	private String userType;

	/**
	 * 权限列表
	 */
	private Set<String> authorities;

	/**
	 * 其他扩展属性
	 */
	private Map<String, Object> attributes;

	// ========== 时间信息 ==========
	/**
	 * 颁发时间
	 */
	private Instant issuedAt;

	/**
	 * 过期时间
	 */
	private Instant expiresAt;
}
