package com.ingot.cloud.pms.api.model.enums;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : DictTypeEnum.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/12.</p>
 * <p>Time         : 10:59.</p>
 */
@Getter
@RequiredArgsConstructor
public enum DictTypeEnum {
    TYPE("0", "字典类型"),
    ITEM("1", "字典项");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;

    private static final Map<String, DictTypeEnum> valueMap = new HashMap<>();
    private static final Map<String, DictTypeEnum> textMap = new HashMap<>();

    static {
        for (DictTypeEnum item : DictTypeEnum.values()) {
            valueMap.put(item.getValue(), item);
            textMap.put(item.getText(), item);
        }
    }

    public static DictTypeEnum getEnumByText(String text) {
        return textMap.get(text);
    }

    @JsonCreator
    public static DictTypeEnum getEnum(String value) {
        return valueMap.get(value);
    }
}
