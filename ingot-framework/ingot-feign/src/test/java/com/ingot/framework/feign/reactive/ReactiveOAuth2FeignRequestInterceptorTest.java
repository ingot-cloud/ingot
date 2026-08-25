package com.ingot.framework.feign.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import com.ingot.framework.commons.constants.HeaderConstants;
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
    void relaysIdentityHeadersFromExchange() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/demo")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .header(FeignHeaderRelay.LEGACY_DEVICE_ID_HEADER, "dev-1")
                        .header(HeaderConstants.INNER_CLIENT_REAL_IP, "192.168.0.190")
                        .header(HeaderConstants.INNER_USER_ID, "42")
                        .header(HeaderConstants.INNER_CLIENT_ID, "web")
                        .header(HeaderConstants.BFF_DEVICE_FINGERPRINT_HEADER, "fp-1")
                        .header(HeaderConstants.SECURITY_FROM, "spoofed")
                        .header("X-Trace", "ignored")
                        .build());
        FeignReactiveContextHolder.setExchange(exchange);

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get(HttpHeaders.AUTHORIZATION)).containsExactly("Bearer token");
        assertThat(template.headers().get(FeignHeaderRelay.LEGACY_DEVICE_ID_HEADER)).containsExactly("dev-1");
        assertThat(template.headers().get(HeaderConstants.INNER_CLIENT_REAL_IP)).containsExactly("192.168.0.190");
        assertThat(template.headers().get(HeaderConstants.INNER_USER_ID)).containsExactly("42");
        assertThat(template.headers().get(HeaderConstants.INNER_CLIENT_ID)).containsExactly("web");
        assertThat(template.headers().get(HeaderConstants.BFF_DEVICE_FINGERPRINT_HEADER)).containsExactly("fp-1");
        assertThat(template.headers().get("X-Trace")).isNull();
        assertThat(template.headers().get(SecurityConstants.HEADER_FROM))
                .containsExactly(SecurityConstants.HEADER_FROM_INSIDE_VALUE);
    }

    @Test
    void relayHeaderListCoversIdentityPropagation() {
        assertThat(FeignHeaderRelay.RELAY_HEADERS).contains(
                HttpHeaders.AUTHORIZATION.toLowerCase(Locale.ROOT),
                FeignHeaderRelay.LEGACY_DEVICE_ID_HEADER,
                HeaderConstants.INNER_CLIENT_REAL_IP.toLowerCase(Locale.ROOT),
                HeaderConstants.INNER_USER_ID.toLowerCase(Locale.ROOT),
                HeaderConstants.INNER_CLIENT_ID.toLowerCase(Locale.ROOT),
                HeaderConstants.BFF_DEVICE_FINGERPRINT_HEADER.toLowerCase(Locale.ROOT));
        assertThat(FeignHeaderRelay.RELAY_HEADERS)
                .doesNotContain(HeaderConstants.SECURITY_FROM.toLowerCase(Locale.ROOT));
    }
}
