package com.ingot.framework.security.core.userdetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.ingot.framework.commons.model.common.TenantMainDTO;
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

/**
 * <p>扩展认证用户加载协议，将内部身份响应转换为保留单一成员上下文的认证用户。</p>
 *
 * @author wangchao
 * @since 1.0.0
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
                    List<String> scopes = Optional.ofNullable(data.getScopes()).orElse(ListUtil.empty());
                    List<TenantMainDTO> allowTenants = Optional.ofNullable(data.getAllows()).orElse(ListUtil.empty());
                    List<GrantedAuthority> authorities = new ArrayList<>(CollUtil.size(scopes) + CollUtil.size(allowTenants));
                    authorities.addAll(AuthorityUtils.createAuthorityList(scopes.toArray(new String[0])));
                    authorities.addAll(InAuthorityUtils.createAllowTenantAuthorityList(allowTenants.toArray(new TenantMainDTO[0])));

                    // 账号状态
                    boolean enabled = data.getEnabled();
                    boolean nonLocked = !data.getLocked();
                    // 凭证状态：null 视为未过期（社交登录等场景不设置此字段）
                    boolean credentialsNonExpired = data.getCredentialsNonExpired() == null
                            || data.getCredentialsNonExpired();
                    return InUser.userDetails(data.getId(), data.getUserType(), data.getTenant(),
                            data.getUsername(), data.getPassword(),
                            enabled, true, credentialsNonExpired, nonLocked, authorities,
                            data.getMeta(),
                            data.getDeptIds(), data.getTenantDeptIds())
                            .toBuilder().authorizationContext(data.getAuthorizationContext()).build();
                })
                .orElseThrow(() -> new UsernameNotFoundException("Username not found"));
    }
}
