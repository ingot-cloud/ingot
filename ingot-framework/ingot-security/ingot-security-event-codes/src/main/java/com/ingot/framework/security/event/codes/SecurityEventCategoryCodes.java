package com.ingot.framework.security.event.codes;

/**
 * <p>安全事件分类 code 的唯一字面量来源，与 {@link SecurityEventCodes} 同层、无业务依赖。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class SecurityEventCategoryCodes {

    private SecurityEventCategoryCodes() {
    }

    /** 认证类：登录成功/失败、登出、会话撤销等。 */
    public static final String AUTH = "AUTH";

    /** 账号类：创建、启禁、锁定、删除等。 */
    public static final String ACCOUNT = "ACCOUNT";

    /** 凭证类：改密、重置、强制改密等。 */
    public static final String CREDENTIAL = "CREDENTIAL";

    /** 访问防护类：黑名单拦截、限流违规、登录失败超阈值等。 */
    public static final String ACCESS = "ACCESS";
}
