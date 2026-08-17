package com.ingot.framework.security.account.domain.service;

import java.util.Optional;

import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.model.AccountSecurityEvent;
import com.ingot.framework.security.account.domain.model.UserAccount;
import com.ingot.framework.security.account.domain.model.enums.EventSource;
import com.ingot.framework.security.account.domain.model.enums.SecurityEventType;
import com.ingot.framework.security.account.domain.port.inbound.ManageAccountStatusUseCase;
import com.ingot.framework.security.account.domain.port.outbound.SecurityEventPort;
import com.ingot.framework.security.account.domain.port.outbound.UserAccountPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ManageAccountStatusUseCaseService} 启禁用边沿检测单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class ManageAccountStatusUseCaseServiceTest {

    private final UserAccountPort userAccountPort = mock(UserAccountPort.class);
    private final SecurityEventPort securityEventPort = mock(SecurityEventPort.class);

    private ManageAccountStatusUseCaseService service;

    @BeforeEach
    void setUp() {
        service = new ManageAccountStatusUseCaseService(userAccountPort, securityEventPort);
    }

    @Test
    void enableAccount_alreadyEnabled_isNoOp() {
        when(userAccountPort.findById(1L, UserTypeEnum.ADMIN))
                .thenReturn(Optional.of(UserAccount.builder().enabled(true).build()));

        service.enableAccount(statusCommand());

        verify(userAccountPort, never()).updateStatus(anyLong(), any(), anyBoolean());
        verify(securityEventPort, never()).publishEvent(any());
    }

    @Test
    void enableAccount_disabled_publishesEnabled() {
        when(userAccountPort.findById(1L, UserTypeEnum.ADMIN))
                .thenReturn(Optional.of(UserAccount.builder().enabled(false).build()));

        service.enableAccount(statusCommand());

        ArgumentCaptor<AccountSecurityEvent> captor = ArgumentCaptor.forClass(AccountSecurityEvent.class);
        verify(securityEventPort).publishEvent(captor.capture());
        assertEquals(SecurityEventType.ACCOUNT_ENABLED, captor.getValue().getEventType());
        verify(userAccountPort).updateStatus(1L, UserTypeEnum.ADMIN, true);
    }

    @Test
    void disableAccount_alreadyDisabled_isNoOp() {
        when(userAccountPort.findById(1L, UserTypeEnum.ADMIN))
                .thenReturn(Optional.of(UserAccount.builder().enabled(false).build()));

        service.disableAccount(statusCommand());

        verify(userAccountPort, never()).updateStatus(anyLong(), any(), anyBoolean());
        verify(securityEventPort, never()).publishEvent(any());
    }

    @Test
    void disableAccount_enabled_publishesDisabled() {
        when(userAccountPort.findById(1L, UserTypeEnum.ADMIN))
                .thenReturn(Optional.of(UserAccount.builder().enabled(true).build()));

        service.disableAccount(statusCommand());

        ArgumentCaptor<AccountSecurityEvent> captor = ArgumentCaptor.forClass(AccountSecurityEvent.class);
        verify(securityEventPort).publishEvent(captor.capture());
        assertEquals(SecurityEventType.ACCOUNT_DISABLED, captor.getValue().getEventType());
        verify(userAccountPort).updateStatus(1L, UserTypeEnum.ADMIN, false);
    }

    private static ManageAccountStatusUseCase.StatusCommand statusCommand() {
        return ManageAccountStatusUseCase.StatusCommand.builder()
                .userId(1L)
                .userType(UserTypeEnum.ADMIN)
                .reason("test")
                .operatorId(9L)
                .operatorName("admin")
                .source(EventSource.PMS)
                .build();
    }
}
