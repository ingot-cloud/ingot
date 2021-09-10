package com.ingot.framework.security.oauth2.server.authorization.authentication;

import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import static com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2AuthenticationProviderUtils.getAuthenticatedClientElseThrowInvalidClient;

/**
 * <p>Description  : OAuth2UsernamePasswordAuthenticationProvider.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/9.</p>
 * <p>Time         : 6:11 下午.</p>
 */
public class OAuth2UsernamePasswordAuthenticationProvider extends DaoAuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuth2UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                (OAuth2UsernamePasswordAuthenticationToken) authentication;

        // 验证 client
        OAuth2ClientAuthenticationToken clientPrincipal =
                getAuthenticatedClientElseThrowInvalidClient(usernamePasswordAuthenticationToken);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();

        if (registeredClient == null ||
                !registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.PASSWORD)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
        }

        // user password 认证
        return super.authenticate(authentication);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (OAuth2UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }

    /**
     * 重写创建认证成功信息
     *
     * @param principal      that should be the principal in the returned object (defined by the isForcePrincipalAsString() method)
     * @param authentication that was presented to the provider for validation
     * @param user           that was loaded by the implementation
     * @return the successful authentication token
     */
    @Override
    protected Authentication createSuccessAuthentication(Object principal,
                                                         Authentication authentication,
                                                         UserDetails user) {
        Authentication superAuth = super.createSuccessAuthentication(principal, authentication, user);
        OAuth2UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                (OAuth2UsernamePasswordAuthenticationToken) authentication;

        return new OAuth2UsernamePasswordAuthenticationToken(
                superAuth, usernamePasswordAuthenticationToken.getClientPrincipal());
    }
}
