package com.ingot.framework.gateway.rule.client.ratelimit.internal;

import com.ingot.cloud.security.api.model.vo.policy.SecurityPolicySnapshotVO;
import com.ingot.framework.cache.derived.VersionedDerivedCache;
import com.ingot.framework.gateway.rule.client.internal.RemoteSnapshotFetcher;
import com.ingot.framework.gateway.rule.client.internal.SnapshotAssembler;
import com.ingot.framework.gateway.rule.client.ratelimit.RateLimitRuleService;
import com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitSnapshot;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>限流规则服务 — remote 模式实现。</p>
 *
 * <p>激活条件：{@code ingot.security.ratelimit.enabled=true} 且
 * {@code ingot.security.ratelimit.policy.mode=remote}。</p>
 *
 * <p>快照本身由 {@link RemoteSnapshotFetcher} 背后的四域共享分层缓存提供，本类只在其上做
 * 版本驱动的派生缓存：快照版本未变时直接复用上次转换结果，避免上游 TTL 刷新引发无谓重建
 * 与 Sentinel 规则抖动。</p>
 *
 * @author jy
 * @since 2026/5/26
 * @see VersionedDerivedCache
 */
@Slf4j
public class RemoteRateLimitRuleService implements RateLimitRuleService {

    private final RemoteSnapshotFetcher fetcher;
    private final VersionedDerivedCache<SecurityPolicySnapshotVO, RateLimitSnapshot> cache;

    public RemoteRateLimitRuleService(RemoteSnapshotFetcher fetcher) {
        this.fetcher = fetcher;
        this.cache = new VersionedDerivedCache<>(fetcher::versionOf, vo -> {
            RateLimitSnapshot snapshot = SnapshotAssembler.toRateLimitSnapshot(vo);
            log.info("[RateLimit] remote snapshot compiled, rules={} version={}",
                    snapshot.getRules().size(), snapshot.getVersion());
            return snapshot;
        });
    }

    /** 获取限流快照；共享快照版本变化时重新转换。 */
    @Override
    public RateLimitSnapshot getSnapshot() {
        return cache.get(fetcher.fetch());
    }

    /**
     * 清空共享快照与本域派生缓存，下次 {@link #getSnapshot()} 穿透到远端。
     *
     * <p>共享层也一并清理，使本方法不依赖失效回调的注册顺序：
     * {@code SentinelGatewayConfiguration#reloadRules} 在事件回调中先调本方法再立即读取，
     * 若只清派生层就会重新编译出同一份旧快照。</p>
     */
    @Override
    public void evictAll() {
        fetcher.evictAll();
        cache.evictAll();
        log.debug("[RateLimit] remote snapshot evicted");
    }
}
