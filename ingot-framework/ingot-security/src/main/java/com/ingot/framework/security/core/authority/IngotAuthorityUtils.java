package com.ingot.framework.security.core.authority;

import com.ingot.framework.core.model.common.AllowTenantDTO;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>Description  : IngotAuthorityUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/30.</p>
 * <p>Time         : 2:00 PM.</p>
 */
public final class IngotAuthorityUtils {
    private IngotAuthorityUtils() {
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
    public static List<GrantedAuthority> createAllowTenantAuthorityList(AllowTenantDTO... authorities) {
        List<GrantedAuthority> grantedAuthorities = new ArrayList<>(authorities.length);
        for (AllowTenantDTO authority : authorities) {
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
    public static Set<AllowTenantDTO> extractAllowTenants(Collection<? extends GrantedAuthority> userAuthorities) {
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
    public static Set<String> authorityListToSet(Collection<? extends GrantedAuthority> userAuthorities) {
        Assert.notNull(userAuthorities, "userAuthorities cannot be null");
        return userAuthorities.stream()
                .filter(authority -> !(authority instanceof AllowTenantGrantedAuthority) && !(authority instanceof ClientGrantedAuthority))
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
