package com.ingot.cloud.auth.utils;

import java.security.Principal;
import java.util.Optional;

import com.ingot.framework.security.core.userdetails.IngotUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

/**
 * <p>Description  : OAuth2AuthorizationUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/25.</p>
 * <p>Time         : 11:31 上午.</p>
 */
public class OAuth2AuthorizationUtils {

    /**
     * 获取{@link IngotUser}
     *
     * @param authorization {@link OAuth2Authorization}
     * @return {@link IngotUser}
     */
    public static Optional<IngotUser> getUser(OAuth2Authorization authorization) {
        Object principal = authorization.getAttribute(Principal.class.getName());
        if (principal instanceof Authentication) {
            return Optional.of((IngotUser) ((Authentication) principal).getPrincipal());
        }
        return Optional.empty();
    }
}
