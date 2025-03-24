package com.ingot.cloud.gateway.openapi;

import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.webflux.ui.SwaggerResourceResolver;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.resource.ResourceResolverChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * <p>Description  : CustomSwaggerResourceResolver.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/3/24.</p>
 * <p>Time         : 15:31.</p>
 */
class CustomSwaggerResourceResolver extends SwaggerResourceResolver {

    /**
     * 构造方法
     *
     * @param swaggerUiConfigProperties Swagger UI 配置属性
     */
    public CustomSwaggerResourceResolver(SwaggerUiConfigProperties swaggerUiConfigProperties) {
        super(swaggerUiConfigProperties);
    }

    /**
     * 解析资源
     *
     * @param exchange    ServerWebExchange 对象
     * @param requestPath 请求路径
     * @param locations   资源位置列表
     * @param chain       ResourceResolverChain 对象
     * @return 解析后的 Mono<Resource> 对象
     */
    @Override
    @NonNull
    public Mono<Resource> resolveResource(ServerWebExchange exchange, @NonNull String requestPath,
                                          @NonNull List<? extends Resource> locations, ResourceResolverChain chain) {
        Mono<Resource> resolved = chain.resolveResource(exchange, requestPath, locations);
        if (!Mono.empty().equals(resolved) && requestPath.startsWith("swagger-ui")) {
            String webJarResourcePath = findWebJarResourcePath(requestPath);
            if (webJarResourcePath != null)
                return chain.resolveResource(exchange, webJarResourcePath, locations);
        }
        return resolved;
    }

}
