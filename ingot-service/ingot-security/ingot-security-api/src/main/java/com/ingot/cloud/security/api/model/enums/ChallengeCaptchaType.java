package com.ingot.cloud.security.api.model.enums;

/**
 * <p>L6 挑战执行面允许的验证码类型：图形或滑块，均路由到 {@code /vc/image}。</p>
 *
 * <p>短信 / 邮箱取值仍可能出现在历史库行中，但执行面不识别，编译时跳过。
 * YAML 与库字面量为大写 {@code IMAGE}/{@code SLIDER}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public enum ChallengeCaptchaType {

    /**
     * 图形验证码，VC 路由 {@code image}。
     */
    IMAGE,

    /**
     * 滑块拼图（anji blockPuzzle），VC 路由同样是 {@code image}。
     */
    SLIDER;

    /**
     * YAML / DB 字面量 {@code IMAGE}。
     */
    public static final String VALUE_IMAGE = "IMAGE";

    /**
     * YAML / DB 字面量 {@code SLIDER}。
     */
    public static final String VALUE_SLIDER = "SLIDER";

    /**
     * 验证码 HTTP 路径前缀，与 {@code VCConstants.PATH_PREFIX} 对齐。
     * 挑战策略不得覆盖此前缀，以免验码接口自挑战。
     */
    public static final String VC_PATH_PREFIX = "/vc";

    /**
     * 判断策略 {@code challengeType} 是否为 L6 可执行类型。
     *
     * @param raw 策略字段，允许大小写混用；{@code null} / 空白视为不支持
     * @return {@code true} 当且仅当规范化后为 {@link #VALUE_IMAGE} 或 {@link #VALUE_SLIDER}
     */
    public static boolean isSupported(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String normalized = raw.trim().toUpperCase();
        return VALUE_IMAGE.equals(normalized) || VALUE_SLIDER.equals(normalized);
    }

    /**
     * 判断路径是否为验证码接口（{@code /vc} 或其子路径）。
     *
     * @param path 网关 path 或策略 pattern，可为 {@code null}
     * @return {@code true} 当路径为 {@link #VC_PATH_PREFIX} 或其子路径
     */
    public static boolean isVcPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String normalized = path.trim();
        return VC_PATH_PREFIX.equals(normalized) || normalized.startsWith(VC_PATH_PREFIX + "/");
    }
}
