package com.ingot.framework.cache.config;

import com.ingot.framework.cache.actuate.LayeredCacheEndpoint;
import com.ingot.framework.cache.registry.LayeredCacheRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * <p>分层缓存框架的自动配置，只提供跨模块共享的注册表与汇总观测端点。</p>
 *
 * <p>刻意不装配任何具体缓存实例：缓存的键类型、值类型、加载器与降级来源都由消费模块决定，
 * 框架无从预设。消费模块在自己的自动配置里用 {@link LayeredCacheBuilder} 组装即可。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see LayeredCacheBuilder
 */
@AutoConfiguration
public class LayeredCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LayeredCacheRegistry layeredCacheRegistry() {
        return new LayeredCacheRegistry();
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    @ConditionalOnBean(LayeredCacheRegistry.class)
    @ConditionalOnMissingBean
    public LayeredCacheEndpoint layeredCacheEndpoint(LayeredCacheRegistry registry) {
        return new LayeredCacheEndpoint(registry);
    }
}
