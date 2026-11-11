package com.ingot.cloud.iam.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.cache.spi.LayeredCache;
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
 * <p>验证授权缓存可订阅由后续自动配置提供的事件总线，并保留无总线启动方式。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class AuthorizationCacheInvalidationAutoConfigurationTest {
    private static final String COORDINATOR = "iamAuthorizationCacheCoordinator";
    private final LayeredCache<?, ?> cache = mock(LayeredCache.class);
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    AuthorizationCacheInvalidationAutoConfiguration.class, EventBusAutoConfiguration.class))
            .withBean(AuthorizationCacheConfiguration.CACHE_BEAN_NAME, LayeredCache.class, () -> cache);

    @Test
    void redisAutoConfigurationProvidesBusBeforeIamSubscribes() throws Exception {
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
                    verify(cache).evictAll();
                });
    }

    @Test
    void cacheCanStartWithoutAnEventBus() {
        context.run(application -> {
            assertThat(application).hasBean(AuthorizationCacheConfiguration.CACHE_BEAN_NAME);
            assertThat(application).doesNotHaveBean(COORDINATOR);
        });
    }
}
