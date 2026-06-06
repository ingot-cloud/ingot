package com.ingot.framework.gateway.rule.client.ratelimit.model;

import java.util.Locale;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Sentinel 流控行为，对应限流规则 {@code control_behavior} 字段（DB / yaml）。
 *
 * <ul>
 *     <li>{@link #FAST_FAIL}（{@code F}）— 超限立即拒绝，网关返回 429 或 412</li>
 *     <li>{@link #QUEUE}（{@code Q}）— 匀速排队，由网关编译为 Sentinel RateLimiter 行为</li>
 * </ul>
 *
 * <p>yaml 示例：{@code control-behavior: F}</p>
 *
 * @author jy
 * @since 2026/6/4
 */
@Getter
@RequiredArgsConstructor
public enum RateLimitControlBehavior {

    /** 快速失败（默认）。 */
    FAST_FAIL("F"),

    /** 排队等待。 */
    QUEUE("Q");

    private final String code;

    /** 是否为排队（Q）行为。 */
    public boolean isQueue() {
        return this == QUEUE;
    }

    public static RateLimitControlBehavior fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return FAST_FAIL;
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        for (RateLimitControlBehavior b : values()) {
            if (b.code.equals(upper) || b.name().equals(upper)) {
                return b;
            }
        }
        return FAST_FAIL;
    }
}
