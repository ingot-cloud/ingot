package com.ingot.framework.security.recording.model;

import java.util.Locale;

/**
 * <p>安全事件投递目标拓扑。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public enum RecordingTarget {

    /** 写入本地 SecurityEventStore。 */
    LOCAL,

    /** 经 SecurityEventTransport 送达安全中心。 */
    CENTER;

    /**
     * 解析 YAML / Environment 字面量。
     * <p>接受 {@code local}、{@code center}；历史值 {@code remote} 视为 {@link #CENTER}。
     * 空白视为 {@link #LOCAL}。</p>
     *
     * @throws IllegalArgumentException 无法识别的取值
     */
    public static RecordingTarget fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return LOCAL;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "center", "remote" -> CENTER;
            case "local" -> LOCAL;
            default -> throw new IllegalArgumentException("Unknown recording target: " + raw);
        };
    }
}
