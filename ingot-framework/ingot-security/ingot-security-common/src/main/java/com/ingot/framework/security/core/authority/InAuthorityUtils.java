package com.ingot.framework.security.core.authority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.model.common.TenantMainDTO;
import com.ingot.framework.security.oauth2.server.resource.authentication.InJwtAuthenticationConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

/**
 * <p>Description  : Authority Utils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/30.</p>
 * <p>Time         : 2:00 PM.</p>
 */
public final class InAuthorityUtils {
    private InAuthorityUtils() {
    }

    /**
     * 将权限转化为 {@link ClientGrantedAuthority} 列表
     *
     * @param authorities the authorities to convert
     * @return a List of GrantedAuthority objects
     */
    public static List<GrantedAuthority> createClientAuthorityList(String... authorities) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>(authorities.length);
        for (String authority : authorities) {
            grantedAuthorities.add(new ClientGrantedAuthority(authority));
        }
        return grantedAuthorities;
    }

    /**
     * 在权限中提取客户端ID
     *
     * @param userAuthorities 用户权限
     * @return 客户端ID集合
     */
    public static Set<String> extractClientIds(Collection<? extends GrantedAuthority> userAuthorities) {
        Assert.notNull(userAuthorities, "userAuthorities cannot be null");
        return userAuthorities.stream()
                .filter(authority -> authority instanceof ClientGrantedAuthority)
                .map(authority -> ((ClientGrantedAuthority) authority).extract())
                .collect(Collectors.toSet());
    }

    /**
     * 将权限转化为 {@link AllowTenantGrantedAuthority} 列表
     *
     * @param authorities the authorities to convert
     * @return a List of GrantedAuthority objects
     */
    public static List<GrantedAuthority> createAllowTenantAuthorityList(TenantMainDTO... authorities) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>(authorities.length);
        for (TenantMainDTO authority : authorities) {
            grantedAuthorities.add(new AllowTenantGrantedAuthority(authority));
        }
        return grantedAuthorities;
    }

    /**
     * 在权限中提取允许访问的租户列表
     *
     * @param userAuthorities 用户权限
     * @return 客户端ID集合
     */
    public static Set<TenantMainDTO> extractAllowTenants(Collection<? extends GrantedAuthority> userAuthorities) {
        Assert.notNull(userAuthorities, "userAuthorities cannot be null");
        return userAuthorities.stream()
                .filter(authority -> authority instanceof AllowTenantGrantedAuthority)
                .map(authority -> ((AllowTenantGrantedAuthority) authority).extract())
                .collect(Collectors.toSet());
    }

    /**
     * 将 {@link GrantedAuthority} 列表转换为Set，
     * 并且不包括 {@link AllowTenantGrantedAuthority} 和 {@link ClientGrantedAuthority}
     *
     * @return a Set of the Strings obtained from each call to
     * GrantedAuthority.getAuthority()
     */
    public static Set<String> authorityListToSet(Collection<? extends GrantedAuthority> userAuthorities, long tenantId) {
        Assert.notNull(userAuthorities, "userAuthorities cannot be null");
        return userAuthorities.stream()
                .filter(authority -> !(authority instanceof AllowTenantGrantedAuthority) && !(authority instanceof ClientGrantedAuthority))
                .filter(authority -> {
                    String value = authority.getAuthority();
                    // 如果存在可选权限，那么过滤为当前租户权限
                    if (StrUtil.contains(value, "@")) {
                        return StrUtil.startWith(value, tenantId + "@");
                    }
                    return true;
                })
                .map(authority -> {
                    String value = authority.getAuthority();
                    if (StrUtil.contains(value, "@")) {
                        return StrUtil.subAfter(value, "@", false);
                    }
                    return value;
                })
                .collect(Collectors.toSet());
    }

    /**
     * 获取Scopes，并且去掉{@link InJwtAuthenticationConverter#AUTHORITY_PREFIX}前缀
     *
     * @param userAuthorities 用户权限
     * @return Scope集合，去掉了{@link InJwtAuthenticationConverter#AUTHORITY_PREFIX}前缀
     */
    public static Set<String> authorityListToScopes(Collection<? extends GrantedAuthority> userAuthorities) {
        Assert.notNull(userAuthorities, "userAuthorities cannot be null");
        return userAuthorities.stream()
                .filter(authority ->
                        !(authority instanceof AllowTenantGrantedAuthority) && !(authority instanceof ClientGrantedAuthority))
                .map(GrantedAuthority::getAuthority)
                .map(code -> StrUtil.subAfter(code, InJwtAuthenticationConverter.AUTHORITY_PREFIX, false))
                .collect(Collectors.toSet());
    }

    /**
     * 租户权限
     */
    public static String authorityWithTenant(String authority, long tenant) {
        return tenant + "@" + authority;
    }
}
