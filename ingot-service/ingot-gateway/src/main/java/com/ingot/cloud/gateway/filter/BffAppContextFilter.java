package com.ingot.cloud.gateway.filter;

import com.ingot.cloud.gateway.config.GatewayBffProperties;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.utils.BffOriginPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <p>
 * 所有已注册同源入口按可信Host注入应用身份，移除旧admin/login主机角色。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class BffAppContextFilter implements GlobalFilter, Ordered {

    private final GatewayBffProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        BffAppRegistration app = BffOriginPolicy.match(exchange.getRequest().getHeaders().getFirst(HttpHeaders.HOST),
                properties.getApps());
        if (app == null && exchange.getRequest().getURI().getPath().startsWith(BffConstants.ROOT)) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
        var request = exchange.getRequest().mutate().headers(headers -> {
            headers.remove(HeaderConstants.INNER_BFF_APP_ID);
            headers.remove(HeaderConstants.INNER_BFF_ENTRY);
            if (app != null)
                headers.set(HeaderConstants.INNER_BFF_APP_ID, app.getAppId());
        }).build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrders.BFF_APP_CONTEXT;
    }

}
