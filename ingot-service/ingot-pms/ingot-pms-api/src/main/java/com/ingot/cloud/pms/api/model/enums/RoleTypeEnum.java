package com.ingot.cloud.pms.api.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description  : RoleTypeEnum.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/11.</p>
 * <p>Time         : 15:47.</p>
 */
@Getter
@RequiredArgsConstructor
public enum RoleTypeEnum {
    ROLE("0", "角色"),
    GROUP("1", "角色组");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;

    private static final Map<String, RoleTypeEnum> valueMap = new HashMap<>();
    private static final Map<String, RoleTypeEnum> textMap = new HashMap<>();

    static {
        for (RoleTypeEnum item : RoleTypeEnum.values()) {
            valueMap.put(item.getValue(), item);
            textMap.put(item.getText(), item);
        }
    }

    public static RoleTypeEnum getEnumByText(String text) {
        return textMap.get(text);
    }

    @JsonCreator
    public static RoleTypeEnum getEnum(String value) {
        return valueMap.get(value);
    }
}
