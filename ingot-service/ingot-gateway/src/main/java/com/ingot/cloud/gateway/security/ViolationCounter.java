package com.ingot.cloud.gateway.security;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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
 * Redis 原子计数器：限流命中后累加违规次数，并附带 TTL 滑动窗口。
 *
 * <p>{@link SentinelBlockHandler} 在 Sentinel 抛出 BlockException 时调用 {@link #incr}；
 * 返回值达到 {@link com.ingot.framework.gateway.rule.client.violation.model.ViolationEscalationConfig#getBlockThreshold()}
 * 即触发 {@link TempBlockStore#block} 与 {@link BlacklistEventReporter} 审计上报。</p>
 *
 * <h3>Key 规范</h3>
 * <p>{@link GatewaySecurityConstants#REDIS_KEY_VIOLATION_PREFIX}{@code {keyType}:{keyValue}:{ruleCode}}</p>
 *
 * <h3>Lua 原子语义</h3>
 * <pre>
 *   local v = redis.call('INCR', KEYS[1])
 *   if v == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
 *   return v
 * </pre>
 * <p>窗口长度由 {@link com.ingot.framework.gateway.rule.client.violation.ViolationEscalationService}
 * 提供；TTL 下限见 {@link GatewaySecurityConstants#MIN_VIOLATION_WINDOW_MS}。</p>
 *
 * <h3>相关配置</h3>
 * <pre>{@code
 * ingot:
 *   security:
 *     ratelimit:
 *       enabled: true          # 限流开启后 SentinelBlockHandler 才会调用 incr
 *     violation-escalation:
 *       enabled: true
 *       policy:
 *         window-sec: 60
 * spring:
 *   data:
 *     redis:
 *       host: localhost        # 未配置时 incr 恒返回 0，不触发升级封禁
 * }</pre>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViolationCounter {

    private static final String KEY_PREFIX = GatewaySecurityConstants.REDIS_KEY_VIOLATION_PREFIX;

    private static final RedisScript<Long> INCR_SCRIPT = RedisScript.of(
            "local v = redis.call('INCR', KEYS[1])\n" +
                    "if v == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end\n" +
                    "return v", Long.class);

    private final ObjectProvider<ReactiveStringRedisTemplate> redisProvider;
    private ReactiveStringRedisTemplate redisTemplate;

    @PostConstruct
    void init() {
        redisTemplate = redisProvider.getIfAvailable();
        if (redisTemplate == null) {
            log.info("[ViolationCounter] reactive redis not available, violation counting disabled");
        }
    }

    public Mono<Long> incr(String keyType, String keyValue, String ruleCode, Duration window) {
        if (redisTemplate == null || keyValue == null) {
            return Mono.just(0L);
        }
        String key = KEY_PREFIX + keyType + ":" + keyValue + ":" + ruleCode;
        List<String> keys = Collections.singletonList(key);
        long ttlMs = Math.max(GatewaySecurityConstants.MIN_VIOLATION_WINDOW_MS, window.toMillis());
        return redisTemplate.execute(INCR_SCRIPT, keys, Collections.singletonList(String.valueOf(ttlMs)))
                .next()
                .map(v -> (Long) v)
                .onErrorResume(e -> {
                    log.warn("[ViolationCounter] incr failed", e);
                    return Mono.just(0L);
                });
    }
}
