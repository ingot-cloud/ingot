package com.ingot.framework.vc.module.reactive;

import com.ingot.framework.vc.common.VCType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * <p>Description  : VCProcessorManager.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/20.</p>
 * <p>Time         : 2:00 PM.</p>
 */
public interface VCProcessorManager {

    /**
     * Handle the given request.
     *
     * @param type    {@link VCType}
     * @param request the request to handle
     * @return the response
     */
    Mono<ServerResponse> handle(VCType type, ServerRequest request);

    /**
     * Process the Web request and (optionally) delegate to the next
     * {@code WebFilter} through the given {@link WebFilterChain}.
     *
     * @param type     {@link VCType}
     * @param exchange the current server exchange
     * @param chain    provides a way to delegate to the next filter
     * @return {@code Mono<Void>} to indicate when request processing is complete
     */
    Mono<Void> checkOnly(VCType type, ServerWebExchange exchange, WebFilterChain chain);

    /**
     * Check request
     *
     * @param type    {@link VCType}
     * @param request the request to check
     * @return the response
     */
    Mono<ServerResponse> check(VCType type, ServerRequest request);
}
