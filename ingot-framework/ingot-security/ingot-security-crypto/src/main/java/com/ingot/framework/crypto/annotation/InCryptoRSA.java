package com.ingot.framework.crypto.annotation;

import java.lang.annotation.*;

import com.ingot.framework.crypto.model.CryptoType;

/**
 * <p>Description  : IngotCryptoRSA.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 3:03 PM.</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@InDecrypt(CryptoType.RSA)
@InEncrypt(CryptoType.RSA)
public @interface InCryptoRSA {
}
