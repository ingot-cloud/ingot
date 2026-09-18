package com.ingot.framework.commons.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>BFF Cookie 名称与 Set-Cookie 属性随 requireHttps 切换。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class BffCookiePolicyTest {

    @Test
    void hostPrefixWhenRequireHttps() {
        assertEquals(BffCookiePolicy.SESSION_COOKIE_HOST, BffCookiePolicy.sessionCookieName(true));
        assertEquals(BffCookiePolicy.BINDING_COOKIE_HOST, BffCookiePolicy.bindingCookieName(true));
        String header = BffCookiePolicy.setCookieHeader(
                BffCookiePolicy.sessionCookieName(true), "sid", 60, true);
        assertTrue(header.startsWith("__Host-IN_SESSION=sid"));
        assertTrue(header.contains("Secure"));
        assertTrue(header.contains("HttpOnly"));
        assertTrue(header.contains("SameSite=Lax"));
        assertFalse(header.contains("Domain="));
    }

    @Test
    void plainNameWithoutSecureWhenHttpDev() {
        assertEquals(BffCookiePolicy.SESSION_COOKIE_PLAIN, BffCookiePolicy.sessionCookieName(false));
        assertEquals(BffCookiePolicy.BINDING_COOKIE_PLAIN, BffCookiePolicy.bindingCookieName(false));
        String header = BffCookiePolicy.setCookieHeader(
                BffCookiePolicy.sessionCookieName(false), "sid", 60, false);
        assertTrue(header.startsWith("IN_SESSION=sid"));
        assertFalse(header.contains("Secure"));
        assertTrue(header.contains("Path=/"));
    }
}
