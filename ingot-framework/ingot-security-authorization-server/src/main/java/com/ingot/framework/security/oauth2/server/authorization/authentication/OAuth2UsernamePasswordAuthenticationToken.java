package com.ingot.framework.security.oauth2.server.authorization.authentication;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * <p>Description  : OAuth2UsernamePasswordAuthenticationToken.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/9.</p>
 * <p>Time         : 5:30 下午.</p>
 */
public class OAuth2UsernamePasswordAuthenticationToken extends UsernamePasswordAuthenticationToken {
    private final Authentication clientPrincipal;

    public OAuth2UsernamePasswordAuthenticationToken(Object principal,
                                                     Object credentials,
                                                     Authentication clientPrincipal) {
        super(principal, credentials);
        this.clientPrincipal = clientPrincipal;
    }

    public OAuth2UsernamePasswordAuthenticationToken(Authentication userPrincipal,
                                                     Authentication clientPrincipal) {
        super(userPrincipal.getPrincipal(), userPrincipal.getCredentials(), userPrincipal.getAuthorities());
        setDetails(userPrincipal.getDetails());
        this.clientPrincipal = clientPrincipal;
    }

    public Authentication getClientPrincipal() {
        return this.clientPrincipal;
    }
}
