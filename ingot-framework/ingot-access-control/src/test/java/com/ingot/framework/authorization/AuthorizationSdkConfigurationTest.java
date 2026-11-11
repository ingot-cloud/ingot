package com.ingot.framework.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationInvalidationEvent;
import com.ingot.framework.eventbus.config.EventBusAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>验证 Redis 总线自动装配后 SDK 才订阅授权失效，远端变更能清理本节点热缓存。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class AuthorizationSdkConfigurationTest {
    private static final String COORDINATOR = "iamV2CacheCoordinator";
    private final RemoteAuthorizationClient client = mock(RemoteAuthorizationClient.class);
    private final ApplicationContextRunner context = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(AuthorizationSdkConfiguration.class, EventBusAutoConfiguration.class))
        .withBean(RemoteAuthorizationClient.class, () -> client);

    @Test
    void actualRedisBusIsAvailableBeforeSdkSubscription() throws Exception {
        var container = mock(RedisMessageListenerContainer.class);
        var mapper = new ObjectMapper();
        var event = AuthorizationInvalidationEvent.all();
        event.setOrigin("other-iam-node");
        var message = mock(Message.class);
        when(message.getBody()).thenReturn(mapper.writeValueAsBytes(event));
        context.withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
            .withBean(RedisMessageListenerContainer.class, () -> container)
            .withBean(ObjectMapper.class, () -> mapper)
            .run(application -> {
                assertThat(application).hasBean(COORDINATOR);
                var listener = ArgumentCaptor.forClass(MessageListener.class);
                verify(container).addMessageListener(listener.capture(), any(ChannelTopic.class));
                listener.getValue().onMessage(message, null);
                verify(client).evictAll();
            });
    }

    @Test
    void sdkCanStartWithoutAnEventBus() {
        context.run(application -> assertThat(application).doesNotHaveBean(COORDINATOR));
    }
}
