package com.ingot.framework.commons.model.security;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : 身份类型.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/1.</p>
 * <p>Time         : 17:38.</p>
 */
@Getter
@RequiredArgsConstructor
public enum UserIdentityTypeEnum {
    /**
     * 用户名，包括用户名，手机号，邮箱
     */
    USERNAME("0", "账号登录"),
    /**
     * 社交登录
     */
    SOCIAL("1", "社交登录");


    @JsonValue
    private final String value;
    private final String text;

    private static final Map<String, UserIdentityTypeEnum> valueMap = new HashMap<>();
    private static final Map<String, UserIdentityTypeEnum> textMap = new HashMap<>();

    static {
        for (UserIdentityTypeEnum item : UserIdentityTypeEnum.values()) {
            valueMap.put(item.getValue(), item);
            textMap.put(item.getText(), item);
        }
    }

    public static UserIdentityTypeEnum getEnumByText(String text) {
        return textMap.get(text);
    }

    @JsonCreator
    public static UserIdentityTypeEnum getEnum(String value) {
        return valueMap.get(value);
    }
}
