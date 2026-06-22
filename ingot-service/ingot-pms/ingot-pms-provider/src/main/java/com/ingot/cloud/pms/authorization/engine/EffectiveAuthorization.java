package com.ingot.cloud.pms.authorization.engine;

import java.util.LinkedHashSet;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;

/**
 * <p>用户在当前上下文下的有效授权集合，区分精确权限码与通配权限码。</p>
 *
 * <p>{@link #hasPermission(String)} 结合 {@link PermissionMatcher} 完成精确与通配匹配。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Builder
public class EffectiveAuthorization {
    private final Set<String> exactPermissionCodes;
    private final Set<String> wildcardPermissionCodes;
    private final Set<Long> accessibleAppIds;

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
}
