package com.ingot.cloud.bff.service;

import com.ingot.cloud.auth.api.rpc.RemoteAuthSessionService;
import com.ingot.cloud.auth.api.rpc.RemoteAuthTokenService;
import com.ingot.cloud.bff.config.AccountLockBffProperties;
import com.ingot.cloud.bff.config.BffAppRegistry;
import com.ingot.cloud.bff.config.BffProperties;
import com.ingot.cloud.bff.model.AuthBinding;
import com.ingot.cloud.bff.model.LoginTransaction;
import com.ingot.cloud.bff.model.dto.BffLoginDTO;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.model.status.BaseErrorCode;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.feign.exception.InFeignException;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Auth 预授权失败时 BFF 透传业务错误，而不是改写为 {@code S0500}。
 *
 * @author jy
 * @since 1.0.0
 */
class BffAuthServicePreAuthorizeErrorTest {

    private RemoteAuthTokenService remoteAuthTokenService;
    private BffAuthService service;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        BffProperties properties = new BffProperties();
        properties.setUserType(UserTypeEnum.ADMIN.getValue());
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
        transaction.setStage("LOGIN");
        transaction.setCodeVerifier("verifier");
        transaction.setState("state");
        when(transactionService.require("tx-1")).thenReturn(transaction);

        BffSessionService sessionService = mock(BffSessionService.class);
        AuthBinding binding = new AuthBinding();
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
                remoteAuthTokenService,
                mock(RemoteAuthSessionService.class),
                mock(AccountLockSignalPort.class),
                new AccountLockBffProperties());
        request = mock(HttpServletRequest.class);
        when(request.getHeader(BffConstants.CSRF_HEADER)).thenReturn("csrf-1");
    }

    @Test
    void login_preAuthorizeFeignError_preservesCodeAndMessage() {
        when(remoteAuthTokenService.preAuthorize(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any())).thenThrow(new InFeignException("S0400", "用户名或密码错误"));

        R<?> result = service.login(BffConstants.ENTRY_TENANT, loginDto(), request, mock(HttpServletResponse.class));

        assertFalse(result.isSuccess());
        assertEquals("S0400", result.getCode());
        assertEquals("用户名或密码错误", result.getMessage());
    }

    @Test
    void login_preAuthorizeUnexpectedError_returnsUnknown() {
        when(remoteAuthTokenService.preAuthorize(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any())).thenThrow(new IllegalStateException("boom"));

        R<?> result = service.login(BffConstants.ENTRY_TENANT, loginDto(), request, mock(HttpServletResponse.class));

        assertFalse(result.isSuccess());
        assertEquals(BaseErrorCode.INTERNAL_SERVER_ERROR.getCode(), result.getCode());
        assertEquals(BaseErrorCode.INTERNAL_SERVER_ERROR.getText(), result.getMessage());
    }

    private static BffLoginDTO loginDto() {
        BffLoginDTO dto = new BffLoginDTO();
        dto.setTransactionId("tx-1");
        dto.setUsername("owner");
        dto.setPassword("bad");
        return dto;
    }
}
