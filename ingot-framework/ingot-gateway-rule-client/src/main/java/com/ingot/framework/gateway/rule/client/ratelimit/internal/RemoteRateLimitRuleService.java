package com.ingot.framework.gateway.rule.client.ratelimit.internal;

import com.ingot.framework.gateway.rule.client.internal.LocalCompiledCache;
import com.ingot.framework.gateway.rule.client.internal.RemoteSnapshotFetcher;
import com.ingot.framework.gateway.rule.client.internal.SnapshotAssembler;
import com.ingot.framework.gateway.rule.client.ratelimit.RateLimitRuleService;
import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * remote 模式：通过 {@link RemoteSnapshotFetcher} 调 ingot-service-security 拉规则快照，
 * 命中失败时返回空快照（不抛异常，由 SentinelGatewayFilter 退化为放行）。
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteRateLimitRuleService implements RateLimitRuleService {

    private final RemoteSnapshotFetcher fetcher;
    private final LocalCompiledCache<RateLimitSnapshot> cache = new LocalCompiledCache<>();

    @Override
    public RateLimitSnapshot getSnapshot() {
        return cache.get(this::load);
    }

    @Override
    public void evictAll() {
        cache.evictAll();
        log.debug("[RateLimit] remote snapshot evicted");
    }

    private RateLimitSnapshot load() {
        RateLimitSnapshot snapshot = SnapshotAssembler.toRateLimitSnapshot(fetcher.fetch());
        log.info("[RateLimit] remote snapshot loaded, rules={} version={}",
                snapshot.getRules().size(), snapshot.getVersion());
        return snapshot;
    }
}
