package com.ingot.cloud.gateway.security;

import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 带白名单 / PassToken 跳过的 Sentinel Gateway 过滤器。
 *
 * <p>在委托 {@link SentinelGatewayFilter} 之前检查：</p>
 * <ul>
 *     <li>{@link BlacklistFilter#ATTR_WHITELISTED} — 白名单直接放行</li>
 *     <li>{@link ChallengeFilter#ATTR_PASS_TOKEN_OK} — 已通过挑战的 PassToken 直接放行</li>
 * </ul>
 *
 * @author jy
 * @since 2026/5/26
 */
public class WhitelistAwareSentinelGatewayFilter extends SentinelGatewayFilter {

    public WhitelistAwareSentinelGatewayFilter(int order) {
        super(order);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (Boolean.TRUE.equals(exchange.getAttributes().get(BlacklistFilter.ATTR_WHITELISTED))
                || Boolean.TRUE.equals(exchange.getAttributes().get(ChallengeFilter.ATTR_PASS_TOKEN_OK))) {
            return chain.filter(exchange);
        }
        return super.filter(exchange, chain);
    }
}
