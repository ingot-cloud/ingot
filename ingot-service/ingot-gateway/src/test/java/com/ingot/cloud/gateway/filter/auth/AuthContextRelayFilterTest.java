package com.ingot.cloud.gateway.filter.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.ingot.cloud.gateway.filter.auth.internal.ReactiveOnlineTokenUserTypeReader;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link AuthContextRelayFilter} 按 sid 从会话补全 userType。
 *
 * @author jy
 * @since 1.0.0
 */
class AuthContextRelayFilterTest {

    private static final String SID = "session-1";

    @Test
    void slimJwt_enrichesUserTypeFromSession() {
        ReactiveOnlineTokenUserTypeReader reader = mock(ReactiveOnlineTokenUserTypeReader.class);
        when(reader.readUserType(SID)).thenReturn(Mono.just("0"));
        AuthContextRelayFilter filter = new AuthContextRelayFilter(reader);

        MockServerWebExchange exchange = exchangeWithPayload("{\"i\":9,\"sid\":\"" + SID + "\"}");
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        assertEquals("9", exchange.getAttributes().get(AuthContextAttributes.USER_ID));
        assertEquals("0", exchange.getAttributes().get(AuthContextAttributes.USER_TYPE));
        verify(chain).filter(exchange);
    }

    @Test
    void slimJwt_sessionMiss_leavesUserTypeEmpty() {
        ReactiveOnlineTokenUserTypeReader reader = mock(ReactiveOnlineTokenUserTypeReader.class);
        when(reader.readUserType(SID)).thenReturn(Mono.empty());
        AuthContextRelayFilter filter = new AuthContextRelayFilter(reader);

        MockServerWebExchange exchange = exchangeWithPayload("{\"i\":9,\"sid\":\"" + SID + "\"}");
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        assertEquals("9", exchange.getAttributes().get(AuthContextAttributes.USER_ID));
        assertNull(exchange.getAttributes().get(AuthContextAttributes.USER_TYPE));
        verify(chain).filter(exchange);
    }

    @Test
    void jwtWithoutSid_doesNotTouchRedis() {
        ReactiveOnlineTokenUserTypeReader reader = mock(ReactiveOnlineTokenUserTypeReader.class);
        AuthContextRelayFilter filter = new AuthContextRelayFilter(reader);

        MockServerWebExchange exchange = exchangeWithPayload("{\"i\":9}");
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        assertEquals("9", exchange.getAttributes().get(AuthContextAttributes.USER_ID));
        assertNull(exchange.getAttributes().get(AuthContextAttributes.USER_TYPE));
        verify(chain).filter(exchange);
        verifyNoInteractions(reader);
    }

    private MockServerWebExchange exchangeWithPayload(String claimsJson) {
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(claimsJson.getBytes(StandardCharsets.UTF_8));
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/iam/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer hdr." + payload + ".sig")
                        .build());
    }
}
