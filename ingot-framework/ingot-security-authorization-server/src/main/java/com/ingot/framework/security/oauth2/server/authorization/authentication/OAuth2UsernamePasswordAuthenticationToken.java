package com.ingot.framework.security.oauth2.server.authorization.authentication;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

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

    public OAuth2UsernamePasswordAuthenticationToken(Object principal,
                                                     Object credentials,
                                                     Collection<? extends GrantedAuthority> authorities,
                                                     Authentication clientPrincipal) {
        super(principal, credentials, authorities);
        this.clientPrincipal = clientPrincipal;
    }

    @JsonIgnore
    public Authentication getClientPrincipal() {
        return this.clientPrincipal;
    }
}
