package com.ingot.framework.data.mybatis.scope.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.security.core.userdetails.InUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>在线快照的超管事实独立于业务码，租户、旧缓存和故障不能提升接口资格。</p>
 * @author jy
 * @since 1.0.0
 */
class RemoteTrustedAuthoritySourceTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void administratorRequiresSeparateServerFactAndCurrentSnapshot() {
        authenticate(AuthorizationDomain.PLATFORM);
        var snapshot = snapshot(0);
        var source = new RemoteTrustedAuthoritySource((tenant, user) -> snapshot);
        snapshot.setPermissionCodes(Set.of(RoleConstants.ROLE_ADMIN_CODE));
        assertFalse(source.currentAuthorities().contains(RoleConstants.ROLE_ADMIN_CODE));
        snapshot.setPlatformAdministrator(true);
        assertTrue(source.currentAuthorities().contains(RoleConstants.ROLE_ADMIN_CODE));
        snapshot.setPlatformAdministrator(false);
        assertFalse(source.currentAuthorities().contains(RoleConstants.ROLE_ADMIN_CODE));
        snapshot.setExpiresAt(Instant.now().minusSeconds(1));
        assertThrows(AuthorizationDeniedException.class, source::currentAuthorities);
    }

    @Test void tenantCannotInheritPlatformFactAndIdentityMismatchIsRejected() {
        authenticate(AuthorizationDomain.TENANT);
        var snapshot = snapshot(10);
        snapshot.setPlatformAdministrator(true);
        snapshot.setPermissionCodes(Set.of(RoleConstants.ROLE_ADMIN_CODE, "iam-tenant:member:read"));
        var source = new RemoteTrustedAuthoritySource((tenant, user) -> snapshot);
        assertEquals(Set.of("iam-tenant:member:read"), source.currentAuthorities());
        snapshot.setUserId(999L);
        assertThrows(AuthorizationDeniedException.class, source::currentAuthorities);
    }

    @Test void missingAndUnavailableSnapshotsNeverUseJwtRoles() {
        authenticate(AuthorizationDomain.PLATFORM);
        assertThrows(AuthorizationDeniedException.class,
                () -> new RemoteTrustedAuthoritySource((tenant, user) -> null).currentAuthorities());
        assertThrows(IllegalStateException.class,
                () -> new RemoteTrustedAuthoritySource((tenant, user) -> { throw new IllegalStateException("offline"); })
                        .currentAuthorities());
    }

    @Test void forcedAndMissingPasswordStateNeverAllowBusinessOrAdministratorAndRequestSharesSnapshot() {
        authenticate(AuthorizationDomain.PLATFORM);
        var snapshot = snapshot(0);
        snapshot.setPlatformAdministrator(true);
        snapshot.setPermissionCodes(Set.of("iam-platform:member:read"));
        snapshot.setPasswordChangeRequired(true);
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var source = new RemoteTrustedAuthoritySource((tenant, user) -> { calls.incrementAndGet(); return snapshot; });
        var request = org.mockito.Mockito.mock(jakarta.servlet.http.HttpServletRequest.class);
        var attributes = new java.util.HashMap<String, Object>();
        org.mockito.Mockito.when(request.getAttribute(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(call -> attributes.get(call.getArgument(0)));
        org.mockito.Mockito.doAnswer(call -> { attributes.put(call.getArgument(0), call.getArgument(1)); return null; })
                .when(request).setAttribute(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
                new org.springframework.web.context.request.ServletRequestAttributes(request));
        try {
            assertTrue(source.requiresPasswordChange());
            assertEquals(Set.of(com.ingot.framework.commons.constants.PermissionConstants.INIT_PASSWORD), source.currentAuthorities());
            assertEquals(1, calls.get());
        } finally {
            org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        }
        snapshot.setPasswordChangeRequired(null);
        assertThrows(AuthorizationDeniedException.class, source::currentAuthorities);
    }

    private AuthorizationSnapshotDTO snapshot(long tenant) {
        var snapshot = new AuthorizationSnapshotDTO();
        snapshot.setPasswordChangeRequired(false);
        snapshot.setUserId(1L); snapshot.setTenantId(tenant);
        snapshot.setExpiresAt(Instant.now().plusSeconds(30));
        return snapshot;
    }

    private void authenticate(AuthorizationDomain domain) {
        Long tenant = domain == AuthorizationDomain.TENANT ? 10L : null;
        var context = new AuthorizationContext(domain, tenant == null ? null : tenant.toString(), "1", "2");
        var user = InUser.stateless(1L, tenant, "web", "standard", "0", "user", List.of(), List.of(), Map.of())
                .toBuilder().authorizationContext(context).build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, "unused", List.of()));
    }
}
