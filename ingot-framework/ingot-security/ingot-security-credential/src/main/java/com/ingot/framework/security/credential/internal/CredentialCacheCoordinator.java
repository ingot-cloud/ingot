package com.ingot.framework.security.credential.internal;

import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.Subscription;
import com.ingot.framework.security.credential.event.CredentialInvalidationEvent;
import com.ingot.framework.security.credential.service.CredentialPolicyConfigService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 订阅 {@link CredentialInvalidationEvent} 的协调器：
 * 收到广播后清空本节点的 L1+L2 凭证策略配置缓存。
 * <p>
 * 注意：发起广播的节点不会收到自身事件（{@code RedisInvalidationBus} 已按 {@code origin} 过滤），
 * 所以发起节点必须由 {@code CredentialInvalidationPublisher} 显式调用 evict 清自己的缓存。
 * </p>
 *
 * @author jy
 * @since 2026/5/16
 */
@Slf4j
@RequiredArgsConstructor
public class CredentialCacheCoordinator {

    private final InvalidationBus bus;
    private final CredentialPolicyConfigService policyConfigService;

    private Subscription subscription;

    @PostConstruct
    public void start() {
        this.subscription = bus.subscribe(CredentialInvalidationEvent.class, this::handle);
        log.info("[Credential] cache coordinator subscribed");
    }

    @PreDestroy
    public void stop() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    void handle(CredentialInvalidationEvent event) {
        log.info("[Credential] cache invalidate all (origin={})", event.getOrigin());
        try {
            policyConfigService.evictAll();
        } catch (Exception e) {
            log.warn("[Credential] L1+L2 evict failed", e);
        }
    }
}
