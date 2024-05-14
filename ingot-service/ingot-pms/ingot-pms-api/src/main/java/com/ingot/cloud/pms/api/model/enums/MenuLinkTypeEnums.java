package com.ingot.cloud.pms.api.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description  : MenuLinkTypeEnums.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/5/13.</p>
 * <p>Time         : 17:30.</p>
 */
@Getter
@RequiredArgsConstructor
public enum MenuLinkTypeEnums {
    Default("0", "正常链接"),
    IFrame("1", "内嵌链接"),
    External("2", "外部链接");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;

    private static final Map<String, MenuLinkTypeEnums> valueMap = new HashMap<>();
    private static final Map<String, MenuLinkTypeEnums> textMap = new HashMap<>();

    static {
        for (MenuLinkTypeEnums item : MenuLinkTypeEnums.values()) {
            valueMap.put(item.getValue(), item);
            textMap.put(item.getText(), item);
        }
    }

    public static MenuLinkTypeEnums getEnumByText(String text) {
        return textMap.get(text);
    }

    @JsonCreator
    public static MenuLinkTypeEnums getEnum(String value) {
        return valueMap.get(value);
    }
}
