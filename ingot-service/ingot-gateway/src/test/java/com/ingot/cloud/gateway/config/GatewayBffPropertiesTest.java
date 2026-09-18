package com.ingot.cloud.gateway.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>生产 Gateway 拒绝空 BFF 注册表。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class GatewayBffPropertiesTest {

    @Test
    void requireHttpsRejectsEmptyApps() {
        GatewayBffProperties properties = new GatewayBffProperties();
        properties.setRequireHttps(true);
        assertThrows(IllegalStateException.class, properties::validateProductionRegistry);
    }

    @Test
    void localHttpAllowsEmptyApps() {
        GatewayBffProperties properties = new GatewayBffProperties();
        properties.setRequireHttps(false);
        assertDoesNotThrow(properties::validateProductionRegistry);
    }
}
