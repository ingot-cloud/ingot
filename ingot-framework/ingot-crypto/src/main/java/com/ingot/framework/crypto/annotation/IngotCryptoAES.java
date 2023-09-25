package com.ingot.framework.crypto.annotation;

import com.ingot.framework.crypto.model.CryptoType;

import java.lang.annotation.*;

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
@IngotDecrypt(CryptoType.AES)
@IngotEncrypt(CryptoType.AES)
public @interface IngotCryptoAES {
}
