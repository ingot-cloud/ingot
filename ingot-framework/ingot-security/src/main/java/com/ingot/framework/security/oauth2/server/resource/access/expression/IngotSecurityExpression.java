package com.ingot.framework.security.oauth2.server.resource.access.expression;

import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Sets;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.oauth2.server.resource.authentication.IngotJwtAuthenticationConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * <p>Description  : IngotSecurityExpression.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/25.</p>
 * <p>Time         : 3:09 下午.</p>
 */
public class IngotSecurityExpression {

    /**
     * 判断是否包含权限
     *
     * @param authority 权限
     * @return {boolean}
     */
    public boolean hasAuthority(String authority) {
        return hasAnyAuthorityName(IngotJwtAuthenticationConverter.AUTHORITY_PREFIX, authority);
    }

    /**
     * 判断是否包含任一权限
     *
     * @param authorities 权限
     * @return {boolean}
     */
    public boolean hasAnyAuthority(String... authorities) {
        return hasAnyAuthorityName(IngotJwtAuthenticationConverter.AUTHORITY_PREFIX, authorities);
    }

    /**
     * 判断是否包含SCOPE_scope
     *
     * @param scope scope
     * @return {boolean}
     */
    public boolean hasScope(String scope) {
        return hasAnyAuthorityName(null, scope);
    }

    /**
     * 判断是否包含任一SCOPE_scope
     *
     * @param scopes scope
     * @return {boolean}
     */
    public boolean hasAnyScope(String... scopes) {
        return hasAnyAuthorityName(null, scopes);
    }

    private boolean hasAnyAuthorityName(String prefix, String... authorities) {
        Authentication authentication = SecurityAuthContext.getAuthentication();
        if (authentication == null) {
            return false;
        }
        Set<String> userAuth = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StrUtil::isNotEmpty).collect(Collectors.toSet());
        Set<String> requiredAuth = Sets.newHashSet(authorities);
        return requiredAuth.stream()
                .anyMatch(req -> userAuth.contains(getAuthorityWithPrefix(prefix, req)));
    }

    private String getAuthorityWithPrefix(String prefix, String authority) {
        if (authority == null) {
            return null;
        }
        if (prefix == null || prefix.length() == 0) {
            return authority;
        }
        if (authority.startsWith(prefix)) {
            return authority;
        }
        return prefix + authority;
    }
}
