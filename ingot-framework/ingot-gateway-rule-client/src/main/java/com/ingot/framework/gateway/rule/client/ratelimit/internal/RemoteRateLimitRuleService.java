package com.ingot.framework.gateway.rule.client.ratelimit.internal;

import com.ingot.framework.gateway.rule.client.internal.LocalCompiledCache;
import com.ingot.framework.gateway.rule.client.internal.RemoteSnapshotFetcher;
import com.ingot.framework.gateway.rule.client.internal.SnapshotAssembler;
import com.ingot.framework.gateway.rule.client.ratelimit.RateLimitRuleService;
import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 限流规则服务 — remote 模式实现。
 *
 * <p>激活条件：{@code ingot.security.ratelimit.enabled=true} 且
 * {@code ingot.security.ratelimit.policy.mode=remote}。</p>
 *
 * <p>通过 {@link RemoteSnapshotFetcher} 调 ingot-service-security 拉取全量快照，
 * 由 {@link SnapshotAssembler#toRateLimitSnapshot} 转换为 {@link RateLimitSnapshot}
 * 并缓存到 {@link LocalCompiledCache}。</p>
 *
 * <p>拉取失败时返回空快照（规则数为 0，不抛异常），网关侧退化为不限流。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteRateLimitRuleService implements RateLimitRuleService {

    private final RemoteSnapshotFetcher fetcher;
    private final LocalCompiledCache<RateLimitSnapshot> cache = new LocalCompiledCache<>();

    /** 获取限流快照；cache miss 时通过 Feign 拉取远端并转换。 */
    @Override
    public RateLimitSnapshot getSnapshot() {
        return cache.get(this::load);
    }

    /** 清空 L1 缓存，下次 getSnapshot 重新拉取远端。 */
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
