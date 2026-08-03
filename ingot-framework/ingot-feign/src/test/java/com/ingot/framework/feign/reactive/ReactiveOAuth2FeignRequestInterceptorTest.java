package com.ingot.framework.feign.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import com.ingot.framework.commons.constants.SecurityConstants;
import com.ingot.framework.feign.FeignHeaderRelay;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@link ReactiveOAuth2FeignRequestInterceptor} 行为验证。
 */
class ReactiveOAuth2FeignRequestInterceptorTest {

    private final ReactiveOAuth2FeignRequestInterceptor interceptor = new ReactiveOAuth2FeignRequestInterceptor();

    @AfterEach
    void tearDown() {
        FeignReactiveContextHolder.clear();
    }

    @Test
    void appliesInsideHeaderWithoutWebContext() {
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get(SecurityConstants.HEADER_FROM))
                .containsExactly(SecurityConstants.HEADER_FROM_INSIDE_VALUE);
    }

    @Test
    void relaysAuthorizationAndDeviceIdFromExchange() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/demo")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .header("deviceid", "dev-1")
                        .header("X-Trace", "ignored")
                        .build());
        FeignReactiveContextHolder.setExchange(exchange);

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get(HttpHeaders.AUTHORIZATION)).containsExactly("Bearer token");
        assertThat(template.headers().get("deviceid")).containsExactly("dev-1");
        assertThat(template.headers().get("X-Trace")).isNull();
        assertThat(template.headers().get(SecurityConstants.HEADER_FROM))
                .containsExactly(SecurityConstants.HEADER_FROM_INSIDE_VALUE);
    }

    @Test
    void relayHeaderListMatchesServletInterceptor() {
        assertThat(FeignHeaderRelay.RELAY_HEADERS)
                .containsExactly("authorization", "deviceid");
    }
}
