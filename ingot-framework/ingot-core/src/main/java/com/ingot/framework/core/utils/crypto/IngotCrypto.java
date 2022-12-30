package com.ingot.framework.core.utils.crypto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * <p>Description  : IngotCrypto.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/12/30.</p>
 * <p>Time         : 5:13 PM.</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonDeserialize(using = CryptoDeserializer.class)
public @interface IngotCrypto {

    /**
     * 密码类型
     *
     * @return {@link CryptoType}
     */
    CryptoType type() default CryptoType.AES;

    /**
     * 加密key
     *
     * @return key
     */
    String key() default "";
}
