package com.ingot.framework.gateway.rule.client.challenge.internal;

import lombok.experimental.UtilityClass;

/**
 * 挑战策略验证码类型 → VC 模块路由类型映射。
 *
 * <p>Platform / DB 侧策略字段 {@code challengeType} 常用枚举名（如 {@code SLIDER}）；
 * 网关转发到 VC 服务时需转换为 VC 路由识别的类型字符串（如 {@code image}）。</p>
 *
 * @author jy
 * @since 2026/5/28
 */
@UtilityClass
public class ChallengeTypes {

    /** 滑块 / 图形验证码，对应 VC 路由 {@code /vc/image/**}。 */
    public static final String VC_IMAGE = "image";

    /** 短信验证码，对应 VC 路由 {@code /vc/sms/**}。 */
    public static final String VC_SMS = "sms";

    /** 邮件验证码，对应 VC 路由 {@code /vc/email/**}。 */
    public static final String VC_EMAIL = "email";

    /**
     * 将策略 challengeType 转换为 VC 路由类型。
     * <ul>
     *     <li>{@code SLIDER} / {@code IMAGE} → {@link #VC_IMAGE}</li>
     *     <li>{@code SMS} → {@link #VC_SMS}</li>
     *     <li>{@code EMAIL} → {@link #VC_EMAIL}</li>
     *     <li>其他值 → 原值转小写透传</li>
     *     <li>null / 空白 → 默认 {@link #VC_IMAGE}</li>
     * </ul>
     */
    public static String toVcType(String challengeType) {
        if (challengeType == null || challengeType.isBlank()) {
            return VC_IMAGE;
        }
        return switch (challengeType.trim().toUpperCase()) {
            case "SLIDER", "IMAGE" -> VC_IMAGE;
            case "SMS" -> VC_SMS;
            case "EMAIL" -> VC_EMAIL;
            default -> challengeType.trim().toLowerCase();
        };
    }
}
