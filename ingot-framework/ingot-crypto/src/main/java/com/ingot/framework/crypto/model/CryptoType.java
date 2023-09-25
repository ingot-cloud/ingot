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
    AES("aes", "AES"),
    RSA("ras", "RSA");

    private final String value;
    private final String text;
}
