package com.ingot.framework.security.recording.support;

import java.util.UUID;

/**
 * <p>安全事件 {@code eventId} 生成工具，产出无连字符的 32 字符 UUID。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class SecurityEventIds {

    private SecurityEventIds() {
    }

    public static String newEventId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
