package com.ingot.framework.security.core.userdetails;

import com.ingot.framework.core.model.enums.SocialTypeEnum;
import com.ingot.framework.core.model.security.UserDetailsRequest;
import com.ingot.framework.core.model.security.UserTypeEnum;
import com.ingot.framework.core.utils.SocialUtils;
import com.ingot.framework.security.oauth2.core.InAuthorizationGrantType;
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
        return InAuthorizationGrantType.PASSWORD.equals(grantType)
                || InAuthorizationGrantType.SOCIAL.equals(grantType);
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
    public InUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("[RemoteOAuth2UserDetailsService] - loadUserByUsername: username={}", username);
        UsernameUri uri = UsernameUri.of(username);
        String grantType = uri.getGrantType();
        UserTypeEnum userType = uri.getUserType();
        long tenant = uri.getTenant();

        UserDetailsRequest params = new UserDetailsRequest();
        params.setUsername(uri.getPrincipal());
        params.setGrantType(grantType);
        params.setUserType(userType);
        if (tenant > 0) {
            params.setTenant(tenant);
        }

        if (grantType.equals(InAuthorizationGrantType.SOCIAL.getValue())) {
            String unique = params.getUsername();
            String[] extract = SocialUtils.extract(unique);
            SocialTypeEnum socialType = SocialTypeEnum.get(extract[0]);
            if (socialType == null) {
                log.error("[RemoteOAuth2UserDetailsService] 非法社交类型={}", extract[0]);
                return null;
            }
            String socialCode = extract[1];
            params.setSocialType(socialType);
            params.setSocialCode(socialCode);
        }

        return parse(remoteUserDetailsService.fetchUserDetails(params));
    }
}
