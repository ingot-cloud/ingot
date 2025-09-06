package com.ingot.framework.commons.model.enums;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : UserStatusEnum.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/19.</p>
 * <p>Time         : 5:16 下午.</p>
 */
@Getter
@RequiredArgsConstructor
public enum UserStatusEnum {
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


    private static final Map<String, UserStatusEnum> valueMap = new HashMap<>();

    static {
        for (UserStatusEnum item : UserStatusEnum.values()) {
            valueMap.put(item.getValue(), item);
        }
    }

    @JsonCreator
    public static UserStatusEnum getEnum(String value) {
        return valueMap.get(value);
    }
}
