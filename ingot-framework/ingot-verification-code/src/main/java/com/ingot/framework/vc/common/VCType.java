package com.ingot.framework.vc.common;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.module.servlet.VCProvider;
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

    SMS("sms", "短信验证码"),
    EMAIL("email", "邮箱验证码"),
    IMAGE("image", "图形验证码");

    @JsonValue
    private final String value;
    private final String text;

    /**
     * 获取Provider bean name
     *
     * @return bean name
     */
    public String getProviderBeanName() {
        return value + VCProvider.class.getSimpleName();
    }

    /**
     * 获取生成验证码类 Bean Name
     *
     * @return bean name
     */
    public String getGeneratorBeanName() {
        return value + VCGenerator.class.getSimpleName();
    }

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
