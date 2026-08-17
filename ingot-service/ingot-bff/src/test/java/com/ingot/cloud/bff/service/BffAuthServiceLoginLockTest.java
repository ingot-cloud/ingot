package com.ingot.cloud.bff.service;

import com.ingot.cloud.bff.client.AuthClient;
import com.ingot.cloud.bff.config.AccountLockBffProperties;
import com.ingot.cloud.bff.config.BffProperties;
import com.ingot.cloud.bff.model.dto.BffLoginDTO;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        AuthClient authClient = mock(AuthClient.class);
        when(signalPort.isLockedByUsername(UserTypeEnum.ADMIN, "admin")).thenReturn(true);

        BffAuthService service = new BffAuthService(
                properties,
                mock(BffSessionService.class),
                authClient,
                signalPort,
                lockProps);

        BffLoginDTO dto = new BffLoginDTO();
        dto.setUsername("admin");
        dto.setPassword("x");

        R<?> result = service.login(dto, mock(HttpServletRequest.class), mock(HttpServletResponse.class));

        assertFalse(result.isSuccess());
        assertEquals("ACCOUNT_LOCKED", result.getCode());
        verifyNoInteractions(authClient);
    }
}
