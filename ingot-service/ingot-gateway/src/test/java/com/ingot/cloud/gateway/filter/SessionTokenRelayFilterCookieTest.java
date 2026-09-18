package com.ingot.cloud.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.gateway.config.GatewayBffProperties;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.commons.utils.BffCookiePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>网关 JWT 中继按 requireHttps 读取对应会话 Cookie 名。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class SessionTokenRelayFilterCookieTest {

    @Test
    void relaysPlainSessionCookieWhenHttpsNotRequired() {
        GatewayBffProperties properties = new GatewayBffProperties();
        properties.setRequireHttps(false);
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> ops = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(CacheConstants.bffSessionKey("sid-1")))
                .thenReturn(Mono.just("{\"accessToken\":\"tok\",\"appId\":\"platform-admin\"}"));

        SessionTokenRelayFilter filter = new SessionTokenRelayFilter(redis, new ObjectMapper(), properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/iam/v1/me")
                        .cookie(new HttpCookie(BffCookiePolicy.SESSION_COOKIE_PLAIN, "sid-1"))
                        .build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertEquals("Bearer tok",
                captor.getValue().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void ignoresPlainCookieWhenHttpsRequired() {
        GatewayBffProperties properties = new GatewayBffProperties();
        properties.setRequireHttps(true);
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> ops = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);

        SessionTokenRelayFilter filter = new SessionTokenRelayFilter(redis, new ObjectMapper(), properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/iam/v1/me")
                        .cookie(new HttpCookie(BffCookiePolicy.SESSION_COOKIE_PLAIN, "sid-1"))
                        .build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(ops, never()).get(eq(CacheConstants.bffSessionKey("sid-1")));
        assertNull(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void relaysHostSessionCookieWhenHttpsRequired() {
        GatewayBffProperties properties = new GatewayBffProperties();
        properties.setRequireHttps(true);
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> ops = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(CacheConstants.bffSessionKey("sid-1")))
                .thenReturn(Mono.just("{\"accessToken\":\"tok\",\"appId\":\"platform-admin\"}"));

        SessionTokenRelayFilter filter = new SessionTokenRelayFilter(redis, new ObjectMapper(), properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/iam/v1/me")
                        .cookie(new HttpCookie(BffCookiePolicy.SESSION_COOKIE_HOST, "sid-1"))
                        .build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertEquals("Bearer tok",
                captor.getValue().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }
}
