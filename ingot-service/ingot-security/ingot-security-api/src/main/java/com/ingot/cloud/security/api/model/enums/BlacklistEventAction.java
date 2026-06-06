package com.ingot.cloud.security.api.model.enums;

import java.util.Locale;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 网关黑名单审计事件动作，对应 {@code gateway_blacklist_event.action} {@code char(1)}。
 *
 * @author jy
 * @since 2026/6/4
 */
@Getter
@RequiredArgsConstructor
public enum BlacklistEventAction {

    /** 封禁（Block）。 */
    BLOCK("B"),

    /** 解封（Unblock）。 */
    UNBLOCK("U"),

    /** 续期（Renew TTL）。 */
    RENEW("R");

    private final String code;

    public static BlacklistEventAction fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return BLOCK;
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        for (BlacklistEventAction a : values()) {
            if (a.code.equals(upper) || a.name().equals(upper)) {
                return a;
            }
        }
        return BLOCK;
    }
}
