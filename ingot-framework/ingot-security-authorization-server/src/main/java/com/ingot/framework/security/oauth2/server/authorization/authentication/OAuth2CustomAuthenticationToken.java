package com.ingot.framework.security.oauth2.server.authorization.authentication;

import com.ingot.framework.security.oauth2.core.InAuthorizationGrantType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.util.Map;

/**
 * <p>Description  : 自定义OAuth2认证Token.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/8.</p>
 * <p>Time         : 5:24 下午.</p>
 */
public class OAuth2CustomAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {
    private final Authentication userPrincipal;

    /**
     * Sub-class constructor.
     *
     * @param userPrincipal        the authenticated user principal {@link OAuth2UserDetailsAuthenticationToken}
     * @param clientPrincipal      the authenticated client principal
     * @param additionalParameters the additional parameters
     */
    public OAuth2CustomAuthenticationToken(Authentication userPrincipal,
                                           Authentication clientPrincipal,
                                           Map<String, Object> additionalParameters) {
        super(InAuthorizationGrantType.PASSWORD, clientPrincipal, additionalParameters);
        this.userPrincipal = userPrincipal;
        setDetails(userPrincipal.getDetails());
    }

    public Authentication getUserPrincipal() {
        return this.userPrincipal;
    }
}
