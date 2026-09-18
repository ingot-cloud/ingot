package com.ingot.framework.security.oauth2.server.authorization.authentication;

import java.util.List;
import java.util.Map;

import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.core.userdetails.OAuth2UserDetailsServiceManager;
import com.ingot.framework.security.oauth2.core.OAuth2Authentication;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>验证租户签发会重载成员上下文，而不会把管理用户切到旧预授权切片。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class AuthenticatedMemberBinderTest {

    private static final AuthorizationContext TENANT_CONTEXT =
            new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");

    @Test
    void tenantAdminReloadBindsMemberContextAndKeepsClient() {
        OAuth2UserDetailsServiceManager users = mock(OAuth2UserDetailsServiceManager.class);
        InUser loaded = InUser.stateless(1L, 10L, "N/A", "N/A", UserTypeEnum.ADMIN.getValue(),
                        "owner", List.of(), List.of(11L), Map.of())
                .toBuilder().authorizationContext(TENANT_CONTEXT).build();
        when(users.loadUser(any())).thenReturn(loaded);

        InUser bound = new AuthenticatedMemberBinder(users)
                .resolveForIssuance(selectionUser(), 10L);

        assertEquals(TENANT_CONTEXT, bound.getAuthorizationContext());
        assertEquals("in-bff-tenant", bound.getClientId());
        assertEquals("standard", bound.getTokenAuthType());
        assertEquals(10L, bound.getTenantId());
        ArgumentCaptor<OAuth2Authentication> captor = ArgumentCaptor.forClass(OAuth2Authentication.class);
        verify(users).loadUser(captor.capture());
        String name = captor.getValue().getName();
        assertTrue(name.contains("tenant=10"));
        assertTrue(name.contains("domain=TENANT"));
    }

    @Test
    void platformContextIsKeptWithoutReload() {
        OAuth2UserDetailsServiceManager users = mock(OAuth2UserDetailsServiceManager.class);
        InUser platform = InUser.stateless(1L, null, "in-bff-platform", "standard",
                        UserTypeEnum.ADMIN.getValue(), "ops", List.of(), List.of(), Map.of())
                .toBuilder()
                .authorizationContext(new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "9"))
                .build();

        InUser resolved = new AuthenticatedMemberBinder(users).resolveForIssuance(platform, null);

        assertSame(platform, resolved);
        verify(users, never()).loadUser(any());
    }

    @Test
    void boundIdentityCannotSwitchTenant() {
        OAuth2UserDetailsServiceManager users = mock(OAuth2UserDetailsServiceManager.class);
        InUser tenant = InUser.stateless(1L, 10L, "in-bff-tenant", "standard",
                        UserTypeEnum.ADMIN.getValue(), "owner", List.of(), List.of(11L), Map.of())
                .toBuilder().authorizationContext(TENANT_CONTEXT).build();

        assertThrows(OAuth2AuthenticationException.class,
                () -> new AuthenticatedMemberBinder(users).resolveForIssuance(tenant, 20L));
        verify(users, never()).loadUser(any());
    }

    @Test
    void missingMemberContextIsRejected() {
        OAuth2UserDetailsServiceManager users = mock(OAuth2UserDetailsServiceManager.class);
        when(users.loadUser(any())).thenReturn(selectionUser());

        assertThrows(OAuth2AuthenticationException.class,
                () -> new AuthenticatedMemberBinder(users).resolveForIssuance(selectionUser(), 10L));
    }

    @Test
    void appUserStillUsesLegacyTenantSlice() {
        OAuth2UserDetailsServiceManager users = mock(OAuth2UserDetailsServiceManager.class);
        InUser app = InUser.stateless(2L, null, "app", "standard", UserTypeEnum.APP.getValue(),
                "member", List.of(), List.of(1L), Map.of(10L, List.of(8L)));

        InUser sliced = new AuthenticatedMemberBinder(users).resolveForIssuance(app, 10L);

        assertNull(sliced.getAuthorizationContext());
        assertEquals(10L, sliced.getTenantId());
        assertEquals(List.of(8L), sliced.getDeptIds());
        verify(users, never()).loadUser(any());
    }

    private static InUser selectionUser() {
        return new InUser(1L, null, "in-bff-tenant", "standard", UserTypeEnum.ADMIN.getValue(),
                "owner", "n", true, true, true, true, List.of(), null, null, null);
    }
}
