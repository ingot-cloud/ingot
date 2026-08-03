package com.ingot.framework.feign.reactive;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 将 {@link ServerWebExchange} 绑定到当前请求线程，供同步 Feign 拦截器转发请求头。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FeignReactiveContextWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.defer(() -> {
            FeignReactiveContextHolder.setExchange(exchange);
            return chain.filter(exchange).doFinally(signal -> FeignReactiveContextHolder.clear());
        });
    }
}
