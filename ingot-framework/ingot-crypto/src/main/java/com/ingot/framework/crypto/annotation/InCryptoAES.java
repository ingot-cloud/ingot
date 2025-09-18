package com.ingot.framework.crypto.annotation;

import java.lang.annotation.*;

import com.ingot.framework.crypto.model.CryptoType;

/**
 * <p>Description  : IngotCryptoAES.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 3:02 PM.</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@InDecrypt(CryptoType.AES)
@InEncrypt(CryptoType.AES)
public @interface InCryptoAES {
}
