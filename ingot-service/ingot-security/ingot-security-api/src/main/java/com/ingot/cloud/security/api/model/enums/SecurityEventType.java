package com.ingot.cloud.security.api.model.enums;

import com.ingot.framework.security.event.codes.SecurityEventCodes;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>统一安全事件类型（跨模块 wire / 校验 SoT）。</p>
 *
 * <p>{@code code} 字面量来自 {@link SecurityEventCodes}，与 recording 默认优先级表共用，
 * 避免枚举与 classifier 漂移。未知 code 由 {@link #fromCode(String)} 返回 {@code null}。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see SecurityEventCodes
 * @see SecurityEventCategory
 */
@Getter
@RequiredArgsConstructor
public enum SecurityEventType {

    LOGIN_SUCCESS(SecurityEventCodes.LOGIN_SUCCESS, SecurityEventCategory.AUTH),
    LOGIN_FAILURE(SecurityEventCodes.LOGIN_FAILURE, SecurityEventCategory.AUTH),
    LOGOUT(SecurityEventCodes.LOGOUT, SecurityEventCategory.AUTH),
    TOKEN_REFRESH(SecurityEventCodes.TOKEN_REFRESH, SecurityEventCategory.AUTH),
    SESSION_REVOKED(SecurityEventCodes.SESSION_REVOKED, SecurityEventCategory.AUTH),
    SESSION_CONCURRENT_KICKOUT(SecurityEventCodes.SESSION_CONCURRENT_KICKOUT, SecurityEventCategory.AUTH),

    ACCOUNT_CREATED(SecurityEventCodes.ACCOUNT_CREATED, SecurityEventCategory.ACCOUNT),
    ACCOUNT_ENABLED(SecurityEventCodes.ACCOUNT_ENABLED, SecurityEventCategory.ACCOUNT),
    ACCOUNT_DISABLED(SecurityEventCodes.ACCOUNT_DISABLED, SecurityEventCategory.ACCOUNT),
    ACCOUNT_LOCKED(SecurityEventCodes.ACCOUNT_LOCKED, SecurityEventCategory.ACCOUNT),
    ACCOUNT_UNLOCKED(SecurityEventCodes.ACCOUNT_UNLOCKED, SecurityEventCategory.ACCOUNT),
    ACCOUNT_DELETED(SecurityEventCodes.ACCOUNT_DELETED, SecurityEventCategory.ACCOUNT),

    PASSWORD_CHANGED(SecurityEventCodes.PASSWORD_CHANGED, SecurityEventCategory.CREDENTIAL),
    PASSWORD_RESET(SecurityEventCodes.PASSWORD_RESET, SecurityEventCategory.CREDENTIAL),
    PASSWORD_EXPIRED(SecurityEventCodes.PASSWORD_EXPIRED, SecurityEventCategory.CREDENTIAL),
    FORCE_CHANGE_PASSWORD(SecurityEventCodes.FORCE_CHANGE_PASSWORD, SecurityEventCategory.CREDENTIAL),

    BLACKLIST_BLOCK(SecurityEventCodes.BLACKLIST_BLOCK, SecurityEventCategory.ACCESS),
    RATE_LIMIT_VIOLATION(SecurityEventCodes.RATE_LIMIT_VIOLATION, SecurityEventCategory.ACCESS),
    LOGIN_FAIL_IP_EXCEED(SecurityEventCodes.LOGIN_FAIL_IP_EXCEED, SecurityEventCategory.ACCESS),
    LOGIN_FAIL_DEVICE_EXCEED(SecurityEventCodes.LOGIN_FAIL_DEVICE_EXCEED, SecurityEventCategory.ACCESS),
    LOGIN_FAIL_CLIENT_EXCEED(SecurityEventCodes.LOGIN_FAIL_CLIENT_EXCEED, SecurityEventCategory.ACCESS),
    LOGIN_FAIL_ACCOUNT_IP_EXCEED(SecurityEventCodes.LOGIN_FAIL_ACCOUNT_IP_EXCEED, SecurityEventCategory.ACCESS);

    /**
     * 事件类型 code，与 {@link SecurityEventCodes} 对应常量逐字相同。
     */
    private final String code;

    /**
     * 所属大类，用于 recording 类别开关与入库 {@code event_category}。
     */
    private final SecurityEventCategory category;

    /**
     * 按 code 解析类型；空白或未知值返回 {@code null}，不抛异常。
     *
     * @param raw 事件类型 code，允许首尾空白
     * @return 匹配的枚举；无法识别时为 {@code null}
     */
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
