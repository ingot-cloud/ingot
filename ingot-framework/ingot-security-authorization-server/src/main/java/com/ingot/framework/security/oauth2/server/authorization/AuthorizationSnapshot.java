package com.ingot.framework.security.oauth2.server.authorization;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import lombok.Data;

/**
 * OAuth2Authorization 的快照类
 * 用于 Redis 存储，避免直接序列化 OAuth2Authorization 的复杂性
 *
 * <p>Author: jy</p>
 * <p>Date: 2024/12/17</p>
 */
@Data
public class AuthorizationSnapshot implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // ========== 基本信息 ==========
    /**
     * Authorization ID
     */
    private String id;

    /**
     * 注册的客户端 ID
     */
    private String registeredClientId;

    /**
     * 用户主体名称
     */
    private String principalName;

    /**
     * 授权类型（如：authorization_code, refresh_token）
     */
    private String authorizationGrantType;

    /**
     * 授权的 Scope
     */
    private Set<String> authorizedScopes;

    /**
     * Attributes（存储额外信息）
     */
    private Map<String, Object> attributes;

    // ========== Authorization Code ==========
    /**
     * 授权码值
     */
    private String authorizationCodeValue;

    /**
     * 授权码颁发时间
     */
    private Instant authorizationCodeIssuedAt;

    /**
     * 授权码过期时间
     */
    private Instant authorizationCodeExpiresAt;

    /**
     * 授权码元数据
     */
    private Map<String, Object> authorizationCodeMetadata;

    // ========== Access Token ==========
    /**
     * Access Token 值
     */
    private String accessTokenValue;

    /**
     * Access Token 颁发时间
     */
    private Instant accessTokenIssuedAt;

    /**
     * Access Token 过期时间
     */
    private Instant accessTokenExpiresAt;

    /**
     * Access Token 类型（如：Bearer）
     */
    private String accessTokenType;

    /**
     * Access Token Scopes
     */
    private Set<String> accessTokenScopes;

    /**
     * Access Token 元数据（包含 claims 等）
     */
    private Map<String, Object> accessTokenMetadata;

    // ========== Refresh Token ==========
    /**
     * Refresh Token 值
     */
    private String refreshTokenValue;

    /**
     * Refresh Token 颁发时间
     */
    private Instant refreshTokenIssuedAt;

    /**
     * Refresh Token 过期时间
     */
    private Instant refreshTokenExpiresAt;

    /**
     * Refresh Token 元数据
     */
    private Map<String, Object> refreshTokenMetadata;

    // ========== OIDC ID Token ==========
    /**
     * ID Token 值
     */
    private String oidcIdTokenValue;

    /**
     * ID Token 颁发时间
     */
    private Instant oidcIdTokenIssuedAt;

    /**
     * ID Token 过期时间
     */
    private Instant oidcIdTokenExpiresAt;

    /**
     * ID Token 元数据（包含 claims 等）
     */
    private Map<String, Object> oidcIdTokenMetadata;

    // ========== User Code (Device Flow) ==========
    /**
     * User Code 值
     */
    private String userCodeValue;

    /**
     * User Code 颁发时间
     */
    private Instant userCodeIssuedAt;

    /**
     * User Code 过期时间
     */
    private Instant userCodeExpiresAt;

    /**
     * User Code 元数据
     */
    private Map<String, Object> userCodeMetadata;

    // ========== Device Code (Device Flow) ==========
    /**
     * Device Code 值
     */
    private String deviceCodeValue;

    /**
     * Device Code 颁发时间
     */
    private Instant deviceCodeIssuedAt;

    /**
     * Device Code 过期时间
     */
    private Instant deviceCodeExpiresAt;

    /**
     * Device Code 元数据
     */
    private Map<String, Object> deviceCodeMetadata;

    // ========== State ==========
    /**
     * State 参数（CSRF 保护）
     */
    private String state;
}
