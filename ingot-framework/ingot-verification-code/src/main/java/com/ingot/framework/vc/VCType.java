package com.ingot.framework.vc;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : VerificationCodeType.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/3/28.</p>
 * <p>Time         : 11:49 PM.</p>
 */
@Getter
@RequiredArgsConstructor
public enum VCType {

    SMS("1", "短信验证码"),
    EMAIL("2", "邮箱验证码"),
    IMAGE("3", "图形验证码");

    @JsonValue
    private final String value;
    private final String text;

    private static final Map<String, VCType> valueMap = new HashMap<>();

    static {
        for (VCType item : VCType.values()) {
            valueMap.put(item.getValue(), item);
        }
    }

    @JsonCreator
    public static VCType getEnum(String value) {
        return valueMap.get(value);
    }
}
