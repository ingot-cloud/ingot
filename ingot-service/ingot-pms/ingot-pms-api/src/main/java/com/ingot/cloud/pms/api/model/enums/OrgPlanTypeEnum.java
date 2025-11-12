package com.ingot.cloud.pms.api.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description  : OrgPlanTypeEnum.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/11.</p>
 * <p>Time         : 15:19.</p>
 */
@Getter
@RequiredArgsConstructor
public enum OrgPlanTypeEnum {
    LONG("0", "长期"),
    SHORT("1", "短期");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;

    private static final Map<String, OrgPlanTypeEnum> valueMap = new HashMap<>();
    private static final Map<String, OrgPlanTypeEnum> textMap = new HashMap<>();

    static {
        for (OrgPlanTypeEnum item : OrgPlanTypeEnum.values()) {
            valueMap.put(item.getValue(), item);
            textMap.put(item.getText(), item);
        }
    }

    public static OrgPlanTypeEnum getEnumByText(String text) {
        return textMap.get(text);
    }

    @JsonCreator
    public static OrgPlanTypeEnum getEnum(String value) {
        return valueMap.get(value);
    }
}
