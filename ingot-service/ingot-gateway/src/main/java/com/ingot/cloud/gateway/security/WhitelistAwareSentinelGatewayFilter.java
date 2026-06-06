package com.ingot.cloud.gateway.security;

import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 带白名单 / PassToken 跳过逻辑的 Sentinel Gateway 过滤器。
 *
 * <p>替换 Spring Cloud Alibaba 默认 {@link SentinelGatewayFilter}（见
 * {@link SecurityPolicySentinelConfiguration}），在委托 Sentinel 限流前短路以下场景：</p>
 * <ul>
 *     <li>{@link GatewaySecurityConstants#ATTR_WHITELISTED} — 静态白名单命中，跳过限流</li>
 *     <li>{@link GatewaySecurityConstants#ATTR_PASS_TOKEN_OK} — PassToken 验码成功，跳过限流</li>
 * </ul>
 *
 * <p>执行顺序为 {@link SecurityPolicyFilterOrder#SENTINEL}，确保前述 attribute 已由
 * {@link BlacklistFilter} / {@link ChallengeFilter} 写入。</p>
 *
 * <h3>相关配置</h3>
 * <pre>{@code
 * spring:
 *   cloud:
 *     sentinel:
 *       scg:
 *         enabled: true          # 默认 true；关闭则本 Filter 不注册
 * ingot:
 *   security:
 *     ratelimit:
 *       enabled: true          # SDK 规则编译；与本 Filter 注册独立
 * }</pre>
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
        if (Boolean.TRUE.equals(exchange.getAttributes().get(GatewaySecurityConstants.ATTR_WHITELISTED))
                || Boolean.TRUE.equals(exchange.getAttributes().get(GatewaySecurityConstants.ATTR_PASS_TOKEN_OK))) {
            return chain.filter(exchange);
        }
        return super.filter(exchange, chain);
    }
}
