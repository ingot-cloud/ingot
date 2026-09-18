package com.ingot.cloud.gateway.filter;

import java.util.List;

import com.ingot.cloud.gateway.config.GatewayBffProperties;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Host 匹配应用注册表并注入内部头。
 *
 * @author jy
 * @since 1.0.0
 */
class BffAppContextFilterTest {

    @Test
    void injectsAppIdAndLoginEntry() {
        BffAppContextFilter filter = new BffAppContextFilter(properties());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/bff/auth/platform/login")
                        .header("Host", "localhost:1798")
                        .build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertEquals("platform-admin",
                captor.getValue().getRequest().getHeaders().getFirst(HeaderConstants.INNER_BFF_APP_ID));
        assertEquals(BffConstants.ENTRY_LOGIN,
                captor.getValue().getRequest().getHeaders().getFirst(HeaderConstants.INNER_BFF_ENTRY));
    }

    @Test
    void unknownHostIsForbidden() {
        BffAppContextFilter filter = new BffAppContextFilter(properties());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/bff/auth/me")
                        .header("Host", "evil.example")
                        .build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verifyNoInteractions(chain);
    }

    @Test
    void emptyRegistrySkips() {
        BffAppContextFilter filter = new BffAppContextFilter(new GatewayBffProperties());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/bff/auth/me").build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertNull(exchange.getRequest().getHeaders().getFirst(HeaderConstants.INNER_BFF_APP_ID));
    }

    private static GatewayBffProperties properties() {
        BffAppRegistration app = new BffAppRegistration();
        app.setAppId("platform-admin");
        app.setDomain(AuthorizationDomain.PLATFORM);
        app.setAdminOrigin("http://localhost:5798");
        app.setLoginOrigin("http://localhost:1798");
        GatewayBffProperties properties = new GatewayBffProperties();
        properties.setApps(List.of(app));
        return properties;
    }
}
