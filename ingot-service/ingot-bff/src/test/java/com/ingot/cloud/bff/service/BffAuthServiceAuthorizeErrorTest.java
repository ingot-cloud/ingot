package com.ingot.cloud.bff.service;

import java.util.List;

import com.ingot.cloud.auth.api.rpc.RemoteAuthSessionService;
import com.ingot.cloud.auth.api.rpc.RemoteAuthTokenService;
import com.ingot.cloud.bff.config.AccountLockBffProperties;
import com.ingot.cloud.bff.config.BffAppRegistry;
import com.ingot.cloud.bff.config.BffProperties;
import com.ingot.cloud.bff.error.BffAuthException;
import com.ingot.cloud.bff.model.AuthBinding;
import com.ingot.cloud.bff.model.LoginTransaction;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.model.bff.BffErrorCode;
import com.ingot.framework.commons.model.common.TenantMainDTO;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.feign.exception.InFeignException;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * authorize / token 窗口失效时映射为事务过期，而不是身份不可用。
 *
 * @author jy
 * @since 1.0.0
 */
class BffAuthServiceAuthorizeErrorTest {

    private RemoteAuthTokenService remoteAuthTokenService;
    private BffAuthService service;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        remoteAuthTokenService = mock(RemoteAuthTokenService.class);
        BffAppRegistry registry = mock(BffAppRegistry.class);
        BffAppRegistration app = new BffAppRegistration();
        app.setAppId("tenant-admin");
        app.setDomain(AuthorizationDomain.TENANT);
        app.setOauthClientId("in-bff-tenant");
        app.setOauthRedirectUri("http://localhost:5400/bff/auth/tenant/callback");
        app.setOauthScope("system");
        when(registry.requireFromRequest(any())).thenReturn(app);

        LoginTransactionService transactionService = mock(LoginTransactionService.class);
        LoginTransaction transaction = new LoginTransaction();
        transaction.setTransactionId("tx-1");
        transaction.setAppId("tenant-admin");
        transaction.setStage("SELECT_TENANT");
        transaction.setCodeVerifier("verifier");
        transaction.setState("state");
        transaction.setAuthCookie("JSESSIONID=abc");
        TenantMainDTO allow = new TenantMainDTO();
        allow.setId("t1");
        allow.setName("租户一");
        transaction.setAllows(List.of(allow));
        when(transactionService.require("tx-1")).thenReturn(transaction);

        BffSessionService sessionService = mock(BffSessionService.class);
        AuthBinding binding = new AuthBinding();
        binding.setBindingId("bind-1");
        binding.setAppId("tenant-admin");
        binding.setCsrfToken("csrf-1");
        when(sessionService.getBindingIdFromCookie(any())).thenReturn("bind-1");
        when(transactionService.requireBinding("bind-1")).thenReturn(binding);

        service = new BffAuthService(
                new BffProperties(),
                registry,
                sessionService,
                transactionService,
                remoteAuthTokenService,
                mock(RemoteAuthSessionService.class),
                mock(AccountLockSignalPort.class),
                new AccountLockBffProperties());
        request = mock(HttpServletRequest.class);
        when(request.getHeader(BffConstants.CSRF_HEADER)).thenReturn("csrf-1");
    }

    @Test
    void selectTenant_authorizeFeignError_throwsTransactionExpired() {
        when(remoteAuthTokenService.authorize(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new InFeignException("invalid_request", "OAuth 2.0 Parameter: state"));

        BffAuthException error = assertThrows(BffAuthException.class,
                () -> service.selectTenant("t1", "tx-1", request, mock(HttpServletResponse.class)));

        assertEquals(BffErrorCode.TRANSACTION_EXPIRED, error.getErrorCode());
    }

    @Test
    void selectTenant_authorizeUnsuccessfulResult_throwsTransactionExpired() {
        when(remoteAuthTokenService.authorize(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(R.error500());

        BffAuthException error = assertThrows(BffAuthException.class,
                () -> service.selectTenant("t1", "tx-1", request, mock(HttpServletResponse.class)));

        assertEquals(BffErrorCode.TRANSACTION_EXPIRED, error.getErrorCode());
    }
}
