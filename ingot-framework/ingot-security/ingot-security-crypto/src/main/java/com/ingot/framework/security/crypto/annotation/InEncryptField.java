package com.ingot.framework.security.crypto.annotation;

import java.lang.annotation.*;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ingot.framework.security.crypto.jackson.CryptoSerializer;
import com.ingot.framework.security.crypto.model.CryptoType;

/**
 * <p>字段级加密注解，在 Jackson 序列化时对标注字段进行加密。</p>
 *
 * <p>用于响应实体或嵌套对象中的敏感字段；HYBRID 模式下复用
 * {@link InCryptoHybridContext} 建立的 CEK/AAD，{@code secretKey} 对 HYBRID 无意义。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@JacksonAnnotationsInside
@JsonSerialize(using = CryptoSerializer.class)
public @interface InEncryptField {
    /**
     * 加解密类型
     */
    CryptoType value() default CryptoType.HYBRID;

    /**
     * 秘钥，如果没有配置则使用全局配置
     */
    String secretKey() default "";
}
