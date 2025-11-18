package com.ingot.framework.commons.utils;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.constants.RoleConstants;

/**
 * <p>Description  : RoleUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/21.</p>
 * <p>Time         : 7:31 PM.</p>
 */
public final class RoleUtil {
    private RoleUtil() {
    }

    /**
     * 是否为管理员
     *
     * @param code 校验编码
     * @return True 意思是管理员
     */
    public static boolean isAdmin(String code) {
        return StrUtil.equals(code, RoleConstants.ROLE_ADMIN_CODE);
    }

    /**
     * 判断是否为组织自定义角色编码
     *
     * @param scope 角色编码 | 权限编码
     * @return boolean
     */
    public static boolean isOrgRoleCode(String scope) {
        return scope.startsWith(RoleConstants.ORG_ROLE_CODE_PREFIX);
    }

    /**
     * 判断是否为元角色编码
     *
     * @param scope 角色编码 | 权限编码
     * @return boolean
     */
    public static boolean isMetaRoleCode(String scope) {
        return scope.startsWith(RoleConstants.META_ROLE_CODE_PREFIX);
    }

    /**
     * 判断是否为角色编码
     *
     * @param scope 角色编码 | 权限编码
     * @return boolean
     */
    public static boolean isRoleCode(String scope) {
        return isOrgRoleCode(scope) || isMetaRoleCode(scope);
    }
}
