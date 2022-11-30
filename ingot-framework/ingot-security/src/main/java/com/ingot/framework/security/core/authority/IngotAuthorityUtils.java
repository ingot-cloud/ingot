package com.ingot.framework.security.core.authority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

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
     * Converts authorities into a List of GrantedAuthority objects.
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
}
