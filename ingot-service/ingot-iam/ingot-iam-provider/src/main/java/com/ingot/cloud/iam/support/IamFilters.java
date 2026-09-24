package com.ingot.cloud.iam.support;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.ConfigurationStatus;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberStatus;

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
     * 解析必填管理域字面量。
     *
     * @param domain {@link AuthorizationDomain} 稳定字面量
     * @return 管理域
     */
    public static AuthorizationDomain requireDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        try {
            return AuthorizationDomain.getEnum(domain.trim());
        } catch (IllegalArgumentException ex) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
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

    /**
     * 把成员资格字面量转成枚举。
     *
     * @param status {@link MemberStatus} 稳定字面量，空白表示不限制
     * @return 成员资格；空白为 {@code null}
     */
    public static MemberStatus memberStatusOf(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return MemberStatus.getEnum(status.trim());
        } catch (IllegalArgumentException ex) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }
}
