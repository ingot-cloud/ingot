package com.ingot.framework.feign.reactive;

import java.util.Optional;

import org.springframework.web.server.ServerWebExchange;

/**
 * 在当前 reactive 请求链路中持有 {@link ServerWebExchange}，供同步 Feign 拦截器读取请求头。
 *
 * <p>由 {@link FeignReactiveContextWebFilter} 在请求入口设置并在结束时清理。
 * 无 Web 上下文时（如启动期 ApplicationRunner）为空，行为与 Servlet 侧 request 为 null 一致。</p>
 */
public final class FeignReactiveContextHolder {

    private static final ThreadLocal<ServerWebExchange> EXCHANGE = new ThreadLocal<>();

    private FeignReactiveContextHolder() {
    }

    public static void setExchange(ServerWebExchange exchange) {
        EXCHANGE.set(exchange);
    }

    public static void clear() {
        EXCHANGE.remove();
    }

    public static Optional<ServerWebExchange> getExchange() {
        return Optional.ofNullable(EXCHANGE.get());
    }
}
