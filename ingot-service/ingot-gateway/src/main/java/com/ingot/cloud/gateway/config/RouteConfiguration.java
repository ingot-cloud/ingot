package com.ingot.cloud.gateway.config;

import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Description  : RouteConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/30.</p>
 * <p>Time         : 上午11:31.</p>
 */
@Configuration
public class RouteConfiguration {

    @Bean
    public RouteDefinitionLocator discoveryClientRouteDefinitionLocator(ReactiveDiscoveryClient discoveryClient,
                                                                        DiscoveryLocatorProperties properties) {
        return new DiscoveryClientRouteDefinitionLocator(discoveryClient, properties);
    }
}
