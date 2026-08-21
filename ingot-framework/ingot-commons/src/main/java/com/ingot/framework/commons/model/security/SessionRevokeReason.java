package com.ingot.framework.commons.model.security;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>会话撤销原因，决定安全事件类型与审计口径。</p>
 *
 * <p>放在 commons 是因为它同时出现在三处：Auth 执行面的领域服务入参、
 * {@code ingot-auth-api} 的 Inner 契约 DTO、以及安全事件 payload。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum SessionRevokeReason {
    /**
     * 用户自助登出，由边缘代理编排。
     */
    USER_LOGOUT("USER_LOGOUT", false),
    /**
     * 管理员在安全中心强制下线。
     */
    ADMIN_REVOKE("ADMIN_REVOKE", true),
    /**
     * 并发会话策略踢出旧会话。
     */
    CONCURRENT_KICKOUT("CONCURRENT_KICKOUT", true),
    /**
     * 密码修改或重置联动撤销。
     */
    PASSWORD_CHANGED("PASSWORD_CHANGED", true),
    /**
     * 账号锁定联动撤销。
     */
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", true),
    /**
     * 账号禁用联动撤销。
     */
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", true),
    /**
     * 租户级批量撤销。
     */
    TENANT_REVOKE("TENANT_REVOKE", true);

    private final String value;

    /**
     * 是否属于「被动下线」：由他人或系统发起，而非用户自己主动登出。
     */
    private final boolean forced;

    private static final Map<String, SessionRevokeReason> VALUE_MAP = new HashMap<>();

    static {
        for (SessionRevokeReason item : values()) {
            VALUE_MAP.put(item.getValue(), item);
        }
    }

    public static SessionRevokeReason getEnum(String value) {
        return VALUE_MAP.get(value);
    }
}
