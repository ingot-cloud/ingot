package com.ingot.cloud.gateway.filter.auth;

import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.gateway.filter.GatewayFilterOrders;
import com.ingot.cloud.gateway.filter.RequestGlobalFilter;
import com.ingot.cloud.gateway.security.ClientIdentity;
import com.ingot.cloud.gateway.security.GatewaySecurityConstants;
import com.ingot.framework.commons.constants.HeaderConstants;
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
 * 客户端身份解析过滤器（身份前置 pipeline 第三步）。
 *
 * <p>在 {@link AuthContextRelayFilter} 解析 JWT 之后执行（order =
 * {@link GatewayFilterOrders#IDENTITY}），聚合各维度并写入
 * {@link GatewaySecurityConstants#ATTR_CLIENT_IDENTITY}，供
 * {@link com.ingot.cloud.gateway.security.BlacklistFilter}、
 * {@link com.ingot.cloud.gateway.security.ChallengeFilter}、Sentinel 限流共用。</p>
 *
 * <h3>聚合规则</h3>
 * <ul>
 *     <li>IP / 设备 / UA / Referer — 读 {@link RequestGlobalFilter} 标准化后的 Header</li>
 *     <li>userId — 读 {@link AuthContextAttributes#USER_ID}（{@link AuthContextRelayFilter} 写入）</li>
 *     <li>非空 userId 时回填 {@code In-Inner-User-Id} Header，供 Sentinel {@code USER} 资源维度</li>
 * </ul>
 *
 * <h3>Pipeline 位置</h3>
 * <pre>
 * RequestGlobalFilter → SessionTokenRelayFilter → AuthContextRelayFilter → 本 Filter → BlacklistFilter
 * </pre>
 *
 * <p>本 Filter 不做鉴权；JWT 签名校验由下游 Resource Server 负责。</p>
 */
@Slf4j
@Component
public class IdentityResolveFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String ip = headers.getFirst(HeaderConstants.INNER_CLIENT_REAL_IP);
        String device = headers.getFirst(HeaderConstants.BFF_DEVICE_FINGERPRINT_HEADER);
        String ua = headers.getFirst(HttpHeaders.USER_AGENT);
        String referer = headers.getFirst(HttpHeaders.REFERER);

        String userId = (String) exchange.getAttributes().get(AuthContextAttributes.USER_ID);
        userId = StrUtil.blankToDefault(userId, null);

        ClientIdentity identity = ClientIdentity.builder()
                .ip(StrUtil.blankToDefault(ip, null))
                .device(StrUtil.blankToDefault(device, null))
                .userId(userId)
                .userAgent(ua)
                .referer(StrUtil.blankToDefault(referer, null))
                .build();
        exchange.getAttributes().put(GatewaySecurityConstants.ATTR_CLIENT_IDENTITY, identity);

        if (userId != null) {
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header(HeaderConstants.INNER_USER_ID, userId)
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrders.IDENTITY;
    }
}
