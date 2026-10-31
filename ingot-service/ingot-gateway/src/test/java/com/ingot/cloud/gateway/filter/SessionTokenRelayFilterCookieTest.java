package com.ingot.cloud.gateway.filter;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.constants.*;
import com.ingot.framework.commons.model.bff.BffSession;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <p>
 * 双端口只消费当前应用Cookie，拒绝无效sid/域，并清匿名端点Bearer。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
class SessionTokenRelayFilterCookieTest {

    private static final String ID = "abcdefghijklmnopqrstuv1234";

    @Test
    void ignoresOtherApplicationAndLegacyCookies() {
        var redis = mock(ReactiveStringRedisTemplate.class);
        var x = MockServerWebExchange.from(MockServerHttpRequest.get("/iam/me")
            .header("Host", "127.0.0.1:8443")
            .cookie(new HttpCookie("__Host-IN_TENANT_SESSION", ID), new HttpCookie("__Host-IN_SESSION", ID)));
        new SessionTokenRelayFilter(redis, new ObjectMapper(), BffAppContextFilterTest.properties()).filter(x, e -> {
            assertNull(e.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            return Mono.empty();
        }).block();
        verifyNoInteractions(redis);
    }

    @Test
    void stripsBearerForAnonymousLoginAndMe() {
        for (String path : List.of(BffConstants.CSRF, BffConstants.PLATFORM_LOGIN, BffConstants.TENANT_SELECT,
                BffConstants.ME, BffConstants.PLATFORM_TRANSACTIONS + "/" + ID + "/resume")) {
            var redis = mock(ReactiveStringRedisTemplate.class);
            var x = MockServerWebExchange
                .from(MockServerHttpRequest.post(path).header(HttpHeaders.AUTHORIZATION, "Bearer attacker"));
            new SessionTokenRelayFilter(redis, new ObjectMapper(), BffAppContextFilterTest.properties())
                .filter(x, e -> {
                    assertNull(e.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
                    return Mono.empty();
                })
                .block();
            verifyNoInteractions(redis);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void relayConfirmsDeliveryAndOnlyUsesPlatformKey() throws Exception {
        var mapper = new ObjectMapper();
        var redis = mock(ReactiveStringRedisTemplate.class);
        var session = new BffSession();
        session.setAppId("platform-admin");
        session.setSessionId(ID);
        session.setBindingId(ID);
        session.setDomain("PLATFORM");
        session.setClientId("platform-admin-client");
        session.setSid("internal-sid");
        session.setAccessToken("server-token");
        String json = mapper.writeValueAsString(session);
        doAnswer(i -> {
            DefaultRedisScript script = i.getArgument(0);
            List<String> keys = i.getArgument(1);
            assertEquals(BffRedisKeys.session("platform-admin", ID), keys.getFirst());
            return script.getResultType() == String.class ? Flux.just(json) : Flux.just(1L);
        }).when(redis).execute(any(DefaultRedisScript.class), anyList(), anyList());
        when(redis.hasKey(RedisKeyConstants.OnlineToken.sidKey("internal-sid"))).thenReturn(Mono.just(true));
        var x = MockServerWebExchange.from(MockServerHttpRequest.get("/iam/me")
            .header("Host", "127.0.0.1:8443")
            .cookie(new HttpCookie("__Host-IN_PLATFORM_SESSION", ID), new HttpCookie("__Host-IN_TENANT_SESSION", ID)));
        var f = new SessionTokenRelayFilter(redis, mapper, BffAppContextFilterTest.properties());
        f.filter(x, e -> {
            assertEquals("Bearer server-token", e.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            return Mono.empty();
        }).block();
        when(redis.hasKey(anyString())).thenReturn(Mono.just(false));
        var invalid = MockServerWebExchange.from(MockServerHttpRequest.get("/iam/me")
            .header("Host", "127.0.0.1:8443")
            .cookie(new HttpCookie("__Host-IN_PLATFORM_SESSION", ID)));
        f.filter(invalid, e -> {
            fail("revoked sid routed");
            return Mono.empty();
        }).block();
        assertEquals(HttpStatus.UNAUTHORIZED, invalid.getResponse().getStatusCode());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void unavailableRedisIs503AndDoesNotClearCookies() {
        var redis = mock(ReactiveStringRedisTemplate.class);
        doReturn(Flux.error(new IllegalStateException("down"))).when(redis)
            .execute(any(DefaultRedisScript.class), anyList(), anyList());
        var x = MockServerWebExchange.from(MockServerHttpRequest.get("/iam/me")
            .header("Host", "127.0.0.1:8443")
            .cookie(new HttpCookie("__Host-IN_PLATFORM_SESSION", ID)));
        new SessionTokenRelayFilter(redis, new ObjectMapper(), BffAppContextFilterTest.properties()).filter(x, e -> {
            fail("unavailable Redis routed");
            return Mono.empty();
        }).block();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, x.getResponse().getStatusCode());
        assertTrue(x.getResponse().getCookies().isEmpty());
    }

    @Test
    void crossOriginCookieWritesFailBeforeReadingRedisButExplicitBearerIsUnchanged() {
        var redis = mock(ReactiveStringRedisTemplate.class);
        var filter = new SessionTokenRelayFilter(redis, new ObjectMapper(), BffAppContextFilterTest.properties());
        for (String origin : List.of("null", "https://127.0.0.1:9443", "https://evil.example")) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/iam/v1/mutation")
                .header("Host", "127.0.0.1:8443")
                .header("Origin", origin)
                .cookie(new HttpCookie("__Host-IN_PLATFORM_SESSION", ID)));
            filter.filter(exchange, e -> {
                fail("cross origin Cookie write routed");
                return Mono.empty();
            }).block();
            assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        }
        var explicit = MockServerWebExchange.from(MockServerHttpRequest.post("/oauth/client")
            .header("Host", "127.0.0.1:8443")
            .header("Authorization", "Basic explicit-client")
            .cookie(new HttpCookie("__Host-IN_PLATFORM_SESSION", ID)));
        filter.filter(explicit, e -> {
            assertEquals("Basic explicit-client", e.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            return Mono.empty();
        }).block();
        verifyNoInteractions(redis);
    }

}
