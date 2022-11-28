package com.ingot.framework.security.core.userdetails;

import java.util.List;

import com.ingot.framework.common.status.BaseStatusCode;
import com.ingot.framework.core.model.enums.UserStatusEnum;
import com.ingot.framework.core.wrapper.R;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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
     * 解析响应为 {@link UserDetails}
     *
     * @param response 响应结果
     * @return {@link UserDetails}
     */
    default UserDetails parse(R<UserDetailsResponse> response) {
        if (response == null) {
            throw new BadCredentialsException(BaseStatusCode.INTERNAL_SERVER_ERROR.getText());
        }
        OAuth2ErrorUtils.checkResponse(response);

        UserDetailsResponse data = response.getData();
        if (data == null) {
            throw new BadCredentialsException(BaseStatusCode.INTERNAL_SERVER_ERROR.getText());
        }

        List<String> userAuthorities = data.getRoles();
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(userAuthorities.toArray(new String[0]));
        boolean enabled = data.getStatus() == UserStatusEnum.ENABLE;
        boolean nonLocked = data.getStatus() != UserStatusEnum.LOCK;
        return IngotUser.noClientInfo(data.getId(), data.getDeptId(), data.getTenantId(),
                data.getUsername(), data.getPassword(),
                enabled, true, true, nonLocked, authorities);
    }
}
