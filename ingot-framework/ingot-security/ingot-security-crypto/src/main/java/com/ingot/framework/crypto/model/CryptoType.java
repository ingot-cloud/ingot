package com.ingot.framework.crypto.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : CryptoType.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 10:43 AM.</p>
 */
@Getter
@RequiredArgsConstructor
public enum CryptoType {
    /**
     * AES默认CBC模式
     */
    AES("aes", "AES"),
    /**
     * AES, GCM模式
     */
    AES_GCM("aes_gcm", "AES_GCM"),
    /**
     * RSA
     */
    RSA("rsa", "RSA");

    private final String value;
    private final String text;
}
