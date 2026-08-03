package com.ingot.framework.gateway.rule.client.blacklist.internal;

import com.ingot.cloud.security.api.model.vo.policy.SecurityPolicySnapshotVO;
import com.ingot.framework.cache.derived.VersionedDerivedCache;
import com.ingot.framework.gateway.rule.client.blacklist.BlacklistService;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpKeyType;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpListSnapshot;
import com.ingot.framework.gateway.rule.client.internal.RemoteSnapshotFetcher;
import com.ingot.framework.gateway.rule.client.internal.SnapshotAssembler;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>黑白名单服务 — remote 模式实现。</p>
 *
 * <p>激活条件：{@code ingot.security.blacklist.enabled=true} 且
 * {@code ingot.security.blacklist.policy.mode=remote}。</p>
 *
 * <p>快照来自四域共享的分层缓存，本类在其上按版本缓存编译后的 {@link CompiledIpList} 索引。
 * 该索引含 CIDR 前缀树等不可序列化结构，只能留在进程内，因此不参与 L2。</p>
 *
 * @author jy
 * @since 2026/5/26
 * @see VersionedDerivedCache
 */
@Slf4j
public class RemoteBlacklistService implements BlacklistService {

    private final RemoteSnapshotFetcher fetcher;
    private final VersionedDerivedCache<SecurityPolicySnapshotVO, Compiled> cache;

    public RemoteBlacklistService(RemoteSnapshotFetcher fetcher) {
        this.fetcher = fetcher;
        this.cache = new VersionedDerivedCache<>(fetcher::versionOf, vo -> {
            IpListSnapshot snap = SnapshotAssembler.toIpListSnapshot(vo);
            CompiledIpList compiled = CompiledIpList.compile(snap.getItems());
            log.info("[Blacklist] remote index compiled, size={} version={}",
                    compiled.size(), snap.getVersion());
            return new Compiled(snap, compiled);
        });
    }

    /** 委托编译索引做黑名单匹配。 */
    @Override
    public boolean isBlocked(String ip, String device, String userId, String clientId, String ua, String referer) {
        return resolve().compiled.isBlocked(ip, device, userId, clientId, ua, referer);
    }

    /** 委托编译索引做白名单匹配。 */
    @Override
    public boolean isWhitelisted(String ip, String device, String userId, String clientId, String ua, String referer) {
        return resolve().compiled.isWhitelisted(ip, device, userId, clientId, ua, referer);
    }

    /** 委托编译索引做精确键查询。 */
    @Override
    public boolean contains(IpKeyType keyType, String keyValue, boolean blacklist) {
        return resolve().compiled.contains(keyType, keyValue, blacklist);
    }

    /** 返回远端快照原始条目 + 版本号。 */
    @Override
    public IpListSnapshot getSnapshot() {
        return resolve().snapshot;
    }

    /** 清空共享快照与本域编译索引，下次查询穿透到远端。 */
    @Override
    public void evictAll() {
        fetcher.evictAll();
        cache.evictAll();
        log.debug("[Blacklist] remote index evicted");
    }

    private Compiled resolve() {
        return cache.get(fetcher.fetch());
    }

    /** 缓存条目：原始快照 + 编译索引。 */
    private record Compiled(IpListSnapshot snapshot, CompiledIpList compiled) {
    }
}
