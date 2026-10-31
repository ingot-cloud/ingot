package com.ingot.cloud.bff.service;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ingot.cloud.auth.api.model.vo.InnerSessionVO;
import com.ingot.cloud.auth.api.rpc.RemoteAuthSessionService;
import com.ingot.cloud.auth.api.rpc.RemoteAuthTokenService;
import com.ingot.cloud.bff.config.*;
import com.ingot.cloud.bff.error.BffAuthException;
import com.ingot.cloud.bff.model.LoginTransaction;
import com.ingot.cloud.bff.model.dto.*;
import com.ingot.framework.commons.constants.*;
import com.ingot.framework.commons.model.bff.*;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.utils.BffCookiePolicy;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import com.ingot.framework.security.oauth2.server.authorization.http.converter.CustomOAuth2AccessTokenResponseMapConverter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <p>
 * 真实隔离Redis上验证登录CAS、Cookie交付、回收与并发；Auth边界可控，不代替真实账号验收。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SameOriginLoginRedisTest {

    private static final long TEST_USER_ID = 9007199254740993L;

    private static final String PLATFORM_CLIENT_ID = "in-bff-platform";

    private static final String TENANT_CLIENT_ID = "in-bff-tenant";

    private final java.util.List<Process> processes = new java.util.ArrayList<>();

    private LettuceConnectionFactory connection;

    private StringRedisTemplate redis;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private BffProperties properties;

    private BffAppRegistry registry;

    private LoginTransactionService store;

    private BffSessionService sessions;

    private BffCleanupService cleanup;

    private BffAuthService service;

    private RemoteAuthTokenService auth;

    private RemoteAuthSessionService sid;

    private AccountLockSignalPort locks;

    private MutableClock clock;

    private final AtomicLong tokenIds = new AtomicLong();

    @BeforeAll
    void redisServer() throws Exception {
        boolean cluster = Boolean.parseBoolean(System.getenv("BFF_TEST_REDIS_CLUSTER"));
        var ports = new java.util.ArrayList<Integer>();
        for (int node = 0; node < (cluster ? 3 : 1); node++) {
            int port;
            do {
                try (ServerSocket free = new ServerSocket(0)) {
                    port = free.getLocalPort();
                }
            }
            while (port > 55000);
            ports.add(port);
            var directory = Files.createTempDirectory("ingot-login-redis-");
            String executable = System.getenv().getOrDefault("REDIS_SERVER", "/opt/homebrew/bin/redis-server");
            var command = new java.util.ArrayList<>(List.of(executable, "--bind", "127.0.0.1", "--port",
                    Integer.toString(port), "--save", "", "--appendonly", "no", "--dir", directory.toString()));
            if (cluster)
                command.addAll(List.of("--cluster-enabled", "yes", "--cluster-config-file", "nodes.conf",
                        "--cluster-node-timeout", "1000"));
            processes.add(new ProcessBuilder(command).redirectErrorStream(true)
                .redirectOutput(directory.resolve("redis.log").toFile())
                .start());
            boolean ready = false;
            for (int i = 0; i < 100; i++) {
                try (Socket ignored = new Socket("127.0.0.1", port)) {
                    ready = true;
                    break;
                }
                catch (java.io.IOException ignored) {
                    Thread.sleep(20);
                }
            }
            assertTrue(ready, "isolated redis failed to start");
        }
        if (cluster) {
            var create = new java.util.ArrayList<>(List.of("/opt/homebrew/bin/redis-cli", "--cluster", "create"));
            ports.forEach(port -> create.add("127.0.0.1:" + port));
            create.addAll(List.of("--cluster-replicas", "0", "--cluster-yes"));
            var output = Files.createTempFile("ingot-login-cluster-", ".log");
            var init = new ProcessBuilder(create).redirectErrorStream(true).redirectOutput(output.toFile()).start();
            assertTrue(init.waitFor(15, TimeUnit.SECONDS));
            assertEquals(0, init.exitValue(), Files.readString(output));
            connection = new LettuceConnectionFactory(
                    new org.springframework.data.redis.connection.RedisClusterConfiguration(
                            ports.stream().map(port -> "127.0.0.1:" + port).toList()));
        }
        else
            connection = new LettuceConnectionFactory("127.0.0.1", ports.getFirst());
        connection.afterPropertiesSet();
        connection.start();
        redis = new StringRedisTemplate(connection);
        redis.afterPropertiesSet();
    }

    @AfterAll
    void closeRedis() throws Exception {
        if (connection != null)
            connection.destroy();
        for (Process process : processes) {
            process.destroy();
            assertTrue(process.waitFor(5, TimeUnit.SECONDS));
        }
    }

    @BeforeEach
    void setup() {
        if (connection.isClusterAware()) {
            try (var c = connection.getClusterConnection()) {
                for (var node : c.clusterGetNodes())
                    if (node.isMaster())
                        c.serverCommands().flushDb(node);
            }
        }
        else
            try (var c = connection.getConnection()) {
                c.serverCommands().flushDb();
            }
        clock = new MutableClock();
        properties = new BffProperties();
        properties.setRequireHttps(true);
        properties.getSecurity().setFingerprintEnabled(false);
        properties.setApps(List.of(app("platform-admin", AuthorizationDomain.PLATFORM, "https://127.0.0.1:8443"),
                app("tenant-admin", AuthorizationDomain.TENANT, "https://127.0.0.1:9443")));
        registry = new BffAppRegistry(properties);
        registry.validate();
        properties.validate();
        store = new LoginTransactionService(redis, mapper, properties, clock);
        sid = mock(RemoteAuthSessionService.class);
        auth = mock(RemoteAuthTokenService.class);
        locks = mock(AccountLockSignalPort.class);
        sessions = new BffSessionService(properties, registry, store, sid);
        BffMetrics metrics = new BffMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        cleanup = new BffCleanupService(store, sid, properties, metrics);
        service = new BffAuthService(properties, registry, sessions, store, auth, cleanup, locks,
                new AccountLockBffProperties(), metrics);
        when(sid.revokeBySid(any(), anyString(), any())).thenReturn(R.ok(true));
        when(sid.getBySid(anyString())).thenAnswer(invocation -> {
            String value = invocation.getArgument(0);
            return R.ok(InnerSessionVO.builder()
                .sid(value)
                .clientId(value.startsWith("platform") ? PLATFORM_CLIENT_ID : TENANT_CLIENT_ID)
                .userId(TEST_USER_ID)
                .tenantId(42L)
                .expiresAt(clock.instant().plusSeconds(7200))
                .build());
        });
        when(auth.preAuthorize(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyMap()))
            .thenAnswer(invocation -> pre(List.of(Map.of("id", "42", "name", "A"))));
        when(auth.authorize(any(), anyString(), any(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString()))
            .thenReturn(R.ok(Map.of("code", "server-code")));
        when(auth.token(anyMap())).thenAnswer(invocation -> {
            Map<?, ?> form = invocation.getArgument(0);
            String name = form.get("client_id").toString().contains("platform") ? "platform" : "tenant";
            return tokenResponse(name + tokenIds.incrementAndGet());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = { BffConstants.ENTRY_PLATFORM, BffConstants.ENTRY_TENANT })
    void realAuthTokenResponseCompletesPlatformAndTenantSelection(String entry) {
        Browser b = browser(entry);
        b.create();
        var response = new MockHttpServletResponse();
        if (BffConstants.ENTRY_TENANT.equals(entry)) {
            doReturn(pre(List.of(Map.of("id", "42", "name", "A"), Map.of("id", "43", "name", "B")))).when(auth)
                .preAuthorize(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                        anyString(), anyString(), anyString(), anyMap());
            assertEquals(BffLoginStage.SELECT_TENANT,
                    ((BffTransactionView) service.login(entry, b.dto(), b.request(), response).getData()).stage());
            assertEquals(BffLoginStage.COMPLETED,
                    ((BffTransactionView) service.selectTenant("42", b.tx, b.request(), response).getData()).stage());
        }
        else {
            assertEquals(BffLoginStage.COMPLETED,
                    ((BffTransactionView) service.login(entry, b.dto(), b.request(), response).getData()).stage());
        }
        assertNotNull(CookieValue(response, "SESSION"));
        var session = store.readSession(b.app, CookieValue(response, "SESSION"));
        assertEquals(TEST_USER_ID, session.getUserId());
        verify(sid).getBySid(session.getSid());
        verify(auth, times(1)).token(anyMap());
    }

    @Test
    void issuedSessionLookupFailureCannotSetCookieAndKeepsKnownSidCleanup() {
        Browser b = browser(BffConstants.ENTRY_TENANT);
        b.create();
        doThrow(new IllegalStateException("Auth session unavailable")).when(sid).getBySid(anyString());
        var response = new MockHttpServletResponse();
        assertError(BffErrorCode.AUTH_UNAVAILABLE, () -> service.login(b.entry, b.dto(), b.request(), response));
        assertNull(CookieValue(response, "SESSION"));
        assertEquals(BffLoginStage.UNKNOWN, service.readTransaction(b.entry, b.tx, b.request()).stage());
        assertError(BffErrorCode.TRANSACTION_BUSY, b::login);
        verify(auth, times(1)).token(anyMap());
        cleanup.cleanup();
        verify(sid).revokeBySid(any(), startsWith(BffConstants.ENTRY_TENANT), any());
    }

    @Test
    void issuedSessionWithoutUserCannotSetCookie() {
        Browser b = browser(BffConstants.ENTRY_PLATFORM);
        b.create();
        when(sid.getBySid(anyString())).thenAnswer(invocation -> R.ok(InnerSessionVO.builder()
            .sid(invocation.getArgument(0))
            .clientId(PLATFORM_CLIENT_ID)
            .expiresAt(clock.instant().plusSeconds(7200))
            .build()));
        var response = new MockHttpServletResponse();
        assertError(BffErrorCode.AUTH_UNAVAILABLE, () -> service.login(b.entry, b.dto(), b.request(), response));
        assertNull(CookieValue(response, "SESSION"));
        verify(auth, times(1)).token(anyMap());
    }

    private R<Map<String, Object>> tokenResponse(String sessionId) {
        String token = "e30."
                + Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(("{\"sid\":\"" + sessionId + "\"}").getBytes(StandardCharsets.UTF_8))
                + ".signature";
        var response = OAuth2AccessTokenResponse.withToken(token)
            .tokenType(OAuth2AccessToken.TokenType.BEARER)
            .additionalParameters(Map.of(InOAuth2ParameterNames.USER_ID, TEST_USER_ID))
            .build();
        Map<String, Object> wire = new CustomOAuth2AccessTokenResponseMapConverter().convert(response);
        assertFalse(((Map<?, ?>) wire.get(R.DATA)).containsKey(InOAuth2ParameterNames.USER_ID));
        return mapper.convertValue(wire, new TypeReference<>() {
        });
    }

    @Test
    void csrfReuseCapacityAndReadonly() {
        Browser b = browser("platform");
        String binding = b.binding;
        var response = new MockHttpServletResponse();
        assertEquals(b.csrf, service.issueCsrf(b.request(), response).get("csrfToken"));
        assertEquals(binding, CookieValue(response, "BINDING"));
        for (int i = 0; i < 5; i++)
            b.create();
        assertError(BffErrorCode.LOGIN_CAPACITY, b::create);
        var tx = store.require(b.app, b.tx, b.binding);
        long ttl = store.bindingTtl(b.app, binding);
        service.readTransaction(b.entry, b.tx, b.request());
        assertEquals(tx.getVersion(), store.require(b.app, b.tx, binding).getVersion());
        assertTrue(store.bindingTtl(b.app, binding) <= ttl);
        verifyNoInteractions(auth);
    }

    @Test
    void directCookieAndSameResultResumeWithoutReauthorization() {
        Browser b = browser("platform");
        b.create();
        MockHttpServletResponse response = b.login();
        assertNotEquals(b.binding, CookieValue(response, "SESSION"));
        assertFalse(service.readTransaction(b.entry, b.tx, b.request()).sessionEstablished());
        var resumed = new MockHttpServletResponse();
        service.resume(b.entry, b.tx, b.request(), resumed);
        assertEquals(CookieValue(response, "SESSION"), CookieValue(resumed, "SESSION"));
        b.session = CookieValue(resumed, "SESSION");
        assertTrue(service.readTransaction(b.entry, b.tx, b.request()).sessionEstablished());
        assertEquals(TEST_USER_ID, service.me(b.request()).getData().get("userId"));
        verify(auth, times(1)).token(anyMap());
        String exposed = mapperValue(service.readTransaction(b.entry, b.tx, b.request()));
        assertFalse(exposed.contains("accessToken"));
        assertFalse(exposed.contains("resultSessionId"));
        assertFalse(exposed.contains("codeVerifier"));
    }

    @Test
    void oldCookieIsNotProofOfNewLoginAndOldResultCannotReactivate() {
        Browser b = browser("platform");
        b.create();
        b.session = CookieValue(b.login(), "SESSION");
        service.me(b.request());
        String oldTx = b.tx, oldSession = b.session;
        b.create();
        String next = CookieValue(b.login(), "SESSION");
        assertFalse(service.readTransaction(b.entry, b.tx, b.request()).sessionEstablished());
        assertNull(store.readSession(b.app, oldSession));
        assertError(BffErrorCode.RESULT_UNAVAILABLE,
                () -> service.resume(b.entry, oldTx, b.request(), new MockHttpServletResponse()));
        b.session = next;
        assertTrue(service.me(b.request()).isSuccess());
        cleanup.cleanupApp(b.app);
        verify(sid).revokeBySid(any(), startsWith("platform"), any());
    }

    @Test
    void deliveredSessionSurvivesRecoveryExpiryButUndeliveredIsRevoked() {
        Browser delivered = browser("platform");
        delivered.create();
        delivered.session = CookieValue(delivered.login(), "SESSION");
        service.me(delivered.request());
        Browser lost = browser("tenant");
        lost.create();
        String undelivered = CookieValue(lost.login(), "SESSION");
        clock.advance(61);
        cleanup.cleanup();
        assertNotNull(store.readSession(delivered.app, delivered.session));
        assertNull(store.readSession(lost.app, undelivered));
        assertTrue(service.me(delivered.request()).isSuccess());
        verify(sid, never()).revokeBySid(any(), startsWith("platform"), any());
        verify(sid).revokeBySid(any(), startsWith("tenant"), any());
        assertError(BffErrorCode.TRANSACTION_EXPIRED,
                () -> service.resume(lost.entry, lost.tx, lost.request(), new MockHttpServletResponse()));
    }

    @Test
    void logoutDoesNotAffectOtherApplicationAndCannotResume() {
        Browser p = browser("platform"), t = browser("tenant");
        p.create();
        t.create();
        p.session = CookieValue(p.login(), "SESSION");
        t.session = CookieValue(t.login(), "SESSION");
        service.me(p.request());
        service.me(t.request());
        var response = new MockHttpServletResponse();
        service.logout(p.request(), response);
        assertTrue(response.getHeaders("Set-Cookie").stream().noneMatch(v -> v.contains("TENANT")));
        assertError(BffErrorCode.BINDING_MISMATCH,
                () -> service.resume(p.entry, p.tx, p.request(), new MockHttpServletResponse()));
        assertTrue(service.me(t.request()).isSuccess());
        verify(sid, never()).revokeBySid(any(), startsWith("tenant"), any());
    }

    @ParameterizedTest
    @ValueSource(strings = { BffConstants.ENTRY_PLATFORM, BffConstants.ENTRY_TENANT })
    void logoutDuringTokenExchangePreventsLateSessionCommit(String entry) throws Exception {
        Browser b = browser(entry);
        Browser other = browser(BffConstants.ENTRY_PLATFORM.equals(entry) ? BffConstants.ENTRY_TENANT
                : BffConstants.ENTRY_PLATFORM);
        other.create();
        other.session = CookieValue(other.login(), "SESSION");
        service.me(other.request());
        b.create();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        String lateSid = entry + "-logout-late";
        doAnswer(invocation -> {
            entered.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return tokenResponse(lateSid);
        }).when(auth).token(anyMap());
        var response = new MockHttpServletResponse();
        try (var pool = Executors.newSingleThreadExecutor()) {
            var pending = pool.submit(() -> assertError(BffErrorCode.TRANSACTION_CONFLICT,
                    () -> service.login(entry, b.dto(), b.request(), response)));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            service.logout(b.request(), new MockHttpServletResponse());
            release.countDown();
            pending.get(5, TimeUnit.SECONDS);
        }
        finally {
            release.countDown();
        }
        assertNull(CookieValue(response, "SESSION"));
        assertTrue(service.me(other.request()).isSuccess());
        cleanup.cleanup();
        verify(sid).revokeBySid(any(), eq(lateSid), any());
        verify(sid, never()).revokeBySid(any(), startsWith(other.entry), any());
    }

    @Test
    void multiTenantStaysOnPageAndSelectionRechecksAuth() {
        when(auth.preAuthorize(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyMap()))
            .thenReturn(pre(List.of(Map.of("id", "42", "name", "A"), Map.of("id", "43", "name", "B"))));
        Browser b = browser("tenant");
        b.create();
        var response = new MockHttpServletResponse();
        BffTransactionView view = (BffTransactionView) service.login(b.entry, b.dto(), b.request(), response).getData();
        assertEquals(BffLoginStage.SELECT_TENANT, view.stage());
        assertEquals(2, view.allows().size());
        assertNull(CookieValue(response, "SESSION"));
        assertError(BffErrorCode.INVALID_REQUEST, () -> service.selectTenant("99", b.tx, b.request(), response));
        when(auth.authorize(any(), anyString(), any(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString()))
            .thenReturn(R.error("S0403", "membership removed"));
        assertError(BffErrorCode.IDENTITY_UNAVAILABLE, () -> service.selectTenant("42", b.tx, b.request(), response));
        verify(auth, never()).token(anyMap());
    }

    @Test
    void concurrentLoginCallsAuthOnlyOnce() throws Exception {
        Browser b = browser("platform");
        b.create();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        when(auth.preAuthorize(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyMap()))
            .thenAnswer(i -> {
                entered.countDown();
                assertTrue(release.await(5, TimeUnit.SECONDS));
                return pre(List.of());
            });
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(b::login);
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertError(BffErrorCode.TRANSACTION_BUSY, b::login);
            release.countDown();
            assertNotNull(CookieValue(first.get(5, TimeUnit.SECONDS), "SESSION"));
        }
        finally {
            release.countDown();
        }
        verify(auth, times(1)).token(anyMap());
    }

    @Test
    void expiredWorkerCannotCommitAndKnownSidIsReclaimed() {
        Browser b = browser("platform");
        b.create();
        doAnswer(i -> {
            clock.advance(31);
            return tokenResponse("platform-late");
        }).when(auth).token(anyMap());
        assertError(BffErrorCode.TRANSACTION_CONFLICT, b::login);
        cleanup.cleanup();
        verify(sid).revokeBySid(any(), eq("platform-late"), any());
        assertNull(service.me(b.request()).getData());
        assertError(BffErrorCode.TRANSACTION_CONFLICT, b::login);
    }

    @Test
    void authFailureIs503NotCredentialErrorAndNoAutomaticReplay() {
        Browser b = browser("platform");
        b.create();
        when(auth.preAuthorize(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyMap()))
            .thenThrow(new IllegalStateException("unavailable"));
        assertError(BffErrorCode.AUTH_UNAVAILABLE, b::login);
        assertEquals(BffLoginStage.UNKNOWN, service.readTransaction(b.entry, b.tx, b.request()).stage());
        assertError(BffErrorCode.TRANSACTION_BUSY, b::login);
        verifyNoInteractions(sid);
    }

    @Test
    void missingAuthDeadlineFailsClosedAndExpiredDeadlineIs410() {
        Browser b = browser("platform");
        b.create();
        when(auth.preAuthorize(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyMap()))
            .thenReturn(ResponseEntity.ok(R.ok(Map.of())));
        assertError(BffErrorCode.AUTH_UNAVAILABLE, b::login);
        b.create();
        when(auth.preAuthorize(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyMap()))
            .thenReturn(ResponseEntity
                .ok(R.ok(Map.of(BffConstants.AUTHENTICATION_EXPIRES_AT, clock.instant().minusSeconds(1).toString()))));
        assertError(BffErrorCode.TRANSACTION_EXPIRED, b::login);
        verify(auth, never()).token(anyMap());
    }

    @Test
    void bindingCsrfOriginEntryAndUnknownFieldsAreRejected() throws Exception {
        Browser b = browser("platform");
        b.create();
        var request = b.request();
        request.removeHeader(BffConstants.CSRF_HEADER);
        request.addHeader(BffConstants.CSRF_HEADER, "forged");
        var forged = request;
        assertError(BffErrorCode.CSRF_INVALID,
                () -> service.login(b.entry, b.dto(), forged, new MockHttpServletResponse()));
        request = b.request();
        request.removeHeader(HttpHeaders.ORIGIN);
        var absent = request;
        assertError(BffErrorCode.ENTRY_MISMATCH, () -> service.issueCsrf(absent, new MockHttpServletResponse()));
        var cross = b.request();
        cross.removeHeader(HttpHeaders.ORIGIN);
        cross.addHeader(HttpHeaders.ORIGIN, "https://127.0.0.1:9443");
        assertError(BffErrorCode.ENTRY_MISMATCH, () -> service.issueCsrf(cross, new MockHttpServletResponse()));
        Browser stranger = browser("platform");
        stranger.tx = b.tx;
        assertError(BffErrorCode.BINDING_MISMATCH,
                () -> service.readTransaction(stranger.entry, stranger.tx, stranger.request()));
        assertError(BffErrorCode.ENTRY_MISMATCH, () -> service.readTransaction("tenant", b.tx, b.request()));
        assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class,
                () -> mapper.readValue("{\"domain\":\"TENANT\"}", BffLoginDTO.class));
    }

    @Test
    void cleanupFailureRetainsDurableJobAndRevokedSidCannotResume() {
        Browser b = browser("platform");
        b.create();
        String issued = CookieValue(b.login(), "SESSION");
        when(sid.getBySid(anyString())).thenReturn(R.ok(null));
        assertError(BffErrorCode.RESULT_UNAVAILABLE,
                () -> service.resume(b.entry, b.tx, b.request(), new MockHttpServletResponse()));
        when(sid.revokeBySid(any(), anyString(), any())).thenThrow(new IllegalStateException("Auth down"));
        clock.advance(61);
        cleanup.cleanup();
        assertTrue(redis.opsForHash().hasKey(BffRedisKeys.cleanup(b.app), issued));
        assertNull(store.readSession(b.app, issued));
        redis.delete(BffRedisKeys.cleanupLease(b.app, issued));
        doReturn(R.ok(false)).when(sid).revokeBySid(any(), anyString(), any());
        cleanup.cleanup();
        assertFalse(redis.opsForHash().hasKey(BffRedisKeys.cleanup(b.app), issued));
    }

    @Test
    void bindingRefreshDoesNotExtendHardTransactionOrClaimNewOwner() {
        Browser b = browser("tenant");
        b.create();
        String id = b.tx;
        long deadline = store.require(b.app, id, b.binding).getExpiresAt();
        clock.advance(590);
        b.create();
        assertEquals(deadline, store.require(b.app, id, b.binding).getExpiresAt());
        assertTrue(store.bindingTtl(b.app, b.binding) >= 599);
        clock.advance(11);
        assertError(BffErrorCode.TRANSACTION_EXPIRED, () -> store.require(b.app, id, b.binding));
    }

    @Test
    void commitAcknowledgementLossKeepsResultAndDoesNotRevokeIt() {
        Browser b = browser("platform");
        b.create();
        LoginTransactionService uncertain = spy(store);
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new org.springframework.dao.DataAccessResourceFailureException("reply lost");
        }).when(uncertain).commit(any(), any(), any());
        var failing = new BffAuthService(properties, registry, sessions, uncertain, auth, cleanup, locks,
                new AccountLockBffProperties(),
                new BffMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
        assertError(BffErrorCode.AUTH_UNAVAILABLE,
                () -> failing.login(b.entry, b.dto(), b.request(), new MockHttpServletResponse()));
        var result = service.readTransaction(b.entry, b.tx, b.request());
        assertEquals(BffLoginStage.COMPLETED, result.stage());
        var response = new MockHttpServletResponse();
        service.resume(b.entry, b.tx, b.request(), response);
        b.session = CookieValue(response, "SESSION");
        assertTrue(service.me(b.request()).isSuccess());
        clock.advance(61);
        cleanup.cleanup();
        assertNotNull(store.readSession(b.app, b.session));
        verify(sid, never()).revokeBySid(any(), anyString(), any());
        verify(auth, times(1)).token(anyMap());
    }

    @Test
    void storageFailureBeforeCommitAttemptsDirectRevokeOfKnownSid() {
        Browser b = browser("platform");
        b.create();
        LoginTransactionService unavailable = spy(store);
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("down")).when(unavailable)
            .transition(any(), eq(BffLoginStage.COMMITTING), any());
        doThrow(new org.springframework.dao.DataAccessResourceFailureException("down")).when(unavailable)
            .enqueueRevoke(any());
        var failing = new BffAuthService(properties, registry, sessions, unavailable, auth, cleanup, locks,
                new AccountLockBffProperties(),
                new BffMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()));
        assertError(BffErrorCode.AUTH_UNAVAILABLE,
                () -> failing.login(b.entry, b.dto(), b.request(), new MockHttpServletResponse()));
        verify(sid).revokeBySid(any(), startsWith("platform"), any());
        assertNull(store.readSession(b.app, b.session));
    }

    @Test
    void accountLockAndCredentialFailuresPreserveBusinessCodesWithoutReplacingSession() {
        Browser b = browser("platform");
        b.create();
        b.session = CookieValue(b.login(), "SESSION");
        service.me(b.request());
        String old = b.session;
        b.create();
        when(locks.isLockedByUsername(any(), anyString())).thenReturn(true);
        var locked = service.login(b.entry, b.dto(), b.request(), new MockHttpServletResponse());
        assertEquals("ACCOUNT_LOCKED", locked.getCode());
        assertNotNull(store.readSession(b.app, old));
        when(locks.isLockedByUsername(any(), anyString())).thenReturn(false);
        b.create();
        when(auth.preAuthorize(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyMap()))
            .thenReturn(ResponseEntity.ok(R.error("bad_credentials", "incorrect password")));
        var rejected = service.login(b.entry, b.dto(), b.request(), new MockHttpServletResponse());
        assertEquals("bad_credentials", rejected.getCode());
        assertNotNull(store.readSession(b.app, old));
        verify(auth, times(1)).token(anyMap());
    }

    @Test
    void authoritativeSidOutageIs503AndDoesNotInvalidateKnownSession() {
        Browser b = browser("platform");
        b.create();
        b.session = CookieValue(b.login(), "SESSION");
        service.me(b.request());
        doThrow(new IllegalStateException("Auth down")).when(sid).getBySid(anyString());
        assertError(BffErrorCode.AUTH_UNAVAILABLE, () -> service.me(b.request()));
        assertNotNull(store.readSession(b.app, b.session));
    }

    @Test
    void fingerprintModeCannotSilentlyFallBackOrResumeDifferentDevice() {
        properties.getSecurity().setFingerprintEnabled(true);
        Browser b = browser("platform");
        b.create();
        var request = b.request();
        assertError(BffErrorCode.INVALID_REQUEST,
                () -> service.login(b.entry, b.dto(), request, new MockHttpServletResponse()));
        request.addHeader(HeaderConstants.BFF_DEVICE_FINGERPRINT_HEADER, "fixture-device");
        var response = new MockHttpServletResponse();
        assertTrue(service.login(b.entry, b.dto(), request, response).isSuccess());
        b.session = CookieValue(response, "SESSION");
        assertError(BffErrorCode.RESULT_UNAVAILABLE,
                () -> service.resume(b.entry, b.tx, b.request(), new MockHttpServletResponse()));
        var same = b.request();
        same.addHeader(HeaderConstants.BFF_DEVICE_FINGERPRINT_HEADER, "fixture-device");
        assertTrue(service.me(same).isSuccess());
    }

    @Test
    void publicHttpContractRejectsFormsMissingOriginsAndLegacyComplete() throws Exception {
        var metrics = new BffMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
            .standaloneSetup(new com.ingot.cloud.bff.web.BffAuthAPI(service))
            .setControllerAdvice(new com.ingot.cloud.bff.error.BffAuthExceptionHandler(metrics))
            .setMessageConverters(
                    new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(mapper.copy()
                        .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)))
            .addFilters(new BffOriginFilter(registry))
            .build();
        var post = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(BffConstants.CSRF)
            .header(HttpHeaders.HOST, "127.0.0.1:8443")
            .header(HeaderConstants.INNER_BFF_APP_ID, "platform-admin")
            .header(HttpHeaders.ORIGIN, "https://127.0.0.1:8443");
        mvc.perform(post.contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED).content(""))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        var absent = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(BffConstants.CSRF)
            .header(HttpHeaders.HOST, "127.0.0.1:8443")
            .header(HeaderConstants.INNER_BFF_APP_ID, "platform-admin")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("{}");
        mvc.perform(absent)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        mvc.perform(post.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("{\"appId\":\"tenant-admin\"}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        mvc.perform(post.contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                .string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.expiresAt",
                    org.hamcrest.Matchers.endsWith("Z")));
        Browser browser = browser("platform");
        browser.create();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .get(BffConstants.PLATFORM_TRANSACTIONS + "/" + browser.tx)
            .header(HttpHeaders.HOST, "127.0.0.1:8443")
            .header(HeaderConstants.INNER_BFF_APP_ID, "platform-admin")
            .header(BffConstants.FETCH_SITE_HEADER, BffConstants.SAME_ORIGIN)
            .cookie(new Cookie(BffCookiePolicy.bindingCookieName(true, browser.app), browser.binding)))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.stage")
                .value("LOGIN"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                .doesNotExist(HttpHeaders.SET_COOKIE));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(BffConstants.ME)
            .header(HttpHeaders.HOST, "127.0.0.1:8443")
            .header(HeaderConstants.INNER_BFF_APP_ID, "platform-admin")
            .header(BffConstants.FETCH_SITE_HEADER, BffConstants.SAME_ORIGIN))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                .doesNotExist(HttpHeaders.SET_COOKIE));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .post(BffConstants.ROOT + "/platform/complete")
            .header(HttpHeaders.HOST, "127.0.0.1:8443")
            .header(HeaderConstants.INNER_BFF_APP_ID, "platform-admin")
            .header(HttpHeaders.ORIGIN, "https://127.0.0.1:8443")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void authWindowAlsoFencesSlowSingleTenantCompletion() {
        Browser b = browser("platform");
        b.create();
        doReturn(ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, "JSESSIONID=internal-auth; Path=/")
            .body(R.ok(Map.of(BffConstants.AUTHENTICATION_EXPIRES_AT, clock.instant().plusSeconds(2).toString()))))
            .when(auth)
            .preAuthorize(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString(), anyMap());
        doAnswer(i -> {
            clock.advance(3);
            return tokenResponse("platform-late");
        }).when(auth).token(anyMap());
        assertError(BffErrorCode.TRANSACTION_EXPIRED, b::login);
        cleanup.cleanup();
        verify(sid).revokeBySid(any(), eq("platform-late"), any());
    }

    @Test
    void zeroTenantQualificationCannotIssueSession() {
        Browser b = browser("tenant");
        b.create();
        doReturn(pre(List.of())).when(auth)
            .preAuthorize(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString(), anyMap());
        assertError(BffErrorCode.IDENTITY_UNAVAILABLE, b::login);
        verify(auth, never()).token(anyMap());
    }

    @Test
    void concurrentTenantSelectionCallsAuthorizeOnlyOnce() throws Exception {
        Browser b = browser("tenant");
        b.create();
        doReturn(pre(List.of(Map.of("id", "42", "name", "A"), Map.of("id", "43", "name", "B")))).when(auth)
            .preAuthorize(any(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                    anyString(), anyString(), anyString(), anyMap());
        service.login(b.entry, b.dto(), b.request(), new MockHttpServletResponse());
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        doAnswer(i -> {
            entered.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return R.ok(Map.of("code", "server-code"));
        }).when(auth)
            .authorize(any(), anyString(), any(), anyString(), anyString(), anyString(), anyString(), anyString(),
                    anyString(), anyString());
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> service.selectTenant("42", b.tx, b.request(), new MockHttpServletResponse()));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertError(BffErrorCode.TRANSACTION_BUSY,
                    () -> service.selectTenant("42", b.tx, b.request(), new MockHttpServletResponse()));
            release.countDown();
            assertTrue(first.get(5, TimeUnit.SECONDS).isSuccess());
        }
        finally {
            release.countDown();
        }
        verify(auth, times(1)).token(anyMap());
    }

    private ResponseEntity<R<Map<String, Object>>> pre(List<Map<String, String>> candidates) {
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, "JSESSIONID=internal-auth; Path=/; HttpOnly")
            .body(R.ok(Map.of(BffConstants.AUTHENTICATION_EXPIRES_AT, clock.instant().plusSeconds(300).toString(),
                    InOAuth2ParameterNames.PRE_ALLOW_LIST, candidates)));
    }

    private Browser browser(String entry) {
        Browser b = new Browser(entry);
        var response = new MockHttpServletResponse();
        b.csrf = (String) service.issueCsrf(b.request(), response).get("csrfToken");
        b.binding = CookieValue(response, "BINDING");
        return b;
    }

    private static String CookieValue(MockHttpServletResponse response, String type) {
        return response.getHeaders(HttpHeaders.SET_COOKIE)
            .stream()
            .filter(v -> v.split("=", 2)[0].endsWith(type))
            .map(v -> v.substring(v.indexOf('=') + 1, v.indexOf(';')))
            .findFirst()
            .orElse(null);
    }

    private static BffAppRegistration app(String id, AuthorizationDomain domain, String origin) {
        var app = new BffAppRegistration();
        app.setAppId(id);
        app.setDomain(domain);
        app.setOrigin(origin);
        app.setOauthClientId(domain == AuthorizationDomain.PLATFORM ? PLATFORM_CLIENT_ID : TENANT_CLIENT_ID);
        app.setOauthRedirectUri(origin + "/internal-callback");
        return app;
    }

    private static void assertError(BffErrorCode expected, org.junit.jupiter.api.function.Executable executable) {
        assertEquals(expected, assertThrows(BffAuthException.class, executable).getErrorCode());
    }

    private String mapperValue(Object value) {
        try {
            return mapper.writeValueAsString(value);
        }
        catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private final class Browser {

        private final String entry, app, origin;

        private String binding, csrf, tx, session;

        Browser(String e) {
            entry = e;
            app = e + "-admin";
            origin = "https://127.0.0.1:" + (e.equals("platform") ? 8443 : 9443);
        }

        MockHttpServletRequest request() {
            var q = new MockHttpServletRequest("POST", "/bff/auth/" + entry + "/login");
            q.addHeader(HttpHeaders.HOST, origin.substring(8));
            q.addHeader(HttpHeaders.ORIGIN, origin);
            q.addHeader(HeaderConstants.INNER_BFF_APP_ID, app);
            if (csrf != null)
                q.addHeader(BffConstants.CSRF_HEADER, csrf);
            var c = new java.util.ArrayList<Cookie>();
            if (binding != null)
                c.add(new Cookie(BffCookiePolicy.bindingCookieName(true, app), binding));
            if (session != null)
                c.add(new Cookie(BffCookiePolicy.sessionCookieName(true, app), session));
            q.setCookies(c.toArray(Cookie[]::new));
            return q;
        }

        void create() {
            tx = service.createTransaction(entry, request(), new MockHttpServletResponse()).transactionId();
        }

        BffLoginDTO dto() {
            var d = new BffLoginDTO();
            d.setUsername("operator");
            d.setPassword("memory-only");
            d.setTransactionId(tx);
            return d;
        }

        MockHttpServletResponse login() {
            var response = new MockHttpServletResponse();
            assertTrue(service.login(entry, dto(), request(), response).isSuccess());
            return response;
        }

    }

    /** 可推进的UTC测试时钟，RedisTTL仍由真实服务器处理。 */
    private static final class MutableClock extends Clock {

        private final Instant initial = Instant.now();

        private final AtomicLong offset = new AtomicLong();

        void advance(long seconds) {
            offset.addAndGet(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return initial.plusSeconds(offset.get());
        }

    }

}
