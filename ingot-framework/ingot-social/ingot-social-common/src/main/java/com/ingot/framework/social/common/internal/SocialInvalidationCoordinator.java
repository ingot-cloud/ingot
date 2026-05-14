package com.ingot.framework.social.common.internal;

import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.Subscription;
import com.ingot.framework.social.common.event.SocialConfigMessageHandler;
import com.ingot.framework.social.common.event.SocialInvalidationEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 订阅 {@link SocialInvalidationEvent}，将远端广播转为本地 {@link com.ingot.framework.social.common.event.SocialConfigChangedEvent}。
 * <p>
 * 发布端须在 {@code bus.publish} 之前自行触发本节点本地事件（总线会过滤 origin 回环，此处收不到本机发出的消息）。
 * </p>
 *
 * @author jy
 * @since 2026/5/13
 */
@Slf4j
@RequiredArgsConstructor
public class SocialInvalidationCoordinator {

    private final InvalidationBus bus;
    private final SocialConfigMessageHandler messageHandler;

    private Subscription subscription;

    @PostConstruct
    public void start() {
        this.subscription = bus.subscribe(SocialInvalidationEvent.class, this::handle);
        log.info("[Social] invalidation coordinator subscribed");
    }

    @PreDestroy
    public void stop() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    void handle(SocialInvalidationEvent event) {
        log.info("[Social] invalidation received socialType={}, changeType={}, appId={}, origin={}",
                event.getSocialType(), event.getChangeType(), event.getAppId(), event.getOrigin());
        messageHandler.handleInvalidation(event, this);
    }
}
