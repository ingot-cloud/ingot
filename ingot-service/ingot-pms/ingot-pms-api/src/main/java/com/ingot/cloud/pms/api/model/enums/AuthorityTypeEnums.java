package com.ingot.cloud.pms.api.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Description  : AuthorityTypeEnums.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/21.</p>
 * <p>Time         : 11:35 AM.</p>
 */
@Getter
@RequiredArgsConstructor
public enum AuthorityTypeEnums {
    System("0", "系统默认"),
    Tenant("1", "组织");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;

    private static final Map<String, AuthorityTypeEnums> valueMap = new HashMap<>();
    private static final Map<String, AuthorityTypeEnums> textMap = new HashMap<>();

    static {
        for (AuthorityTypeEnums item : AuthorityTypeEnums.values()) {
            valueMap.put(item.getValue(), item);
            textMap.put(item.getText(), item);
        }
    }

    public static AuthorityTypeEnums getEnumByText(String text) {
        return textMap.get(text);
    }

    @JsonCreator
    public static AuthorityTypeEnums getEnum(String value) {
        return valueMap.get(value);
    }
}
