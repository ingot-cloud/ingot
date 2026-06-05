package com.ingot.cloud.gateway.filter.auth;

/**
 * {@link AuthContextRelayFilter} 写入的 exchange attribute 键。
 *
 * <p>userId 仅在网关内传递；{@link IdentityResolveFilter} 聚合进
 * {@link com.ingot.cloud.gateway.security.ClientIdentity} 并按需回填
 * {@code X-User-Id} Header 供 Sentinel USER 维度使用。</p>
 */
public final class AuthContextAttributes {

    public static final String USER_ID = "ingot.security.auth.userId";

    private AuthContextAttributes() {
    }
}
