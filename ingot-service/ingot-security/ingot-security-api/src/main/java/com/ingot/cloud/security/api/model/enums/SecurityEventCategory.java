package com.ingot.cloud.security.api.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 统一安全事件分类。
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum SecurityEventCategory {

    AUTH("AUTH"),
    ACCOUNT("ACCOUNT"),
    CREDENTIAL("CREDENTIAL"),
    ACCESS("ACCESS");

    private final String code;

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
