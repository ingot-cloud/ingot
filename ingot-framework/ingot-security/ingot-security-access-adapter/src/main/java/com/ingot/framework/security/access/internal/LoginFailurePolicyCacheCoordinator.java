package com.ingot.framework.security.access.internal;

import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.cloud.security.api.event.SecurityPolicyInvalidationEvent;
import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.Subscription;
import com.ingot.framework.security.access.service.LoginFailurePolicyLoader;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 订阅登录失败策略失效事件并触发重新加载。
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class LoginFailurePolicyCacheCoordinator {

    private final InvalidationBus bus;
    private final LoginFailurePolicyLoader policyLoader;

    private Subscription subscription;

    @PostConstruct
    public void start() {
        subscription = bus.subscribe(SecurityPolicyInvalidationEvent.class, this::handle);
        log.info("[LoginFailure] policy cache coordinator subscribed");
    }

    @PreDestroy
    public void stop() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    void handle(SecurityPolicyInvalidationEvent event) {
        if (event.getDomain() != SecurityPolicyDomain.LOGIN_FAILURE_PROTECTION
                && event.getDomain() != SecurityPolicyDomain.ALL) {
            return;
        }
        log.info("[LoginFailure] policy invalidate domain={}", event.getDomain());
        try {
            policyLoader.evictAll();
        } catch (Exception e) {
            log.warn("[LoginFailure] policy evict failed", e);
        }
    }
}
