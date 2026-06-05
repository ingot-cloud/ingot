package com.ingot.cloud.gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 基于 Redis 的短期临时封禁存储。
 *
 * <p>Key 规范：{@code in:gw:bl:tmp:{keyType}:{keyValue}}，value 为触发的规则编码，
 * TTL 由触发方设定。</p>
 *
 * <p>{@link ReactiveStringRedisTemplate} 不可用（无 Redis 配置）时，本类所有方法
 * 返回 {@code Mono.just(false)} / {@code Mono.empty()}，相当于禁用临时封禁能力。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@Component
public class TempBlockStore {

    private static final String KEY_PREFIX = "in:gw:bl:tmp:";

    private final ReactiveStringRedisTemplate redisTemplate;

    public TempBlockStore(ObjectProvider<ReactiveStringRedisTemplate> redisProvider) {
        this.redisTemplate = redisProvider.getIfAvailable();
        if (this.redisTemplate == null) {
            log.info("[TempBlockStore] ReactiveStringRedisTemplate not available, temp-block disabled");
        }
    }

    public Mono<Boolean> isBlocked(String keyType, String keyValue) {
        if (redisTemplate == null || keyValue == null) return Mono.just(false);
        return redisTemplate.hasKey(buildKey(keyType, keyValue));
    }

    public Mono<Boolean> block(String keyType, String keyValue, String ruleCode, Duration ttl) {
        if (redisTemplate == null || keyValue == null) return Mono.just(false);
        return redisTemplate.opsForValue()
                .set(buildKey(keyType, keyValue), ruleCode == null ? "" : ruleCode, ttl);
    }

    public Mono<Long> unblock(String keyType, String keyValue) {
        if (redisTemplate == null || keyValue == null) return Mono.just(0L);
        return redisTemplate.delete(buildKey(keyType, keyValue));
    }

    private static String buildKey(String keyType, String keyValue) {
        return KEY_PREFIX + keyType + ":" + keyValue;
    }
}
