package com.ingot.cloud.auth.service;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.security.common.utils.RSAUtils;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Description  : JWKService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/28.</p>
 * <p>Time         : 5:16 下午.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JWKService {
    private final StringRedisTemplate stringRedisTemplate;

    private final AtomicBoolean needUpdate = new AtomicBoolean(false);

    public JWKSet fetch() {
        try {
            RSAPublicKey publicKey;
            RSAPrivateKey privateKey;

            String priStr = stringRedisTemplate.opsForValue()
                    .get(CacheConstants.Security.AUTHORIZATION_PRI);
            String pubStr = stringRedisTemplate.opsForValue()
                    .get(CacheConstants.Security.AUTHORIZATION_PUB);
            String keyID = stringRedisTemplate.opsForValue()
                    .get(CacheConstants.Security.AUTHORIZATION_KEY_ID);
            if (StrUtil.isEmpty(priStr) || StrUtil.isEmpty(pubStr) || StrUtil.isEmpty(keyID)) {
                KeyPair keyPair = RSAUtils.generateKey();
                keyID = UUID.randomUUID().toString();
                publicKey = (RSAPublicKey) keyPair.getPublic();
                privateKey = (RSAPrivateKey) keyPair.getPrivate();

                this.cache(keyPair, keyID);
            } else {
                publicKey = (RSAPublicKey) RSAUtils.getPublicKey(RSAUtils.toBytes(pubStr));
                privateKey = (RSAPrivateKey) RSAUtils.getPrivateKey(RSAUtils.toBytes(priStr));
            }

            log.info("[JWKService] fetch(), keyID={}", keyID);

            RSAKey rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(keyID)
                    .build();
            return new JWKSet(rsaKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JWKSet forceRefresh() {
        try {
            KeyPair keyPair = RSAUtils.generateKey();
            String keyID = UUID.randomUUID().toString();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

            this.cache(keyPair, keyID);

            RSAKey rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(keyID)
                    .build();
            return new JWKSet(rsaKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void cache(KeyPair keyPair, String keyID) {
        stringRedisTemplate.opsForValue().set(CacheConstants.Security.AUTHORIZATION_PRI,
                RSAUtils.toHexString(keyPair.getPrivate().getEncoded()));
        stringRedisTemplate.opsForValue().set(CacheConstants.Security.AUTHORIZATION_PUB,
                RSAUtils.toHexString(keyPair.getPublic().getEncoded()));
        stringRedisTemplate.opsForValue().set(CacheConstants.Security.AUTHORIZATION_KEY_ID,
                keyID);
        this.needUpdate.set(true);
    }
}
