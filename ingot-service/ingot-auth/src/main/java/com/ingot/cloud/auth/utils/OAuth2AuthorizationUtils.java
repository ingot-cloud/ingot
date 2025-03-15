package com.ingot.cloud.auth.utils;

import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationCodeRequestAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

import java.security.Principal;
import java.util.Optional;

/**
 * <p>Description  : OAuth2AuthorizationUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/25.</p>
 * <p>Time         : 11:31 上午.</p>
 */
public class OAuth2AuthorizationUtils {

    /**
     * 获取{@link InUser}
     *
     * @param authorization {@link OAuth2Authorization}
     * @return {@link InUser}
     */
    public static Optional<InUser> getUser(OAuth2Authorization authorization) {
        if (authorization == null) {
            return Optional.empty();
        }
        Object principal = authorization.getAttribute(Principal.class.getName());
        if (principal instanceof OAuth2PreAuthorizationCodeRequestAuthenticationToken preAuthToken) {
            principal = preAuthToken.getPrincipal();
        }
        if (principal instanceof OAuth2UserDetailsAuthenticationToken userDetailsToken) {
            principal = userDetailsToken.getPrincipal();
        }
        if (principal instanceof InUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }
}
