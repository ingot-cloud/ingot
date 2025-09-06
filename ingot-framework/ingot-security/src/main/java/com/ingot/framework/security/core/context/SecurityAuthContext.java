package com.ingot.framework.security.core.context;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.server.resource.authentication.InJwtAuthenticationConverter;
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
     * @return {@link InUser}
     */
    public static InUser getUser(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof InUser) {
            return (InUser) principal;
        }
        return null;
    }

    /**
     * 获取用户
     *
     * @return {@link InUser}
     */
    public static InUser getUser() {
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

        String adminAuthority = (InJwtAuthenticationConverter.AUTHORITY_PREFIX + RoleConstants.ROLE_ADMIN_CODE)
                .toUpperCase();
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StrUtil::isNotEmpty)
                .map(String::toUpperCase).anyMatch(scope -> StrUtil.equals(scope, adminAuthority));
    }

    /**
     * 获取用户角色信息
     *
     * @return 角色编码集合，去掉了{@link InJwtAuthenticationConverter#AUTHORITY_PREFIX}前缀
     */
    public static List<String> getRoles() {
        Authentication authentication = getAuthentication();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .map(code -> StrUtil.subAfter(code, InJwtAuthenticationConverter.AUTHORITY_PREFIX, false))
                .collect(Collectors.toList());
    }

}
