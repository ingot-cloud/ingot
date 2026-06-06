package com.ingot.framework.gateway.rule.client.ratelimit.internal;

import com.ingot.framework.gateway.rule.client.internal.LocalCompiledCache;
import com.ingot.framework.gateway.rule.client.ratelimit.RateLimitRuleService;
import com.ingot.framework.gateway.rule.client.ratelimit.config.RateLimitProperties;
import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 限流规则服务 — local 模式实现。
 *
 * <p>激活条件：{@code ingot.security.ratelimit.enabled=true} 且
 * {@code ingot.security.ratelimit.policy.mode=local}（默认）。</p>
 *
 * <p>直接读取 {@link RateLimitProperties.Policy} 中的 yaml 配置，编译为
 * {@link RateLimitSnapshot} 并缓存到 {@link LocalCompiledCache}。
 * 版本号使用进程内自增计数，不依赖外部版本源。</p>
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

    /** 获取限流快照；cache miss 时从 yaml properties 编译。 */
    @Override
    public RateLimitSnapshot getSnapshot() {
        return cache.get(this::compile);
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
