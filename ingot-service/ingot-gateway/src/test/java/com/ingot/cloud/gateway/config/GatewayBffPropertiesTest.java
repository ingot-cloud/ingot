package com.ingot.cloud.gateway.config;

import java.util.List;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.utils.BffOriginPolicy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>
 * 同源双入口配置启动门禁，接受HTTPS IP，拒绝四站与非loopback HTTP。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
class GatewayBffPropertiesTest {

    private BffAppRegistration app(String id, AuthorizationDomain domain, String origin) {
        var a = new BffAppRegistration();
        a.setAppId(id);
        a.setDomain(domain);
        a.setOrigin(origin);
        a.setOauthClientId(id);
        a.setOauthRedirectUri("https://auth.example/callback");
        return a;
    }

    @Test
    void twoHttpsIpPortsAreValid() {
        var p = new GatewayBffProperties();
        p.setApps(List.of(app("platform-admin", AuthorizationDomain.PLATFORM, "https://192.168.1.10:8443"),
                app("tenant-admin", AuthorizationDomain.TENANT, "https://192.168.1.10:9443")));
        assertDoesNotThrow(p::validateProductionRegistry);
    }

    @Test
    void rejectsMissingDuplicateAndProductionHttp() {
        var p = new GatewayBffProperties();
        assertThrows(IllegalStateException.class, p::validateProductionRegistry);
        p.setApps(List.of(app("platform-admin", AuthorizationDomain.PLATFORM, "https://p.example"),
                app("tenant-admin", AuthorizationDomain.TENANT, "https://p.example:443")));
        assertThrows(IllegalStateException.class, p::validateProductionRegistry);
        p.setRequireHttps(false);
        p.setApps(List.of(app("platform-admin", AuthorizationDomain.PLATFORM, "http://192.168.1.10:8443"),
                app("tenant-admin", AuthorizationDomain.TENANT, "http://192.168.1.10:9443")));
        assertThrows(IllegalStateException.class, p::validateProductionRegistry);
    }

    @Test
    void devMustBeLoopbackAndOriginHasNoUserPathQuery() {
        var p = new GatewayBffProperties();
        p.setRequireHttps(false);
        p.setApps(List.of(app("platform-admin", AuthorizationDomain.PLATFORM, "http://platform.localhost:5799"),
                app("tenant-admin", AuthorizationDomain.TENANT, "http://tenant.localhost:5798")));
        assertDoesNotThrow(p::validateProductionRegistry);
        for (String invalid : List.of("null", "https://p.example/login", "https://user@p.example",
                "https://p.example?q=1", "https://p.example:70000"))
            assertThrows(IllegalArgumentException.class, () -> BffOriginPolicy.normalize(invalid));
        assertEquals("https://p.example", BffOriginPolicy.normalize("https://P.EXAMPLE:443/"));
    }

}
