package com.ingot.framework.security.crypto.web;

import com.ingot.framework.security.crypto.InCryptoProperties;
import com.ingot.framework.security.crypto.model.CryptoType;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : DefaultSecretKeyResolver.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 2:36 PM.</p>
 */
@RequiredArgsConstructor
public class DefaultSecretKeyResolver implements SecretKeyResolver {
    private final InCryptoProperties properties;

    @Override
    public String get(CryptoType type) {
        switch (type) {
            case AES, AES_GCM, RSA -> {
                return properties.getSecretKeys().get(type.getValue());
            }
        }
        return null;
    }
}
