package com.ingot.framework.security.common.utils;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.common.constants.RoleConstants;

/**
 * <p>Description  : RoleUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/21.</p>
 * <p>Time         : 7:31 PM.</p>
 */
public final class RoleUtils {
    private RoleUtils() {
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
}
