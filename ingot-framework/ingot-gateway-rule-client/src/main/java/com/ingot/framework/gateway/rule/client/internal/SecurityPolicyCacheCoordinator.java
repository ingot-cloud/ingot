package com.ingot.framework.gateway.rule.client.internal;

import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.cloud.security.api.event.SecurityPolicyInvalidationEvent;
import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.Subscription;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 安全策略缓存协调器：订阅 {@link SecurityPolicyInvalidationEvent}，
 * 按事件携带的 {@link SecurityPolicyDomain} 分发到对应域的 evictor。
 *
 * <p>各域子模块（ratelimit / blacklist / challenge / vc）在自己的
 * AutoConfiguration 中通过 {@link #register(SecurityPolicyDomain, Runnable)} 注册回调；
 * 同一域允许 <b>多个回调</b>（例如 SDK 缓存 evict + Sentinel 规则热加载），按
 * 注册顺序串行执行，单个回调异常不影响其他回调。</p>
 *
 * <p>收到 {@link SecurityPolicyDomain#ALL} 时统一回调所有注册项。</p>
 *
 * <p>本协调器不区分 origin 自身：{@code RedisInvalidationBus} 已按 origin 过滤回环。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@RequiredArgsConstructor
public class SecurityPolicyCacheCoordinator {

    private final InvalidationBus bus;
    /**
     * 同一域可以注册多个 evictor（例如 ratelimit 域同时存在
     * {@code RateLimitRuleService::evictAll} 与 {@code SentinelGatewayConfiguration::reloadRules}）。
     * 使用 {@link ArrayList} 保证按注册顺序串行执行。
     */
    private final Map<SecurityPolicyDomain, List<Runnable>> evictors = new EnumMap<>(SecurityPolicyDomain.class);

    private Subscription subscription;

    /**
     * 子域追加注册自己的 evict 回调。
     *
     * <p>同一域多次调用本方法会按调用顺序追加；后注册者不会覆盖先注册者。
     * 典型场景：ratelimit 域先注册 {@code service::evictAll}，再注册
     * Sentinel 的 {@code reloadRules}；事件到达时按顺序串行执行。</p>
     */
    public synchronized void register(SecurityPolicyDomain domain, Runnable evictor) {
        evictors.computeIfAbsent(domain, d -> new ArrayList<>()).add(evictor);
        log.info("[SecurityPolicy] evictor registered, domain={}, total={}",
                domain, evictors.get(domain).size());
    }

    @PostConstruct
    public void start() {
        this.subscription = bus.subscribe(SecurityPolicyInvalidationEvent.class, this::handle);
        log.info("[SecurityPolicy] cache coordinator subscribed");
    }

    @PreDestroy
    public void stop() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    void handle(SecurityPolicyInvalidationEvent event) {
        SecurityPolicyDomain domain = event.getDomain();
        log.info("[SecurityPolicy] cache invalidate, domain={} origin={}", domain, event.getOrigin());
        if (domain == null || domain == SecurityPolicyDomain.ALL) {
            evictors.forEach(this::runAll);
            return;
        }
        List<Runnable> list = evictors.get(domain);
        if (list != null && !list.isEmpty()) {
            runAll(domain, list);
        }
    }

    private void runAll(SecurityPolicyDomain domain, List<Runnable> list) {
        for (Runnable r : list) {
            try {
                r.run();
            } catch (Exception e) {
                log.warn("[SecurityPolicy] evict failed, domain={}", domain, e);
            }
        }
    }
}
