package com.ingot.cloud.bff.service;

import java.time.Instant;
import java.util.Map;

import com.ingot.cloud.auth.api.rpc.RemoteAuthSessionService;
import com.ingot.cloud.auth.api.rpc.RemoteAuthTokenService;
import com.ingot.cloud.bff.config.AccountLockBffProperties;
import com.ingot.cloud.bff.config.BffAppRegistry;
import com.ingot.cloud.bff.config.BffProperties;
import com.ingot.cloud.bff.model.AuthBinding;
import com.ingot.cloud.bff.model.LoginTransaction;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * complete 成功后将 CSRF 绑定 TTL 延长到正式会话期限。
 *
 * @author jy
 * @since 1.0.0
 */
class BffAuthServiceCompleteTest {

    private BffSessionService sessionService;
    private LoginTransactionService transactionService;
    private BffAuthService service;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private AuthBinding binding;

    @BeforeEach
    void setUp() {
        BffProperties properties = new BffProperties();
        properties.setSessionTtl(7200);
        sessionService = mock(BffSessionService.class);
        transactionService = mock(LoginTransactionService.class);
        BffAppRegistry registry = mock(BffAppRegistry.class);
        BffAppRegistration app = new BffAppRegistration();
        app.setAppId("tenant-admin");
        app.setDomain(AuthorizationDomain.TENANT);
        app.setOauthClientId("in-bff-tenant");
        app.setDefaultReturnTo("/");
        when(registry.requireFromRequest(any())).thenReturn(app);

        LoginTransaction transaction = new LoginTransaction();
        transaction.setTransactionId("tx-1");
        transaction.setAppId("tenant-admin");
        transaction.setStage("READY");
        transaction.setAdminBindingId("bind-1");
        transaction.setTicket("ticket-1");
        transaction.setTicketExpiresAt(Instant.now().getEpochSecond() + 60);
        transaction.setAccessToken("access");
        when(transactionService.requireTransactionIdByTicket("ticket-1")).thenReturn("tx-1");
        when(transactionService.require("tx-1")).thenReturn(transaction);

        binding = new AuthBinding();
        binding.setBindingId("bind-1");
        binding.setAppId("tenant-admin");
        binding.setCsrfToken("csrf-1");
        when(sessionService.getBindingIdFromCookie(any())).thenReturn("bind-1");
        when(transactionService.requireBinding("bind-1")).thenReturn(binding);

        service = new BffAuthService(
                properties,
                registry,
                sessionService,
                transactionService,
                mock(RemoteAuthTokenService.class),
                mock(RemoteAuthSessionService.class),
                mock(AccountLockSignalPort.class),
                new AccountLockBffProperties());
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        when(request.getHeader(BffConstants.CSRF_HEADER)).thenReturn("csrf-1");
    }

    @Test
    void complete_extendsBindingToSessionTtl() {
        Map<String, Object> data = service.complete(BffConstants.ENTRY_TENANT, "ticket-1", request, response);

        assertEquals("/", data.get("returnTo"));
        verify(sessionService).createSession(any(), eq(request), eq(response));
        verify(transactionService).saveBinding(binding, 7200L);
        verify(sessionService).writeBindingCookie("bind-1", 7200L, response);
    }
}
