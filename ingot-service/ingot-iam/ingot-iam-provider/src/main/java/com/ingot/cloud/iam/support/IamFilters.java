package com.ingot.cloud.iam.support;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ConfigurationStatus;
import com.ingot.framework.commons.model.iam.IamReasonCode;

/**
 * <p>解析管理面列表筛选字面量，空白表示不限制，非法值拒绝。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class IamFilters {
    private IamFilters() {
    }

    /**
     * 规范化名称包含匹配关键字。
     *
     * @param name 原始查询，空白表示不限制
     * @return 去空白关键字；空白为 {@code null}
     */
    public static String containsName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim();
    }

    /**
     * 把启停状态字面量转成启用布尔值。
     *
     * @param status {@link ConfigurationStatus} 稳定字面量，空白表示不限制
     * @return 启用时为 {@code true}，停用为 {@code false}；空白为 {@code null}
     */
    public static Boolean enabledOf(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ConfigurationStatus.getEnum(status.trim()) == ConfigurationStatus.ENABLED;
        } catch (IllegalArgumentException ex) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }
}
