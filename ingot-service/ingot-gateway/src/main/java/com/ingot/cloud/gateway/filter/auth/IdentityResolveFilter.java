package com.ingot.cloud.gateway.filter.auth;

import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.gateway.filter.GatewayFilterOrders;
import com.ingot.cloud.gateway.filter.RequestGlobalFilter;
import com.ingot.cloud.gateway.security.ClientIdentity;
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
 * 客户端身份解析过滤器（身份前置 pipeline 第二步）。
 *
 * <p>顺序：{@link GatewayFilterOrders#IDENTITY}，在 {@link AuthContextRelayFilter} 之后执行。</p>
 *
 * <ul>
 *     <li>IP / 设备 / UA / Referer：读网关标准化 Header（{@link RequestGlobalFilter}）</li>
 *     <li>userId：读 {@link AuthContextAttributes#USER_ID} attribute（{@link AuthContextRelayFilter}）</li>
 *     <li>聚合为 {@link ClientIdentity} 写入 attribute；若有 userId 则回填 {@code X-User-Id} 供 Sentinel USER 维度</li>
 * </ul>
 */
@Slf4j
@Component
public class IdentityResolveFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String ip = headers.getFirst(HeaderConstants.CLIENT_REAL_IP);
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
        exchange.getAttributes().put(ClientIdentity.ATTR_KEY, identity);

        if (userId != null) {
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header(HeaderConstants.X_USER_ID, userId)
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
