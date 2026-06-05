package com.ingot.cloud.gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Redis 原子计数器：用于在限流命中后累加"违规次数"，并附带 TTL 滑动窗口。
 *
 * <p>Lua 保证 incr + expire 原子：</p>
 * <pre>
 *   local v = redis.call('INCR', KEYS[1])
 *   if v == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
 *   return v
 * </pre>
 *
 * <p>Key 规范：{@code in:gw:violation:{keyType}:{keyValue}:{ruleCode}}。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@Component
public class ViolationCounter {

    private static final String KEY_PREFIX = "in:gw:violation:";

    private static final RedisScript<Long> INCR_SCRIPT = RedisScript.of(
            "local v = redis.call('INCR', KEYS[1])\n" +
                    "if v == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end\n" +
                    "return v", Long.class);

    private final ReactiveStringRedisTemplate redisTemplate;

    public ViolationCounter(ObjectProvider<ReactiveStringRedisTemplate> redisProvider) {
        this.redisTemplate = redisProvider.getIfAvailable();
        if (this.redisTemplate == null) {
            log.info("[ViolationCounter] reactive redis not available, violation counting disabled");
        }
    }

    public Mono<Long> incr(String keyType, String keyValue, String ruleCode, Duration window) {
        if (redisTemplate == null || keyValue == null) {
            return Mono.just(0L);
        }
        String key = KEY_PREFIX + keyType + ":" + keyValue + ":" + ruleCode;
        List<String> keys = Collections.singletonList(key);
        long ttlMs = Math.max(1000L, window.toMillis());
        return redisTemplate.execute(INCR_SCRIPT, keys, Collections.singletonList(String.valueOf(ttlMs)))
                .next()
                .map(v -> (Long) v)
                .onErrorResume(e -> {
                    log.warn("[ViolationCounter] incr failed", e);
                    return Mono.just(0L);
                });
    }
}
