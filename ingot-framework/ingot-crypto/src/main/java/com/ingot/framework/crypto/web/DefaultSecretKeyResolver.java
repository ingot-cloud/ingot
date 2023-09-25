package com.ingot.framework.crypto.web;

import com.ingot.framework.crypto.IngotCryptoProperties;
import com.ingot.framework.crypto.model.CryptoType;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : DefaultSecretKeyResolver.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 2:36 PM.</p>
 */
@RequiredArgsConstructor
public class DefaultSecretKeyResolver implements SecretKeyResolver {
    private final IngotCryptoProperties properties;

    @Override
    public String get(CryptoType type) {
        switch (type) {
            case AES, RSA -> {
                return properties.getSecretKeys().get(type.getValue());
            }
        }
        return null;
    }
}
