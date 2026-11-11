package com.ingot.cloud.iam.configuration;

import com.ingot.cloud.iam.domain.authorization.AuthorizationView;
import com.ingot.framework.cache.coordinator.LayeredCacheCoordinator;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationInvalidationEvent;
import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.config.EventBusAutoConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

/**
 * <p>在事件总线装配完成后订阅 IAM 授权视图的跨节点失效。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration(after = EventBusAutoConfiguration.class)
@ConditionalOnBean(value = InvalidationBus.class, name = AuthorizationCacheConfiguration.CACHE_BEAN_NAME)
public class AuthorizationCacheInvalidationAutoConfiguration {
    private static final String ALL_DOMAIN = "all";

    /**
     * 接收其他节点的授权变更并清理本节点授权视图。
     *
     * @param bus 失效总线
     * @param cache IAM 授权视图缓存
     * @return 订阅随容器启停的协调器
     */
    @Bean
    public LayeredCacheCoordinator<AuthorizationInvalidationEvent, String> iamAuthorizationCacheCoordinator(
            InvalidationBus bus,
            @Qualifier(AuthorizationCacheConfiguration.CACHE_BEAN_NAME) LayeredCache<String, AuthorizationView> cache) {
        var coordinator = new LayeredCacheCoordinator<>(bus, AuthorizationInvalidationEvent.class,
                event -> ALL_DOMAIN, ALL_DOMAIN);
        coordinator.register(ALL_DOMAIN, cache::evictAll);
        return coordinator;
    }
}
