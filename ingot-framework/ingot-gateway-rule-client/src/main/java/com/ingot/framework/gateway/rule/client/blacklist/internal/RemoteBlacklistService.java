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
 * 黑白名单服务 — remote 模式实现。
 *
 * <p>激活条件：{@code ingot.security.blacklist.enabled=true} 且
 * {@code ingot.security.blacklist.policy.mode=remote}。</p>
 *
 * <p>通过 {@link RemoteSnapshotFetcher} 拉取全量快照，由
 * {@link SnapshotAssembler#toIpListSnapshot} 转换后编译为 {@link CompiledIpList}
 * 索引并缓存到 {@link LocalCompiledCache}。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteBlacklistService implements BlacklistService {

    private final RemoteSnapshotFetcher fetcher;
    private final LocalCompiledCache<Compiled> cache = new LocalCompiledCache<>();

    /** 委托编译索引做黑名单匹配；cache miss 时拉取远端并编译。 */
    @Override
    public boolean isBlocked(String ip, String device, String userId, String ua, String referer) {
        return resolve().compiled.isBlocked(ip, device, userId, ua, referer);
    }

    /** 委托编译索引做白名单匹配。 */
    @Override
    public boolean isWhitelisted(String ip, String device, String userId, String ua, String referer) {
        return resolve().compiled.isWhitelisted(ip, device, userId, ua, referer);
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

    /** 清空 L1 缓存，下次查询重新拉取远端并编译。 */
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

    /** 缓存条目：原始快照 + 编译索引。 */
    private record Compiled(IpListSnapshot snapshot, CompiledIpList compiled) {
    }
}
