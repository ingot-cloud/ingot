package com.ingot.cloud.iam.authorization.read;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.ingot.cloud.iam.authorization.snapshot.LocalTrustedAuthoritySource;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluationRepository;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.identity.ActiveIdentityService;
import com.ingot.cloud.iam.identity.CurrentIdentityService;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.access.AdminOrHasAnyAuthority;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.server.resource.access.expression.InSecurityExpression;
import com.ingot.framework.security.oauth2.server.resource.access.expression.TrustedAuthoritySource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <p>真实 MVC 与方法安全代理验证 interceptor 在在线注解之前启用，下一请求撤权立即阻断。</p>
 * @author jy
 * @since 1.0.0
 */
class IamReadRequestMvcTest {
    private static final AuthorizationContext ACTOR = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "10");

    @Test
    void onlineAnnotationAndControllerShareFreshViewAndNextRequestDoesNotReuseIt() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(Config.class, IamReadRequestConfiguration.class);
            context.refresh();
            var repository = context.getBean(AuthorizationEvaluationRepository.class);
            var identities = context.getBean(ActiveIdentityService.class);
            when(identities.requireActive(ACTOR)).thenReturn(new ActiveIdentity(ACTOR, "0", "0", "0"));
            when(repository.platformAdministrators("10")).thenReturn(List.of(new AuthorizationEvalRows.Assignment(
                    BigInteger.ONE, "{}", null, null, null, BigInteger.TWO, RoleKind.SYSTEM, null)));
            when(repository.enabledPlatformActions()).thenReturn(List.of(new AuthorizationEvalRows.IndexedAction(
                    BigInteger.ONE, IamAction.PLATFORM_MEMBER_READ.getCode(), true, AuthorizationDomain.PLATFORM, true, BigInteger.ONE)));
            var user = InUser.stateless(1L, null, "web", "standard", UserTypeEnum.ADMIN.getValue(), "account", List.of(), List.of(), Map.of())
                    .toBuilder().authorizationContext(ACTOR).build();
            SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
            var mvc = MockMvcBuilders.webAppContextSetup(context).build();
            mvc.perform(get("/read")).andExpect(status().isOk());
            verify(identities, times(1)).requireActive(ACTOR);
            verify(repository, times(1)).passwordChangeRequired(ACTOR);
            verify(repository, times(1)).enabledPlatformActions();
            when(repository.platformAdministrators("10")).thenReturn(List.of());
            var denied = assertThrows(Exception.class, () -> mvc.perform(get("/read")));
            Throwable cause = denied;
            while (cause.getCause() != null) cause = cause.getCause();
            assertInstanceOf(org.springframework.security.authorization.AuthorizationDeniedException.class, cause);
            verify(identities, times(2)).requireActive(ACTOR);
        } finally { SecurityContextHolder.clearContext(); }
    }

    /** <p>装配真实方法安全和 MVC，仅数据库与跨请求缓存使用边界替身。</p> */
    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableMethodSecurity
    static class Config {
        @Bean AuthorizationEvaluationRepository repository() { return mock(AuthorizationEvaluationRepository.class); }
        @Bean ActiveIdentityService identities() { return mock(ActiveIdentityService.class); }
        @Bean CurrentIdentityService current(ActiveIdentityService identities) { return new CurrentIdentityService(identities); }
        @Bean LayeredCache<String, AuthorizationEvaluator.AuthorizationView> cache() {
            LayeredCache<String, AuthorizationEvaluator.AuthorizationView> cache = mock(LayeredCache.class);
            when(cache.get(anyString())).thenReturn(new AuthorizationEvaluator.AuthorizationView(
                    List.of(IamAction.PLATFORM_MEMBER_READ.getCode()), List.of(), Map.of(), "old-hot", Instant.now().plusSeconds(30)));
            return cache;
        }
        @Bean AuthorizationEvaluator evaluator(AuthorizationEvaluationRepository repository,
                ObjectProvider<LayeredCache<String, AuthorizationEvaluator.AuthorizationView>> cache) {
            return new AuthorizationEvaluator(repository, cache, null);
        }
        @Bean LocalTrustedAuthoritySource source(CurrentIdentityService current, AuthorizationEvaluator evaluator) {
            return new LocalTrustedAuthoritySource(current, evaluator);
        }
        @Bean("ingot") InSecurityExpression expression(ObjectProvider<TrustedAuthoritySource> source) { return new InSecurityExpression(source); }
        @Bean ReadController controller(CurrentIdentityService current, AuthorizationEvaluator evaluator) { return new ReadController(current, evaluator); }
    }

    /** <p>以真实注解和服务入口模拟只读人员查询。</p> */
    @RestController
    static class ReadController {
        private final CurrentIdentityService current;
        private final AuthorizationEvaluator evaluator;
        ReadController(CurrentIdentityService current, AuthorizationEvaluator evaluator) { this.current = current; this.evaluator = evaluator; }
        /** 注解、服务准入与能力各自保留判断，同时复用各自来源。 */
        @GetMapping("/read")
        @IamReadOnlyRequest
        @AdminOrHasAnyAuthority(IamAction.VALUE_PLATFORM_MEMBER_READ)
        public String read() {
            var actor = current.requireCurrent().context();
            evaluator.admit(actor, IamAction.PLATFORM_MEMBER_READ);
            evaluator.capabilities(actor, List.of(IamAction.PLATFORM_MEMBER_READ));
            evaluator.evaluate(actor);
            return "ok";
        }
    }
}
