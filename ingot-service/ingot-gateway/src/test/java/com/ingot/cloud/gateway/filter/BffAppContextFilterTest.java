package com.ingot.cloud.gateway.filter;

import java.util.List;
import com.ingot.cloud.gateway.config.GatewayBffProperties;
import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>
 * 精确Host/端口映射与不可信内部头清理。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
class BffAppContextFilterTest {

    static GatewayBffProperties properties() {
        var p = new GatewayBffProperties();
        p.setApps(List.of(app("platform-admin", AuthorizationDomain.PLATFORM, "https://127.0.0.1:8443"),
                app("tenant-admin", AuthorizationDomain.TENANT, "https://127.0.0.1:9443")));
        p.validateProductionRegistry();
        return p;
    }

    static BffAppRegistration app(String id, AuthorizationDomain domain, String origin) {
        var a = new BffAppRegistration();
        a.setAppId(id);
        a.setDomain(domain);
        a.setOrigin(origin);
        a.setOauthClientId(id + "-client");
        a.setOauthRedirectUri(origin + "/callback");
        return a;
    }

    @Test
    void appForAllRegisteredRequestsOverridesForgery() {
        var x = MockServerWebExchange.from(MockServerHttpRequest.get("/iam/v1/me")
            .header("Host", "127.0.0.1:9443")
            .header(HeaderConstants.INNER_BFF_APP_ID, "platform-admin")
            .header(HeaderConstants.INNER_BFF_ENTRY, "login")
            .header("X-Forwarded-Host", "127.0.0.1:8443"));
        new BffAppContextFilter(properties()).filter(x, e -> {
            assertEquals("tenant-admin", e.getRequest().getHeaders().getFirst(HeaderConstants.INNER_BFF_APP_ID));
            assertNull(e.getRequest().getHeaders().getFirst(HeaderConstants.INNER_BFF_ENTRY));
            return Mono.empty();
        }).block();
    }

    @Test
    void unknownHostAndWrongPortFailClosed() {
        for (String host : List.of("evil.example", "127.0.0.1:8444", "127.0.0.1:8443/path")) {
            var x = MockServerWebExchange.from(MockServerHttpRequest.get("/bff/auth/me").header("Host", host));
            new BffAppContextFilter(properties()).filter(x, e -> {
                fail("unknown host was routed");
                return Mono.empty();
            }).block();
            assertEquals(HttpStatus.FORBIDDEN, x.getResponse().getStatusCode());
        }
    }

    @Test
    void otherBearerClientsRetainRouteButNoInjectedIdentity() {
        var x = MockServerWebExchange.from(MockServerHttpRequest.get("/other")
            .header("Host", "api.example")
            .header(HeaderConstants.INNER_BFF_APP_ID, "platform-admin"));
        new BffAppContextFilter(properties()).filter(x, e -> {
            assertNull(e.getRequest().getHeaders().getFirst(HeaderConstants.INNER_BFF_APP_ID));
            return Mono.empty();
        }).block();
    }

}
