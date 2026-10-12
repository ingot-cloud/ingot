package com.ingot.cloud.iam.authorization;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.ingot.cloud.iam.authorization.snapshot.LocalAuthorizationSnapshotLoader;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.identity.CurrentIdentityService;
import com.ingot.cloud.iam.web.inner.InnerAuthorizationAPI;
import com.ingot.cloud.iam.web.inner.InnerResourceAuthorizationAPI;
import com.ingot.framework.authorization.AuthorizationClient;
import com.ingot.framework.commons.constants.SecurityConstants;
import com.ingot.framework.commons.constants.HeaderConstants;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.SecurityResources;
import com.ingot.framework.commons.model.iam.extension.AuthorizationDecision;
import com.ingot.framework.commons.model.security.TokenAuthTypeEnum;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.config.annotation.web.configurers.InHttpConfigurersAdapter;
import com.ingot.framework.security.config.annotation.web.configuration.InOAuth2ResourceServerConfiguration;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.oauth2.core.InOAuth2ResourceProperties;
import com.ingot.framework.security.oauth2.core.PermitResolver;
import com.ingot.framework.security.oauth2.jwt.JwtClaimNamesExtension;
import com.ingot.framework.security.oauth2.server.authorization.OnlineToken;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.security.oauth2.server.authorization.SessionStoreAvailability;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <p>经过真实 Bearer、在线会话和通用 INNER 过滤链验证内部授权身份，避免固定测试身份掩盖缺失认证。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class IamAuthorizationSecurityConfigurationTest {
    private static final String SNAPSHOT = "/inner/authorization/snapshot";
    private static final String EVALUATE = "/inner/authorization/v2/evaluate";
    private static final String PREVIEW = "/inner/authorization/v2/preview";
    private static final String TOKEN = "test-token";
    private static final String SID = "test-session";
    private static final long USER_ID = 900001L;
    private static final String CLIENT = "platform-web";
    private static final AuthorizationContext ACTOR = new AuthorizationContext(
            AuthorizationDomain.PLATFORM, null, Long.toString(USER_ID), "900002");
    private static final String RESOURCE_BODY = """
            {"resource":{"domain":"PLATFORM","applicationCode":"platform:security","resourceCode":"credential-policy"},
             "actionCodes":["iam-platform:credential-policy:read"]}
            """;

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private OnlineTokenService sessions;
    private AuthorizationEvaluator evaluator;
    private AuthorizationClient client;

    @BeforeEach void start() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfiguration.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
        sessions = context.getBean(OnlineTokenService.class);
        evaluator = context.getBean(AuthorizationEvaluator.class);
        client = context.getBean(AuthorizationClient.class);
        var jwt = Jwt.withTokenValue(TOKEN).header("alg", "RS256")
                .claim(JwtClaimNamesExtension.SUB, "administrator")
                .claim(JwtClaimNamesExtension.AUD, List.of(CLIENT))
                .claim(JwtClaimNamesExtension.ID, USER_ID)
                .claim(JwtClaimNamesExtension.SID, SID)
                .claim(JwtClaimNamesExtension.SCOPE, List.of()).build();
        when(context.getBean(JwtDecoder.class).decode(TOKEN)).thenReturn(jwt);
        when(sessions.getBySid(SID)).thenReturn(Optional.of(OnlineToken.builder()
                .sid(SID).userId(USER_ID).tenantId(null).clientId(CLIENT).principalName("administrator")
                .authType(TokenAuthTypeEnum.STANDARD.getValue()).userType(UserTypeEnum.ADMIN.getValue())
                .authorities(Set.of()).deptIds(List.of())
                .authorizationContext(ACTOR).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build()));
        when(context.getBean(CurrentIdentityService.class).requireCurrent())
                .thenAnswer(invocation -> {
                    assertEquals(ACTOR, SecurityAuthContext.getUser().getAuthorizationContext());
                    return new ActiveIdentity(ACTOR, "0", "0", "0");
                });
        when(evaluator.evaluateForExecution(ACTOR, true)).thenReturn(new AuthorizationEvaluator.AuthorizationView(
                List.of("iam-platform:credential-policy:read"), List.of(), Map.of(), "v1",
                Instant.now().plusSeconds(30), List.of(), true));
        when(client.evaluate(any())).thenAnswer(invocation -> decision());
        when(client.preview(any())).thenAnswer(invocation -> decision());
    }

    @AfterEach void close() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test void snapshotRestoresOnlineActorAheadOfGenericInnerChain() throws Exception {
        mvc.perform(internal(SNAPSHOT, "{\"tenantId\":0,\"userId\":900001}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk()).andExpect(jsonPath("data.userId").value(USER_ID))
                .andExpect(jsonPath("data.platformAdministrator").value(true));
        verify(sessions).getBySid(SID);
        verify(evaluator).evaluateForExecution(ACTOR, true);
    }

    @Test void evaluateAndPreviewRestoreTheSameAuthenticatedActor() throws Exception {
        for (var path : List.of(EVALUATE, PREVIEW))
            mvc.perform(internal(path, RESOURCE_BODY).header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                    .andExpect(status().isOk()).andExpect(jsonPath("data.context.memberId").value(ACTOR.memberId()));
        verify(client).evaluate(any());
        verify(client).preview(any());
    }

    @Test void ordinaryActorCanLoadOwnSnapshotWithoutAdministratorAuthority() throws Exception {
        when(evaluator.evaluateForExecution(ACTOR, true)).thenReturn(new AuthorizationEvaluator.AuthorizationView(
                List.of("iam-platform:credential-policy:read"), List.of(), Map.of(), "v1",
                Instant.now().plusSeconds(30), List.of(), false));
        mvc.perform(internal(SNAPSHOT, "{}").header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk()).andExpect(jsonPath("data.platformAdministrator").value(false))
                .andExpect(jsonPath("data.permissionCodes[0]").value("iam-platform:credential-policy:read"));
    }

    @Test void internalHeaderAndForgedUserIdCannotReplaceBearer() throws Exception {
        for (var path : List.of(SNAPSHOT, EVALUATE, PREVIEW))
            mvc.perform(internal(path, path.equals(SNAPSHOT) ? "{}" : RESOURCE_BODY)
                    .header(HeaderConstants.INNER_USER_ID, USER_ID)).andExpect(status().isUnauthorized());
        verifyNoInteractions(sessions, evaluator, client);
    }

    @Test void invalidTokenAndRevokedSessionAreRejectedBeforeAuthorization() throws Exception {
        when(context.getBean(JwtDecoder.class).decode("invalid")).thenThrow(new BadJwtException("Invalid signature"));
        mvc.perform(internal(SNAPSHOT, "{}").header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        when(sessions.getBySid(SID)).thenReturn(Optional.empty());
        mvc.perform(internal(SNAPSHOT, "{}").header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(evaluator, client);
    }

    @Test void bearerWithoutInternalHeaderRemainsRejected() {
        assertThrows(AccessDeniedException.class, () -> mvc.perform(post(SNAPSHOT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN).contentType(MediaType.APPLICATION_JSON).content("{}")));
        verifyNoInteractions(sessions, evaluator, client);
    }

    @Test void bodyCannotReplaceTokenIdentity() throws Exception {
        mvc.perform(internal(SNAPSHOT, "{\"tenantId\":0,\"userId\":999999}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)).andExpect(status().isForbidden());
        verifyNoInteractions(evaluator);
    }

    @Test void forcedPasswordSnapshotStillReportsCurrentStateWithoutRecursiveAdmission() throws Exception {
        when(evaluator.passwordChangeRequired(ACTOR)).thenReturn(true);
        mvc.perform(internal(SNAPSHOT, "{}").header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isOk()).andExpect(jsonPath("data.passwordChangeRequired").value(true))
                .andExpect(jsonPath("data.platformAdministrator").value(false))
                .andExpect(jsonPath("data.permissionCodes").isEmpty());
    }

    private AuthorizationDecision decision() {
        assertEquals(ACTOR, SecurityAuthContext.getUser().getAuthorizationContext());
        return new AuthorizationDecision(SecurityResources.key(com.ingot.framework.commons.model.iam.IamAction.PLATFORM_CREDENTIAL_POLICY_READ),
                ACTOR, Map.of(), "v1", Instant.now().plusSeconds(30));
    }

    private MockHttpServletRequestBuilder internal(String path, String body) {
        return post(path).header(SecurityConstants.HEADER_FROM, SecurityConstants.HEADER_FROM_INSIDE_VALUE)
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    /**
     * <p>装配生产授权链与生产通用 INNER 链，只替换 JWT 校验和外部数据依赖。</p>
     * @author jy
     * @since 1.0.0
     */
    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @Import({IamAuthorizationSecurityConfiguration.class, InnerAuthorizationAPI.class, InnerResourceAuthorizationAPI.class,
            LocalAuthorizationSnapshotLoader.class})
    static class TestConfiguration {
        @Bean OnlineTokenService sessions() { return mock(OnlineTokenService.class); }
        @Bean SessionStoreAvailability availability() { return new SessionStoreAvailability(Duration.ZERO); }
        @Bean JwtDecoder decoder() { return mock(JwtDecoder.class); }
        @Bean AuthorizationEvaluator evaluator() { return mock(AuthorizationEvaluator.class); }
        @Bean CurrentIdentityService identities() { return mock(CurrentIdentityService.class); }
        @Bean AuthorizationClient client() { return mock(AuthorizationClient.class); }
        @Bean InHttpConfigurersAdapter adapter() { return http -> {}; }
        @Bean PermitResolver permits(WebApplicationContext context) {
            return new PermitResolver(context, new InOAuth2ResourceProperties());
        }
        @Bean @Order(Ordered.HIGHEST_PRECEDENCE + 10)
        SecurityFilterChain genericInner(InHttpConfigurersAdapter adapter, PermitResolver permits, HttpSecurity http) throws Exception {
            return new InOAuth2ResourceServerConfiguration().innerResourceSecurityFilterChain(adapter, permits, http);
        }
    }
}
