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
import java.util.UUID;

/**
 * PassToken 存储与消费。
 *
 * <p>Key: {@code in:gw:vc:pass:{scope}:{token}}，value：剩余可消费次数（整数）。
 * 签发 = SET key remaining EX ttl；消费 = DECR + 当 &lt;=0 时删除（Lua 原子）。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@Component
public class PassTokenStore {

    private static final String KEY_PREFIX = "in:gw:vc:pass:";

    private static final RedisScript<Long> CONSUME_SCRIPT = RedisScript.of(
            "local v = redis.call('DECR', KEYS[1])\n" +
                    "if v <= 0 then redis.call('DEL', KEYS[1]) end\n" +
                    "return v", Long.class);

    private final ReactiveStringRedisTemplate redisTemplate;

    public PassTokenStore(ObjectProvider<ReactiveStringRedisTemplate> redisProvider) {
        this.redisTemplate = redisProvider.getIfAvailable();
        if (this.redisTemplate == null) {
            log.info("[PassTokenStore] reactive redis not available, PassToken disabled");
        }
    }

    public Mono<String> issue(String scope, int ttlSec, int remaining) {
        if (redisTemplate == null) return Mono.empty();
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = buildKey(scope, token);
        return redisTemplate.opsForValue()
                .set(key, String.valueOf(Math.max(1, remaining)), Duration.ofSeconds(Math.max(1, ttlSec)))
                .thenReturn(token);
    }

    /**
     * 消费一次：成功返回 true（包括剩余次数减 1 的情况）；token 不存在返回 false。
     */
    public Mono<Boolean> consume(String scope, String token) {
        if (redisTemplate == null || token == null) return Mono.just(false);
        String key = buildKey(scope, token);
        List<String> keys = Collections.singletonList(key);
        return redisTemplate.execute(CONSUME_SCRIPT, keys, Collections.emptyList())
                .next()
                .map(v -> (Long) v >= 0)
                .onErrorResume(e -> {
                    log.warn("[PassTokenStore] consume failed", e);
                    return Mono.just(false);
                });
    }

    private static String buildKey(String scope, String token) {
        return KEY_PREFIX + (scope == null ? "default" : scope) + ":" + token;
    }
}
