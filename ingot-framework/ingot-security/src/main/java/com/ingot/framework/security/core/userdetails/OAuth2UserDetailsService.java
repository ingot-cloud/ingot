package com.ingot.framework.security.core.userdetails;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.ingot.framework.commons.model.common.AllowTenantDTO;
import com.ingot.framework.commons.model.enums.UserStatusEnum;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.core.authority.InAuthorityUtils;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    default InUserDetails parse(R<UserDetailsResponse> response) {
        return Optional.ofNullable(response)
                .map(r -> {
                    OAuth2ErrorUtils.checkResponse(response);
                    return r.getData();
                })
                .map(data -> {
                    List<String> userAuthorities = Optional.ofNullable(data.getRoles()).orElse(ListUtil.empty());
                    List<AllowTenantDTO> allowTenants = Optional.ofNullable(data.getAllows()).orElse(ListUtil.empty());
                    List<GrantedAuthority> authorities = new ArrayList<>(CollUtil.size(userAuthorities) + CollUtil.size(allowTenants));
                    authorities.addAll(AuthorityUtils.createAuthorityList(userAuthorities.toArray(new String[0])));
                    authorities.addAll(InAuthorityUtils.createAllowTenantAuthorityList(allowTenants.toArray(new AllowTenantDTO[0])));

                    boolean enabled = data.getStatus() == UserStatusEnum.ENABLE;
                    boolean nonLocked = data.getStatus() != UserStatusEnum.LOCK;
                    return InUser.userDetails(data.getId(), data.getUserType(), data.getTenant(),
                            data.getUsername(), data.getPassword(),
                            enabled, true, true, nonLocked, authorities);
                })
                .orElseThrow(() -> new UsernameNotFoundException("Username not found"));
    }
}
