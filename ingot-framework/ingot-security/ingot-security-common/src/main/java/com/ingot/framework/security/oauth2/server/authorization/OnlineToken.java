package com.ingot.framework.security.oauth2.server.authorization;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>在线会话主数据，以 {@link #sid} 为主键存放于 Redis，是在线态与权限补全的唯一权威来源。</p>
 *
 * <p>{@link #sid} 等于 {@code OAuth2Authorization.id}，refresh 换发不变；{@link #jti} 只标识
 * 当前那一个 Access Token，每次 refresh 都会被覆盖。因此「会话是否在线」只能按 sid 判断，
 * 不能按 jti 判断。</p>
 *
 * @author wangchao
 * @since 1.0.0
 * @see OnlineTokenService
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
	 * 会话 ID，等于 {@code OAuth2Authorization.id}
	 */
	private String sid;

	/**
	 * 当前 Access Token 的 JWT ID，refresh 后指向新 Token
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
	 * 当前登录租户下用户所属部门 ID 列表
	 * <p>语义对齐 {@link #authorities}：仅存"已切片到当前租户"的形态；
	 * pre_authorization_code 切租户后由 {@code RedisOnlineTokenService.save} 重新落地。</p>
	 */
	private List<Long> deptIds;

	/**
	 * 其他扩展属性
	 */
	private Map<String, Object> attributes;

	// ========== 时间信息 ==========
	/**
	 * 会话创建时间，refresh 不改写
	 */
	private Instant issuedAt;

    /**
     * 会话过期时间，对齐 Refresh Token 剩余寿命（无 Refresh Token 时对齐 Access Token）
     */
    private Instant expiresAt;

    /**
     * 最近一次凭据活动时间，登录与 refresh 时更新
     * <p>资源服务器热路径不写该字段，避免每请求写 Redis 造成写放大。</p>
     */
    private Instant lastAccessAt;

    /**
     * 登录IP地址
     */
    private String ipAddress;

    /**
     * User-Agent
     */
    private String userAgent;

    /**
     * 登录设备类型（PC、Mobile、Tablet等）
     */
    private String deviceType;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 登录地理位置（可选）
     */
    private String location;
}
