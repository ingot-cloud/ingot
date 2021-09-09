package com.ingot.framework.security.oauth2.server.authorization.authentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.util.Map;

/**
 * <p>Description  : OAuth2PasswordAuthenticationToken.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/8.</p>
 * <p>Time         : 5:24 下午.</p>
 */
public class OAuth2PasswordAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {
    private final Object userPrincipal;
    private final Object credentials;

    /**
     * Sub-class constructor.
     *
     * @param userPrincipal        the authenticated user principal
     * @param clientPrincipal      the authenticated client principal
     * @param additionalParameters the additional parameters
     */
    public OAuth2PasswordAuthenticationToken(Object userPrincipal,
                                             Object credentials,
                                             Authentication clientPrincipal,
                                             Map<String, Object> additionalParameters) {
        super(AuthorizationGrantType.PASSWORD, clientPrincipal, additionalParameters);
        this.userPrincipal = userPrincipal;
        this.credentials = credentials;
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.userPrincipal;
    }

    public Object getUserPrincipal() {
        return super.getPrincipal();
    }
}
