package com.ingot.cloud.pms.authorization.engine;

import java.util.Collection;

import cn.hutool.core.util.StrUtil;

/**
 * <p>权限编码匹配器，支持精确匹配、单段通配 {@code :*} 与 Ant 子树通配 {@code :**}。</p>
 *
 * <p>不支持中间通配与隐式父级包含，要求授权码命名空间严格前缀匹配。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class PermissionMatcher {

    /** Ant 子树通配后缀，例如 {@code contacts:**} 匹配应用下全部权限。 */
    public static final String ANT_SUBTREE_SUFFIX = ":**";

    /** 单段通配后缀，例如 {@code contacts:user:*}。 */
    public static final String SINGLE_WILDCARD_SUFFIX = ":*";

    private PermissionMatcher() {
    }

    public static boolean matches(String grantedCode, String requiredCode) {
        if (StrUtil.isBlank(grantedCode) || StrUtil.isBlank(requiredCode)) {
            return false;
        }
        if (isWildcard(grantedCode)) {
            String namespace = wildcardNamespace(grantedCode);
            return requiredCode.startsWith(namespace) && !requiredCode.contains("*");
        }
        return grantedCode.equals(requiredCode);
    }

    public static boolean matchesAny(Collection<String> grantedCodes, String requiredCode) {
        for (String grantedCode : grantedCodes) {
            if (matches(grantedCode, requiredCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否为通配授权码（{@code :*} 或 {@code :**}）。
     */
    public static boolean isWildcard(String code) {
        return isAntSubtreeWildcard(code) || isSingleWildcard(code);
    }

    /**
     * 是否为 Ant 子树通配，例如 {@code contacts:**}。
     */
    public static boolean isAntSubtreeWildcard(String code) {
        return StrUtil.isNotBlank(code) && code.endsWith(ANT_SUBTREE_SUFFIX);
    }

    /**
     * 是否为单段通配，例如 {@code contacts:user:*}。
     */
    public static boolean isSingleWildcard(String code) {
        return StrUtil.isNotBlank(code)
                && code.endsWith(SINGLE_WILDCARD_SUFFIX)
                && !code.endsWith(ANT_SUBTREE_SUFFIX);
    }

    /**
     * 提取通配码对应的命名空间前缀（保留末尾 {@code :}）。
     */
    public static String wildcardNamespace(String wildcardCode) {
        if (isAntSubtreeWildcard(wildcardCode)) {
            return wildcardCode.substring(0, wildcardCode.length() - 2);
        }
        if (isSingleWildcard(wildcardCode)) {
            return wildcardCode.substring(0, wildcardCode.length() - 1);
        }
        return wildcardCode;
    }

    /**
     * 提取通配码去掉通配后缀后的路径前缀（不含末尾 {@code :}）。
     */
    public static String wildcardPathPrefix(String wildcardCode) {
        String namespace = wildcardNamespace(wildcardCode);
        if (namespace.endsWith(StrUtil.COLON)) {
            return namespace.substring(0, namespace.length() - 1);
        }
        return namespace;
    }
}
