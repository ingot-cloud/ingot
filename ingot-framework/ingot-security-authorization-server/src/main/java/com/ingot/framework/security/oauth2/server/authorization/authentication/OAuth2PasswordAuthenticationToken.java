package com.ingot.framework.security.oauth2.server.authorization.authentication;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

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
     * @param userPrincipal        the authenticated user principal {@link OAuth2UserDetailsAuthenticationToken}
     * @param clientPrincipal      the authenticated client principal
     * @param additionalParameters the additional parameters
     */
    public OAuth2PasswordAuthenticationToken(Authentication userPrincipal,
                                             Authentication clientPrincipal,
                                             Map<String, Object> additionalParameters) {
        super(AuthorizationGrantType.PASSWORD, clientPrincipal, additionalParameters);
        this.userPrincipal = userPrincipal;
        setDetails(userPrincipal.getDetails());
    }

    public Authentication getUserPrincipal() {
        return this.userPrincipal;
    }
}
