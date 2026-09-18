package com.ingot.cloud.bff.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.bff.config.BffProperties;
import com.ingot.framework.commons.utils.BffCookiePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * <p>BFF 写出的 Set-Cookie 随 requireHttps 切换名称与 Secure。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class BffSessionServiceCookieTest {

    @Test
    void writesHostCookieWhenRequireHttps() {
        String header = writeBinding(true);
        assertTrue(header.startsWith(BffCookiePolicy.BINDING_COOKIE_HOST + "="));
        assertTrue(header.contains("Secure"));
    }

    @Test
    void writesPlainCookieWhenLocalHttp() {
        String header = writeBinding(false);
        assertTrue(header.startsWith(BffCookiePolicy.BINDING_COOKIE_PLAIN + "="));
        assertFalse(header.contains("Secure"));
    }

    private static String writeBinding(boolean requireHttps) {
        BffProperties properties = new BffProperties();
        properties.setRequireHttps(requireHttps);
        BffSessionService service = new BffSessionService(
                mock(StringRedisTemplate.class), properties, new ObjectMapper());
        MockHttpServletResponse response = new MockHttpServletResponse();
        service.writeBindingCookie("bind-1", 60, response);
        return response.getHeader("Set-Cookie");
    }
}
