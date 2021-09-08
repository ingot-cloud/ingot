package com.ingot.framework.security.core.userdetails;

import com.ingot.framework.common.status.BaseStatusCode;
import com.ingot.framework.core.model.enums.UserStatusEnum;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.security.common.utils.SecurityUtils;
import com.ingot.framework.security.common.utils.SocialUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

/**
 * <p>Description  : RemoteIngotUserDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/7.</p>
 * <p>Time         : 3:48 下午.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteIngotUserDetailsService implements IngotUserDetailsService {
    private final RemoteUserDetailsService remoteUserDetailsService;

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
        String clientId = SecurityUtils.getClientIdFromRequest();
        log.info(">>> RemoteIngotUserDetailsService - user detail service, loadUserByUsername: {}, " +
                        "clientId={}",
                username, clientId);

        UserDetailsRequest params = new UserDetailsRequest();
        params.setMode(UserDetailsModeEnum.PASSWORD);
        params.setUniqueCode(username);
        params.setClientId(clientId);
        IngotResponse<UserDetailsResponse> response = remoteUserDetailsService.fetchUserDetails(params);
        log.info(">>> RemoteIngotUserDetailsService - user detail service, response: {}", response);
        return loadDetail(response);
    }

    /**
     * 根据社交登录 openId 获取 UserDetails
     *
     * @param socialType 社交类型
     * @param openId     社交登录唯一Id
     * @return {@link UserDetails}
     * @throws UsernameNotFoundException if the user could not be found or the user has no
     *                                   GrantedAuthority
     */
    @Override
    public UserDetails loadUserBySocial(String socialType, String openId) throws UsernameNotFoundException {
        log.info(">>> RemoteIngotUserDetailsService - user detail service, loadUserBySocial: openId={}",
                openId);
        String clientId = SecurityUtils.getClientIdFromRequest();

        String uniqueCode = SocialUtils.uniqueCode(socialType, openId);
        UserDetailsRequest params = new UserDetailsRequest();
        params.setMode(UserDetailsModeEnum.SOCIAL);
        params.setUniqueCode(uniqueCode);
        params.setClientId(clientId);
        IngotResponse<UserDetailsResponse> response = remoteUserDetailsService.fetchUserDetails(params);
        log.info(">>> RemoteIngotUserDetailsService - user detail service, response: {}", response);
        return loadDetail(response);
    }

    private IngotUser loadDetail(IngotResponse<UserDetailsResponse> response) {
        if (response == null) {
            throw new BadCredentialsException(BaseStatusCode.INTERNAL_SERVER_ERROR.message());
        }

        OAuth2ErrorUtils.checkResponse(response);

        UserDetailsResponse data = response.getData();
        if (data == null) {
            throw new BadCredentialsException(BaseStatusCode.INTERNAL_SERVER_ERROR.message());
        }

        List<String> userAuthorities = data.getRoles();
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(userAuthorities.toArray(new String[0]));
        log.info(">>> UserDetail - user={} role={}", data.getUsername(), authorities);
        boolean enabled = data.getStatus() == UserStatusEnum.ENABLE;
        boolean nonLocked = data.getStatus() != UserStatusEnum.LOCK;
        return new IngotUser(data.getId(), data.getDeptId(), data.getTenantId(), data.getTokenAuthenticationMethod(),
                data.getUsername(), data.getPassword(), enabled, true,
                true, nonLocked, authorities);
    }
}
