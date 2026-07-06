package com.ingot.framework.crypto.annotation;

import java.lang.annotation.*;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ingot.framework.crypto.jackson.CryptoSerializer;
import com.ingot.framework.crypto.model.CryptoType;

/**
 * <p>Description  : InFieldEncrypt.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/8/23.</p>
 * <p>Time         : 11:22.</p>
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@JacksonAnnotationsInside
@JsonSerialize(using = CryptoSerializer.class)
public @interface InFieldEncrypt {
    /**
     * 加解密类型
     */
    CryptoType value();

    /**
     * 秘钥，如果没有配置则使用全局配置
     */
    String secretKey() default "";
}
