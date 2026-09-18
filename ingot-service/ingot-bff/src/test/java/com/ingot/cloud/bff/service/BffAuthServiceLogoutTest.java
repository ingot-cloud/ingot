package com.ingot.cloud.bff.service;

import com.ingot.cloud.auth.api.model.dto.InnerSessionRevokeDTO;
import com.ingot.cloud.auth.api.rpc.RemoteAuthSessionService;
import com.ingot.cloud.auth.api.rpc.RemoteAuthTokenService;
import com.ingot.cloud.bff.config.AccountLockBffProperties;
import com.ingot.cloud.bff.config.BffAppRegistry;
import com.ingot.cloud.bff.config.BffProperties;
import com.ingot.cloud.bff.model.AuthBinding;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.model.bff.BffSession;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.security.SessionRevokeReason;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * BFF 登出撤销链路单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class BffAuthServiceLogoutTest {

    private static final String SID = "sid-1";
    private static final String AUTH_COOKIE = "JSESSIONID=abc";

    private BffSessionService sessionService;
    private RemoteAuthSessionService remoteAuthSessionService;
    private BffAuthService service;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        sessionService = mock(BffSessionService.class);
        remoteAuthSessionService = mock(RemoteAuthSessionService.class);
        LoginTransactionService transactionService = mock(LoginTransactionService.class);
        BffAppRegistry registry = mock(BffAppRegistry.class);
        BffAppRegistration app = new BffAppRegistration();
        app.setAppId("platform-admin");
        app.setDomain(AuthorizationDomain.PLATFORM);
        when(registry.requireFromRequest(any())).thenReturn(app);
        AuthBinding binding = new AuthBinding();
        binding.setBindingId("bind-1");
        binding.setAppId("platform-admin");
        binding.setCsrfToken("csrf-1");
        when(sessionService.getBindingIdFromCookie(any())).thenReturn("bind-1");
        when(transactionService.requireBinding("bind-1")).thenReturn(binding);
        service = new BffAuthService(
                new BffProperties(),
                registry,
                sessionService,
                transactionService,
                mock(RemoteAuthTokenService.class),
                remoteAuthSessionService,
                mock(AccountLockSignalPort.class),
                new AccountLockBffProperties());
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(request.getHeader(BffConstants.CSRF_HEADER)).thenReturn("csrf-1");
    }

    @Test
    void logout_withSid_revokesThroughInnerApi() {
        when(sessionService.getSession(request)).thenReturn(session(SID));

        R<?> result = service.logout(request, response);

        ArgumentCaptor<InnerSessionRevokeDTO> captor = ArgumentCaptor.forClass(InnerSessionRevokeDTO.class);
        verify(remoteAuthSessionService).revokeBySid(eq(AUTH_COOKIE), eq(SID), captor.capture());
        assertEquals(SessionRevokeReason.USER_LOGOUT, captor.getValue().getReason());
        verify(sessionService).removeSession(request, response);
        assertTrue(result.isSuccess());
    }

    @Test
    void logout_withoutSid_skipsRevoke() {
        when(sessionService.getSession(request)).thenReturn(session(null));

        R<?> result = service.logout(request, response);

        verifyNoInteractions(remoteAuthSessionService);
        verify(sessionService).removeSession(request, response);
        assertTrue(result.isSuccess());
    }

    @Test
    void logout_withoutSession_skipsRevoke() {
        when(sessionService.getSession(request)).thenReturn(null);

        service.logout(request, response);

        verifyNoInteractions(remoteAuthSessionService);
        verify(sessionService).removeSession(request, response);
    }

    @Test
    void logout_authFailure_stillClearsLocalSession() {
        when(sessionService.getSession(request)).thenReturn(session(SID));
        doThrow(new IllegalStateException("auth down"))
                .when(remoteAuthSessionService).revokeBySid(anyString(), anyString(), any());

        R<?> result = service.logout(request, response);

        verify(sessionService, times(1)).removeSession(request, response);
        assertTrue(result.isSuccess());
    }

    private static BffSession session(String sid) {
        BffSession session = new BffSession();
        session.setSid(sid);
        session.setAuthCookie(AUTH_COOKIE);
        session.setAccessToken("token");
        return session;
    }
}
