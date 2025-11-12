package com.ingot.cloud.pms.api.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description  : OrgPlanUnitEnum.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/11/11.</p>
 * <p>Time         : 15:18.</p>
 */
@Getter
@RequiredArgsConstructor
public enum OrgPlanUnitEnum {
    MONTH("1", "月"),
    YEAR("2", "年");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;

    private static final Map<String, OrgPlanUnitEnum> valueMap = new HashMap<>();
    private static final Map<String, OrgPlanUnitEnum> textMap = new HashMap<>();

    static {
        for (OrgPlanUnitEnum item : OrgPlanUnitEnum.values()) {
            valueMap.put(item.getValue(), item);
            textMap.put(item.getText(), item);
        }
    }

    public static OrgPlanUnitEnum getEnumByText(String text) {
        return textMap.get(text);
    }

    @JsonCreator
    public static OrgPlanUnitEnum getEnum(String value) {
        return valueMap.get(value);
    }
}
