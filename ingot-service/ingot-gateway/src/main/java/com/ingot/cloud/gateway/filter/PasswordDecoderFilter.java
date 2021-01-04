package com.ingot.cloud.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

/**
 * <p>Description  : PasswordDecoderFilter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/4.</p>
 * <p>Time         : 8:37 下午.</p>
 */
@Slf4j
@Component
@SuppressWarnings("all")
public class PasswordDecoderFilter extends AbstractGatewayFilterFactory {
    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            log.info("PasswordDecoderFilter pre doUrl={}", path);

            return chain.filter(exchange);
        };
    }
}
