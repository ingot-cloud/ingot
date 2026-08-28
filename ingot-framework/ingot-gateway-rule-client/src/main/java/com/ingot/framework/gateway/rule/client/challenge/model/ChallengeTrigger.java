package com.ingot.framework.gateway.rule.client.challenge.model;

import com.ingot.cloud.security.api.model.vo.policy.ChallengePolicyVO;

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

    /**
     * YAML / DB 字面量 {@code always}。
     */
    public static final String VALUE_ALWAYS = ChallengePolicyVO.TRIGGER_ALWAYS;

    /**
     * YAML / DB 字面量 {@code on_rate_limit}。
     */
    public static final String VALUE_ON_RATE_LIMIT = ChallengePolicyVO.TRIGGER_ON_RATE_LIMIT;

    /**
     * 解析触发条件。空白或未知取值回落到 {@link #ON_RATE_LIMIT}；
     * {@code on_failure_threshold} 已废弃，抛出异常。
     *
     * @param raw 策略字段，忽略大小写
     * @return 对应枚举；{@code null} 视为 {@link #ON_RATE_LIMIT}
     */
    public static ChallengeTrigger parse(String raw) {
        if (raw == null) return ON_RATE_LIMIT;
        return switch (raw.toLowerCase()) {
            case VALUE_ALWAYS -> ALWAYS;
            case VALUE_ON_RATE_LIMIT -> ON_RATE_LIMIT;
            case "on_failure_threshold" -> throw new IllegalArgumentException(
                    "unsupported trigger: on_failure_threshold (use account-domain lockout instead)");
            default -> ON_RATE_LIMIT;
        };
    }
}
