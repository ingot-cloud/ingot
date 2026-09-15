package com.ingot.cloud.iam.identity;

import java.util.List;
import java.util.Map;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.userdetails.InUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>验证 IAM 入口只接受已认证且携带独立成员身份的安全上下文。</p>
 * @author jy
 * @since 1.0.0
 */
class CurrentIdentityServiceTest {
    private final ActiveIdentityService identities = mock(ActiveIdentityService.class);
    private final CurrentIdentityService service = new CurrentIdentityService(identities);
    private static final AuthorizationContext PLATFORM = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001");

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatedPrincipalIsStillRevalidatedInDatabase() {
        var user = user(UserTypeEnum.ADMIN).toBuilder().authorizationContext(PLATFORM).build();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
        when(identities.requireActive(PLATFORM)).thenThrow(new BizException(IamReasonCode.IDENTITY_INVALID));
        assertThrows(BizException.class, service::requireCurrent);
        verify(identities).requireActive(PLATFORM);
    }

    @Test
    void legacyAndAnonymousContextsCannotInventMembers() {
        assertThrows(BizException.class, service::requireCurrent);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user(UserTypeEnum.ADMIN), null, List.of()));
        assertThrows(BizException.class, service::requireCurrent);
        verifyNoInteractions(identities);
    }

    @Test
    void unauthenticatedPrincipalAndOtherUserSystemReject() {
        var user = user(UserTypeEnum.ADMIN).toBuilder().authorizationContext(PLATFORM).build();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.unauthenticated(user, null));
        assertThrows(BizException.class, service::requireCurrent);
        var other = user.toBuilder().userType(UserTypeEnum.APP.getValue()).build();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(other, null, List.of()));
        assertThrows(BizException.class, service::requireCurrent);
        verifyNoInteractions(identities);
    }

    @Test
    void platformManagementCannotEstablishTenantSession() {
        var user = user(UserTypeEnum.ADMIN).toBuilder().authorizationContext(PLATFORM).build();
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
        when(identities.requireActive(PLATFORM)).thenReturn(new ActiveIdentity(PLATFORM, "0", "0", null));
        assertThrows(BizException.class, () -> service.requireDomain(AuthorizationDomain.TENANT));
        assertEquals(PLATFORM, service.requireDomain(AuthorizationDomain.PLATFORM).context());
    }

    private InUser user(UserTypeEnum type) {
        return InUser.stateless(1L, null, "web", "standard", type.getValue(), "account", List.of(), List.of(), Map.of());
    }
}
