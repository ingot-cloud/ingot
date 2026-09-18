package com.ingot.cloud.bff.config;

import java.util.List;

import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>生产注册表启动校验：空表、HTTP origin 失败；DEV 允许空表与 HTTP。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class BffAppRegistryTest {

    @Test
    void requireHttpsRejectsEmptyApps() {
        BffProperties properties = new BffProperties();
        properties.setRequireHttps(true);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new BffAppRegistry(properties).validate());
        assertTrue(error.getMessage().contains("不能为空"));
    }

    @Test
    void requireHttpsRejectsHttpOrigin() {
        BffProperties properties = new BffProperties();
        properties.setRequireHttps(true);
        properties.setApps(List.of(completeApp(
                "http://admin.ingotcloud.top", "https://login.ingotcloud.top")));
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new BffAppRegistry(properties).validate());
        assertTrue(error.getMessage().contains("HTTPS"));
    }

    @Test
    void requireHttpsAllowsHttpsSameDomain() {
        BffProperties properties = new BffProperties();
        properties.setRequireHttps(true);
        properties.setApps(List.of(completeApp(
                "https://admin.ingotcloud.top", "https://login.ingotcloud.top")));
        assertDoesNotThrow(() -> new BffAppRegistry(properties).validate());
    }

    @Test
    void localHttpAllowsEmptyAndHttpOrigins() {
        BffProperties properties = new BffProperties();
        properties.setRequireHttps(false);
        assertDoesNotThrow(() -> new BffAppRegistry(properties).validate());
        properties.setApps(List.of(completeApp(
                "http://localhost:5798", "http://localhost:1798")));
        assertDoesNotThrow(() -> new BffAppRegistry(properties).validate());
    }

    private static BffAppRegistration completeApp(String adminOrigin, String loginOrigin) {
        BffAppRegistration app = new BffAppRegistration();
        app.setAppId("tenant-admin");
        app.setDomain(AuthorizationDomain.TENANT);
        app.setAdminOrigin(adminOrigin);
        app.setLoginOrigin(loginOrigin);
        app.setOauthClientId("in-bff-tenant");
        app.setOauthRedirectUri("http://localhost:5400/bff/auth/tenant/callback");
        return app;
    }
}
