package com.ingot.cloud.acs.service;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.dto.user.UserAuthDetailDto;
import com.ingot.cloud.pms.api.model.vo.user.UserVo;
import com.ingot.cloud.pms.api.rpc.client.PmsSocialFeignApi;
import com.ingot.cloud.pms.api.rpc.client.PmsUserAuthFeignApi;
import com.ingot.framework.base.constants.GlobalConstants;
import com.ingot.framework.base.exception.BaseException;
import com.ingot.framework.base.status.BaseStatusCode;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.core.userdetails.IngotUserDetailsService;
import com.ingot.framework.security.utils.SecurityUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.ingot.framework.core.constants.BeanIds.USER_DETAIL_SERVICE;

/**
 * <p>Description  : IngotUserDetailService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/3.</p>
 * <p>Time         : 4:37 下午.</p>
 */
@Slf4j
@Component(USER_DETAIL_SERVICE)
@AllArgsConstructor
public class IngotUserDetailService implements IngotUserDetailsService {
    private final PmsUserAuthFeignApi userCenterFeignApi;
    private final PmsSocialFeignApi socialFeignApi;

    /**
     * 根据用户名称登录
     *
     * @param username 用户名称
     * @return {@link UserDetails}
     * @throws UsernameNotFoundException if the user could not be found or the user has no
     *                                   GrantedAuthority
     */
    @Override public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String clientId = SecurityUtils.getClientIdFromRequest();
        String tenantCode = SecurityUtils.getTenantCodeFromRequest();
        log.info(">>> IngotUserDetailServiceImpl - user detail service, loadUserByUsername: {}, " +
                        "clientId={}, tenantCode={}",
                username, clientId, tenantCode);
        IngotResponse<UserAuthDetailDto> response = userCenterFeignApi.getUserAuthDetail(
                username, clientId, tenantCode);
        log.info(">>> IngotUserDetailServiceImpl - user detail service, response: {}", response);
        return loadDetail(response);
    }

    /**
     * 根据社交登录 code 获取 UserDetails
     *
     * @param code 社交类型@社交code
     * @return {@link UserDetails}
     * @throws UsernameNotFoundException if the user could not be found or the user has no
     *                                   GrantedAuthority
     */
    @Override
    public UserDetails loadUserBySocial(String code) throws UsernameNotFoundException {
        log.info(">>> IngotUserDetailServiceImpl - user detail service, loadUserBySocial: code={}",
                code);
        String clientId = SecurityUtils.getClientIdFromRequest();
        String tenantCode = SecurityUtils.getTenantCodeFromRequest();
        Pair<String, String> extract = extractCode(code);
        IngotResponse<UserAuthDetailDto> response = socialFeignApi.getUserAuthDetail(extract.getKey(),
                extract.getValue(), clientId, tenantCode);
        log.info(">>> IngotUserDetailServiceImpl - user detail service, response: {}", response);
        return loadDetail(response);
    }

    private IngotUser loadDetail(IngotResponse<UserAuthDetailDto> response) {
        if (response == null) {
            throw new BadCredentialsException(BaseStatusCode.INTERNAL_SERVER_ERROR.message());
        }

        if (!response.isSuccess()) {
            throw new BaseException(response.getCode(), response.getMessage());
        }

        UserAuthDetailDto data = response.getData();
        if (data == null) {
            throw new BadCredentialsException(BaseStatusCode.INTERNAL_SERVER_ERROR.message());
        }

        UserVo user = data.getUser();

        List<String> userAuthorities = data.getRoleList();
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(userAuthorities.toArray(new String[0]));
        log.info(">>> UserDetail, {} role={}", user.getUsername(), authorities);
        return new IngotUser(user.getId(), user.getTenant_id(), data.getAuthType(),
                user.getUsername(), user.getPassword(), authorities);
    }

    private Pair<String, String> extractCode(String code) {
        if (StrUtil.isEmpty(code)) {
            return new Pair<>("", code);
        }

        String[] result = code.split(GlobalConstants.AT);
        String type = result.length > 1 ? result[0] : "";
        String finalCode = result.length > 1 ? result[1] : code;
        return new Pair<>(type, finalCode);
    }
}
