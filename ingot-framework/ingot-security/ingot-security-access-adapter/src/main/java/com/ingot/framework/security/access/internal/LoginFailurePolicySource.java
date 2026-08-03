package com.ingot.framework.security.access.internal;

/**
 * 登录失败策略当前生效来源。
 *
 * @author jy
 * @since 1.0.0
 */
public enum LoginFailurePolicySource {

    REMOTE,
    LAST_KNOWN_GOOD,
    LOCAL_FLOOR
}
