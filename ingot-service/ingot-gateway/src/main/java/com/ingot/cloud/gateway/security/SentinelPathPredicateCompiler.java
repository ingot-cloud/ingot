package com.ingot.cloud.gateway.security;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;

import lombok.experimental.UtilityClass;

/**
 * 将 SDK 限流规则的 path 编译为 Sentinel SCG {@link ApiPathPredicateItem}。
 *
 * <p>由 {@link SentinelGatewayConfiguration} 在启动期把
 * {@link com.ingot.framework.gateway.rule.client.ratelimit.model.RateLimitRule} 编译为
 * Sentinel API 分组时调用，保证路径匹配策略与 Spring Cloud Gateway 路由一致。</p>
 *
 * <h3>匹配策略选择</h3>
 * <ul>
 *     <li>含 {@code *} 或 {@code ?} — {@code URL_MATCH_STRATEGY_PREFIX} + 完整 Ant pattern</li>
 *     <li>无通配符 — {@code URL_MATCH_STRATEGY_EXACT} + 精确路径</li>
 * </ul>
 *
 * <p><b>注意</b>：SCG 适配器的 PREFIX 策略调用 {@code RouteMatchers.antPath(pattern)}
 *（Spring {@code AntPathMatcher}），<b>不会</b>做「去掉 {@code /**} 后按字符串前缀」匹配。
 * 若把 {@code /pms/**} 截成 {@code /pms}，{@code AntPathMatcher.isPattern("/pms")} 为 false，
 * 规则永远不命中。</p>
 *
 * <h3>编译示例</h3>
 * <pre>
 * /api/pms/order/**  → PREFIX, pattern=/api/pms/order/**
 * /api/health       → EXACT,  pattern=/api/health
 * </pre>
 */
@UtilityClass
final class SentinelPathPredicateCompiler {

    static ApiPathPredicateItem compile(String rawPath) {
        String path = rawPath == null ? "" : rawPath.trim();
        ApiPathPredicateItem item = new ApiPathPredicateItem();
        if (isAntStylePattern(path)) {
            item.setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX);
            item.setPattern(path);
        } else {
            item.setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_EXACT);
            item.setPattern(path);
        }
        return item;
    }

    static boolean isAntStylePattern(String path) {
        return path.contains("*") || path.contains("?");
    }
}
