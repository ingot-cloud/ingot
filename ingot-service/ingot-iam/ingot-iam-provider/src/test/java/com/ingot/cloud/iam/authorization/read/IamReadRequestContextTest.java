package com.ingot.cloud.iam.authorization.read;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.ingot.cloud.iam.authorization.snapshot.LocalTrustedAuthoritySource;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluationRepository;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator.AuthorizationView;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.identity.ActiveIdentityService;
import com.ingot.cloud.iam.identity.CurrentIdentityService;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.userdetails.InUser;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>验证请求复用的来源、身份、事务、到期与故障边界，以及原始 loader 不参与复用。</p>
 * @author jy
 * @since 1.0.0
 */
class IamReadRequestContextTest {
    private static final AuthorizationContext ACTOR = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "10");
    private final AuthorizationEvaluationRepository repository = mock(AuthorizationEvaluationRepository.class);
    private final ActiveIdentityService identities = mock(ActiveIdentityService.class);
    private final CurrentIdentityService current = new CurrentIdentityService(identities);
    private final LayeredCache<String, AuthorizationView> cache = mock(LayeredCache.class);
    private AuthorizationEvaluator evaluator;
    private MockHttpServletRequest request;

    @BeforeEach
    void setup() {
        authenticate(ACTOR);
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("cache", cache);
        evaluator = new AuthorizationEvaluator(repository, (org.springframework.beans.factory.ObjectProvider) beans.getBeanProvider(LayeredCache.class), null);
        when(identities.requireActive(ACTOR)).thenReturn(new ActiveIdentity(ACTOR, "0", "0", "0"));
        when(repository.platformAdministrators("10")).thenReturn(List.of(new AuthorizationEvalRows.Assignment(
                BigInteger.ONE, "{}", null, null, null, BigInteger.TWO, RoleKind.SYSTEM, null)));
        when(repository.enabledPlatformActions()).thenReturn(List.of(new AuthorizationEvalRows.IndexedAction(
                BigInteger.ONE, IamAction.PLATFORM_MEMBER_READ.getCode(), true, AuthorizationDomain.PLATFORM, true, BigInteger.ONE)));
        request = new MockHttpServletRequest("GET", "/read");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        IamReadRequestContext.begin(request);
    }

    @AfterEach
    void cleanup() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        TransactionSynchronizationManager.clear();
    }

    @Test
    void identityPasswordOrdinaryAndLiveSourcesAreIndependentAndReused() {
        var hot = view("hot", Instant.now().plusSeconds(30));
        when(cache.get(AuthorizationEvaluator.cacheKey(ACTOR))).thenReturn(hot);
        var source = new LocalTrustedAuthoritySource(current, evaluator);
        source.currentAuthorities();
        current.requireCurrent();
        evaluator.admit(ACTOR, IamAction.PLATFORM_MEMBER_READ);
        assertSame(hot, evaluator.evaluate(ACTOR));
        evaluator.capabilities(ACTOR, List.of(IamAction.PLATFORM_MEMBER_READ));
        evaluator.evaluateForExecution(ACTOR, true);
        verify(identities, times(1)).requireActive(ACTOR);
        verify(repository, times(1)).passwordChangeRequired(ACTOR);
        verify(repository, times(1)).enabledPlatformActions();
        verify(cache, times(1)).get(AuthorizationEvaluator.cacheKey(ACTOR));
    }

    @Test
    void hotGrantCannotSatisfyFreshQualificationAndNextRequestRechecksRevocation() {
        var source = new LocalTrustedAuthoritySource(current, evaluator);
        assertTrue(source.currentAuthorities().contains(IamAction.PLATFORM_MEMBER_READ.getCode()));
        IamReadRequestContext.clear(request);
        when(repository.platformAdministrators("10")).thenReturn(List.of());
        IamReadRequestContext.begin(request);
        when(cache.get(anyString())).thenReturn(view("stale-hot", Instant.now().plusSeconds(30)));
        assertFalse(source.currentAuthorities().contains(IamAction.PLATFORM_MEMBER_READ.getCode()));
        assertTrue(evaluator.capabilities(ACTOR, List.of(IamAction.PLATFORM_MEMBER_READ)).isEmpty());
        verify(identities, times(2)).requireActive(ACTOR);
    }

    @Test
    void changedAuthenticationOrFullIdentityCannotReuseResults() {
        current.requireCurrent();
        authenticate(ACTOR);
        current.requireCurrent();
        var differentAccount = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "2", "10");
        var calls = new AtomicInteger();
        Supplier<AuthorizationView> loader = () -> { calls.incrementAndGet(); return view("other", Instant.now().plusSeconds(30)); };
        IamReadRequestContext.view(differentAccount, true, loader);
        IamReadRequestContext.view(differentAccount, true, loader);
        assertEquals(2, calls.get());
        verify(identities, times(2)).requireActive(ACTOR);
    }

    @Test
    void failuresAreNotSavedAndPasswordOrDisabledIdentityFailsClosed() {
        when(identities.requireActive(ACTOR)).thenThrow(new BizException(IamReasonCode.IDENTITY_INVALID));
        assertThrows(BizException.class, current::requireCurrent);
        assertThrows(BizException.class, current::requireCurrent);
        verify(identities, times(2)).requireActive(ACTOR);
        when(repository.passwordChangeRequired(ACTOR)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("offline"));
        assertEquals(IamReasonCode.AUTHORIZATION_UNAVAILABLE.getCode(), assertThrows(BizException.class,
                () -> evaluator.admit(ACTOR, IamAction.PLATFORM_MEMBER_READ)).getCode());
        doReturn(true).when(repository).passwordChangeRequired(ACTOR);
        assertEquals(IamReasonCode.PASSWORD_CHANGE_REQUIRED.getCode(), assertThrows(BizException.class,
                () -> evaluator.admit(ACTOR, IamAction.PLATFORM_MEMBER_READ)).getCode());
    }

    @Test
    void expiredViewsReloadWithoutRenewingOrdinaryOrFreshDeadline() {
        var calls = new AtomicInteger();
        Supplier<AuthorizationView> loader = () -> { calls.incrementAndGet(); return view("expired", Instant.EPOCH); };
        IamReadRequestContext.view(ACTOR, true, loader);
        IamReadRequestContext.view(ACTOR, true, loader);
        IamReadRequestContext.view(ACTOR, false, loader);
        IamReadRequestContext.view(ACTOR, false, loader);
        assertEquals(4, calls.get());
    }

    @Test
    void writeTransactionsNonGetAsyncAndLoaderAlwaysRequery() {
        evaluator.evaluateForExecution(ACTOR, true);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        evaluator.evaluateForExecution(ACTOR, true);
        evaluator.evaluateForExecution(ACTOR, true);
        TransactionSynchronizationManager.clear();
        request.setMethod("POST");
        evaluator.evaluateForExecution(ACTOR, true);
        evaluator.evaluateForExecution(ACTOR, true);
        request.setMethod("GET"); request.setDispatcherType(DispatcherType.ASYNC);
        evaluator.evaluateForExecution(ACTOR, true);
        evaluator.evaluateForExecution(ACTOR, true);
        request.setDispatcherType(DispatcherType.REQUEST);
        evaluator.evaluateRaw(AuthorizationEvaluator.cacheKey(ACTOR));
        evaluator.evaluateRaw(AuthorizationEvaluator.cacheKey(ACTOR));
        verify(repository, times(9)).enabledPlatformActions();
    }

    @Test
    void interceptorOnlyEnablesMarkedMethodsAndClearsOnFailureOrAsyncHandoff() throws Exception {
        var interceptor = new IamReadRequestInterceptor();
        var response = new MockHttpServletResponse();
        var calls = new AtomicInteger();
        Supplier<Boolean> loader = () -> { calls.incrementAndGet(); return false; };
        var unmarked = new HandlerMethod(new Handler(), Handler.class.getDeclaredMethod("unmarked"));
        interceptor.preHandle(request, response, unmarked);
        IamReadRequestContext.passwordRequired(ACTOR, loader); IamReadRequestContext.passwordRequired(ACTOR, loader);
        assertEquals(2, calls.get());
        var marked = new HandlerMethod(new Handler(), Handler.class.getDeclaredMethod("marked"));
        interceptor.preHandle(request, response, marked);
        IamReadRequestContext.passwordRequired(ACTOR, loader); IamReadRequestContext.passwordRequired(ACTOR, loader);
        assertEquals(3, calls.get());
        interceptor.afterCompletion(request, response, marked, new RuntimeException());
        IamReadRequestContext.passwordRequired(ACTOR, loader);
        interceptor.preHandle(request, response, marked);
        IamReadRequestContext.passwordRequired(ACTOR, loader);
        interceptor.afterConcurrentHandlingStarted(request, response, marked);
        IamReadRequestContext.passwordRequired(ACTOR, loader);
        assertEquals(6, calls.get());
    }

    private static AuthorizationView view(String version, Instant expires) {
        return new AuthorizationView(List.of(IamAction.PLATFORM_MEMBER_READ.getCode()), List.of(), Map.of(), version, expires);
    }

    private static void authenticate(AuthorizationContext actor) {
        var user = InUser.stateless(1L, null, "web", "standard", UserTypeEnum.ADMIN.getValue(), "account", List.of(), List.of(), Map.of())
                .toBuilder().authorizationContext(actor).build();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
    }

    /** <p>模拟显式声明与普通同步方法。</p> */
    static class Handler {
        @IamReadOnlyRequest
        public void marked() { }
        public void unmarked() { }
    }
}
