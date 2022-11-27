package com.ingot.framework.security.core.userdetails;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.ingot.framework.security.core.OAuth2Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

/**
 * <p>Description  : DefaultOAuth2UserDetailsServiceManager.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/26.</p>
 * <p>Time         : 10:54 PM.</p>
 */
public final class DefaultOAuth2UserDetailsServiceManager implements OAuth2UserDetailsServiceManager {
    private final List<OAuth2UserDetailsService> userDetailsServices;

    public DefaultOAuth2UserDetailsServiceManager(List<OAuth2UserDetailsService> userDetailsServices) {
        Assert.notEmpty(userDetailsServices, "userDetailsServices cannot be empty");
        this.userDetailsServices = Collections.unmodifiableList(new LinkedList<>(userDetailsServices));
    }

    @Override
    public UserDetails loadUser(OAuth2Authentication authentication) throws UsernameNotFoundException {
        UserDetails userDetails = null;
        for (OAuth2UserDetailsService service : userDetailsServices) {
            if (!service.supports(authentication.getGrantType())) {
                continue;
            }
            userDetails = service.loadUser(authentication);
            if (userDetails == null) {
                userDetails = service.loadUserByUsername(authentication.getName());
            }
        }
        return userDetails;
    }
}
