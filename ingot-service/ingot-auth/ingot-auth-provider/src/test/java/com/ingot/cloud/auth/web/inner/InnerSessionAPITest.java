package com.ingot.cloud.auth.web.inner;

import com.ingot.cloud.auth.api.model.dto.InnerSessionRevokeDTO;
import com.ingot.cloud.auth.api.model.dto.InnerUserSessionRevokeDTO;
import com.ingot.cloud.auth.service.biz.BizSessionService;
import com.ingot.framework.commons.model.security.SessionRevokeReason;
import com.ingot.framework.security.oauth2.server.authorization.session.SessionRevocationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.context.SecurityContextRevokeRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link InnerSessionAPI} 撤销路径单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class InnerSessionAPITest {

    private static final String SID = "session-1";

    private final BizSessionService bizSessionService = mock(BizSessionService.class);
    private final SessionRevocationService sessionRevocationService = mock(SessionRevocationService.class);
    private final SecurityContextRevokeRepository securityContextRevokeRepository =
            mock(SecurityContextRevokeRepository.class);
    private final InnerSessionAPI api = new InnerSessionAPI(
            bizSessionService, sessionRevocationService, securityContextRevokeRepository);

    @Test
    void revokeBySid_delegatesAndClearsSecurityContext() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        InnerSessionRevokeDTO params = new InnerSessionRevokeDTO();
        params.setReason(SessionRevokeReason.USER_LOGOUT);
        params.setActorId(9L);
        when(sessionRevocationService.revokeBySid(SID, SessionRevokeReason.USER_LOGOUT, 9L)).thenReturn(true);

        assertTrue(api.revokeBySid(request, SID, params).getData());

        verify(securityContextRevokeRepository).revokeContext(request);
    }

    @Test
    void revokeBySid_withoutReason_fallsBackToAdminRevoke() {
        api.revokeBySid(mock(HttpServletRequest.class), SID, new InnerSessionRevokeDTO());

        verify(sessionRevocationService).revokeBySid(SID, SessionRevokeReason.ADMIN_REVOKE, null);
    }

    @Test
    void revokeByUser_passesNullClientIdThrough() {
        InnerUserSessionRevokeDTO params = InnerUserSessionRevokeDTO.builder()
                .tenantId(1L)
                .userId(9L)
                .reason(SessionRevokeReason.PASSWORD_CHANGED)
                .build();
        when(sessionRevocationService.revokeByUser(
                1L, null, 9L, SessionRevokeReason.PASSWORD_CHANGED, null)).thenReturn(2);

        assertEquals(2, api.revokeByUser(params).getData());
    }

    @Test
    void revokeByTenant_isNotImplemented() {
        assertEquals(HttpStatus.NOT_IMPLEMENTED, api.revokeByTenant(1L).getStatusCode());

        verify(sessionRevocationService, never()).revokeByUser(
                anyLong(), anyString(), anyLong(), any(), isNull());
    }
}
