package com.ingot.cloud.gateway.security;

import com.ingot.framework.gateway.rule.client.challenge.ChallengePolicyService;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengeTrigger;
import com.ingot.framework.vc.common.VCConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
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
 * {@link ChallengeFilter} 按请求 Header scope 消费 PassToken，并绑定策略路径。
 *
 * @author jy
 * @since 1.0.0
 */
class ChallengeFilterTest {

    private final ChallengePolicyService challengeService = mock(ChallengePolicyService.class);
    private final PassTokenStore passTokenStore = mock(PassTokenStore.class);
    private final ReactiveResponseWriter responseWriter = mock(ReactiveResponseWriter.class);
    private ChallengeFilter filter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<ChallengePolicyService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(challengeService);
        filter = new ChallengeFilter(provider, passTokenStore, responseWriter);
        when(responseWriter.writeJson(any(), any(), any())).thenReturn(Mono.empty());
    }

    @Test
    void consume_usesRequestScope_setsPassTokenOk() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/pms/user")
                        .header(VCConstants.HEADER_PASS_TOKEN, "tok")
                        .header(VCConstants.HEADER_SCOPE, "e2e-anon")
                        .build());
        when(challengeService.match(eq("/pms/user"), any(), eq(ChallengeTrigger.ALWAYS))).thenReturn(null);
        when(challengeService.matchByScope(eq("/pms/user"), any(), eq("e2e-anon"))).thenReturn(anonPolicy());
        when(passTokenStore.consume("e2e-anon", "tok")).thenReturn(Mono.just(true));
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(passTokenStore).consume("e2e-anon", "tok");
        verify(chain).filter(exchange);
        assertEquals(Boolean.TRUE, exchange.getAttributes().get(ChallengeFilter.ATTR_PASS_TOKEN_OK));
    }

    @Test
    void consume_loginTokenOnUnrelatedPath_doesNotConsume() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/pms/user")
                        .header(VCConstants.HEADER_PASS_TOKEN, "tok")
                        .header(VCConstants.HEADER_SCOPE, "login")
                        .build());
        when(challengeService.match(eq("/pms/user"), any(), eq(ChallengeTrigger.ALWAYS))).thenReturn(null);
        when(challengeService.matchByScope(eq("/pms/user"), any(), eq("login"))).thenReturn(null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(passTokenStore, never()).consume(any(), any());
        verify(chain).filter(exchange);
        assertNull(exchange.getAttributes().get(ChallengeFilter.ATTR_PASS_TOKEN_OK));
    }

    @Test
    void consume_loginPath_consumesLoginScope() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/bff/auth/login")
                        .header(VCConstants.HEADER_PASS_TOKEN, "tok")
                        .header(VCConstants.HEADER_SCOPE, "login")
                        .build());
        when(challengeService.match(eq("/bff/auth/login"), any(), eq(ChallengeTrigger.ALWAYS)))
                .thenReturn(alwaysLogin());
        when(challengeService.matchByScope(eq("/bff/auth/login"), any(), eq("login"))).thenReturn(alwaysLogin());
        when(passTokenStore.consume("login", "tok")).thenReturn(Mono.just(true));
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(passTokenStore).consume("login", "tok");
        verify(chain).filter(exchange);
        assertEquals(Boolean.TRUE, exchange.getAttributes().get(ChallengeFilter.ATTR_PASS_TOKEN_OK));
    }

    @Test
    void consume_queryOnly_isIgnoredAsNoToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/pms/user")
                        .queryParam("_vc_pass_token", "tok")
                        .queryParam("_vc_scope", "e2e-anon")
                        .build());
        when(challengeService.match(eq("/pms/user"), any(), eq(ChallengeTrigger.ALWAYS))).thenReturn(null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(passTokenStore, never()).consume(any(), any());
        verify(chain).filter(exchange);
        assertNull(exchange.getAttributes().get(ChallengeFilter.ATTR_PASS_TOKEN_OK));
    }

    @Test
    void consume_missingScopeWithoutAlways_doesNotSkipSentinel() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/pms/user")
                        .header(VCConstants.HEADER_PASS_TOKEN, "tok")
                        .build());
        when(challengeService.match(eq("/pms/user"), any(), eq(ChallengeTrigger.ALWAYS))).thenReturn(null);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(passTokenStore, never()).consume(any(), any());
        verify(chain).filter(exchange);
        assertNull(exchange.getAttributes().get(ChallengeFilter.ATTR_PASS_TOKEN_OK));
    }

    @Test
    void consume_missingScopeWithAlways_returns412() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/bff/auth/login")
                        .header(VCConstants.HEADER_PASS_TOKEN, "tok")
                        .build());
        when(challengeService.match(eq("/bff/auth/login"), any(), eq(ChallengeTrigger.ALWAYS)))
                .thenReturn(alwaysLogin());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        filter.filter(exchange, chain).block();

        verify(passTokenStore, never()).consume(any(), any());
        verify(chain, never()).filter(any());
        verify(responseWriter).writeJson(any(), eq(HttpStatus.PRECONDITION_FAILED), any());
    }

    @Test
    void vcPath_skipsAlways() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/vc/image/check").build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(challengeService, never()).match(any(), any(), any());
        verify(chain).filter(exchange);
    }

    @Test
    void whitelist_skipsChallenge() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/bff/auth/login").build());
        exchange.getAttributes().put(BlacklistFilter.ATTR_WHITELISTED, Boolean.TRUE);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(challengeService, never()).match(any(), any(), any());
        verify(chain).filter(exchange);
    }

    private static ChallengePolicy alwaysLogin() {
        return ChallengePolicy.builder()
                .code("login-always")
                .trigger(ChallengeTrigger.ALWAYS)
                .challengeType("SLIDER")
                .scope("login")
                .passTokenTtlSec(300)
                .passTokenRemaining(3)
                .enabled(true)
                .build();
    }

    private static ChallengePolicy anonPolicy() {
        return ChallengePolicy.builder()
                .code("anon-rl")
                .trigger(ChallengeTrigger.ON_RATE_LIMIT)
                .challengeType("SLIDER")
                .scope("e2e-anon")
                .enabled(true)
                .build();
    }
}
