package com.ingot.cloud.gateway.filter;

import com.ingot.framework.commons.constants.SecurityConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 * <p>Description  : RequestGlobalFilter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-07-30.</p>
 * <p>Time         : 11:59.</p>
 */
@Slf4j
@Component
public class RequestGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("[Filter] - RequestGlobalFilter - path={}", exchange.getRequest().getPath());

        // 清洗 SecurityConstants.HEADER_FROM
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(httpHeaders -> httpHeaders.remove(SecurityConstants.HEADER_FROM))
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
