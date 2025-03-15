package com.ingot.framework.security.oauth2.server.authorization;

import java.time.Instant;

import com.ingot.framework.security.core.userdetails.InUser;

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
     * @param user      {@link InUser}
     * @param expiresAt 过期时间
     * @param value     {@link AuthorizationCache} 缓存授权信息
     */
    void save(InUser user, Instant expiresAt, AuthorizationCache value);

    /**
     * 移除用户授权信息，并且校验 tokenValue
     *
     * @param user       {@link InUser}
     * @param tokenValue token
     */
    void remove(InUser user, String tokenValue);

    /**
     * 移除用户授权信息
     *
     * @param user {@link InUser}
     */
    void remove(InUser user);

    /**
     * 获取当前 token value
     *
     * @param user {@link InUser}
     * @return {@link AuthorizationCache} 缓存授权信息
     */
    AuthorizationCache get(InUser user);
}
