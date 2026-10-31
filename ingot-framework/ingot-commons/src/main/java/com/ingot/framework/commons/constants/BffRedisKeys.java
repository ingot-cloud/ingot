package com.ingot.framework.commons.constants;

/**
 * <p>
 * BFF v2 安全状态键；同应用 hash tag 保证跨绑定替换也能同槽提交。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class BffRedisKeys {

    private static final String PREFIX = "in:bff:v2:";

    private BffRedisKeys() {
    }

    /** 限定两个服务端应用，不接受客户端任意 hash tag。 */
    public static String prefix(String appId) {
        if (!BffConstants.APP_PLATFORM.equals(appId) && !BffConstants.APP_TENANT.equals(appId))
            throw new IllegalArgumentException("未知 BFF 应用");
        return PREFIX + "{" + appId + "}:";
    }

    /** 匿名绑定。 */
    public static String binding(String appId, String id) {
        return prefix(appId) + "binding:" + id;
    }

    /** 登录事务。 */
    public static String transaction(String appId, String id) {
        return prefix(appId) + "tx:" + id;
    }

    /** 当前绑定的正式会话代次（不可复用的随机 sessionId）。 */
    public static String current(String appId, String id) {
        return prefix(appId) + "binding-current:" + id;
    }

    /** 正式会话。 */
    public static String session(String appId, String id) {
        return prefix(appId) + "session:" + id;
    }

    /** 正式 Cookie 的交付确认。 */
    public static String delivered(String appId, String id) {
        return prefix(appId) + "delivered:" + id;
    }

    /** 绑定未终结事务的到期索引。 */
    public static String active(String appId, String id) {
        return prefix(appId) + "active:" + id;
    }

    /** 可靠撤销载荷，无 TTL，撤销成功才移除。 */
    public static String cleanup(String appId) {
        return prefix(appId) + "cleanup";
    }

    /** 有界清理到期索引。 */
    public static String cleanupDue(String appId) {
        return prefix(appId) + "cleanup-due";
    }

    /** 后台清理租约。 */
    public static String cleanupLease(String appId, String id) {
        return prefix(appId) + "cleanup-lease:" + id;
    }

}
