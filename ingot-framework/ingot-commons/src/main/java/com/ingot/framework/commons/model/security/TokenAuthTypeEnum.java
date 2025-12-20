package com.ingot.framework.commons.model.security;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : TokenAuthType.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/22.</p>
 * <p>Time         : 10:15 上午.</p>
 */
@Getter
@RequiredArgsConstructor
public enum TokenAuthTypeEnum {
    /**
     * 登录类型，默认标准类型，登录不互踢
     */
    STANDARD("0"),
    /**
     * 登录类型，唯一类型，当前账号只能在一个地方登录
     */
    UNIQUE("1");

    private final String value;

    private static final Map<String, TokenAuthTypeEnum> valueMap = new HashMap<>();

    static {
        for (TokenAuthTypeEnum item : TokenAuthTypeEnum.values()) {
            valueMap.put(item.getValue(), item);
        }
    }

    public static TokenAuthTypeEnum getEnum(String value) {
        return valueMap.get(value);
    }
}
