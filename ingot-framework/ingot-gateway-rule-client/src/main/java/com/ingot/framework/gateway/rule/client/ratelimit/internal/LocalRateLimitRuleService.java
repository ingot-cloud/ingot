package com.ingot.framework.gateway.rule.client.ratelimit.internal;

import com.ingot.framework.gateway.rule.client.internal.LocalCompiledCache;
import com.ingot.framework.gateway.rule.client.ratelimit.RateLimitRuleService;
import com.ingot.framework.gateway.rule.client.ratelimit.config.RateLimitProperties;
import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

/**
 * local 模式实现：直接读 {@link RateLimitProperties.Policy} 内 yaml 配置。
 *
 * <p>缓存编译结果到 {@link LocalCompiledCache}；{@link #evictAll()} 仅清缓存，下次重新从
 * properties 编译（典型场景：yaml 刷新或测试时显式调用）。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@RequiredArgsConstructor
public class LocalRateLimitRuleService implements RateLimitRuleService {

    private final RateLimitProperties properties;
    private final LocalCompiledCache<RateLimitSnapshot> cache = new LocalCompiledCache<>();
    private final AtomicLong version = new AtomicLong();

    @Override
    public RateLimitSnapshot getSnapshot() {
        return cache.get(this::compile);
    }

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
