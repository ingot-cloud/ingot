package com.ingot.cloud.iam.api.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.cloud.iam.api.rpc.RemoteIamAuthorizationService;
import com.ingot.framework.commons.constants.PermissionConstants;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.server.resource.access.expression.TrustedAuthoritySource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <p>Auth/BFF 无数据范围 SDK 时仍获得实时门禁，本地来源不被覆盖，RPC 失败拒绝授权。</p>
 * @author jy
 * @since 1.0.0
 */
class OnlineAuthorityConfigurationTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void nonSdkServiceGetsLiveStatusAndAdministratorCannotBypassIt() {
        authenticate();
        var remote = mock(RemoteIamAuthorizationService.class);
        var snapshot = new AuthorizationSnapshotDTO();
        snapshot.setTenantId(0L); snapshot.setUserId(1L); snapshot.setExpiresAt(Instant.now().plusSeconds(30));
        snapshot.setPlatformAdministrator(true); snapshot.setPasswordChangeRequired(true);
        when(remote.snapshot(any())).thenReturn(R.ok(snapshot));
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(OnlineAuthorityConfiguration.class))
                .withBean(RemoteIamAuthorizationService.class, () -> remote).run(context -> {
                    assertNull(context.getStartupFailure());
                    var source = context.getBean(TrustedAuthoritySource.class);
                    assertTrue(source.requiresPasswordChange());
                    assertEquals(Set.of(PermissionConstants.INIT_PASSWORD), source.currentAuthorities());
                    snapshot.setPasswordChangeRequired(false);
                    assertTrue(source.currentAuthorities().contains(RoleConstants.ROLE_ADMIN_CODE));
                    verify(remote, atLeastOnce()).snapshot(argThat(input -> input.getTenantId().equals(0L) && input.getUserId().equals(1L)));
                });
    }

    @Test void localSourceTakesPrecedenceWithoutCallingRemote() {
        var remote = mock(RemoteIamAuthorizationService.class);
        TrustedAuthoritySource local = () -> Set.of("local");
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(OnlineAuthorityConfiguration.class))
                .withBean(RemoteIamAuthorizationService.class, () -> remote)
                .withBean(TrustedAuthoritySource.class, () -> local).run(context -> {
                    assertSame(local, context.getBean(TrustedAuthoritySource.class));
                    assertEquals(1, context.getBeansOfType(TrustedAuthoritySource.class).size());
                    verifyNoInteractions(remote);
                });
    }

    @Test void missingAndFailedRpcNeverFallBackToTokenAuthority() {
        authenticate();
        var remote = mock(RemoteIamAuthorizationService.class);
        var source = new OnlineAuthorityConfiguration().iamOnlineTrustedAuthoritySource(remote);
        assertThrows(RuntimeException.class, source::requiresPasswordChange);
        when(remote.snapshot(any())).thenReturn(R.error("failed", "unavailable"));
        assertThrows(RuntimeException.class, source::currentAuthorities);
    }

    private void authenticate() {
        var user = InUser.stateless(1L, null, "web", "standard", "0", "user", List.of(), List.of(), Map.of())
                .toBuilder().authorizationContext(new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "2")).build();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, "unused", List.of()));
    }
}
