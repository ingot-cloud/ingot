package com.ingot.framework.commons.model.security;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : UserType.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/12.</p>
 * <p>Time         : 7:40 PM.</p>
 */
@Getter
@RequiredArgsConstructor
public enum UserTypeEnum {
    ADMIN("0", "管理用户"),
    APP("1", "C端用户");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;

    private static final Map<String, UserTypeEnum> valueMap = new HashMap<>();
    private static final Map<String, UserTypeEnum> textMap = new HashMap<>();

    static {
        for (UserTypeEnum item : UserTypeEnum.values()) {
            valueMap.put(item.getValue(), item);
            textMap.put(item.getText(), item);
        }
    }

    public static UserTypeEnum getEnumByText(String text) {
        return textMap.get(text);
    }

    @JsonCreator
    public static UserTypeEnum getEnum(String value) {
        return valueMap.get(value);
    }
}
