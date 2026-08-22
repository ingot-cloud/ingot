package com.ingot.framework.security.event.codes;

/**
 * <p>安全事件类型 code 的唯一字面量来源，供 recording 默认优先级表与
 * {@code ingot-security-api} 枚举共用。</p>
 *
 * <p>本模块零业务依赖。recording 只引用这些常量做 String switch；
 * 类型安全枚举仍只存在于 {@code ingot-security-api}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class SecurityEventCodes {

    private SecurityEventCodes() {
    }

    // ── AUTH ──

    /** 登录成功。 */
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";

    /** 登录失败。 */
    public static final String LOGIN_FAILURE = "LOGIN_FAILURE";

    /** 用户自助登出。 */
    public static final String LOGOUT = "LOGOUT";

    /** Token 刷新。 */
    public static final String TOKEN_REFRESH = "TOKEN_REFRESH";

    /** 会话被撤销（管理端或系统强制下线）。 */
    public static final String SESSION_REVOKED = "SESSION_REVOKED";

    /** 并发会话策略踢人。 */
    public static final String SESSION_CONCURRENT_KICKOUT = "SESSION_CONCURRENT_KICKOUT";

    // ── ACCOUNT ──

    /** 账号创建。 */
    public static final String ACCOUNT_CREATED = "ACCOUNT_CREATED";

    /** 账号启用。 */
    public static final String ACCOUNT_ENABLED = "ACCOUNT_ENABLED";

    /** 账号禁用。 */
    public static final String ACCOUNT_DISABLED = "ACCOUNT_DISABLED";

    /** 账号锁定。 */
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";

    /** 账号解锁。 */
    public static final String ACCOUNT_UNLOCKED = "ACCOUNT_UNLOCKED";

    /** 账号删除。 */
    public static final String ACCOUNT_DELETED = "ACCOUNT_DELETED";

    // ── CREDENTIAL ──

    /** 密码修改。 */
    public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";

    /** 密码重置。 */
    public static final String PASSWORD_RESET = "PASSWORD_RESET";

    /** 密码过期。 */
    public static final String PASSWORD_EXPIRED = "PASSWORD_EXPIRED";

    /** 强制修改密码。 */
    public static final String FORCE_CHANGE_PASSWORD = "FORCE_CHANGE_PASSWORD";

    // ── ACCESS ──

    /** 黑名单拦截。 */
    public static final String BLACKLIST_BLOCK = "BLACKLIST_BLOCK";

    /** 限流违规。 */
    public static final String RATE_LIMIT_VIOLATION = "RATE_LIMIT_VIOLATION";

    /** 同一 IP 登录失败超阈值。 */
    public static final String LOGIN_FAIL_IP_EXCEED = "LOGIN_FAIL_IP_EXCEED";

    /** 同一设备登录失败超阈值。 */
    public static final String LOGIN_FAIL_DEVICE_EXCEED = "LOGIN_FAIL_DEVICE_EXCEED";

    /** 同一客户端登录失败超阈值。 */
    public static final String LOGIN_FAIL_CLIENT_EXCEED = "LOGIN_FAIL_CLIENT_EXCEED";

    /** 同一账号+IP 登录失败超阈值。 */
    public static final String LOGIN_FAIL_ACCOUNT_IP_EXCEED = "LOGIN_FAIL_ACCOUNT_IP_EXCEED";
}
