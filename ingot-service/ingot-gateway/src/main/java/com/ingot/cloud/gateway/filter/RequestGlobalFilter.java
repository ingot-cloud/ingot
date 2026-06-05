package com.ingot.cloud.gateway.filter;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.utils.FingerprintUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关全局前置过滤器：统一剥离内部 Header，并标准化客户端 IP。
 *
 * <p>内部 Header 清单见 {@link HeaderConstants#GATEWAY_INTERNAL_HEADERS}；
 * 后续 Filter 只负责写入可信值，不再各自 remove。</p>
 */
@Slf4j
@Component
public class RequestGlobalFilter implements GlobalFilter, Ordered {
    private static final String[] IP_HEADERS = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("[Filter] - RequestGlobalFilter - path={}", exchange.getRequest().getPath());

        String clientIp = resolveClientIp(exchange.getRequest());

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(httpHeaders -> {
                    for (String header : HeaderConstants.GATEWAY_INTERNAL_HEADERS) {
                        httpHeaders.remove(header);
                    }
                    httpHeaders.set(HeaderConstants.CLIENT_REAL_IP, clientIp);
                })
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrders.REQUEST_GLOBAL;
    }

    private String resolveClientIp(ServerHttpRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeaders().getFirst(header);
            if (StrUtil.isNotEmpty(ip) && !NetUtil.isUnknown(ip)) {
                return FingerprintUtil.normalizeIp(NetUtil.getMultistageReverseProxyIp(ip));
            }
        }
        if (request.getRemoteAddress() != null) {
            return FingerprintUtil.normalizeIp(request.getRemoteAddress().getAddress().getHostAddress());
        }
        return "";
    }
}
