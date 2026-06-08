package com.ingot.cloud.gateway.security;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 基于 Redis 的短期临时封禁存储。
 *
 * <p>当限流违规次数在滑动窗口内达到
 * {@link com.ingot.framework.gateway.rule.client.violation.model.ViolationEscalationConfig#getBlockThreshold()} 时，
 * {@link SentinelBlockHandler} 调用 {@link #block} 写入临时封禁；{@link BlacklistFilter} 在静态名单
 * 未命中时通过 {@link #isBlocked} 读取并返回 403。</p>
 *
 * <h3>Key 规范</h3>
 * <p>{@link GatewaySecurityConstants#REDIS_KEY_TEMP_BLOCK_PREFIX}{@code {keyType}:{keyValue}}，
 * value 为触发的规则编码（如 {@link GatewaySecurityConstants#RULE_CODE_RATE_LIMIT}），
 * TTL 由 {@link com.ingot.framework.gateway.rule.client.violation.model.ViolationEscalationConfig#getTempBlockTtlSec()} 决定。</p>
 *
 * <h3>相关配置</h3>
 * <pre>{@code
 * ingot:
 *   security:
 *     violation-escalation:
 *       enabled: true
 *       policy:
 *         temp-block-ttl-sec: 900
 *     blacklist:
 *       enabled: true          # BlacklistFilter 才会查询本 Store
 * spring:
 *   data:
 *     redis:
 *       host: localhost        # 未配置时本类静默降级，临时封禁不可用
 * }</pre>
 *
 * <p>{@link ReactiveStringRedisTemplate} 不可用时，所有方法返回 {@code false} / {@code empty()}，
 * 主链路不受影响。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TempBlockStore {

    private static final String KEY_PREFIX = GatewaySecurityConstants.REDIS_KEY_TEMP_BLOCK_PREFIX;

    private final ObjectProvider<ReactiveStringRedisTemplate> redisProvider;
    private ReactiveStringRedisTemplate redisTemplate;

    @PostConstruct
    void init() {
        redisTemplate = redisProvider.getIfAvailable();
        if (redisTemplate == null) {
            log.info("[TempBlockStore] ReactiveStringRedisTemplate not available, temp-block disabled");
        }
    }

    public Mono<Boolean> isBlocked(String keyType, String keyValue) {
        if (redisTemplate == null || keyValue == null) {
            return Mono.just(false);
        }
        return redisTemplate.hasKey(buildKey(keyType, keyValue));
    }

    public Mono<Boolean> block(String keyType, String keyValue, String ruleCode, Duration ttl) {
        if (redisTemplate == null || keyValue == null) {
            return Mono.just(false);
        }
        return redisTemplate.opsForValue()
                .set(buildKey(keyType, keyValue), ruleCode == null ? "" : ruleCode, ttl);
    }

    public Mono<Long> unblock(String keyType, String keyValue) {
        if (redisTemplate == null || keyValue == null) {
            return Mono.just(0L);
        }
        return redisTemplate.delete(buildKey(keyType, keyValue));
    }

    private static String buildKey(String keyType, String keyValue) {
        return KEY_PREFIX + keyType + ":" + keyValue;
    }
}
