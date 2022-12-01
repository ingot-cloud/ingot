package com.ingot.framework.core.utils.sensitive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;

/**
 * <p>Description  : Sensitive.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/1.</p>
 * <p>Time         : 5:38 PM.</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
public @interface Sensitive {

    /**
     * 模式，默认自定义
     */
    SensitiveMode mode() default SensitiveMode.CUSTOMER;

    /**
     * 前缀明文长度
     */
    int prefixPlaintextLength() default 0;

    /**
     * 后缀明文长度
     */
    int suffixPlaintextLength() default 0;

    /**
     * 遮罩字符
     */
    String maskCode() default "*";
}
