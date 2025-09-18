package com.ingot.cloud.pms.api.model.enums;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : OrgTypeEnums.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/11/22.</p>
 * <p>Time         : 10:23.</p>
 */
@Getter
@RequiredArgsConstructor
public enum OrgTypeEnum {
    System("0", "系统默认"),
    Tenant("1", "组织"),
    Custom("9", "自定义");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;

    private static final Map<String, OrgTypeEnum> valueMap = new HashMap<>();
    private static final Map<String, OrgTypeEnum> textMap = new HashMap<>();

    static {
        for (OrgTypeEnum item : OrgTypeEnum.values()) {
            valueMap.put(item.getValue(), item);
            textMap.put(item.getText(), item);
        }
    }

    public static OrgTypeEnum getEnumByText(String text) {
        return textMap.get(text);
    }

    @JsonCreator
    public static OrgTypeEnum getEnum(String value) {
        return valueMap.get(value);
    }
}
