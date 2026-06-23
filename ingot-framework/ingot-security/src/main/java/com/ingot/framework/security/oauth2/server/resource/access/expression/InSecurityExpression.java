package com.ingot.framework.security.oauth2.server.resource.access.expression;

import java.util.Set;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.oauth2.server.resource.authentication.InJwtAuthenticationConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * <p>Description  : InSecurityExpression.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/25.</p>
 * <p>Time         : 3:09 下午.</p>
 */
public class InSecurityExpression {

    /** Ant 子树通配后缀，例如 {@code a:**} 匹配 a 命名空间下全部权限。 */
    private static final String ANT_SUBTREE_SUFFIX = ":**";

    /** 单段通配后缀，例如 {@code a:b:*}。 */
    private static final String SINGLE_WILDCARD_SUFFIX = ":*";

    /**
     * 判断是否包含权限
     *
     * @param authority 权限
     * @return {boolean}
     */
    public boolean hasAuthority(String authority) {
        return hasAnyAuthorityName(InJwtAuthenticationConverter.AUTHORITY_PREFIX, authority);
    }

    /**
     * 判断是否包含任一权限
     *
     * @param authorities 权限
     * @return {boolean}
     */
    public boolean hasAnyAuthority(String... authorities) {
        return hasAnyAuthorityName(InJwtAuthenticationConverter.AUTHORITY_PREFIX, authorities);
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
        return hasAnyAuthorityName(InJwtAuthenticationConverter.AUTHORITY_PREFIX, authSet);
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

        return authorities.stream()
                .filter(StrUtil::isNotEmpty)
                .map(req -> getAuthorityWithPrefix(prefix, req))
                .filter(StrUtil::isNotEmpty)
                .map(String::toUpperCase)
                .anyMatch(reqAuth -> userAuth.stream().anyMatch(granted -> matches(granted, reqAuth)));
    }

    /**
     * Ant 风格权限匹配，与 PMS 端 {@code PermissionMatcher} 语义保持一致：
     * <ul>
     *     <li>精确码只匹配自身（持有父级权限不再隐式包含子级）；</li>
     *     <li>单段通配 {@code ns:*} 匹配命名空间 {@code ns:} 前缀下的全部权限；</li>
     *     <li>Ant 子树通配 {@code ns:**} 匹配命名空间 {@code ns:} 前缀下的全部权限。</li>
     * </ul>
     * 示例：
     * <pre>
     * granted: a       - required: a       => true
     * granted: a       - required: a:b:c   => false
     * granted: a:*     - required: a:b     => true
     * granted: a:*     - required: a:b:c   => true
     * granted: a:**    - required: a:b:c   => true
     * granted: a:b:c   - required: a:b:c   => true
     * granted: a:b:c   - required: a:b     => false
     * </pre>
     *
     * @param granted  用户持有的权限码（可能为通配码）
     * @param required 接口要求的权限码（精确码）
     * @return 是否匹配
     */
    private boolean matches(String granted, String required) {
        if (StrUtil.isBlank(granted) || StrUtil.isBlank(required)) {
            return false;
        }
        if (isWildcard(granted)) {
            String namespace = wildcardNamespace(granted);
            return required.startsWith(namespace) && !required.contains("*");
        }
        return StrUtil.equals(granted, required);
    }

    private boolean isWildcard(String code) {
        return code.endsWith(ANT_SUBTREE_SUFFIX) || code.endsWith(SINGLE_WILDCARD_SUFFIX);
    }

    /**
     * 提取通配码对应的命名空间前缀（保留末尾 {@code :}）。
     */
    private String wildcardNamespace(String code) {
        if (code.endsWith(ANT_SUBTREE_SUFFIX)) {
            return code.substring(0, code.length() - ANT_SUBTREE_SUFFIX.length() + 1);
        }
        if (code.endsWith(SINGLE_WILDCARD_SUFFIX)) {
            return code.substring(0, code.length() - SINGLE_WILDCARD_SUFFIX.length() + 1);
        }
        return code;
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
