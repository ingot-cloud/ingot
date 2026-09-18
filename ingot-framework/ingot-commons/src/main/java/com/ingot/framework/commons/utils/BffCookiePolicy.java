package com.ingot.framework.commons.utils;

import cn.hutool.core.util.StrUtil;

/**
 * <p>按 {@code ingot.bff.require-https} 选择 BFF host-only Cookie 名称与 {@code Set-Cookie} 属性。</p>
 *
 * <p>已部署环境（内网测试/生产、公网生产）必须 {@code requireHttps=true}：名称带 {@code __Host-} 前缀并加
 * {@code Secure}。本机 DEV 可 {@code false}：无前缀、不加 {@code Secure}，以便 {@code http://localhost} 收下 Cookie。
 * 一律不写 {@code Domain}。不要按请求 scheme 或 {@code request.isSecure()} 分支，避免边缘 TLS 终止后误判。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class BffCookiePolicy {

    /**
     * HTTPS 正式会话 Cookie 名；浏览器要求 Secure、Path=/、无 Domain。
     */
    public static final String SESSION_COOKIE_HOST = "__Host-IN_SESSION";

    /**
     * DEV HTTP 正式会话 Cookie 名。
     */
    public static final String SESSION_COOKIE_PLAIN = "IN_SESSION";

    /**
     * HTTPS 临时绑定 Cookie 名。
     */
    public static final String BINDING_COOKIE_HOST = "__Host-IN_AUTH_BINDING";

    /**
     * DEV HTTP 临时绑定 Cookie 名。
     */
    public static final String BINDING_COOKIE_PLAIN = "IN_AUTH_BINDING";

    private static final String SAME_SITE_LAX = "Lax";

    private BffCookiePolicy() {
    }

    /**
     * 当前环境正式会话 Cookie 名。
     *
     * @param requireHttps 与 {@code ingot.bff.require-https} 相同；true 为 {@link #SESSION_COOKIE_HOST}
     * @return 不可空的 Cookie 名
     */
    public static String sessionCookieName(boolean requireHttps) {
        return requireHttps ? SESSION_COOKIE_HOST : SESSION_COOKIE_PLAIN;
    }

    /**
     * 当前环境绑定 Cookie 名。
     *
     * @param requireHttps 与 {@code ingot.bff.require-https} 相同；true 为 {@link #BINDING_COOKIE_HOST}
     * @return 不可空的 Cookie 名
     */
    public static String bindingCookieName(boolean requireHttps) {
        return requireHttps ? BINDING_COOKIE_HOST : BINDING_COOKIE_PLAIN;
    }

    /**
     * 拼 host-only {@code Set-Cookie}：Path=/、HttpOnly、SameSite=Lax；{@code requireHttps} 时追加 Secure。
     *
     * @param name           {@link #sessionCookieName(boolean)} 或 {@link #bindingCookieName(boolean)}
     * @param value          Cookie 值；null 当空串，配合 Max-Age=0 用于清除
     * @param maxAgeSeconds  存活秒数
     * @param requireHttps   true 时写 Secure（与 {@code __Host-} 前缀配套）
     * @return 完整 Set-Cookie 头值
     */
    public static String setCookieHeader(String name, String value, long maxAgeSeconds, boolean requireHttps) {
        StringBuilder builder = new StringBuilder();
        builder.append(name).append('=').append(StrUtil.nullToEmpty(value));
        builder.append("; Path=/");
        builder.append("; Max-Age=").append(maxAgeSeconds);
        builder.append("; HttpOnly");
        if (requireHttps) {
            builder.append("; Secure");
        }
        builder.append("; SameSite=").append(SAME_SITE_LAX);
        return builder.toString();
    }
}
