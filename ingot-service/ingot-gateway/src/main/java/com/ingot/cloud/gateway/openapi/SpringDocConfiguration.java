package com.ingot.cloud.gateway.openapi;

import com.alibaba.nacos.common.notify.NotifyCenter;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.webflux.ui.SwaggerResourceResolver;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * <p>Description  : SpringDocConfiguration.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/24.</p>
 * <p>Time         : 15:32.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "springdoc.api-docs.enabled", matchIfMissing = true)
@RequiredArgsConstructor
public class SpringDocConfiguration implements InitializingBean {

    private final SwaggerUiConfigProperties swaggerUiConfigProperties;

    private final DiscoveryClient discoveryClient;

    /**
     * 在初始化后调用的方法，用于注册SwaggerDocRegister订阅器
     */
    @Override
    public void afterPropertiesSet() {
        DocSubscriber swaggerDocRegister = new DocSubscriber(swaggerUiConfigProperties, discoveryClient);
        // 手动调用一次，避免监听事件掉线问题
        swaggerDocRegister.onEvent(null);
        NotifyCenter.registerSubscriber(swaggerDocRegister);
    }

    /**
     * Swagger resource resolver swagger resource resolver.
     *
     * @param swaggerUiConfigProperties the swagger ui config properties
     * @return the swagger resource resolver
     */
    @Bean
    @Lazy(false)
    SwaggerResourceResolver swaggerResourceResolver(SwaggerUiConfigProperties swaggerUiConfigProperties) {
        return new CustomSwaggerResourceResolver(swaggerUiConfigProperties);
    }
}
