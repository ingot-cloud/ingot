package com.ingot.framework.security.core;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * <p>Description  : OAuth2认证信息，包含用户和client.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/27.</p>
 * <p>Time         : 7:46 PM.</p>
 */
public interface OAuth2Authentication extends Authentication {

    /**
     * 获取 {@link AuthorizationGrantType}
     *
     * @return {@link AuthorizationGrantType}
     */
    AuthorizationGrantType getGrantType();

    /**
     * 获取Client认证信息
     *
     * @return {@link Authentication}
     */
    Authentication getClient();
}
