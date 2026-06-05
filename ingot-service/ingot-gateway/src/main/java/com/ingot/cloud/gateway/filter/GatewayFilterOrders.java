package com.ingot.cloud.gateway.filter;

import org.springframework.core.Ordered;

/**
 * 网关 GlobalFilter 执行顺序（order 越小越先执行）。
 *
 * <pre>
 * RequestGlobalFilter (HIGHEST)     剥离内部 Header + 写入 X-Client-Real-IP
 *   → SessionTokenRelayFilter (+10) Cookie → Bearer
 *   → AuthContextRelayFilter (+15)  JWT → userId attribute
 *   → IdentityResolveFilter (+20)   聚合 ClientIdentity + 回填 X-User-Id（Sentinel）
 *   → BlacklistFilter (+30)
 *   → ChallengeFilter (+40)
 *   → WhitelistAwareSentinelGatewayFilter (+50)
 * </pre>
 */
public final class GatewayFilterOrders {

    public static final int REQUEST_GLOBAL = Ordered.HIGHEST_PRECEDENCE;
    public static final int SESSION_RELAY = Ordered.HIGHEST_PRECEDENCE + 10;
    public static final int AUTH_CONTEXT = Ordered.HIGHEST_PRECEDENCE + 15;
    public static final int IDENTITY = Ordered.HIGHEST_PRECEDENCE + 20;

    private GatewayFilterOrders() {
    }
}
