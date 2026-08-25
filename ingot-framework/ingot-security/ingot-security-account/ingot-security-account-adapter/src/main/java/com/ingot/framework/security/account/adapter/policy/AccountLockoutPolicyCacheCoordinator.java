package com.ingot.framework.security.account.adapter.policy;

import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.cloud.security.api.event.SecurityPolicyInvalidationEvent;
import com.ingot.framework.cache.coordinator.LayeredCacheCoordinator;
import com.ingot.framework.eventbus.InvalidationBus;

/**
 * <p>账号锁定策略失效广播的订阅入口。</p>
 *
 * <p>只注册 {@link SecurityPolicyDomain#ACCOUNT_LOCKOUT}；收到 {@link SecurityPolicyDomain#ALL}
 * 时框架会回调全部已注册项。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote 发布方收不到自身广播（bus 已按 origin 过滤回环），因此写侧必须自行清理本地缓存。
 */
public class AccountLockoutPolicyCacheCoordinator
        extends LayeredCacheCoordinator<SecurityPolicyInvalidationEvent, SecurityPolicyDomain> {

    /**
     * @param bus 跨节点失效总线
     */
    public AccountLockoutPolicyCacheCoordinator(InvalidationBus bus) {
        super(bus, SecurityPolicyInvalidationEvent.class,
                SecurityPolicyInvalidationEvent::getDomain, SecurityPolicyDomain.ALL);
    }
}
