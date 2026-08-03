package com.ingot.framework.gateway.rule.client.ratelimit.internal;

import java.util.concurrent.atomic.AtomicLong;

import com.ingot.framework.cache.derived.LazyDerivedCache;
import com.ingot.framework.gateway.rule.client.ratelimit.RateLimitRuleService;
import com.ingot.framework.gateway.rule.client.ratelimit.config.RateLimitProperties;
import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitSnapshot;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>限流规则服务 — local 模式实现。</p>
 *
 * <p>激活条件：{@code ingot.security.ratelimit.enabled=true} 且
 * {@code ingot.security.ratelimit.policy.mode=local}（默认）。</p>
 *
 * <p>直接读取 {@link RateLimitProperties.Policy} 中的 yaml 配置，编译为
 * {@link RateLimitSnapshot} 并缓存到 {@link LazyDerivedCache}。
 * 本地配置没有外部版本源，版本号用进程内自增计数表示「第几次编译」。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
public class LocalRateLimitRuleService implements RateLimitRuleService {

    private final RateLimitProperties properties;
    private final LazyDerivedCache<RateLimitSnapshot> cache;
    private final AtomicLong version = new AtomicLong();

    public LocalRateLimitRuleService(RateLimitProperties properties) {
        this.properties = properties;
        this.cache = new LazyDerivedCache<>(this::compile);
    }

    /** 获取限流快照；cache miss 时从 yaml properties 编译。 */
    @Override
    public RateLimitSnapshot getSnapshot() {
        return cache.get();
    }

    /** 清空 L1 缓存，下次 getSnapshot 重新编译。 */
    @Override
    public void evictAll() {
        cache.evictAll();
        log.debug("[RateLimit] local snapshot evicted");
    }

    private RateLimitSnapshot compile() {
        RateLimitProperties.Policy p = properties.getPolicy();
        RateLimitSnapshot snapshot = new RateLimitSnapshot(p.getRules(), p.getGroups(),
                version.incrementAndGet());
        log.info("[RateLimit] local snapshot compiled, rules={} groups={} version={}",
                snapshot.getRules().size(), snapshot.getGroups().size(), snapshot.getVersion());
        return snapshot;
    }
}
