package com.ingot.cloud.pms.api.model.enums;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.core.convert.EnumDeserializeMethod;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : DictScopeEnum.字典作用域</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/4/25.</p>
 */
@Getter
@RequiredArgsConstructor
public enum DictScopeEnum {
    /**
     * 平台级字典：跨租户共享，由平台管理员维护
     */
    PLATFORM("0", "平台"),
    /**
     * 租户级字典：仅当前租户可见，可覆盖同 code 的平台字典项
     */
    TENANT("1", "租户"),
    /**
     * 应用级字典：仅当前应用作用域可见，可覆盖同 code 的平台字典项
     */
    APP("2", "应用");

    @JsonValue
    @EnumValue
    private final String value;
    private final String text;

    private static final Map<String, DictScopeEnum> valueMap = new HashMap<>();

    static {
        for (DictScopeEnum item : DictScopeEnum.values()) {
            valueMap.put(item.getValue(), item);
        }
    }

    @EnumDeserializeMethod
    @JsonCreator
    public static DictScopeEnum getEnum(String value) {
        return valueMap.get(value);
    }
}
