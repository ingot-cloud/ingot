package com.ingot.cloud.acs.token;

import com.ingot.cloud.acs.properties.OAuth2Properties;
import com.ingot.framework.core.constants.RedisConstants;
import com.ingot.framework.security.core.bus.RefreshJwtKeySender;
import com.ingot.framework.security.utils.RSAUtils;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.util.Map;

/**
 * <p>Description  : JwtKeyGenerator.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-26.</p>
 * <p>Time         : 15:36.</p>
 */
@Slf4j
@Component
@AllArgsConstructor
public class JwtKeyGenerator implements CommandLineRunner {
    private final StringRedisTemplate stringRedisTemplate;
    private final RefreshJwtKeySender refreshJwtKeySender;
    private final OAuth2Properties oAuth2Properties;

    /**
     * 是否需要通知
     */
    private boolean needNotify;
    /**
     * 已经初始化
     */
    private boolean already;

    /**
     * 获取 {@link KeyPair}，如果缓存中不存在，那么生成新的 {@link KeyPair}
     * 并且发送需要更新 Jwt key 事件
     * @return {@link KeyPair}
     */
    @SneakyThrows
    public KeyPair getKeyPair() {
        byte[] pri, pub;
        try {
            pri = RSAUtils.toBytes(stringRedisTemplate.opsForValue().get(RedisConstants.REDIS_JWT_PRI_KEY));
            pub = RSAUtils.toBytes(stringRedisTemplate.opsForValue().get(RedisConstants.REDIS_JWT_PUB_KEY));
            this.needNotify = false;
        } catch (Exception e) {
            final String secret = oAuth2Properties.getRsaSecret();
            if (StringUtils.isEmpty(secret)) {
                log.warn(">>> JwtKeyGenerator - 请配置 ingot.oauth.rsaSecret !!!");
                throw new IllegalArgumentException("请配置 ingot.oauth.rsaSecret !!!");
            }
            Map<String, byte[]> keyMap = RSAUtils.generateKey(secret);
            pri = keyMap.get("pri");
            pub = keyMap.get("pub");
            stringRedisTemplate.opsForValue().set(RedisConstants.REDIS_JWT_PRI_KEY, RSAUtils.toHexString(pri));
            stringRedisTemplate.opsForValue().set(RedisConstants.REDIS_JWT_PUB_KEY, RSAUtils.toHexString(pub));
            // 缓存中更新的 key，需要发送刷新事件
            this.needNotify = true;
            if (this.already) {
                log.info(">>> JwtKeyGenerator - jwt key 已更新，发送刷新事件。");
                refreshJwtKeySender.send();
            }
        }

        return new KeyPair(RSAUtils.getPublicKey(pub), RSAUtils.getPrivateKey(pri));
    }

    @SneakyThrows
    @Override public void run(String... args) {
        this.already = true;
        if (needNotify){
            log.info(">>> JwtKeyGenerator - jwt key 已更新，发送刷新事件。");
            refreshJwtKeySender.send();
        }
    }
}
