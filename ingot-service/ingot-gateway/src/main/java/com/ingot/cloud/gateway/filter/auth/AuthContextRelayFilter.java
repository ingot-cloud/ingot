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
 * <p>身份前置 pipeline 第二步（order = {@link GatewayFilterOrders#AUTH_CONTEXT}）：
 * 在 {@link SessionTokenRelayFilter} 将 Session 转为 Bearer 之后、
 * {@link IdentityResolveFilter} 聚合 {@link com.ingot.cloud.gateway.security.ClientIdentity} 之前执行。</p>
 *
 * <h3>行为说明</h3>
 * <ul>
 *     <li>通过 {@link BearerJwtPayloadReader} 读取 claim {@code i}，不验签</li>
 *     <li>解析成功写入 {@link AuthContextAttributes#USER_ID}</li>
 *     <li>无 Bearer / 匿名 / 解析失败时不写入 attribute，不阻断请求</li>
 * </ul>
 *
 * <p>鉴权与 token 有效性由下游 Resource Server 负责；本 Filter 仅提取网关内部限流 / 名单维度。</p>
 *
 * <h3>Pipeline 位置</h3>
 * <pre>
 * SessionTokenRelayFilter → 本 Filter → IdentityResolveFilter → BlacklistFilter
 * </pre>
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
