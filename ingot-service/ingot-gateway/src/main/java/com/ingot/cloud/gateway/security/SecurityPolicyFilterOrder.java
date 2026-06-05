package com.ingot.cloud.gateway.security;

import com.ingot.cloud.gateway.filter.GatewayFilterOrders;
import org.springframework.core.Ordered;

/**
 * 安全策略相关 GlobalFilter 的执行顺序常量。
 *
 * <p>Spring Cloud Gateway 中 order 越小越先执行。完整链路见
 * {@link GatewayFilterOrders}。</p>
 *
 * <pre>
 * RequestGlobalFilter (HIGHEST)
 *   → SessionTokenRelayFilter (+10)
 *   → AuthContextRelayFilter (+15)
 *   → IdentityResolveFilter (+20)
 *   → BlacklistFilter (+30)
 *   → ChallengeFilter (+40)
 *   → WhitelistAwareSentinelGatewayFilter (+50)
 * </pre>
 */
public final class SecurityPolicyFilterOrder {

    public static final int BLACKLIST = Ordered.HIGHEST_PRECEDENCE + 30;
    public static final int CHALLENGE = Ordered.HIGHEST_PRECEDENCE + 40;
    public static final int SENTINEL = Ordered.HIGHEST_PRECEDENCE + 50;

    private SecurityPolicyFilterOrder() {
    }
}
