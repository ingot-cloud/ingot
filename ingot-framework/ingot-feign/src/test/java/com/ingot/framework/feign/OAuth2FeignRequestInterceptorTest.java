package com.ingot.framework.feign;

import static org.assertj.core.api.Assertions.assertThat;

import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.constants.SecurityConstants;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * {@link OAuth2FeignRequestInterceptor} 与 WebFlux 拦截器共用 {@link FeignHeaderRelay}。
 */
class OAuth2FeignRequestInterceptorTest {

    private final OAuth2FeignRequestInterceptor interceptor = new OAuth2FeignRequestInterceptor();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void relaysIdentityHeadersFromServletRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token");
        request.addHeader(HeaderConstants.INNER_CLIENT_REAL_IP, "192.168.0.190");
        request.addHeader(HeaderConstants.BFF_DEVICE_FINGERPRINT_HEADER, "fp-1");
        request.addHeader("X-Trace", "ignored");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get(HttpHeaders.AUTHORIZATION)).containsExactly("Bearer token");
        assertThat(template.headers().get(HeaderConstants.INNER_CLIENT_REAL_IP)).containsExactly("192.168.0.190");
        assertThat(template.headers().get(HeaderConstants.BFF_DEVICE_FINGERPRINT_HEADER)).containsExactly("fp-1");
        assertThat(template.headers().get("X-Trace")).isNull();
        assertThat(template.headers().get(SecurityConstants.HEADER_FROM))
                .containsExactly(SecurityConstants.HEADER_FROM_INSIDE_VALUE);
    }
}
