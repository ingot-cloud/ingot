package com.ingot.framework.security.common.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description  : UserType.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/12.</p>
 * <p>Time         : 7:40 PM.</p>
 */
@Getter
@RequiredArgsConstructor
public enum UserType {
    ADMIN("0", "管理用户"),
    APP("1", "app用户");

    @JsonValue
    private final String value;
    private final String text;

    private static final Map<String, UserType> valueMap = new HashMap<>();
    private static final Map<String, UserType> textMap = new HashMap<>();

    static {
        for (UserType item : UserType.values()) {
            valueMap.put(item.getValue(), item);
            textMap.put(item.getText(), item);
        }
    }

    public static UserType getEnumByText(String text) {
        return textMap.get(text);
    }

    @JsonCreator
    public static UserType getEnum(String value) {
        return valueMap.get(value);
    }
}
