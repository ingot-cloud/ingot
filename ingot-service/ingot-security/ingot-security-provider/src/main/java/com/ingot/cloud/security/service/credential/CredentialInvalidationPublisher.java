package com.ingot.cloud.security.service.credential;

import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.security.credential.event.CredentialInvalidationEvent;
import com.ingot.framework.security.credential.internal.LocalCompiledPolicyCache;
import com.ingot.framework.security.credential.service.CredentialPolicyConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 凭证策略写操作事务提交后的本地缓存清理 + 跨节点失效广播发布器。
 * <p>
 * 工作流（参照 dict 模块）：
 * <ol>
 *     <li>同步清 origin 节点自身的 L1+L2（直接调 {@link CredentialPolicyConfigService#evictAll()}，
 *         由装饰器链 Caffeine -> Redis -> delegate 自顶向下逐层清理），并清编译策略缓存。
 *         <p>这一步必须由本节点显式执行：{@code RedisInvalidationBus} 在订阅端会过滤 origin
 *         自身的回环事件，故 {@code CredentialCacheCoordinator} 不会回调本节点。</p></li>
 *     <li>通过 {@link InvalidationBus} 广播 {@link CredentialInvalidationEvent}，
 *         其它节点订阅后清理各自的 L1+L2 与编译策略。</li>
 * </ol>
 * </p>
 * <p>
 * 装载条件用 {@link ObjectProvider} 而非 {@code @ConditionalOnBean}：普通
 * {@code @Configuration} 在 component scan 阶段就被处理，时机早于
 * {@code EventBusAutoConfiguration} 的 auto-config，会因为 {@link InvalidationBus}
 * 尚未注册而误判跳过。运行期按需取用，没有总线时仅清本节点缓存、不广播。
 * </p>
 *
 * @author jy
 * @since 2026/5/16
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class CredentialInvalidationPublisher {

    private final ObjectProvider<CredentialPolicyConfigService> policyConfigServiceProvider;
    private final ObjectProvider<LocalCompiledPolicyCache> compiledPolicyCacheProvider;
    private final ObjectProvider<InvalidationBus> invalidationBusProvider;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCredentialPolicyChanged(CredentialPolicyChangedSpringEvent event) {
        evictLocal();
        publishInvalidation();
    }

    private void evictLocal() {
        CredentialPolicyConfigService service = policyConfigServiceProvider.getIfAvailable();
        if (service != null) {
            try {
                service.evictAll();
                log.info("[Credential] origin local L1+L2 evicted (all)");
            } catch (Exception e) {
                log.warn("[Credential] origin local L1+L2 evict failed", e);
            }
        }
        LocalCompiledPolicyCache compiled = compiledPolicyCacheProvider.getIfAvailable();
        if (compiled != null) {
            try {
                compiled.evictAll();
                log.info("[Credential] origin compiled policy evicted (all)");
            } catch (Exception e) {
                log.warn("[Credential] origin compiled policy evict failed", e);
            }
        }
    }

    private void publishInvalidation() {
        InvalidationBus bus = invalidationBusProvider.getIfAvailable();
        if (bus == null) {
            log.debug("[Credential] InvalidationBus not present, skip cross-node broadcast");
            return;
        }
        try {
            bus.publish(CredentialInvalidationEvent.all());
            log.info("[Credential] published invalidation all=true");
        } catch (Exception e) {
            log.warn("[Credential] publish invalidation failed", e);
        }
    }
}
