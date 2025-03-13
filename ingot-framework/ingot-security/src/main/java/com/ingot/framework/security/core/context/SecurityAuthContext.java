package com.ingot.framework.security.core.context;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.constants.RoleConstants;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.oauth2.server.resource.authentication.IngotJwtAuthenticationConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>Description  : SecurityAuthContext.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/26.</p>
 * <p>Time         : 3:18 下午.</p>
 */
public final class SecurityAuthContext {

    /**
     * 获取Authentication
     *
     * @return {@link Authentication}
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 获取指定{@link Authentication}中的用户信息
     *
     * @param authentication Target Authentication
     * @return {@link IngotUser}
     */
    public static IngotUser getUser(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof IngotUser) {
            return (IngotUser) principal;
        }
        return null;
    }

    /**
     * 获取用户
     *
     * @return {@link IngotUser}
     */
    public static IngotUser getUser() {
        return getUser(getAuthentication());
    }

    /**
     * 是否为管理员
     *
     * @return Boolean
     */
    public static boolean isAdmin() {
        Authentication authentication = getAuthentication();
        if (authentication == null) {
            return false;
        }

        String adminAuthority = (IngotJwtAuthenticationConverter.AUTHORITY_PREFIX + RoleConstants.ROLE_ADMIN_CODE)
                .toUpperCase();
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StrUtil::isNotEmpty)
                .map(String::toUpperCase).anyMatch(scope -> StrUtil.equals(scope, adminAuthority));
    }

    /**
     * 获取用户角色信息
     *
     * @return 角色集合
     */
    public static List<String> getRoles() {
        Authentication authentication = getAuthentication();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

}
