package com.ingot.framework.feign;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import com.ingot.framework.core.context.RequestContextHolder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Servlet 环境的 Feign 请求拦截器：转发网关标准化身份头并标记内部调用。</p>
 *
 * <p>转发清单与 {@link FeignHeaderRelay} 一致，保证 BFF → Auth 看到的 IP / 设备 /
 * userId / clientId 与网关 {@code BlacklistFilter} 相同。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
@Slf4j
public class OAuth2FeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        log.info(">>> OAuth2FeignRequestInterceptor - start.");

        relayHeader(template);
        FeignHeaderRelay.applyInsideHeader(template);

        log.info(">>> OAuth2FeignRequestInterceptor - end.");
    }

    private void relayHeader(RequestTemplate template) {
        HttpServletRequest request = RequestContextHolder.getRequest().orElse(null);
        if (request == null) {
            return;
        }

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!FeignHeaderRelay.shouldRelay(headerName)) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(headerName);
            List<String> headerValues = Collections.list(values);
            log.info(">>> OAuth2FeignRequestInterceptor - relay header >> set key={}, values={}",
                    headerName, headerValues);
            template.header(headerName, headerValues);
        }
    }
}
