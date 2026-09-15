package com.ingot.cloud.iam.role;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangedSpringEvent;
import com.ingot.framework.cache.derived.SnapshotVersion;
import com.ingot.framework.cache.derived.VersionedDerivedCache;
import com.ingot.framework.cache.source.CacheSource;
import com.ingot.framework.commons.model.iam.ActionGrant;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * <p>把角色合成结果留在进程内派生缓存，失效键为来源加全部相关修订指纹，不把合成定义写入租户库。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
public class RoleSynthesisCache {
    private final ConcurrentHashMap<Long, VersionedDerivedCache<RoleRevisionSnapshot, List<ActionGrant>>> caches =
            new ConcurrentHashMap<>();

    /**
     * 返回合成授权；来源与指纹未变时复用编译结果。
     *
     * @param snapshot 基础与差异来源快照
     * @return 合成后的操作范围
     */
    public List<ActionGrant> grants(RoleRevisionSnapshot snapshot) {
        if (snapshot == null) {
            return List.of();
        }
        return caches.computeIfAbsent(snapshot.revisionId(), id -> new VersionedDerivedCache<>(
                source -> new SnapshotVersion(CacheSource.REMOTE, source.fingerprint()),
                source -> RoleSynthesis.synthesize(source.baseGrants(), source.deltas()).grants())).get(snapshot);
    }

    /**
     * 授权事实变化后丢弃全部合成产物。
     *
     * @param event 全量失效事件
     */
    @EventListener
    public void onAuthorizationChanged(AuthorizationChangedSpringEvent event) {
        if (event == null || !event.isAll()) {
            return;
        }
        caches.values().forEach(VersionedDerivedCache::evictAll);
        caches.clear();
    }
}
