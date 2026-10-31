package com.ingot.framework.commons.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>
 * 应用Cookie名和host-only属性契约。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
class BffCookiePolicyTest {

    @Test
    void separateApplicationNamesAndHttpsAttributes() {
        assertEquals("__Host-IN_PLATFORM_SESSION", BffCookiePolicy.sessionCookieName(true, "platform-admin"));
        assertEquals("__Host-IN_TENANT_BINDING", BffCookiePolicy.bindingCookieName(true, "tenant-admin"));
        String h = BffCookiePolicy.setCookieHeader(BffCookiePolicy.sessionCookieName(true, "tenant-admin"), "opaque",
                60, true);
        assertTrue(h.contains("Secure"));
        assertTrue(h.contains("HttpOnly"));
        assertTrue(h.contains("Path=/"));
        assertTrue(h.contains("SameSite=Lax"));
        assertFalse(h.contains("Domain="));
    }

    @Test
    void plainCookiesOnlyWhenConfiguredAndNoUnknownApplication() {
        assertEquals("IN_TENANT_SESSION", BffCookiePolicy.sessionCookieName(false, "tenant-admin"));
        assertFalse(BffCookiePolicy.setCookieHeader("IN_TENANT_SESSION", "value", 0, false).contains("Secure"));
        assertThrows(IllegalArgumentException.class, () -> BffCookiePolicy.sessionCookieName(true, "untrusted"));
    }

    @Test
    void stableStateAndFingerprintJsonContractRejectsUnknownValues() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        assertEquals("\"COMPLETED\"",
                mapper.writeValueAsString(com.ingot.framework.commons.model.bff.BffLoginStage.COMPLETED));
        assertEquals(com.ingot.framework.commons.model.bff.BffLoginStage.SELECT_TENANT,
                mapper.readValue("\"SELECT_TENANT\"", com.ingot.framework.commons.model.bff.BffLoginStage.class));
        assertEquals("\"ip_ua\"",
                mapper.writeValueAsString(com.ingot.framework.commons.model.bff.BffFingerprintMode.IPUA));
        assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class,
                () -> mapper.readValue("\"READY\"", com.ingot.framework.commons.model.bff.BffLoginStage.class));
        assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class,
                () -> mapper.readValue("\"fallback\"", com.ingot.framework.commons.model.bff.BffFingerprintMode.class));
    }

}
