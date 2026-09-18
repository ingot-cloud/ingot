package com.ingot.cloud.bff.service;

import java.util.Map;

import com.ingot.cloud.auth.api.rpc.RemoteAuthSessionService;
import com.ingot.cloud.auth.api.rpc.RemoteAuthTokenService;
import com.ingot.cloud.bff.config.AccountLockBffProperties;
import com.ingot.cloud.bff.config.BffAppRegistry;
import com.ingot.cloud.bff.config.BffProperties;
import com.ingot.cloud.bff.model.AuthBinding;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CSRF 签发时轮换并清理旧浏览器绑定。
 *
 * @author jy
 * @since 1.0.0
 */
class BffAuthServiceCsrfTest {

    private LoginTransactionService transactionService;
    private BffSessionService sessionService;
    private BffAuthService service;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        transactionService = mock(LoginTransactionService.class);
        sessionService = mock(BffSessionService.class);
        BffAppRegistry registry = mock(BffAppRegistry.class);
        BffAppRegistration app = new BffAppRegistration();
        app.setAppId("tenant-admin");
        app.setDomain(AuthorizationDomain.TENANT);
        when(registry.requireFromRequest(any())).thenReturn(app);
        when(registry.requireEntryRole(any())).thenReturn(BffConstants.ENTRY_ADMIN);
        when(transactionService.ttlSeconds()).thenReturn(600L);
        service = new BffAuthService(
                new BffProperties(),
                registry,
                sessionService,
                transactionService,
                mock(RemoteAuthTokenService.class),
                mock(RemoteAuthSessionService.class),
                mock(AccountLockSignalPort.class),
                new AccountLockBffProperties());
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @Test
    void issueCsrf_withoutCookie_issuesNewBinding() {
        when(sessionService.getBindingIdFromCookie(request)).thenReturn(null);

        Map<String, Object> data = service.issueCsrf(request, response);

        verify(transactionService).deleteBinding(null);
        verify(transactionService).saveBinding(any(), eq(600L));
        assertNotNull(data.get("csrfToken"));
    }

    @Test
    void issueCsrf_withCookie_deletesPreviousBindingThenIssuesNew() {
        when(sessionService.getBindingIdFromCookie(request)).thenReturn("bind-old");

        Map<String, Object> data = service.issueCsrf(request, response);

        verify(transactionService).deleteBinding("bind-old");
        ArgumentCaptor<AuthBinding> captor = ArgumentCaptor.forClass(AuthBinding.class);
        verify(transactionService).saveBinding(captor.capture(), eq(600L));
        assertNotEquals("bind-old", captor.getValue().getBindingId());
        assertEquals("tenant-admin", captor.getValue().getAppId());
        assertEquals(data.get("csrfToken"), captor.getValue().getCsrfToken());
        verify(sessionService).writeBindingCookie(
                eq(captor.getValue().getBindingId()), eq(600L), eq(response));
        verify(sessionService, never()).writeBindingCookie(eq("bind-old"), anyLong(), any());
    }
}
