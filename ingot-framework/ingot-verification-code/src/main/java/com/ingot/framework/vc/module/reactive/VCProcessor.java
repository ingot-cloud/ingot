package com.ingot.framework.vc.module.reactive;

import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.common.VCType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * <p>Description  : VCProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/20.</p>
 * <p>Time         : 10:12 AM.</p>
 */
public interface VCProcessor {

    /**
     * Handle the given request.
     *
     * @param request   the request to handle
     * @param generator the code generator
     * @return the response
     */
    Mono<ServerResponse> handle(ServerRequest request, VCGenerator generator);

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
