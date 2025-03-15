package com.ingot.framework.crypto.annotation;

import com.ingot.framework.crypto.model.CryptoType;

import java.lang.annotation.*;

/**
 * <p>Description  : 解密注解.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 10:50 AM.</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface InDecrypt {

    /**
     * 加解密类型
     */
    CryptoType value();

    /**
     * 秘钥，如果没有配置则使用全局配置
     */
    String secretKey() default "";
}
