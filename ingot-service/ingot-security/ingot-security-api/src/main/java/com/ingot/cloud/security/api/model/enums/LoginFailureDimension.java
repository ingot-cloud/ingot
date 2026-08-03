package com.ingot.cloud.security.api.model.enums;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 登录失败保护维度，对应 {@code login_failure_protection_policy.dimension}。
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum LoginFailureDimension {

    IP("IP", "同一 IP"),
    DEVICE("DEVICE", "同一设备指纹"),
    CLIENT("CLIENT", "同一 OAuth2 Client"),
    ACCOUNT_IP("ACCOUNT_IP", "同一账号+IP 组合");

    @JsonValue
    @EnumValue
    private final String value;

    private final String text;

    private static final Map<String, LoginFailureDimension> VALUE_MAP = new HashMap<>();

    static {
        for (LoginFailureDimension item : LoginFailureDimension.values()) {
            VALUE_MAP.put(item.getValue(), item);
            VALUE_MAP.put(item.name(), item);
        }
    }

    @JsonCreator
    public static LoginFailureDimension getEnum(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return VALUE_MAP.get(value.trim());
    }

    /**
     * 解析维度字符串，兼容 DB 值与枚举名。
     */
    public static LoginFailureDimension fromCode(String raw) {
        return getEnum(raw);
    }
}
