package com.ingot.framework.security.core.userdetails;

import com.ingot.framework.security.core.OAuth2Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * <p>Description  : OAuth2扩展UserDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/7/11.</p>
 * <p>Time         : 4:33 PM.</p>
 */
public interface OAuth2UserDetailsService extends UserDetailsService {
    /**
     * 判断 {@link AuthorizationGrantType}
     *
     * @param grantType {@link AuthorizationGrantType}
     * @return 是否支持目标 {@link AuthorizationGrantType}
     */
    default boolean supports(AuthorizationGrantType grantType) {
        return true;
    }

    /**
     * 根据Authentication加载User
     *
     * @param authentication {@link OAuth2Authentication}
     * @return {@link UserDetails}
     * @throws UsernameNotFoundException if the user could not be found or the user has no GrantedAuthority
     */
    default UserDetails loadUser(OAuth2Authentication authentication) throws UsernameNotFoundException {
        return loadUserByUsername(authentication.getName());
    }
}
