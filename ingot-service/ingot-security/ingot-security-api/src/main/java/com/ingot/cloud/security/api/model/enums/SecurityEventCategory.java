package com.ingot.cloud.security.api.model.enums;

import com.ingot.framework.security.event.codes.SecurityEventCategoryCodes;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>统一安全事件分类（跨模块 wire / 校验 SoT）。</p>
 *
 * <p>{@code code} 字面量来自 {@link SecurityEventCategoryCodes}，与 recording 类别开关共用。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see SecurityEventCategoryCodes
 * @see SecurityEventType
 */
@Getter
@RequiredArgsConstructor
public enum SecurityEventCategory {

    AUTH(SecurityEventCategoryCodes.AUTH),
    ACCOUNT(SecurityEventCategoryCodes.ACCOUNT),
    CREDENTIAL(SecurityEventCategoryCodes.CREDENTIAL),
    ACCESS(SecurityEventCategoryCodes.ACCESS);

    /**
     * 分类 code，与 {@link SecurityEventCategoryCodes} 对应常量逐字相同。
     */
    private final String code;

    /**
     * 按 code 解析分类；空白或未知值返回 {@code null}，比较时忽略大小写。
     *
     * @param raw 分类 code，允许首尾空白
     * @return 匹配的枚举；无法识别时为 {@code null}
     */
    public static SecurityEventCategory fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        for (SecurityEventCategory c : values()) {
            if (c.code.equalsIgnoreCase(raw.trim())) {
                return c;
            }
        }
        return null;
    }
}
