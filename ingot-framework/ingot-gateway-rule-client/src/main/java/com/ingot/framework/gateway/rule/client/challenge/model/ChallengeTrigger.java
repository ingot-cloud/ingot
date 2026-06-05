package com.ingot.framework.gateway.rule.client.challenge.model;

/**
 * 挑战触发条件。
 *
 * @author jy
 * @since 2026/5/26
 */
public enum ChallengeTrigger {
    /**
     * 任何匹配路径都先验证码（多用于注册 / 找回密码 / 评论发表等高危接口）。
     */
    ALWAYS,
    /**
     * Sentinel 限流命中时挑战，验证码通过则放行。
     */
    ON_RATE_LIMIT;

    public static ChallengeTrigger parse(String raw) {
        if (raw == null) return ON_RATE_LIMIT;
        return switch (raw.toLowerCase()) {
            case "always" -> ALWAYS;
            case "on_rate_limit" -> ON_RATE_LIMIT;
            case "on_failure_threshold" -> throw new IllegalArgumentException(
                    "unsupported trigger: on_failure_threshold (use account-domain lockout instead)");
            default -> ON_RATE_LIMIT;
        };
    }
}
