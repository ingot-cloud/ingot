package com.ingot.framework.security.replay.store;

import java.time.Duration;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * <p>基于 Redis 的 {@link NonceStore} 默认实现。</p>
 *
 * <p>利用 {@code SET key value NX PX ttl}（{@code setIfAbsent}）的原子性保证同一 nonce
 * 仅首次占用成功，从而在分布式环境下实现请求防重放。</p>
 *
 * @author jy
 * @since 1.0.0
 * @implNote 依赖 {@link org.springframework.data.redis.core.StringRedisTemplate}，需存在可用的 Redis 连接。
 */
@RequiredArgsConstructor
public class RedisNonceStore implements NonceStore {
    private static final String VALUE = "1";

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean tryAcquire(String key, Duration ttl) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, VALUE, ttl);
        return Boolean.TRUE.equals(result);
    }
}
