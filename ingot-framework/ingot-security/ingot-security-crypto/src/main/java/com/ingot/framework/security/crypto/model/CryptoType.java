package com.ingot.framework.security.crypto.model;

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
    RSA("rsa", "RSA"),
    /**
     * 信封加密（每请求临时 CEK + 服务端非对称密钥包裹），类 JWE
     */
    HYBRID("hybrid", "HYBRID");

    private final String value;
    private final String text;
}
