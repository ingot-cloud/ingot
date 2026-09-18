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
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * BFF 登录锁定短路单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class BffAuthServiceLoginLockTest {

    @Test
    void login_locked_skipsPreAuthorize() {
        BffProperties properties = new BffProperties();
        properties.setUserType(UserTypeEnum.ADMIN.getValue());
        AccountLockBffProperties lockProps = new AccountLockBffProperties();
        lockProps.setEnabled(true);
        lockProps.setEmitLoginFailureOnBffBlock(false);
        AccountLockSignalPort signalPort = mock(AccountLockSignalPort.class);
        RemoteAuthTokenService remoteAuthTokenService = mock(RemoteAuthTokenService.class);
        when(signalPort.isLockedByUsername(UserTypeEnum.ADMIN, "admin")).thenReturn(true);

        BffAppRegistry registry = mock(BffAppRegistry.class);
        BffAppRegistration app = new BffAppRegistration();
        app.setAppId("platform-admin");
        app.setDomain(AuthorizationDomain.PLATFORM);
        when(registry.requireFromRequest(any())).thenReturn(app);

        LoginTransactionService transactionService = mock(LoginTransactionService.class);
        LoginTransaction transaction = new LoginTransaction();
        transaction.setTransactionId("tx-1");
        transaction.setAppId("platform-admin");
        transaction.setStage("LOGIN");
        when(transactionService.require("tx-1")).thenReturn(transaction);

        BffSessionService sessionService = mock(BffSessionService.class);
        AuthBinding binding = new AuthBinding();
        binding.setBindingId("bind-1");
        binding.setAppId("platform-admin");
        binding.setCsrfToken("csrf-1");
        when(sessionService.getBindingIdFromCookie(any())).thenReturn("bind-1");
        when(transactionService.requireBinding("bind-1")).thenReturn(binding);

        BffAuthService service = new BffAuthService(
                properties,
                registry,
                sessionService,
                transactionService,
                remoteAuthTokenService,
                mock(RemoteAuthSessionService.class),
                signalPort,
                lockProps);

        BffLoginDTO dto = new BffLoginDTO();
        dto.setTransactionId("tx-1");
        dto.setUsername("admin");
        dto.setPassword("x");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(BffConstants.CSRF_HEADER)).thenReturn("csrf-1");

        R<?> result = service.login(BffConstants.ENTRY_PLATFORM, dto, request, mock(jakarta.servlet.http.HttpServletResponse.class));

        assertFalse(result.isSuccess());
        assertEquals("ACCOUNT_LOCKED", result.getCode());
        verifyNoInteractions(remoteAuthTokenService);
    }
}
