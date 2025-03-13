package com.ingot.framework.security.oauth2.server.resource.access.expression;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.constants.RoleConstants;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.oauth2.server.resource.authentication.IngotJwtAuthenticationConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    /**
     * 必须为管理员
     *
     * @return {boolean}
     */
    public boolean requiredAdmin() {
        return hasAuthority(RoleConstants.ROLE_ADMIN_CODE);
    }

    /**
     * 管理员或指定权限
     * @param authorities 指定权限
     * @return {boolean}
     */
    public boolean adminOrHasAnyAuthority(String... authorities) {
        Set<String> authSet = CollUtil.newHashSet(authorities);
        authSet.add(RoleConstants.ROLE_ADMIN_CODE);
        return hasAnyAuthorityName(IngotJwtAuthenticationConverter.AUTHORITY_PREFIX, authSet);
    }

    private boolean hasAnyAuthorityName(String prefix, String... authorities) {
        return hasAnyAuthorityName(prefix, CollUtil.newHashSet(authorities));
    }

    private boolean hasAnyAuthorityName(String prefix, Set<String> authorities) {
        Authentication authentication = SecurityAuthContext.getAuthentication();
        if (authentication == null) {
            return false;
        }

        Set<String> userAuth = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(StrUtil::isNotEmpty)
                .map(String::toUpperCase).collect(Collectors.toSet());
        Set<String> requiredAuth = authorities.stream()
                .map(String::toUpperCase).collect(Collectors.toSet());

        return requiredAuth.stream()
                .anyMatch(req -> {
                    // 层级权限，拥有父级权限也可以通过
                    // req: a     - user: a   => true
                    // req: a     - user: a:b => false
                    // req: a:b:c - user: a:b => true
                    // req: a:b   - user: a:b => true
                    // req: a:b   - user: a:c => false
                    // req: a:b   - user: a:b:c => false
                    String reqAuth = getAuthorityWithPrefix(prefix, req);
                    List<String> scopes = StrUtil.split(reqAuth, StrUtil.COLON);
                    return userAuth.stream().anyMatch(user -> {
                        boolean start = StrUtil.startWith(reqAuth, user);
                        if (!start) {
                            return false;
                        }

                        List<String> userScopes = StrUtil.split(user, StrUtil.COLON);
                        int uLen = CollUtil.size(userScopes);
                        for (int i = 0; i < uLen; i++) {
                            if (!StrUtil.equals(userScopes.get(i), scopes.get(i))) {
                                return false;
                            }
                        }
                        return true;
                    });
                });
    }

    private String getAuthorityWithPrefix(String prefix, String authority) {
        if (authority == null) {
            return null;
        }
        if (prefix == null || prefix.isEmpty()) {
            return authority;
        }
        if (authority.startsWith(prefix)) {
            return authority;
        }
        return prefix + authority;
    }
}
