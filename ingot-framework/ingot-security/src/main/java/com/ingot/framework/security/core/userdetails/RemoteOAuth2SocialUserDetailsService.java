package com.ingot.framework.security.core.userdetails;

import com.ingot.framework.security.oauth2.core.IngotAuthorizationGrantType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * <p>Description  : RemoteOAuth2SocialUserDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/30.</p>
 * <p>Time         : 3:52 PM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteOAuth2SocialUserDetailsService implements OAuth2UserDetailsService {
    private final RemoteUserDetailsService remoteUserDetailsService;

    @Override
    public boolean supports(AuthorizationGrantType grantType) {
        return IngotAuthorizationGrantType.SOCIAL.equals(grantType);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("[RemoteOAuth2SocialUserDetailsService] - loadUserByUsername: username={}", username);
        UserDetailsRequest params = new UserDetailsRequest();
        params.setUsername(username);
        return parse(remoteUserDetailsService.fetchUserDetailsSocial(params));
    }
}
