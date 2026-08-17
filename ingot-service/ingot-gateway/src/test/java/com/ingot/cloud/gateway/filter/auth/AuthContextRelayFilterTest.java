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
 * {@link AuthContextRelayFilter} 瘦身 JWT 经 OnlineToken 补全 userType。
 *
 * @author jy
 * @since 1.0.0
 */
class AuthContextRelayFilterTest {

    @Test
    void slimJwt_enrichesUserTypeFromOnlineToken() {
        ReactiveOnlineTokenUserTypeReader reader = mock(ReactiveOnlineTokenUserTypeReader.class);
        when(reader.readUserType("slim-jti")).thenReturn(Mono.just("0"));
        AuthContextRelayFilter filter = new AuthContextRelayFilter(reader);

        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"i\":9,\"jti\":\"slim-jti\"}".getBytes(StandardCharsets.UTF_8));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/pms/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer hdr." + payload + ".sig")
                        .build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        assertEquals("9", exchange.getAttributes().get(AuthContextAttributes.USER_ID));
        assertEquals("0", exchange.getAttributes().get(AuthContextAttributes.USER_TYPE));
        verify(chain).filter(exchange);
    }

    @Test
    void slimJwt_onlineTokenMiss_leavesUserTypeEmpty() {
        ReactiveOnlineTokenUserTypeReader reader = mock(ReactiveOnlineTokenUserTypeReader.class);
        when(reader.readUserType("slim-jti")).thenReturn(Mono.empty());
        AuthContextRelayFilter filter = new AuthContextRelayFilter(reader);

        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"i\":9,\"jti\":\"slim-jti\"}".getBytes(StandardCharsets.UTF_8));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/pms/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer hdr." + payload + ".sig")
                        .build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        assertEquals("9", exchange.getAttributes().get(AuthContextAttributes.USER_ID));
        assertNull(exchange.getAttributes().get(AuthContextAttributes.USER_TYPE));
        verify(chain).filter(exchange);
    }

    @Test
    void legacyJwt_writesUserTypeWithoutRedis() {
        ReactiveOnlineTokenUserTypeReader reader = mock(ReactiveOnlineTokenUserTypeReader.class);
        AuthContextRelayFilter filter = new AuthContextRelayFilter(reader);

        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"i\":9,\"ut\":\"1\"}".getBytes(StandardCharsets.UTF_8));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/pms/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer hdr." + payload + ".sig")
                        .build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        assertEquals("9", exchange.getAttributes().get(AuthContextAttributes.USER_ID));
        assertEquals("1", exchange.getAttributes().get(AuthContextAttributes.USER_TYPE));
        verify(chain).filter(exchange);
        verifyNoInteractions(reader);
    }
}
