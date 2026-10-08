package com.ingot.cloud.iam.authorization.snapshot;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.identity.CurrentIdentityService;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.commons.jackson.InModule;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.security.core.userdetails.InUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>内部快照输出独立服务器超管事实并重验身份，保留字符串标识契约。</p>
 * @author jy
 * @since 1.0.0
 */
class LocalAuthorizationSnapshotLoaderTest {
    private final CurrentIdentityService identities = mock(CurrentIdentityService.class);
    private final AuthorizationEvaluator evaluator = mock(AuthorizationEvaluator.class);
    private final LocalAuthorizationSnapshotLoader loader = new LocalAuthorizationSnapshotLoader(evaluator, identities);
    private final AuthorizationContext context = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "2");

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void snapshotRevalidatesIdentityAndSerializesSeparateFactWithoutSyntheticBusinessCode() throws Exception {
        authenticate();
        when(identities.requireCurrent()).thenReturn(new ActiveIdentity(context, "0", "0", "0"));
        var expires = Instant.now().plusSeconds(30);
        when(evaluator.evaluateForExecution(context, true)).thenReturn(new AuthorizationEvaluator.AuthorizationView(
                List.of("iam-platform:member:read"), List.of("iam-platform:member:read"), Map.of(), "version", expires, List.of(), true));
        var result = loader.assemble(0L, 1L);
        assertTrue(result.isPlatformAdministrator());
        assertFalse(result.getPermissionCodes().contains(RoleConstants.ROLE_ADMIN_CODE));
        verify(identities).requireCurrent();
        verify(evaluator).evaluateForExecution(context, true);
        var json = new ObjectMapper().findAndRegisterModules().registerModule(new InModule()).valueToTree(result);
        assertEquals("0", json.path("tenantId").asText());
        assertTrue(json.path("userId").isTextual());
        assertTrue(json.path("platformAdministrator").isBoolean());
        assertTrue(json.path("platformAdministrator").asBoolean());
        assertEquals(List.of("emptyAuthorization", "expiresAt", "generatedAt", "passwordChangeRequired", "permissionCodes", "platformAdministrator",
                "resourceRules", "roleBindings", "source", "tenantId", "userId", "version"),
                java.util.stream.StreamSupport.stream(java.util.Spliterators.spliteratorUnknownSize(json.fieldNames(), 0), false).sorted().toList());
    }

    @Test void requestedIdentityCannotReplaceAuthenticatedSubjectAndOnlineFailurePropagates() {
        authenticate();
        assertThrows(AuthorizationDeniedException.class, () -> loader.assemble(10L, 1L));
        assertThrows(AuthorizationDeniedException.class, () -> loader.assemble(0L, 99L));
        when(identities.requireCurrent()).thenThrow(new AuthorizationDeniedException("IdentityInvalid"));
        assertThrows(AuthorizationDeniedException.class, () -> loader.assemble(null, null));
        verifyNoInteractions(evaluator);
    }

    @Test void businessOperationCannotForgeAdministratorMarker() {
        var actor = new ActiveIdentity(context, "0", "0", "0");
        when(identities.requireCurrent()).thenReturn(actor);
        when(evaluator.evaluateForExecution(context, true)).thenReturn(new AuthorizationEvaluator.AuthorizationView(
                List.of(RoleConstants.ROLE_ADMIN_CODE), List.of(), Map.of(), "version", Instant.now().plusSeconds(30)));
        assertFalse(new LocalTrustedAuthoritySource(identities, evaluator).currentAuthorities().contains(RoleConstants.ROLE_ADMIN_CODE));
    }

    @Test void forcedPasswordOverridesEvenAnAdministratorViewAndLocalAuthorities() {
        authenticate();
        when(identities.requireCurrent()).thenReturn(new ActiveIdentity(context, "0", "0", "0"));
        when(evaluator.passwordChangeRequired(context)).thenReturn(true);
        when(evaluator.evaluateForExecution(context, true)).thenReturn(new AuthorizationEvaluator.AuthorizationView(
                List.of("iam-platform:member:read"), List.of(), Map.of(), "v", Instant.now().plusSeconds(30), List.of(), true));
        var snapshot = loader.assemble(0L, 1L);
        assertTrue(snapshot.getPasswordChangeRequired());
        assertFalse(snapshot.isPlatformAdministrator());
        assertTrue(snapshot.getPermissionCodes().isEmpty());
        assertEquals(java.util.Set.of(com.ingot.framework.commons.constants.PermissionConstants.INIT_PASSWORD),
                new LocalTrustedAuthoritySource(identities, evaluator).currentAuthorities());
    }

    private void authenticate() {
        var user = InUser.stateless(1L, null, "web", "standard", "0", "user", List.of(), List.of(), Map.of())
                .toBuilder().authorizationContext(context).build();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, "unused", List.of()));
    }
}
