package com.ingot.framework.security.core.userdetails;

import com.ingot.framework.security.oauth2.core.OAuth2Authentication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>Description  : DefaultOAuth2UserDetailsServiceManager.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/26.</p>
 * <p>Time         : 10:54 PM.</p>
 */
@Slf4j
public final class DefaultOAuth2UserDetailsServiceManager implements OAuth2UserDetailsServiceManager {
    private final List<UserDetailsService> userDetailsServices;

    public DefaultOAuth2UserDetailsServiceManager(List<UserDetailsService> userDetailsServices) {
        Assert.notEmpty(userDetailsServices, "userDetailsServices cannot be empty");
        this.userDetailsServices = Collections.unmodifiableList(new LinkedList<>(userDetailsServices));
    }

    @Override
    public UserDetails loadUser(OAuth2Authentication authentication) throws UsernameNotFoundException {
        UserDetails userDetails = null;
        for (UserDetailsService service : userDetailsServices) {
            // 如果不是OAuth2UserDetailsService，那么直接使用loadUserByUsername加载用户
            if (!(service instanceof OAuth2UserDetailsService userDetailsService)) {
                try {
                    userDetails = service.loadUserByUsername(authentication.getName());
                    if (userDetails == null) {
                        continue;
                    }
                } catch (Exception e) {
                    log.debug("[DefaultOAuth2UserDetailsServiceManager] - service[{}] errorMessage={}",
                            service, e.getLocalizedMessage());
                    continue;
                }
                return userDetails;
            }

            // 判断 GrantType
            if (!userDetailsService.supports(authentication.getGrantType())) {
                continue;
            }
            userDetails = service.loadUserByUsername(authentication.getName());
        }
        return userDetails;
    }
}
