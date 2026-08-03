package com.ingot.framework.gateway.rule.client.internal;

import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.cloud.security.api.event.SecurityPolicyInvalidationEvent;
import com.ingot.framework.cache.coordinator.LayeredCacheCoordinator;
import com.ingot.framework.eventbus.InvalidationBus;

/**
 * <p>安全策略失效广播的订阅入口，把通用协调器固化到本 SDK 的事件与域类型上。</p>
 *
 * <p>各域子模块在自己的 AutoConfiguration 中通过继承而来的
 * {@code register(SecurityPolicyDomain, Runnable)} 注册回调；同一域允许多个回调
 * （例如 SDK 缓存清理与 Sentinel 规则重载），按注册顺序串行执行，单个异常不影响其他回调。
 * 收到 {@link SecurityPolicyDomain#ALL} 时统一回调所有注册项。</p>
 *
 * <h3>装配条件</h3>
 * <ul>
 *     <li>{@code ingot.security.policy.client.invalidation-enabled=true}（默认）</li>
 *     <li>容器中存在 {@link InvalidationBus} Bean</li>
 * </ul>
 *
 * @author jy
 * @since 2026/5/26
 * @see LayeredCacheCoordinator
 * @apiNote 发布方收不到自身广播（bus 已按 origin 过滤回环），因此写侧必须自行清理本地缓存。
 */
public class SecurityPolicyCacheCoordinator
        extends LayeredCacheCoordinator<SecurityPolicyInvalidationEvent, SecurityPolicyDomain> {

    public SecurityPolicyCacheCoordinator(InvalidationBus bus) {
        super(bus, SecurityPolicyInvalidationEvent.class,
                SecurityPolicyInvalidationEvent::getDomain, SecurityPolicyDomain.ALL);
    }
}
