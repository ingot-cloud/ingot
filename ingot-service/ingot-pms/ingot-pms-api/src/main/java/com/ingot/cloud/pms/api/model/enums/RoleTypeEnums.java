package com.ingot.cloud.pms.api.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description  : RoleTypeEnums.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/22.</p>
 * <p>Time         : 12:46 PM.</p>
 */
@Getter
@RequiredArgsConstructor
public enum RoleTypeEnums {

    System("0", "系统默认"),
    Tenant("1", "组织"),
    Custom("9", "自定义");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;

    private static final Map<String, RoleTypeEnums> valueMap = new HashMap<>();

    static {
        for (RoleTypeEnums item : RoleTypeEnums.values()) {
            valueMap.put(item.getValue(), item);
        }
    }

    @JsonCreator
    public static RoleTypeEnums getEnum(String value) {
        return valueMap.get(value);
    }
}
