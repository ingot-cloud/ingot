package com.ingot.framework.security.crypto.annotation;

import java.lang.annotation.*;

import com.ingot.framework.security.crypto.model.CryptoType;

/**
 * <p>Description  : 加密注解.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 10:51 AM.</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface InEncrypt {
    /**
     * 加解密类型
     */
    CryptoType value() default CryptoType.HYBRID;

    /**
     * 秘钥，如果没有配置则使用全局配置
     */
    String secretKey() default "";
}
