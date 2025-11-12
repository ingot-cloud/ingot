package com.ingot.cloud.pms.api.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description  : AuthorityTypeEnum.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/12.</p>
 * <p>Time         : 09:11.</p>
 */
@Getter
@RequiredArgsConstructor
public enum AuthorityTypeEnum {
    MENU("0", "菜单权限"),
    API("1", "API权限");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;

    private static final Map<String, AuthorityTypeEnum> valueMap = new HashMap<>();
    private static final Map<String, AuthorityTypeEnum> textMap = new HashMap<>();

    static {
        for (AuthorityTypeEnum item : AuthorityTypeEnum.values()) {
            valueMap.put(item.getValue(), item);
            textMap.put(item.getText(), item);
        }
    }

    public static AuthorityTypeEnum getEnumByText(String text) {
        return textMap.get(text);
    }

    @JsonCreator
    public static AuthorityTypeEnum getEnum(String value) {
        return valueMap.get(value);
    }
}
