package com.ingot.framework.security.oauth2.server.authorization;

import java.time.Instant;

import org.springframework.util.Assert;

/**
 * <p>一次会话落库（登录或 refresh 换发）所需的标识与时效参数。</p>
 *
 * <p>把四个强相关参数聚成一个入参，避免 {@code save} 出现同类型形参相邻而易于错位调用；
 * 同时明确区分「会话寿命」与「Access Token 寿命」两条不同的 TTL。</p>
 *
 * @param sid                   会话 ID，等于 {@code OAuth2Authorization.id}
 * @param jti                   本次签发的 Access Token 的 JWT ID
 * @param accessTokenExpiresAt  Access Token 过期时间，决定 jti 索引的 TTL
 * @param sessionExpiresAt      会话过期时间，决定会话主数据的 TTL；应对齐 Refresh Token 寿命
 * @author jy
 * @since 1.0.0
 */
public record OnlineSessionRegistration(String sid,
                                        String jti,
                                        Instant accessTokenExpiresAt,
                                        Instant sessionExpiresAt) {

    public OnlineSessionRegistration {
        Assert.hasText(sid, "sid cannot be empty");
        Assert.hasText(jti, "jti cannot be empty");
        Assert.notNull(accessTokenExpiresAt, "accessTokenExpiresAt cannot be null");
        Assert.notNull(sessionExpiresAt, "sessionExpiresAt cannot be null");
    }
}
