package com.ingot.framework.commons.model.enums;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : CommonStatusEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/1.</p>
 * <p>Time         : 12:16 下午.</p>
 */
@Getter
@RequiredArgsConstructor
public enum CommonStatusEnum {
    ENABLE("0", "正常可用"),
    LOCK("9", "已锁定");

    /**
     * 状态
     */
    @JsonValue
    @EnumValue
    private final String value;

    /**
     * 描述
     */
    private final String text;

    private static final Map<String, CommonStatusEnum> valueMap = new HashMap<>();

    static {
        for (CommonStatusEnum item : CommonStatusEnum.values()) {
            valueMap.put(item.getValue(), item);
        }
    }

    @JsonCreator
    public static CommonStatusEnum getEnum(String value) {
        return valueMap.get(value);
    }
}
