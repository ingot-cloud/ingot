package com.ingot.cloud.security.service.policy;

import com.ingot.cloud.security.api.event.SecurityPolicyInvalidationEvent;
import com.ingot.framework.eventbus.InvalidationBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 安全策略写操作事务提交后的跨节点失效广播发布器。
 *
 * <p>本服务自身没有 L1 Caffeine（管理面读路径直查 DB），所以提交后只发广播，
 * 让所有订阅方（网关 / 业务侧）的 {@code SecurityPolicyCacheCoordinator} 清缓存。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class SecurityPolicyInvalidationPublisher {

    private final ObjectProvider<InvalidationBus> invalidationBusProvider;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSecurityPolicyChanged(SecurityPolicyChangedSpringEvent event) {
        InvalidationBus bus = invalidationBusProvider.getIfAvailable();
        if (bus == null) {
            log.debug("[SecurityPolicy] InvalidationBus not present, skip cross-node broadcast");
            return;
        }
        try {
            bus.publish(SecurityPolicyInvalidationEvent.of(event.getDomain()));
            log.info("[SecurityPolicy] published invalidation, domain={}", event.getDomain());
        } catch (Exception e) {
            log.warn("[SecurityPolicy] publish invalidation failed, domain={}", event.getDomain(), e);
        }
    }
}
