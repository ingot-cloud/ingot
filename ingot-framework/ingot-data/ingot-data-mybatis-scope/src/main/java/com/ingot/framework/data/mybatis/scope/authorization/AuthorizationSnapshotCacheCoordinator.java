package com.ingot.framework.data.mybatis.scope.authorization;

import com.ingot.framework.cache.coordinator.LayeredCacheCoordinator;
import com.ingot.framework.eventbus.InvalidationBus;

/**
 * <p>订阅授权快照失效广播并清理本节点热缓存。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public class AuthorizationSnapshotCacheCoordinator
        extends LayeredCacheCoordinator<AuthorizationInvalidationEvent, String> {

    private static final String ALL_DOMAIN = "all";

    /**
     * @param bus    失效总线
     * @param access 本节点快照访问器
     */
    public AuthorizationSnapshotCacheCoordinator(InvalidationBus bus, AuthorizationSnapshotAccess access) {
        super(bus, AuthorizationInvalidationEvent.class,
                event -> event.isAll() ? ALL_DOMAIN : ALL_DOMAIN, ALL_DOMAIN);
        register(ALL_DOMAIN, access::evictAll);
    }
}
