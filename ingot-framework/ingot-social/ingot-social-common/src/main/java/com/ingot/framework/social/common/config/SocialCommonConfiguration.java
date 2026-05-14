package com.ingot.framework.social.common.config;

import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.config.EventBusAutoConfiguration;
import com.ingot.framework.social.common.event.SocialConfigMessageHandler;
import com.ingot.framework.social.common.internal.SocialInvalidationCoordinator;
import com.ingot.framework.social.common.publisher.InvalidationBusSocialConfigMessagePublisher;
import com.ingot.framework.social.common.publisher.SocialConfigMessagePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * <p>Description  : 社交公共配置.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 16:10.</p>
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(EventBusAutoConfiguration.class)
public class SocialCommonConfiguration {

    @Bean
    public SocialConfigMessageHandler socialConfigMessageHandler(
            ApplicationEventPublisher eventPublisher) {
        return new SocialConfigMessageHandler(eventPublisher);
    }

    /**
     * 默认通过 {@link InvalidationBus} 广播社交配置变更；本机先收到 {@link SocialConfigMessageHandler#handleInvalidation}，
     * 再 {@code bus.publish}，其它节点由 {@link SocialInvalidationCoordinator} 订阅处理。
     */
    @Bean
    @ConditionalOnMissingBean(SocialConfigMessagePublisher.class)
    public SocialConfigMessagePublisher socialConfigMessagePublisher(
            SocialConfigMessageHandler messageHandler,
            ObjectProvider<InvalidationBus> invalidationBusProvider) {
        log.info("SocialCommonConfiguration - 默认社交配置发布器: InvalidationBus（频道由 ingot.event-bus 的 topic-prefix + social.invalidate 推导）");
        return new InvalidationBusSocialConfigMessagePublisher(messageHandler, invalidationBusProvider);
    }

    @Bean
    @ConditionalOnBean(InvalidationBus.class)
    @ConditionalOnMissingBean(SocialInvalidationCoordinator.class)
    public SocialInvalidationCoordinator socialInvalidationCoordinator(
            InvalidationBus bus,
            SocialConfigMessageHandler messageHandler) {
        return new SocialInvalidationCoordinator(bus, messageHandler);
    }
}
