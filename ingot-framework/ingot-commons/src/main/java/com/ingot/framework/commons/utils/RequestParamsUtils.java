package com.ingot.framework.commons.utils;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.constants.HeaderConstants;
import jakarta.servlet.http.HttpServletRequest;

/**
 * <p>Description  : RequestParamsUtils.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/9/3.</p>
 * <p>Time         : 10:06.</p>
 */
public final class RequestParamsUtils {
    private final static String UNDEFINED_STR = "undefined";

    /**
     * 获取请求中的TenantId
     *
     * @param request {@link HttpServletRequest}
     * @return 租户ID
     */
    public static String getTenantId(HttpServletRequest request) {
        String headerTenantId = request.getHeader(HeaderConstants.TENANT);
        if (StrUtil.isNotBlank(headerTenantId) && !StrUtil.equals(UNDEFINED_STR, headerTenantId)) {
            return headerTenantId;
        }
        String paramTenantId = request.getParameter(HeaderConstants.TENANT);
        if (StrUtil.isNotBlank(headerTenantId) && !StrUtil.equals(UNDEFINED_STR, headerTenantId)) {
            return paramTenantId;
        }

        return null;
    }
}
