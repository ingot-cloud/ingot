package com.ingot.framework.gateway.rule.client.internal;

import com.ingot.cloud.security.api.model.vo.policy.SecurityPolicySnapshotVO;
import com.ingot.framework.cache.derived.SnapshotVersion;
import com.ingot.framework.cache.source.CacheSourceHolder;
import com.ingot.framework.cache.spi.LayeredCache;
import lombok.RequiredArgsConstructor;

/**
 * <p>四个策略域共享的全量快照入口，背后是一条完整的 {@code L1 → L2 → Resilient} 分层缓存链。</p>
 *
 * <p>共享是关键：限流、黑白名单、挑战、违规升级读的是同一份 {@link SecurityPolicySnapshotVO}，
 * 缓存放在这里而非各域内部，冷启动与全量失效后只会产生<b>一次</b>远端调用，
 * 而不是四个域各打一次。</p>
 *
 * <p>各域在本层之上再用 {@link com.ingot.framework.cache.derived.VersionedDerivedCache}
 * 缓存自己的编译产物，通过 {@link #versionOf} 判定是否需要重新编译。</p>
 *
 * @author jy
 * @since 2026/5/26
 * @see #versionOf(SecurityPolicySnapshotVO)
 */
@RequiredArgsConstructor
public class RemoteSnapshotFetcher {

    /**
     * 全量快照的缓存键；单 key 场景，取值本身无语义。
     */
    public static final String CACHE_KEY = "snapshot";

    private final LayeredCache<String, SecurityPolicySnapshotVO> cache;
    private final CacheSourceHolder sourceHolder;

    /**
     * 拉取全量安全策略快照，按需穿透 L1、L2 与降级阶梯。
     *
     * @return 快照；远端不可用且无任何兜底时抛 {@link PolicyRemoteUnavailableException}
     */
    public SecurityPolicySnapshotVO fetch() {
        return cache.get(CACHE_KEY);
    }

    /**
     * 计算快照的派生缓存失效键。
     *
     * <p>来源取自共享的 {@link CacheSourceHolder}，反映最近一次穿透到底层时的降级位置；
     * L1/L2 命中时来源不变，与命中的快照内容始终对应。</p>
     *
     * @param vo 快照
     * @return 由来源与版本号构成的二元组键
     */
    public SnapshotVersion versionOf(SecurityPolicySnapshotVO vo) {
        return new SnapshotVersion(sourceHolder.current(), vo == null ? 0L : vo.getVersion());
    }

    /**
     * 清空共享快照的 L1 与 L2，下次拉取穿透到远端；不影响 LKG。
     */
    public void evictAll() {
        cache.evictAll();
    }
}
