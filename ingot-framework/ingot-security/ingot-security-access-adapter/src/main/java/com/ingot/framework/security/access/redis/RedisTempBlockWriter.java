package com.ingot.framework.security.access.redis;

import com.ingot.framework.commons.constants.RedisKeyConstants;
import com.ingot.framework.security.access.service.TempBlockWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis 临时封禁写入器（与网关共用 key 前缀）。
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
public class RedisTempBlockWriter implements TempBlockWriter {

    private final StringRedisTemplate redisTemplate;

    public RedisTempBlockWriter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void block(String keyType, String keyValue, Duration ttl) {
        if (redisTemplate == null || keyType == null || keyValue == null) {
            return;
        }
        Duration effectiveTtl = ttl == null || ttl.isZero() || ttl.isNegative()
                ? Duration.ofSeconds(60)
                : ttl;
        try {
            redisTemplate.opsForValue().set(buildKey(keyType, keyValue), "login-failure", effectiveTtl);
        } catch (Exception e) {
            log.warn("[LoginFailure] temp block failed keyType={} value={}", keyType, keyValue, e);
        }
    }

    private static String buildKey(String keyType, String keyValue) {
        return RedisKeyConstants.Gateway.tempBlockKey(keyType, keyValue);
    }
}
