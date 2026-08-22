package com.ingot.framework.security.account.domain.service;

import java.util.Optional;

import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.model.AccountSecurityEvent;
import com.ingot.framework.security.account.domain.model.LockState;
import com.ingot.framework.security.account.domain.model.enums.EventSource;
import com.ingot.cloud.security.api.model.enums.SecurityEventType;
import com.ingot.framework.security.account.domain.port.inbound.UnlockAccountUseCase;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import com.ingot.framework.security.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.security.account.domain.port.outbound.SecurityEventPort;
import com.ingot.framework.security.account.domain.port.outbound.UserAccountPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link UnlockAccountUseCaseService} 边沿检测单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class UnlockAccountUseCaseServiceTest {

    private final UserAccountPort userAccountPort = mock(UserAccountPort.class);
    private final LockStatePort lockStatePort = mock(LockStatePort.class);
    private final SecurityEventPort securityEventPort = mock(SecurityEventPort.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final AccountLockSignalPort accountLockSignalPort = mock(AccountLockSignalPort.class);

    private UnlockAccountUseCaseService service;

    @BeforeEach
    void setUp() {
        service = new UnlockAccountUseCaseService(
                userAccountPort, lockStatePort, securityEventPort, transactionManager, accountLockSignalPort);
    }

    @Test
    void unlockManually_alreadyUnlocked_isNoOp() {
        when(lockStatePort.findByUser(1L, UserTypeEnum.ADMIN))
                .thenReturn(Optional.of(LockState.builder().locked(false).build()));

        service.unlockManually(UnlockAccountUseCase.UnlockCommand.builder()
                .userId(1L)
                .userType(UserTypeEnum.ADMIN)
                .reason("noop")
                .operatorId(9L)
                .operatorName("admin")
                .source(EventSource.PMS)
                .build());

        verify(lockStatePort, never()).updateLockStatus(
                anyLong(), any(), anyBoolean(), any(), any(), any(), any(), any());
        verify(securityEventPort, never()).publishEvent(any());
    }

    @Test
    void unlockManually_locked_publishesAccountUnlocked() {
        when(lockStatePort.findByUser(1L, UserTypeEnum.ADMIN))
                .thenReturn(Optional.of(LockState.builder().locked(true).build()));

        service.unlockManually(UnlockAccountUseCase.UnlockCommand.builder()
                .userId(1L)
                .userType(UserTypeEnum.ADMIN)
                .reason("manual unlock")
                .operatorId(9L)
                .operatorName("admin")
                .source(EventSource.PMS)
                .build());

        ArgumentCaptor<AccountSecurityEvent> captor = ArgumentCaptor.forClass(AccountSecurityEvent.class);
        verify(securityEventPort).publishEvent(captor.capture());
        assertEquals(SecurityEventType.ACCOUNT_UNLOCKED, captor.getValue().getEventType());
        verify(userAccountPort).updateLockStatus(1L, UserTypeEnum.ADMIN, false);
    }
}
