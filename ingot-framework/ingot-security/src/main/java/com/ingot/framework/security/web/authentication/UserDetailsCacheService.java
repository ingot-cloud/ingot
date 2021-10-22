package com.ingot.framework.security.web.authentication;

import java.time.Instant;

import com.ingot.framework.security.core.userdetails.IngotUser;

/**
 * <p>Description  : UserDetailsCacheService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/21.</p>
 * <p>Time         : 5:46 下午.</p>
 */
public interface UserDetailsCacheService {

    /**
     * 保存用户授权信息
     *
     * @param user       {@link IngotUser}
     * @param expiresAt  过期时间
     * @param tokenValue token
     */
    void save(IngotUser user, Instant expiresAt, String tokenValue);

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
     * @return token value
     */
    String get(IngotUser user);
}
