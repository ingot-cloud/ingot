package com.ingot.framework.security.oauth2.jwt;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.commons.utils.crypto.RSAUtil;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * <p>Description  : Redis实现.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/22.</p>
 * <p>Time         : 14:41.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class RedisJwkSupplier implements JwkSupplier {
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public JWKSet get() {
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
                KeyPair keyPair = RSAUtil.generateKey();
                keyID = UUID.randomUUID().toString();
                publicKey = (RSAPublicKey) keyPair.getPublic();
                privateKey = (RSAPrivateKey) keyPair.getPrivate();

                this.cache(keyPair, keyID);
            } else {
                publicKey = (RSAPublicKey) RSAUtil.getPublicKey(RSAUtil.toBytes(pubStr));
                privateKey = (RSAPrivateKey) RSAUtil.getPrivateKey(RSAUtil.toBytes(priStr));
            }

            log.info("[RedisJWKSupplier] get jwk, keyID={}", keyID);

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
                RSAUtil.toHexString(keyPair.getPrivate().getEncoded()));
        stringRedisTemplate.opsForValue().set(CacheConstants.Security.AUTHORIZATION_PUB,
                RSAUtil.toHexString(keyPair.getPublic().getEncoded()));
        stringRedisTemplate.opsForValue().set(CacheConstants.Security.AUTHORIZATION_KEY_ID,
                keyID);
    }
}
