package com.ingot.framework.gateway.rule.client.violation.internal;

import java.util.concurrent.atomic.AtomicLong;

import com.ingot.framework.gateway.rule.client.internal.LocalCompiledCache;
import com.ingot.framework.gateway.rule.client.violation.ViolationEscalationService;
import com.ingot.framework.gateway.rule.client.violation.config.ViolationEscalationProperties;
import com.ingot.framework.gateway.rule.client.violation.model.ViolationEscalationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 违规升级配置 — local 模式：从 {@link ViolationEscalationProperties} 编译并 L1 缓存。
 *
 * @author jy
 * @since 2026/6/5
 */
@Slf4j
@RequiredArgsConstructor
public class LocalViolationEscalationService implements ViolationEscalationService {

    private final ViolationEscalationProperties properties;
    private final LocalCompiledCache<ViolationEscalationConfig> cache = new LocalCompiledCache<>();
    private final AtomicLong version = new AtomicLong();

    @Override
    public ViolationEscalationConfig getConfig() {
        return cache.get(this::compile);
    }

    @Override
    public void evictAll() {
        cache.evictAll();
        log.debug("[ViolationEscalation] local config evicted");
    }

    private ViolationEscalationConfig compile() {
        ViolationEscalationProperties.Policy p = properties.getPolicy();
        ViolationEscalationConfig config = ViolationEscalationConfig.builder()
                .windowSec(Math.max(1, p.getWindowSec()))
                .blockThreshold(Math.max(1, p.getBlockThreshold()))
                .tempBlockTtlSec(Math.max(60, p.getTempBlockTtlSec()))
                .enabled(p.isEnabled())
                .version(version.incrementAndGet())
                .build();
        log.info("[ViolationEscalation] local config compiled, windowSec={} threshold={} ttlSec={} enabled={} version={}",
                config.getWindowSec(), config.getBlockThreshold(), config.getTempBlockTtlSec(),
                config.isEnabled(), config.getVersion());
        return config;
    }
}
