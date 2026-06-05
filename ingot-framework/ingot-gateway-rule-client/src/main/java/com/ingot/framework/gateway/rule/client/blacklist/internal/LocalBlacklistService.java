package com.ingot.framework.gateway.rule.client.blacklist.internal;

import com.ingot.framework.gateway.rule.client.blacklist.BlacklistService;
import com.ingot.framework.gateway.rule.client.blacklist.config.BlacklistProperties;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpKeyType;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpListSnapshot;
import com.ingot.framework.gateway.rule.client.internal.LocalCompiledCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

/**
 * local 模式黑白名单：从 {@link BlacklistProperties.Policy} 内 yaml 配置编译索引。
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@RequiredArgsConstructor
public class LocalBlacklistService implements BlacklistService {

    private final BlacklistProperties properties;
    private final LocalCompiledCache<CompiledIpList> cache = new LocalCompiledCache<>();
    private final AtomicLong version = new AtomicLong();

    @Override
    public boolean isBlocked(String ip, String device, String userId, String ua, String referer) {
        return resolve().isBlocked(ip, device, userId, ua, referer);
    }

    @Override
    public boolean isWhitelisted(String ip, String device, String userId, String ua, String referer) {
        return resolve().isWhitelisted(ip, device, userId, ua, referer);
    }

    @Override
    public boolean contains(IpKeyType keyType, String keyValue, boolean blacklist) {
        return resolve().contains(keyType, keyValue, blacklist);
    }

    @Override
    public IpListSnapshot getSnapshot() {
        return new IpListSnapshot(properties.getPolicy().getItems(), version.get());
    }

    @Override
    public void evictAll() {
        cache.evictAll();
        log.debug("[Blacklist] local index evicted");
    }

    private CompiledIpList resolve() {
        return cache.get(() -> {
            CompiledIpList compiled = CompiledIpList.compile(properties.getPolicy().getItems());
            long v = version.incrementAndGet();
            log.info("[Blacklist] local index compiled, size={} version={}", compiled.size(), v);
            return compiled;
        });
    }
}
