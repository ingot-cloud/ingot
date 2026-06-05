package com.ingot.framework.gateway.rule.client.blacklist.internal;

import com.ingot.framework.gateway.rule.client.blacklist.BlacklistService;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpKeyType;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpListSnapshot;
import com.ingot.framework.gateway.rule.client.internal.LocalCompiledCache;
import com.ingot.framework.gateway.rule.client.internal.RemoteSnapshotFetcher;
import com.ingot.framework.gateway.rule.client.internal.SnapshotAssembler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * remote 模式黑白名单：通过 RemoteSnapshotFetcher 拉全量；编译索引到本地 L1。
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteBlacklistService implements BlacklistService {

    private final RemoteSnapshotFetcher fetcher;
    private final LocalCompiledCache<Compiled> cache = new LocalCompiledCache<>();

    @Override
    public boolean isBlocked(String ip, String device, String userId, String ua, String referer) {
        return resolve().compiled.isBlocked(ip, device, userId, ua, referer);
    }

    @Override
    public boolean isWhitelisted(String ip, String device, String userId, String ua, String referer) {
        return resolve().compiled.isWhitelisted(ip, device, userId, ua, referer);
    }

    @Override
    public boolean contains(IpKeyType keyType, String keyValue, boolean blacklist) {
        return resolve().compiled.contains(keyType, keyValue, blacklist);
    }

    @Override
    public IpListSnapshot getSnapshot() {
        return resolve().snapshot;
    }

    @Override
    public void evictAll() {
        cache.evictAll();
        log.debug("[Blacklist] remote index evicted");
    }

    private Compiled resolve() {
        return cache.get(() -> {
            IpListSnapshot snap = SnapshotAssembler.toIpListSnapshot(fetcher.fetch());
            CompiledIpList compiled = CompiledIpList.compile(snap.getItems());
            log.info("[Blacklist] remote index compiled, size={} version={}",
                    compiled.size(), snap.getVersion());
            return new Compiled(snap, compiled);
        });
    }

    private record Compiled(IpListSnapshot snapshot, CompiledIpList compiled) {
    }
}
