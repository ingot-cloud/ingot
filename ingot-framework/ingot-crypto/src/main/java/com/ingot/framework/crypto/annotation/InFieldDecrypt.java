package com.ingot.framework.crypto.annotation;

import java.lang.annotation.*;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ingot.framework.crypto.jackson.CryptoDeserializer;
import com.ingot.framework.crypto.model.CryptoType;

/**
 * <p>Description  : IngotFieldDecrypt.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 4:55 PM.</p>
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@JacksonAnnotationsInside
@JsonDeserialize(using = CryptoDeserializer.class)
public @interface InFieldDecrypt {
    /**
     * 加解密类型
     */
    CryptoType value();

    /**
     * 秘钥，如果没有配置则使用全局配置
     */
    String secretKey() default "";
}
