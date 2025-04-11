package com.ingot.framework.data.mybatis.common.model;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description  : DataScopeTypeEnum.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/12.</p>
 * <p>Time         : 08:57.</p>
 */
@Getter
@RequiredArgsConstructor
public enum DataScopeTypeEnum {
    ALL(0, "全部数据权限"),
    CUSTOM(1, "自定义数据权限"),
    DEPT_AND_CHILD(2, "本部门及以下数据权限"),
    DEPT(3, "本部门数据权限"),
    SELF(9, "仅本人数据权限");

    @JsonValue
    @EnumValue
    private final int value;
    private final String text;

    private static final Map<Integer, DataScopeTypeEnum> valueMap = new HashMap<>();
    private static final Map<String, DataScopeTypeEnum> textMap = new HashMap<>();

    static {
        for (DataScopeTypeEnum item : DataScopeTypeEnum.values()) {
            valueMap.put(item.getValue(), item);
            textMap.put(item.getText(), item);
        }
    }

    public static DataScopeTypeEnum getEnumByText(String text) {
        return textMap.get(text);
    }

    @JsonCreator
    public static DataScopeTypeEnum getEnum(int value) {
        return valueMap.get(value);
    }
}
