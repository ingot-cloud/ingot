package com.ingot.framework.security.core.userdetails;

import com.ingot.framework.security.oauth2.core.IngotAuthorizationGrantType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * <p>Description  : RemoteOAuth2UserDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/7.</p>
 * <p>Time         : 3:48 下午.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteOAuth2UserDetailsService implements OAuth2UserDetailsService {
    private final RemoteUserDetailsService remoteUserDetailsService;

    @Override
    public boolean supports(AuthorizationGrantType grantType) {
        // 密码模式，或者确认模式
        return IngotAuthorizationGrantType.PASSWORD.equals(grantType)
                || IngotAuthorizationGrantType.PRE_AUTHORIZATION_CODE.equals(grantType);
    }

    /**
     * 根据用户名称登录
     *
     * @param username 用户名称
     * @return {@link UserDetails}
     * @throws UsernameNotFoundException if the user could not be found or the user has no
     *                                   GrantedAuthority
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("[RemoteOAuth2UserDetailsService] - loadUserByUsername: username={}", username);
        UserDetailsRequest params = new UserDetailsRequest();
        params.setUsername(username);
        return parse(remoteUserDetailsService.fetchUserDetails(params));
    }
}
