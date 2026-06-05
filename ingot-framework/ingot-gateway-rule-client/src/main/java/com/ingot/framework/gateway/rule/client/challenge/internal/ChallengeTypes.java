package com.ingot.framework.gateway.rule.client.challenge.internal;

/**
 * 挑战策略中的验证码类型与 VC 路由类型映射。
 *
 * <p>Platform / DB 侧常用 {@code SLIDER}；网关 VC 路由实际类型为 {@code image}。</p>
 *
 * @author jy
 * @since 2026/5/28
 */
public final class ChallengeTypes {

    public static final String VC_IMAGE = "image";
    public static final String VC_SMS = "sms";
    public static final String VC_EMAIL = "email";

    private ChallengeTypes() {
    }

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
