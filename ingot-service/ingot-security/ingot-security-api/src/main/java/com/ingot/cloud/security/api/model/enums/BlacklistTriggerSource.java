package com.ingot.cloud.security.api.model.enums;

import java.util.Locale;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 黑名单事件触发来源，对应 {@code gateway_blacklist_event.trigger_source} {@code char(1)}。
 *
 * @author jy
 * @since 2026/6/4
 */
@Getter
@RequiredArgsConstructor
public enum BlacklistTriggerSource {

    /** 自动触发（如限流违规升级）。 */
    AUTO("A"),

    /** 管理面手工操作。 */
    MANUAL("M");

    private final String code;

    public static BlacklistTriggerSource fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return AUTO;
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        for (BlacklistTriggerSource s : values()) {
            if (s.code.equals(upper) || s.name().equals(upper)) {
                return s;
            }
        }
        return AUTO;
    }
}
