package com.ingot.framework.security.oauth2.server.authorization.authentication;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.util.Assert;

/**
 * <p>Description  : OAuth2用户详情认证Token.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/9.</p>
 * <p>Time         : 5:30 下午.</p>
 */
public class OAuth2UserDetailsAuthenticationToken extends AbstractAuthenticationToken {
    private final Authentication clientPrincipal;
    private final Object principal;
    private Object credentials;
    private AuthorizationGrantType grantType;

    public OAuth2UserDetailsAuthenticationToken(Object principal,
                                                Object credentials,
                                                AuthorizationGrantType grantType,
                                                Authentication clientPrincipal) {
        super(null);
        this.principal = principal;
        this.credentials = credentials;
        this.grantType = grantType;
        this.clientPrincipal = clientPrincipal;
    }

    public OAuth2UserDetailsAuthenticationToken(Object principal,
                                                Object credentials,
                                                Authentication clientPrincipal,
                                                Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        this.clientPrincipal = clientPrincipal;
        super.setAuthenticated(true); // must use super, as we override
    }

    public static OAuth2UserDetailsAuthenticationToken unauthenticated(Object principal,
                                                                       Object credentials,
                                                                       AuthorizationGrantType grantType,
                                                                       Authentication clientPrincipal) {
        return new OAuth2UserDetailsAuthenticationToken(principal, credentials,
                grantType, clientPrincipal);
    }

    public static OAuth2UserDetailsAuthenticationToken authenticated(Object principal,
                                                                     Object credentials,
                                                                     Authentication clientPrincipal,
                                                                     Collection<? extends GrantedAuthority> authorities) {
        return new OAuth2UserDetailsAuthenticationToken(principal, credentials, clientPrincipal, authorities);
    }

    @JsonIgnore
    public Authentication getClientPrincipal() {
        return this.clientPrincipal;
    }

    @JsonIgnore
    public AuthorizationGrantType getGrantType() {
        return this.grantType;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        Assert.isTrue(!isAuthenticated,
                "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }
}
