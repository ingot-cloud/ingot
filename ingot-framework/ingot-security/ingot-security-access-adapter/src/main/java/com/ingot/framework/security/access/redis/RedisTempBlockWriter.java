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
        Duration effectiveTtl = effective(ttl);
        try {
            redisTemplate.opsForValue().set(buildKey(keyType, keyValue), "login-failure", effectiveTtl);
        } catch (Exception e) {
            log.warn("[LoginFailure] temp block failed keyType={} value={}", keyType, keyValue, e);
        }
    }

    @Override
    public boolean tryBlockFirst(String keyType, String keyValue, Duration ttl) {
        if (redisTemplate == null || keyType == null || keyValue == null) {
            return false;
        }
        Duration effectiveTtl = effective(ttl);
        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(buildKey(keyType, keyValue), "login-failure", effectiveTtl);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.warn("[LoginFailure] tryBlockFirst failed keyType={} value={}", keyType, keyValue, e);
            return false;
        }
    }

    @Override
    public boolean isBlocked(String keyType, String keyValue) {
        if (redisTemplate == null || keyType == null || keyValue == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(keyType, keyValue)));
        } catch (Exception e) {
            log.warn("[LoginFailure] isBlocked failed keyType={} value={}", keyType, keyValue, e);
            return false;
        }
    }

    @Override
    public void refreshTtl(String keyType, String keyValue, Duration ttl) {
        if (redisTemplate == null || keyType == null || keyValue == null) {
            return;
        }
        Duration effectiveTtl = effective(ttl);
        try {
            redisTemplate.expire(buildKey(keyType, keyValue), effectiveTtl);
        } catch (Exception e) {
            log.warn("[LoginFailure] refreshTtl failed keyType={} value={}", keyType, keyValue, e);
        }
    }

    private static Duration effective(Duration ttl) {
        return ttl == null || ttl.isZero() || ttl.isNegative()
                ? Duration.ofSeconds(60)
                : ttl;
    }

    private static String buildKey(String keyType, String keyValue) {
        return RedisKeyConstants.Gateway.tempBlockKey(keyType, keyValue);
    }
}
