package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>Description  : OAuth2PasswordAuthenticationConverter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/8.</p>
 * <p>Time         : 5:15 下午.</p>
 */
public final class OAuth2PasswordAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        // todo
        return null;
    }
}
