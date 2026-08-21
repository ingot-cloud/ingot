package com.ingot.framework.security.oauth2.server.authorization.session.concurrency;

import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.cloud.security.api.event.SecurityPolicyInvalidationEvent;
import com.ingot.framework.cache.coordinator.LayeredCacheCoordinator;
import com.ingot.framework.eventbus.InvalidationBus;

/**
 * <p>并发会话策略失效广播的订阅入口，把通用协调器固化到安全策略事件与域类型上。</p>
 *
 * <p>安全中心改策略后广播 {@link SecurityPolicyDomain#SESSION_CONCURRENCY}，Auth 各实例清空
 * 本地策略缓存，下一笔登录读到新值；收到 {@link SecurityPolicyDomain#ALL} 时同样清理。
 * 广播丢失由 L1 / L2 TTL 兜底，不会永久 stale。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see LayeredCacheCoordinator
 */
public class SessionConcurrencyCacheCoordinator
        extends LayeredCacheCoordinator<SecurityPolicyInvalidationEvent, SecurityPolicyDomain> {

    public SessionConcurrencyCacheCoordinator(InvalidationBus bus) {
        super(bus, SecurityPolicyInvalidationEvent.class,
                SecurityPolicyInvalidationEvent::getDomain, SecurityPolicyDomain.ALL);
    }
}
