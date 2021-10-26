package com.ingot.framework.security.oauth2.server.authorization;

import java.time.Instant;

import com.ingot.framework.security.core.userdetails.IngotUser;

/**
 * <p>Description  : AuthorizationCacheService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/21.</p>
 * <p>Time         : 5:46 下午.</p>
 */
public interface AuthorizationCacheService {

    /**
     * 保存用户授权信息
     *
     * @param user      {@link IngotUser}
     * @param expiresAt 过期时间
     * @param value     {@link AuthorizationCache} 缓存授权信息
     */
    void save(IngotUser user, Instant expiresAt, AuthorizationCache value);

    /**
     * 移除用户授权信息，并且校验 tokenValue
     *
     * @param user       {@link IngotUser}
     * @param tokenValue token
     */
    void remove(IngotUser user, String tokenValue);

    /**
     * 移除用户授权信息
     *
     * @param user {@link IngotUser}
     */
    void remove(IngotUser user);

    /**
     * 获取当前 token value
     *
     * @param user {@link IngotUser}
     * @return {@link AuthorizationCache} 缓存授权信息
     */
    AuthorizationCache get(IngotUser user);
}
