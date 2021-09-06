package com.ingot.cloud.authold.service;

import com.ingot.cloud.pms.api.rpc.PmsUserAuthFeignApi;
import com.ingot.framework.common.status.BaseStatusCode;
import com.ingot.framework.core.model.dto.user.UserAuthDetails;
import com.ingot.framework.core.model.dto.user.UserDetailsDto;
import com.ingot.framework.core.model.enums.UserDetailsModeEnum;
import com.ingot.framework.core.model.enums.UserStatusEnum;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.core.userdetails.IngotUserDetailsService;
import com.ingot.framework.security.exception.IngotOAuth2Exception;
import com.ingot.framework.security.utils.SecurityUtils;
import com.ingot.framework.security.utils.SocialUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>Description  : IngotUserDetailService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/3.</p>
 * <p>Time         : 4:37 下午.</p>
 */
@Slf4j
@Service
@AllArgsConstructor
public class IngotUserDetailService implements IngotUserDetailsService {
    private final PmsUserAuthFeignApi userCenterFeignApi;

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
        log.info(">>> IngotUserDetailServiceImpl - user detail service, loadUserByUsername: {}, " +
                        "clientId={}",
                username, clientId);

        UserDetailsDto params = new UserDetailsDto();
        params.setMode(UserDetailsModeEnum.PASSWORD);
        params.setUniqueCode(username);
        params.setClientId(clientId);
        IngotResponse<UserAuthDetails> response = userCenterFeignApi.getUserAuthDetail(params);
        log.info(">>> IngotUserDetailServiceImpl - user detail service, response: {}", response);
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
        log.info(">>> IngotUserDetailServiceImpl - user detail service, loadUserBySocial: openId={}",
                openId);
        String clientId = SecurityUtils.getClientIdFromRequest();

        String uniqueCode = SocialUtils.uniqueCode(socialType, openId);
        UserDetailsDto params = new UserDetailsDto();
        params.setMode(UserDetailsModeEnum.SOCIAL);
        params.setUniqueCode(uniqueCode);
        params.setClientId(clientId);
        IngotResponse<UserAuthDetails> response = userCenterFeignApi.getUserAuthDetail(params);
        log.info(">>> IngotUserDetailServiceImpl - user detail service, response: {}", response);
        return loadDetail(response);
    }

    private IngotUser loadDetail(IngotResponse<UserAuthDetails> response) {
        if (response == null) {
            throw new BadCredentialsException(BaseStatusCode.INTERNAL_SERVER_ERROR.message());
        }

        if (!response.isSuccess()) {
            throw new IngotOAuth2Exception(response.getCode(), response.getMessage());
        }

        UserAuthDetails data = response.getData();
        if (data == null) {
            throw new BadCredentialsException(BaseStatusCode.INTERNAL_SERVER_ERROR.message());
        }

        List<String> userAuthorities = data.getRoles();
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(userAuthorities.toArray(new String[0]));
        log.info(">>> UserDetail, {} role={}", data.getUsername(), authorities);
        boolean enabled = data.getStatus() == UserStatusEnum.ENABLE;
        boolean nonLocked = data.getStatus() != UserStatusEnum.LOCK;
        return new IngotUser(data.getId(), data.getDeptId(), data.getTenantId(), data.getAuthType(),
                data.getUsername(), data.getPassword(), enabled, true,
                true, nonLocked, authorities);
    }
}
