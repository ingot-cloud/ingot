package com.ingot.cloud.gateway.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.ingot.cloud.gateway.filter.auth.AuthContextAttributes;
import com.ingot.framework.commons.constants.RedisKeyConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AccountLockFilter} 消费上游身份链路解析结果的单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class AccountLockFilterTest {

    private final ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
    private final ReactiveResponseWriter responseWriter = mock(ReactiveResponseWriter.class);
    private AccountLockFilter filter;

    @BeforeEach
    void setUp() {
        AccountLockGatewayProperties properties = new AccountLockGatewayProperties();
        properties.setEnabled(true);
        ObjectProvider<ReactiveStringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(redis);
        filter = new AccountLockFilter(properties, provider, responseWriter);
        when(responseWriter.writeJson(any(), any(), any())).thenReturn(Mono.empty());
    }

    @Test
    void identityWithUserType_lockHit_returns403() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/iam/user").build());
        exchange.getAttributes().put(GatewaySecurityConstants.ATTR_CLIENT_IDENTITY,
                ClientIdentity.builder().userId("9").userType("0").build());
        when(redis.hasKey(RedisKeyConstants.AccountLock.uidKey("0", 9L))).thenReturn(Mono.just(true));

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any());
        verify(responseWriter).writeJson(any(), org.mockito.ArgumentMatchers.eq(HttpStatus.FORBIDDEN), any());
    }

    @Test
    void jwtWithoutResolvedUserType_passesThrough() {
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"i\":9,\"sid\":\"session-1\"}".getBytes(StandardCharsets.UTF_8));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/iam/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer hdr." + payload + ".sig")
                        .build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(redis, never()).hasKey(any());
    }

    @Test
    void attributeUserType_lockMiss_passesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/iam/user").build());
        exchange.getAttributes().put(AuthContextAttributes.USER_ID, "9");
        exchange.getAttributes().put(AuthContextAttributes.USER_TYPE, "0");
        when(redis.hasKey(RedisKeyConstants.AccountLock.uidKey("0", 9L))).thenReturn(Mono.just(false));

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }
}
