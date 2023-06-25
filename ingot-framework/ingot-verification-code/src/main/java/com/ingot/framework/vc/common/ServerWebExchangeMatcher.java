package com.ingot.framework.vc.common;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <p>Description  : ServerWebExchangeMatcher.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/25.</p>
 * <p>Time         : 1:46 PM.</p>
 */
public interface ServerWebExchangeMatcher {

    /**
     * Determines if a request matches or not
     *
     * @param exchange {@link ServerWebExchange}
     * @return Mono
     */
    Mono<Boolean> matches(ServerWebExchange exchange);
}
