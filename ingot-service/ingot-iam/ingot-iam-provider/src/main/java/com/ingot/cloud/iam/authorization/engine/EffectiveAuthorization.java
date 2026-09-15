package com.ingot.cloud.iam.authorization.engine;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.hutool.core.collection.CollUtil;
import lombok.Builder;
import lombok.Getter;

/**
 * <p>用户在当前租户上下文下的有效授权：角色绑定（含部门）、授予码与启用的具体权限。</p>
 *
 * <p>{@link #hasPermission(String)} 按精确码与通配匹配；具体权限集合只包含当前启用且应用可访问的非通配编码。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Builder
public class EffectiveAuthorization {

    /**
     * 保留部门上下文的角色绑定。
     */
    private final List<RoleBinding> roleBindings;

    /**
     * 角色直接授予的精确权限码。
     */
    private final Set<String> exactPermissionCodes;

    /**
     * 角色直接授予的通配权限码。
     */
    private final Set<String> wildcardPermissionCodes;

    /**
     * 通配展开后、当前启用且应用可访问的具体权限码。
     */
    private final Set<String> concretePermissionCodes;

    /**
     * 当前租户可访问的应用 ID。
     */
    private final Set<Long> accessibleAppIds;

    /**
     * 当前用户是否持有租户管理员角色。
     */
    private final boolean orgAdmin;

    /**
     * 判断是否拥有指定具体权限（通配按命名空间覆盖启用权限）。
     *
     * @param code 具体权限码
     * @return 拥有该权限时返回 {@code true}
     */
    public boolean hasPermission(String code) {
        if (exactPermissionCodes != null && exactPermissionCodes.contains(code)) {
            return true;
        }
        if (wildcardPermissionCodes == null) {
            return false;
        }
        for (String wildcard : wildcardPermissionCodes) {
            if (PermissionMatcher.matches(wildcard, code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 角色授予的精确码与通配码，供会话在 T4 切换前继续做接口鉴权匹配。
     *
     * @return 授予码集合，保持插入顺序
     */
    public Set<String> allGrantedCodes() {
        Set<String> all = new LinkedHashSet<>();
        if (exactPermissionCodes != null) {
            all.addAll(exactPermissionCodes);
        }
        if (wildcardPermissionCodes != null) {
            all.addAll(wildcardPermissionCodes);
        }
        return all;
    }

    /**
     * 安全读取通配授予码。
     *
     * @return 通配码集合，永不为 {@code null}
     */
    public Set<String> safeWildcardCodes() {
        return wildcardPermissionCodes == null ? Set.of() : wildcardPermissionCodes;
    }

    /**
     * 安全读取角色绑定。
     *
     * @return 绑定列表，永不为 {@code null}
     */
    public List<RoleBinding> safeRoleBindings() {
        return roleBindings == null ? List.of() : roleBindings;
    }

    /**
     * 安全读取可访问应用。
     *
     * @return 应用 ID 集合，永不为 {@code null}
     */
    public Set<Long> safeAccessibleAppIds() {
        return accessibleAppIds == null ? Set.of() : accessibleAppIds;
    }

    /**
     * 安全读取展开后的具体权限。
     *
     * @return 具体权限码，永不为 {@code null}
     */
    public Set<String> safeConcretePermissionCodes() {
        return concretePermissionCodes == null ? Collections.emptySet() : concretePermissionCodes;
    }

    /**
     * 绑定中是否包含指定角色编码。
     *
     * @param roleCode 角色编码
     * @return 存在该角色绑定时返回 {@code true}
     */
    public boolean hasRole(String roleCode) {
        if (CollUtil.isEmpty(roleBindings) || roleCode == null) {
            return false;
        }
        return roleBindings.stream().anyMatch(binding -> roleCode.equals(binding.getRoleCode()));
    }
}
