package com.ingot.framework.dict.client.internal;

import com.ingot.framework.dict.client.DictService;
import com.ingot.framework.dict.client.event.DictInvalidationEvent;
import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.Subscription;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 订阅 {@link DictInvalidationEvent}，在收到广播后调用根 {@link DictService}
 * 的 {@code evict} / {@code evictAll} 方法，由装饰器链自顶向下清理 L1 + L2。
 *
 * @author jy
 * @since 2026/4/27
 */
@Slf4j
@RequiredArgsConstructor
public class DictCacheCoordinator {

    private final InvalidationBus bus;
    private final DictService dictService;

    private Subscription subscription;

    @PostConstruct
    public void start() {
        this.subscription = bus.subscribe(DictInvalidationEvent.class, this::handle);
        log.info("[Dict] cache coordinator subscribed");
    }

    @PreDestroy
    public void stop() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    void handle(DictInvalidationEvent event) {
        if (event.isAll() || event.getDictCode() == null || event.getDictCode().isBlank()) {
            log.info("[Dict] cache invalidate all (origin={})", event.getOrigin());
            dictService.evictAll();
        } else {
            log.info("[Dict] cache invalidate dictCode={} (origin={})", event.getDictCode(), event.getOrigin());
            dictService.evict(event.getDictCode());
        }
    }
}
