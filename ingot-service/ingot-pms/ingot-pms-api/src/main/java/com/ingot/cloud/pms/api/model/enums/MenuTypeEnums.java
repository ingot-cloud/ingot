package com.ingot.cloud.pms.api.model.enums;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : MenuTypeEnums.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/24.</p>
 * <p>Time         : 2:55 PM.</p>
 */
@Getter
@RequiredArgsConstructor
public enum MenuTypeEnums {

    Directory("0", "目录"),
    Menu("1", "菜单"),
    Button("9", "按钮");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;

    private static final Map<String, MenuTypeEnums> valueMap = new HashMap<>();

    static {
        for (MenuTypeEnums item : MenuTypeEnums.values()) {
            valueMap.put(item.getValue(), item);
        }
    }

    @JsonCreator
    public static MenuTypeEnums getEnum(String value) {
        return valueMap.get(value);
    }
}
