package com.ingot.framework.social.common.publisher;

import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.social.common.event.SocialConfigRedisMessage;
import com.ingot.framework.social.common.event.SocialConfigMessageHandler;
import com.ingot.framework.social.common.event.SocialInvalidationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 基于 {@link InvalidationBus} 的社交配置变更发布：本机先发布本地 Spring 事件，再广播失效消息供其它节点处理。
 *
 * @author jy
 * @since 2026/5/13
 */
@Slf4j
@RequiredArgsConstructor
public class InvalidationBusSocialConfigMessagePublisher implements SocialConfigMessagePublisher {

    private final SocialConfigMessageHandler messageHandler;
    private final ObjectProvider<InvalidationBus> invalidationBusProvider;

    @Override
    public void publishRefreshAll(SocialTypeEnum socialType) {
        publish(SocialInvalidationEvent.refreshAll(socialType.getValue()));
    }

    @Override
    public void publishAdd(SocialTypeEnum socialType, String appId) {
        publish(SocialInvalidationEvent.add(socialType.getValue(), appId));
    }

    @Override
    public void publishUpdate(SocialTypeEnum socialType, String appId) {
        publish(SocialInvalidationEvent.update(socialType.getValue(), appId));
    }

    @Override
    public void publishDelete(SocialTypeEnum socialType, String appId) {
        publish(SocialInvalidationEvent.delete(socialType.getValue(), appId));
    }

    @Override
    public void publish(SocialConfigRedisMessage message) {
        publish(SocialInvalidationEvent.fromRedisMessage(message));
    }

    private void publish(SocialInvalidationEvent event) {
        if (event == null) {
            return;
        }
        messageHandler.handleInvalidation(event, this);
        InvalidationBus bus = invalidationBusProvider.getIfAvailable();
        if (bus == null) {
            log.debug("[Social] InvalidationBus not present, skip cross-node broadcast");
            return;
        }
        bus.publish(event);
        log.info("InvalidationBusSocialConfigMessagePublisher - 已广播社交配置失效: socialType={}, changeType={}, appId={}",
                event.getSocialType(), event.getChangeType(), event.getAppId());
    }
}
