package com.ingot.cloud.pms.service.dict;

import com.ingot.framework.dict.client.DictService;
import com.ingot.framework.dict.client.event.DictInvalidationEvent;
import com.ingot.framework.eventbus.InvalidationBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 字典写操作事务提交后的本地缓存清理 + 跨节点失效广播发布器。
 * <p>
 * 工作流：
 * <ol>
 *     <li>同步清 origin 节点自身的 L1 + L2（直接调根 {@link DictService}.evict()，
 *         由装饰器链 Caffeine → Redis → delegate 自顶向下逐层清理）。
 *         <p>这一步必须由本节点显式执行：因为
 *         {@link com.ingot.framework.eventbus.redis.RedisInvalidationBus} 在订阅端
 *         过滤 origin 自身的回环事件，所以 {@code DictCacheCoordinator} 不会回调本节点。</p></li>
 *     <li>通过 {@link InvalidationBus} 广播 {@link DictInvalidationEvent}，
 *         其它节点订阅后清理各自的 L1 + L2。</li>
 * </ol>
 * </p>
 * <p>
 * 注意：本类不能再用 {@code @ConditionalOnBean(InvalidationBus.class)} 控制装载——
 * 普通 {@code @Configuration} 在 component scan 阶段就被处理，时机早于
 * {@code EventBusAutoConfiguration} 的 auto-config，会因为 {@link InvalidationBus}
 * 尚未注册而误判跳过。改用 {@link ObjectProvider} 在运行期按需取用，
 * 没有总线时仅清本节点缓存、不广播。
 * </p>
 *
 * @author jy
 * @since 2026/4/27
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class DictInvalidationPublisher {

    private final ObjectProvider<DictService> dictServiceProvider;
    private final ObjectProvider<InvalidationBus> invalidationBusProvider;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDictChanged(DictChangedSpringEvent event) {
        evictLocal(event);
        publishInvalidation(event);
    }

    private void evictLocal(DictChangedSpringEvent event) {
        DictService dictService = dictServiceProvider.getIfAvailable();
        if (dictService == null) {
            return;
        }
        try {
            if (event.isAll()) {
                dictService.evictAll();
                log.info("[Dict] origin local L1+L2 evicted (all)");
            } else if (event.getDictCode() != null && !event.getDictCode().isBlank()) {
                dictService.evict(event.getDictCode());
                log.info("[Dict] origin local L1+L2 evicted dictCode={}", event.getDictCode());
            }
        } catch (Exception e) {
            log.warn("[Dict] origin local cache evict failed dictCode={}", event.getDictCode(), e);
        }
    }

    private void publishInvalidation(DictChangedSpringEvent event) {
        InvalidationBus bus = invalidationBusProvider.getIfAvailable();
        if (bus == null) {
            log.debug("[Dict] InvalidationBus not present, skip cross-node broadcast");
            return;
        }
        DictInvalidationEvent invalidation = event.isAll()
                ? DictInvalidationEvent.all()
                : DictInvalidationEvent.of(event.getDictCode());
        try {
            bus.publish(invalidation);
            log.info("[Dict] published invalidation dictCode={}, all={}", event.getDictCode(), event.isAll());
        } catch (Exception e) {
            log.warn("[Dict] publish invalidation failed dictCode={}", event.getDictCode(), e);
        }
    }
}
