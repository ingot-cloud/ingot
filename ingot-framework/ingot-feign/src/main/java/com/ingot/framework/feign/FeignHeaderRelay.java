package com.ingot.framework.feign;

import java.util.List;
import java.util.Locale;

import com.ingot.framework.commons.constants.SecurityConstants;
import feign.RequestTemplate;
import org.springframework.http.HttpHeaders;

/**
 * Feign 请求头转发的共享约定，供 Servlet 与 WebFlux 拦截器复用。
 */
public final class FeignHeaderRelay {

    public static final List<String> RELAY_HEADERS = List.of(
            HttpHeaders.AUTHORIZATION.toLowerCase(Locale.ROOT),
            "deviceid");

    private FeignHeaderRelay() {
    }

    public static boolean shouldRelay(String headerName) {
        return headerName != null && RELAY_HEADERS.contains(headerName.toLowerCase(Locale.ROOT));
    }

    public static void applyInsideHeader(RequestTemplate template) {
        template.header(SecurityConstants.HEADER_FROM, SecurityConstants.HEADER_FROM_INSIDE_VALUE);
    }
}
