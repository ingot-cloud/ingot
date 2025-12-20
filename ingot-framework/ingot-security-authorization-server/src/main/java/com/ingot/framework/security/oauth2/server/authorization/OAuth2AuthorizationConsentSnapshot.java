package com.ingot.framework.security.oauth2.server.authorization;

import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * OAuth2AuthorizationConsent 的快照类
 * 用于 Redis 存储，避免直接序列化 OAuth2AuthorizationConsent 的复杂性
 *
 * <p>Author: jy</p>
 * <p>Date: 2024/12/17</p>
 */
@Data
public class OAuth2AuthorizationConsentSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 注册的客户端 ID
     */
    private String registeredClientId;

    /**
     * 用户主体名称
     */
    private String principalName;

    /**
     * 用户授予客户端的权限（Scope）
     * 存储权限名称字符串，而非 GrantedAuthority 对象
     */
    private Set<String> authorities;
}
