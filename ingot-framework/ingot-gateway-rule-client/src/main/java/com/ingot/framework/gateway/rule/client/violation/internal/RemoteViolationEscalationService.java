package com.ingot.framework.gateway.rule.client.violation.internal;

import com.ingot.cloud.security.api.model.vo.policy.SecurityPolicySnapshotVO;
import com.ingot.framework.cache.derived.VersionedDerivedCache;
import com.ingot.framework.gateway.rule.client.internal.RemoteSnapshotFetcher;
import com.ingot.framework.gateway.rule.client.internal.SnapshotAssembler;
import com.ingot.framework.gateway.rule.client.violation.ViolationEscalationService;
import com.ingot.framework.gateway.rule.client.violation.model.ViolationEscalationConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>违规升级配置 — remote 模式：从四域共享快照解析并按版本缓存。</p>
 *
 * @author jy
 * @since 2026/6/5
 * @see VersionedDerivedCache
 */
@Slf4j
public class RemoteViolationEscalationService implements ViolationEscalationService {

    private final RemoteSnapshotFetcher fetcher;
    private final VersionedDerivedCache<SecurityPolicySnapshotVO, ViolationEscalationConfig> cache;

    public RemoteViolationEscalationService(RemoteSnapshotFetcher fetcher) {
        this.fetcher = fetcher;
        this.cache = new VersionedDerivedCache<>(fetcher::versionOf, vo -> {
            ViolationEscalationConfig config = SnapshotAssembler.toViolationEscalationConfig(vo);
            log.info("[ViolationEscalation] remote config loaded, windowSec={} threshold={} ttlSec={} "
                            + "enabled={} version={}",
                    config.getWindowSec(), config.getBlockThreshold(), config.getTempBlockTtlSec(),
                    config.isEnabled(), config.getVersion());
            return config;
        });
    }

    @Override
    public ViolationEscalationConfig getConfig() {
        return cache.get(fetcher.fetch());
    }

    @Override
    public void evictAll() {
        fetcher.evictAll();
        cache.evictAll();
        log.debug("[ViolationEscalation] remote config evicted");
    }
}
