package com.ingot.framework.commons.utils;

import com.ingot.framework.commons.constants.BffConstants;

/**
 * <p>
 * 按固定应用命名的 host-only Cookie；端口不是 Cookie 隔离边界。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class BffCookiePolicy {

    /** 仅用于当前Host清理的v1 Cookie名称，不参与认证回退。 */
    public static final java.util.Set<String> LEGACY_COOKIES = java.util.Set.of("__Host-IN_SESSION",
            "__Host-IN_AUTH_BINDING", "IN_SESSION", "IN_AUTH_BINDING");

    private static final String HOST_PREFIX = "__Host-";

    private static final String PLATFORM_PREFIX = "IN_PLATFORM_";

    private static final String TENANT_PREFIX = "IN_TENANT_";

    private static final String SESSION = "SESSION";

    private static final String BINDING = "BINDING";

    private BffCookiePolicy() {
    }

    /** 只选择当前可信应用的正式会话名称。 */
    public static String sessionCookieName(boolean requireHttps, String appId) {
        return prefix(requireHttps, appId) + SESSION;
    }

    /** 只选择当前可信应用的匿名绑定名称。 */
    public static String bindingCookieName(boolean requireHttps, String appId) {
        return prefix(requireHttps, appId) + BINDING;
    }

    private static String prefix(boolean secure, String appId) {
        String app = switch (appId) {
            case BffConstants.APP_PLATFORM -> PLATFORM_PREFIX;
            case BffConstants.APP_TENANT -> TENANT_PREFIX;
            default -> throw new IllegalArgumentException("未知 BFF appId");
        };
        return (secure ? HOST_PREFIX : "") + app;
    }

    /** 签发/清除 HttpOnly Cookie；不允许 Domain，Secure 由受信配置决定。 */
    public static String setCookieHeader(String name, String value, long maxAgeSeconds, boolean requireHttps) {
        return name + "=" + (value == null ? "" : value) + "; Path=/; HttpOnly; SameSite=Lax; Max-Age="
                + Math.max(0, maxAgeSeconds) + (requireHttps ? "; Secure" : "");
    }

}
