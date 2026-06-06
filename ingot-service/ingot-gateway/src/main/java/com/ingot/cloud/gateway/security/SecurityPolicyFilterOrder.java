package com.ingot.cloud.gateway.security;

import com.ingot.cloud.gateway.filter.GatewayFilterOrders;
import org.springframework.core.Ordered;

import lombok.experimental.UtilityClass;

/**
 * 安全策略相关 GlobalFilter 的执行顺序常量。
 *
 * <p>Spring Cloud Gateway 中 {@code order} 越小越先执行。本类取值基于
 * {@link Ordered#HIGHEST_PRECEDENCE}（{@code -2147483648}）偏移，须保证
 * {@link BlacklistFilter} 在 Sentinel 之前写入 {@link GatewaySecurityConstants#ATTR_WHITELISTED}，
 * {@link ChallengeFilter} 在 Sentinel 之前写入 {@link GatewaySecurityConstants#ATTR_PASS_TOKEN_OK}。</p>
 *
 * <h3>完整链路（节选）</h3>
 * <pre>
 * RequestGlobalFilter          (HIGHEST_PRECEDENCE + 0)
 *   → SessionTokenRelayFilter  (+10)
 *   → AuthContextRelayFilter   (+15)
 *   → IdentityResolveFilter    (+20)   写入 ATTR_CLIENT_IDENTITY
 *   → BlacklistFilter          (+30)   写入 ATTR_WHITELISTED
 *   → ChallengeFilter          (+40)   消费 PassToken / 返回 412
 *   → WhitelistAwareSentinel   (+50)   Sentinel 限流
 * </pre>
 *
 * <p>身份前置 Filter 的 order 定义在 {@link GatewayFilterOrders}；
 * 本类仅收纳 security 包内 Filter 的偏移量。</p>
 */
@UtilityClass
public class SecurityPolicyFilterOrder {

    public static final int BLACKLIST = Ordered.HIGHEST_PRECEDENCE + 30;
    public static final int CHALLENGE = Ordered.HIGHEST_PRECEDENCE + 40;
    public static final int SENTINEL = Ordered.HIGHEST_PRECEDENCE + 50;
}
