package com.ingot.framework.security.core.context;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ingot.framework.security.core.userdetails.IngotUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

/**
 * <p>Description  : SecurityAuthContext.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/26.</p>
 * <p>Time         : 3:18 下午.</p>
 */
public final class SecurityAuthContext {

    /**
     * 获取Authentication
     *
     * @return {@link Authentication}
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取指定{@link Authentication}中的用户信息
     *
     * @param authentication Target Authentication
     * @return {@link IngotUser}
     */
    public static IngotUser getUser(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof IngotUser) {
            return (IngotUser) principal;
        }
        return null;
    }

    /**
     * 获取用户
     *
     * @return {@link IngotUser}
     */
    public static IngotUser getUser() {
        return getUser(getAuthentication());
    }

    /**
     * 获取当前token
     *
     * @return {@link Optional} of {@link OAuth2AccessToken}
     */
    public static Optional<OAuth2AccessToken> getToken() {
        DefaultOAuth2AccessToken accessToken = null;
        Authentication authentication = getAuthentication();
        if (authentication != null) {
            Object details = authentication.getDetails();
            if (details instanceof OAuth2AuthenticationDetails) {
                OAuth2AuthenticationDetails holder = (OAuth2AuthenticationDetails) details;
                String token = holder.getTokenValue();
                accessToken = new DefaultOAuth2AccessToken(token);
            }
        }
        return Optional.ofNullable(accessToken);
    }

    /**
     * 获取用户角色信息
     *
     * @return 角色集合
     */
    public static List<String> getRoles() {
        Authentication authentication = getAuthentication();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

}
