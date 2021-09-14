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
    private final Authentication userPrincipal;

    /**
     * Sub-class constructor.
     *
     * @param userPrincipal        the authenticated user principal {@link OAuth2UsernamePasswordAuthenticationToken}
     * @param additionalParameters the additional parameters
     */
    public OAuth2PasswordAuthenticationToken(Authentication userPrincipal,
                                             Map<String, Object> additionalParameters) {
        super(AuthorizationGrantType.PASSWORD,
                ((OAuth2UsernamePasswordAuthenticationToken) userPrincipal).getClientPrincipal(),
                additionalParameters);
        this.userPrincipal = userPrincipal;
        setDetails(userPrincipal.getDetails());
    }

    public Authentication getUserPrincipal() {
        return this.userPrincipal;
    }

    @Override
    public String getName() {
        return userPrincipal.getName();
    }
}
