package com.ingot.framework.gateway.rule.client.challenge.internal;

import com.ingot.cloud.security.api.model.enums.ChallengeCaptchaType;
import lombok.experimental.UtilityClass;

/**
 * <p>挑战策略验证码类型到 VC 模块路由类型的映射。</p>
 *
 * <p>L6 仅 {@link ChallengeCaptchaType#IMAGE} / {@link ChallengeCaptchaType#SLIDER}
 * 可执行，均映射为 {@link #VC_IMAGE}。短信 / 邮箱不进入此映射。</p>
 *
 * @author jy
 * @since 2026/5/28
 */
@UtilityClass
public class ChallengeTypes {

    /**
     * 滑块 / 图形验证码对应的 VC 路由类型，路径前缀 {@code /vc/image}。
     */
    public static final String VC_IMAGE = "image";

    /**
     * VC 接口路径前缀，挑战策略不得匹配此前缀，以免验码接口自挑战。
     */
    public static final String VC_PATH_PREFIX = ChallengeCaptchaType.VC_PATH_PREFIX;

    /**
     * 将策略 challengeType 转为 VC 路由类型。
     *
     * @param challengeType 策略字段；{@code SLIDER}/{@code IMAGE}（忽略大小写）→ {@link #VC_IMAGE}
     * @return VC 类型；不受支持的取值（含 {@code null}、空白、SMS/EMAIL）返回 {@code null}
     */
    public static String toVcType(String challengeType) {
        if (!ChallengeCaptchaType.isSupported(challengeType)) {
            return null;
        }
        return VC_IMAGE;
    }

    /**
     * 判断请求路径是否为验证码接口，此类请求不得再套 ALWAYS 挑战。
     *
     * @param path 网关 path，可为 {@code null}
     * @return {@code true} 当路径为 {@code /vc} 或其子路径
     */
    public static boolean isVcPath(String path) {
        return ChallengeCaptchaType.isVcPath(path);
    }
}
