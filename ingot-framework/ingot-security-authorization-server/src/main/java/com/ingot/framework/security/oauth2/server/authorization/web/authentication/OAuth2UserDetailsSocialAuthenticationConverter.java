package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import com.ingot.framework.security.core.userdetails.UserDetailsAuthorizationGrantType;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * <p>Description  : OAuth2UserDetailsSocialAuthenticationConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/28.</p>
 * <p>Time         : 12:00 PM.</p>
 */
public class OAuth2UserDetailsSocialAuthenticationConverter extends OAuth2UserDetailsAuthenticationConverter {
    @Override
    protected AuthorizationGrantType getGrantType() {
        return UserDetailsAuthorizationGrantType.SOCIAL;
    }
}
