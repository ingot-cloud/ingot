package com.ingot.framework.commons.model.security;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>并发会话数的统计维度，决定「最多 N 个会话」按什么口径计数。</p>
 *
 * <p>当前仅支持 {@link #USER_CLIENT}：同一租户下同一用户在同一 Client 的会话集合。
 * 「每个设备一个会话」需要登录设备指纹能力，不在本闭环范围，因此这里不预留占位值，
 * 避免出现能配置但不生效的维度。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see SessionOverflowStrategy
 */
@Getter
@RequiredArgsConstructor
public enum SessionConcurrencyDimension {
    /**
     * 同一租户 + 同一 Client + 同一用户。
     */
    USER_CLIENT("USER_CLIENT", "同一用户同一客户端");

    @JsonValue
    @EnumValue
    private final String value;

    private final String text;

    private static final Map<String, SessionConcurrencyDimension> VALUE_MAP = new HashMap<>();

    static {
        for (SessionConcurrencyDimension item : values()) {
            VALUE_MAP.put(item.getValue(), item);
        }
    }

    @JsonCreator
    public static SessionConcurrencyDimension getEnum(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return VALUE_MAP.get(value.trim());
    }
}
