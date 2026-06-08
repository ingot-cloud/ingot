package com.ingot.framework.gateway.rule.client.violation.internal;

import com.ingot.framework.gateway.rule.client.internal.LocalCompiledCache;
import com.ingot.framework.gateway.rule.client.internal.RemoteSnapshotFetcher;
import com.ingot.framework.gateway.rule.client.internal.SnapshotAssembler;
import com.ingot.framework.gateway.rule.client.violation.ViolationEscalationService;
import com.ingot.framework.gateway.rule.client.violation.model.ViolationEscalationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 违规升级配置 — remote 模式：从 Feign 全量快照解析并 L1 缓存。
 *
 * @author jy
 * @since 2026/6/5
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteViolationEscalationService implements ViolationEscalationService {

    private final RemoteSnapshotFetcher fetcher;
    private final LocalCompiledCache<ViolationEscalationConfig> cache = new LocalCompiledCache<>();

    @Override
    public ViolationEscalationConfig getConfig() {
        return cache.get(this::load);
    }

    @Override
    public void evictAll() {
        cache.evictAll();
        log.debug("[ViolationEscalation] remote config evicted");
    }

    private ViolationEscalationConfig load() {
        ViolationEscalationConfig config = SnapshotAssembler.toViolationEscalationConfig(fetcher.fetch());
        log.info("[ViolationEscalation] remote config loaded, windowSec={} threshold={} ttlSec={} enabled={} version={}",
                config.getWindowSec(), config.getBlockThreshold(), config.getTempBlockTtlSec(),
                config.isEnabled(), config.getVersion());
        return config;
    }
}
