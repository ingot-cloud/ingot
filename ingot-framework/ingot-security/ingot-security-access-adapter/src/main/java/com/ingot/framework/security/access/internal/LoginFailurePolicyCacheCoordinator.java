package com.ingot.framework.security.access.internal;

import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.cloud.security.api.event.SecurityPolicyInvalidationEvent;
import com.ingot.framework.cache.coordinator.LayeredCacheCoordinator;
import com.ingot.framework.eventbus.InvalidationBus;

/**
 * <p>登录失败策略失效广播的订阅入口，把通用协调器固化到本模块的事件与域类型上。</p>
 *
 * <p>只注册 {@link SecurityPolicyDomain#LOGIN_FAILURE_PROTECTION}；收到
 * {@link SecurityPolicyDomain#ALL} 时框架会回调全部已注册项，因此全量失效同样生效。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see LayeredCacheCoordinator
 * @apiNote 发布方收不到自身广播（bus 已按 origin 过滤回环），因此写侧必须自行清理本地缓存。
 */
public class LoginFailurePolicyCacheCoordinator
        extends LayeredCacheCoordinator<SecurityPolicyInvalidationEvent, SecurityPolicyDomain> {

    public LoginFailurePolicyCacheCoordinator(InvalidationBus bus) {
        super(bus, SecurityPolicyInvalidationEvent.class,
                SecurityPolicyInvalidationEvent::getDomain, SecurityPolicyDomain.ALL);
    }
}
