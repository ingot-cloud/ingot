package com.ingot.framework.commons.model.security;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>并发会话策略的生效范围，决定一条策略记录匹配哪些登录请求。</p>
 *
 * <p>放在 commons 是因为它同时出现在三处：安全中心的策略表实体与 Platform 契约、
 * Auth 执行面的策略选取、以及 Nacos 地板配置。匹配优先级由窄到宽为
 * {@link #CLIENT} → {@link #USER_TYPE} → {@link #GLOBAL}，命中即止，不做字段级合并。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see SessionOverflowStrategy
 */
@Getter
@RequiredArgsConstructor
public enum SessionPolicyScope {
    /**
     * 全局默认，兜底匹配所有登录。
     */
    GLOBAL("GLOBAL", "全局"),
    /**
     * 按 OAuth2 Client 匹配，需填 clientId。
     */
    CLIENT("CLIENT", "按客户端"),
    /**
     * 按用户类型匹配，需填 userType。
     */
    USER_TYPE("USER_TYPE", "按用户类型");

    @JsonValue
    @EnumValue
    private final String value;

    private final String text;

    private static final Map<String, SessionPolicyScope> VALUE_MAP = new HashMap<>();

    static {
        for (SessionPolicyScope item : values()) {
            VALUE_MAP.put(item.getValue(), item);
        }
    }

    @JsonCreator
    public static SessionPolicyScope getEnum(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return VALUE_MAP.get(value.trim());
    }
}
