package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * <p>Description  : OAuth2UserDetailsPasswordAuthenticationConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/27.</p>
 * <p>Time         : 6:14 PM.</p>
 */
public class OAuth2UserDetailsPasswordAuthenticationConverter extends OAuth2UserDetailsAuthenticationConverter {
    @Override
    protected AuthorizationGrantType getGrantType() {
        return AuthorizationGrantType.PASSWORD;
    }
}