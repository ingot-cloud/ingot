package com.ingot.cloud.gateway.filter;

import java.net.URI;
import java.util.List;
import java.util.Locale;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.gateway.config.GatewayBffProperties;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 按 Host 匹配 Nacos 应用注册表并注入内部 appId/入口类型。
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BffAppContextFilter implements GlobalFilter, Ordered {
    private final GatewayBffProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        List<BffAppRegistration> apps = properties.getApps();
        if (CollUtil.isEmpty(apps)) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getURI().getPath();
        if (!path.startsWith("/bff/")) {
            return chain.filter(exchange);
        }
        String host = exchange.getRequest().getHeaders().getFirst(HttpHeaders.HOST);
        BffAppRegistration matched = match(host, apps);
        if (matched == null) {
            log.warn("[BffAppContext] unknown host={}", host);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
        String entry = entryRole(host, matched);
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HeaderConstants.INNER_BFF_APP_ID);
                    headers.remove(HeaderConstants.INNER_BFF_ENTRY);
                    headers.set(HeaderConstants.INNER_BFF_APP_ID, matched.getAppId());
                    headers.set(HeaderConstants.INNER_BFF_ENTRY, entry);
                })
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrders.BFF_APP_CONTEXT;
    }

    private static BffAppRegistration match(String host, List<BffAppRegistration> apps) {
        String normalized = normalizeHost(host);
        for (BffAppRegistration app : apps) {
            if (normalized.equals(hostOf(app.getAdminOrigin()))
                    || normalized.equals(hostOf(app.getLoginOrigin()))) {
                return app;
            }
        }
        return null;
    }

    private static String entryRole(String host, BffAppRegistration app) {
        String normalized = normalizeHost(host);
        if (normalized.equals(hostOf(app.getLoginOrigin()))) {
            return BffConstants.ENTRY_LOGIN;
        }
        return BffConstants.ENTRY_ADMIN;
    }

    private static String hostOf(String origin) {
        if (StrUtil.isBlank(origin)) {
            return "";
        }
        URI uri = URI.create(origin);
        int port = uri.getPort();
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (port > 0 && port != 80 && port != 443) {
            return host + ":" + port;
        }
        return host;
    }

    private static String normalizeHost(String host) {
        if (StrUtil.isBlank(host)) {
            return "";
        }
        String value = host.trim().toLowerCase(Locale.ROOT);
        if (value.endsWith(":443")) {
            return StrUtil.removeSuffix(value, ":443");
        }
        if (value.endsWith(":80")) {
            return StrUtil.removeSuffix(value, ":80");
        }
        return value;
    }
}
