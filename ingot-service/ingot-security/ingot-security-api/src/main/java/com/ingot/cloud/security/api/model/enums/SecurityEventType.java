package com.ingot.cloud.security.api.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 统一安全事件类型（跨模块 SoT）。
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum SecurityEventType {

    LOGIN_SUCCESS("LOGIN_SUCCESS", SecurityEventCategory.AUTH),
    LOGIN_FAILURE("LOGIN_FAILURE", SecurityEventCategory.AUTH),
    LOGOUT("LOGOUT", SecurityEventCategory.AUTH),
    TOKEN_REFRESH("TOKEN_REFRESH", SecurityEventCategory.AUTH),
    SESSION_REVOKED("SESSION_REVOKED", SecurityEventCategory.AUTH),
    SESSION_CONCURRENT_KICKOUT("SESSION_CONCURRENT_KICKOUT", SecurityEventCategory.AUTH),

    ACCOUNT_CREATED("ACCOUNT_CREATED", SecurityEventCategory.ACCOUNT),
    ACCOUNT_ENABLED("ACCOUNT_ENABLED", SecurityEventCategory.ACCOUNT),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", SecurityEventCategory.ACCOUNT),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", SecurityEventCategory.ACCOUNT),
    ACCOUNT_UNLOCKED("ACCOUNT_UNLOCKED", SecurityEventCategory.ACCOUNT),
    ACCOUNT_DELETED("ACCOUNT_DELETED", SecurityEventCategory.ACCOUNT),

    PASSWORD_CHANGED("PASSWORD_CHANGED", SecurityEventCategory.CREDENTIAL),
    PASSWORD_RESET("PASSWORD_RESET", SecurityEventCategory.CREDENTIAL),
    PASSWORD_EXPIRED("PASSWORD_EXPIRED", SecurityEventCategory.CREDENTIAL),
    FORCE_CHANGE_PASSWORD("FORCE_CHANGE_PASSWORD", SecurityEventCategory.CREDENTIAL),

    BLACKLIST_BLOCK("BLACKLIST_BLOCK", SecurityEventCategory.ACCESS),
    RATE_LIMIT_VIOLATION("RATE_LIMIT_VIOLATION", SecurityEventCategory.ACCESS),
    LOGIN_FAIL_IP_EXCEED("LOGIN_FAIL_IP_EXCEED", SecurityEventCategory.ACCESS),
    LOGIN_FAIL_DEVICE_EXCEED("LOGIN_FAIL_DEVICE_EXCEED", SecurityEventCategory.ACCESS),
    LOGIN_FAIL_CLIENT_EXCEED("LOGIN_FAIL_CLIENT_EXCEED", SecurityEventCategory.ACCESS),
    LOGIN_FAIL_ACCOUNT_IP_EXCEED("LOGIN_FAIL_ACCOUNT_IP_EXCEED", SecurityEventCategory.ACCESS);

    private final String code;
    private final SecurityEventCategory category;

    public static SecurityEventType fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim();
        for (SecurityEventType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
