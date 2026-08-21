package com.ingot.framework.commons.model.security;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>会话数达到上限后新登录的处置方式。</p>
 *
 * <p>三者都保证「登录成功后在线会话数不超过上限」，区别只在牺牲谁：{@link #REJECT} 保住旧会话、
 * 拒绝新登录；{@link #KICK_OLDEST} 与 {@link #KICK_ALL} 保住新登录，被踢会话按
 * {@link SessionRevokeReason#CONCURRENT_KICKOUT} 完整撤销（含 Refresh Token）。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see SessionConcurrencyDimension
 */
@Getter
@RequiredArgsConstructor
public enum SessionOverflowStrategy {
    /**
     * 拒绝新登录，旧会话全部保留。
     */
    REJECT("REJECT", "拒绝新登录"),
    /**
     * 按会话创建时间踢除最旧会话，直到腾出一个名额。
     */
    KICK_OLDEST("KICK_OLDEST", "踢除最旧会话"),
    /**
     * 踢除该维度下全部旧会话，新登录成为唯一会话。
     */
    KICK_ALL("KICK_ALL", "踢除全部旧会话");

    @JsonValue
    @EnumValue
    private final String value;

    private final String text;

    private static final Map<String, SessionOverflowStrategy> VALUE_MAP = new HashMap<>();

    static {
        for (SessionOverflowStrategy item : values()) {
            VALUE_MAP.put(item.getValue(), item);
        }
    }

    @JsonCreator
    public static SessionOverflowStrategy getEnum(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return VALUE_MAP.get(value.trim());
    }
}
