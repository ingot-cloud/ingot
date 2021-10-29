package com.ingot.cloud.auth.service;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.constants.RedisConstants;
import com.ingot.framework.security.bus.jwt.JWKSetUpdateSender;
import com.ingot.framework.security.common.utils.RSAUtils;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * <p>Description  : JWKService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/28.</p>
 * <p>Time         : 5:16 下午.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JWKService implements CommandLineRunner {
    private final JWKSetUpdateSender jwkSetUpdateSender;
    private final StringRedisTemplate stringRedisTemplate;

    private final AtomicBoolean needUpdate = new AtomicBoolean(false);

    @SneakyThrows
    public JWKSet fetch() {
        RSAPublicKey publicKey;
        RSAPrivateKey privateKey;

        String priStr = stringRedisTemplate.opsForValue()
                .get(RedisConstants.Security.AUTHORIZATION_PRI);
        String pubStr = stringRedisTemplate.opsForValue()
                .get(RedisConstants.Security.AUTHORIZATION_PUB);
        String keyID = stringRedisTemplate.opsForValue()
                .get(RedisConstants.Security.AUTHORIZATION_KEY_ID);
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
    }

    @SneakyThrows
    public JWKSet forceRefresh() {
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
    }

    @Override
    public void run(String... args) throws Exception {
        if (this.needUpdate.getAndSet(false)) {
            log.info("[JWKService] run() -- UPDATE JWK EVENT --");
            jwkSetUpdateSender.exec();
        }
    }

    private void cache(KeyPair keyPair, String keyID) {
        stringRedisTemplate.opsForValue().set(RedisConstants.Security.AUTHORIZATION_PRI,
                RSAUtils.toHexString(keyPair.getPrivate().getEncoded()));
        stringRedisTemplate.opsForValue().set(RedisConstants.Security.AUTHORIZATION_PUB,
                RSAUtils.toHexString(keyPair.getPublic().getEncoded()));
        stringRedisTemplate.opsForValue().set(RedisConstants.Security.AUTHORIZATION_KEY_ID,
                keyID);
        this.needUpdate.set(true);
    }
}
