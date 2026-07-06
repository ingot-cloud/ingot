package com.ingot.framework.security.core.userdetails;

import com.ingot.framework.security.oauth2.core.OAuth2Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * <p>Description  : OAuth2UserDetailsServiceManager.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/27.</p>
 * <p>Time         : 9:37 PM.</p>
 */
public interface OAuth2UserDetailsServiceManager {

    /**
     * 根据Authentication加载User
     *
     * @param authentication {@link OAuth2Authentication}
     * @return {@link UserDetails}
     * @throws UsernameNotFoundException if the user could not be found or the user has no GrantedAuthority
     */
    UserDetails loadUser(OAuth2Authentication authentication) throws UsernameNotFoundException;
}
