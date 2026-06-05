package com.ingot.cloud.gateway.filter.auth;

import com.ingot.cloud.gateway.filter.GatewayFilterOrders;
import com.ingot.cloud.gateway.filter.SessionTokenRelayFilter;
import com.ingot.cloud.gateway.filter.auth.internal.BearerJwtPayloadReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 从 Bearer JWT payload 解析用户 ID，写入 exchange attribute。
 *
 * <p>顺序：{@link GatewayFilterOrders#AUTH_CONTEXT}，在 {@link SessionTokenRelayFilter}
 * 注入 Bearer 之后、{@link IdentityResolveFilter} 之前。</p>
 *
 * <p>不做签名校验（鉴权由下游 Resource Server 负责）；匿名 / 无 Bearer 时不写入 attribute。</p>
 */
@Slf4j
@Component
public class AuthContextRelayFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String userId = BearerJwtPayloadReader.readUserId(authorization);
        if (userId != null) {
            exchange.getAttributes().put(AuthContextAttributes.USER_ID, userId);
            log.debug("[AuthContextRelay] resolved userId={} path={}", userId, request.getURI().getPath());
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrders.AUTH_CONTEXT;
    }
}
