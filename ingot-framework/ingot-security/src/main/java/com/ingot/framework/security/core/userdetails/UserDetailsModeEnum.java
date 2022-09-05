package com.ingot.framework.security.core.userdetails;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : UserDetailsModeEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/5.</p>
 * <p>Time         : 4:01 下午.</p>
 */
@Getter
@RequiredArgsConstructor
public enum UserDetailsModeEnum {
    PASSWORD("password", "密码登录"),
    SOCIAL("social", "社交登录");

    @JsonValue
    private final String value;
    private final String text;

    private static final Map<String, UserDetailsModeEnum> valueMap = new HashMap<>();

    static {
        for (UserDetailsModeEnum item : UserDetailsModeEnum.values()) {
            valueMap.put(item.getValue(), item);
        }
    }

    public static String getDesc(String value) {
        UserDetailsModeEnum en = get(value);
        return en != null ? en.text : null;
    }

    public static UserDetailsModeEnum get(String value) {
        return valueMap.get(value);
    }
}
