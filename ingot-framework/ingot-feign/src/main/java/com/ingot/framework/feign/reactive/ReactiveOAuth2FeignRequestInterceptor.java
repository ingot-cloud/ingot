package com.ingot.framework.feign.reactive;

import com.ingot.framework.feign.FeignHeaderRelay;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

/**
 * WebFlux / Gateway 环境的 Feign 请求拦截器。
 *
 * <p>语义与 {@link com.ingot.framework.feign.OAuth2FeignRequestInterceptor} 对齐：
 * 转发 {@code Authorization} 与 {@code deviceid}，并标记内部调用头；不依赖
 * {@code jakarta.servlet.http.HttpServletRequest}。</p>
 */
@Slf4j
public class ReactiveOAuth2FeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        log.info(">>> ReactiveOAuth2FeignRequestInterceptor - start.");

        relayHeader(template);
        FeignHeaderRelay.applyInsideHeader(template);

        log.info(">>> ReactiveOAuth2FeignRequestInterceptor - end.");
    }

    private void relayHeader(RequestTemplate template) {
        ServerWebExchange exchange = FeignReactiveContextHolder.getExchange().orElse(null);
        if (exchange == null) {
            return;
        }

        HttpHeaders headers = exchange.getRequest().getHeaders();
        headers.forEach((headerName, headerValues) -> {
            if (!FeignHeaderRelay.shouldRelay(headerName) || headerValues == null || headerValues.isEmpty()) {
                return;
            }
            log.info(">>> ReactiveOAuth2FeignRequestInterceptor - relay header >> set key={}, values={}",
                    headerName, headerValues);
            template.header(headerName, headerValues);
        });
    }
}
