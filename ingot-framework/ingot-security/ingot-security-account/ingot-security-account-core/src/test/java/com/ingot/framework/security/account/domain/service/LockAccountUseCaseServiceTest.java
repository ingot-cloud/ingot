package com.ingot.framework.security.account.domain.service;

import java.util.Optional;

import com.ingot.framework.commons.model.security.SessionRevokeReason;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.model.AccountSecurityEvent;
import com.ingot.framework.security.account.domain.model.LockState;
import com.ingot.framework.security.account.domain.model.enums.EventSource;
import com.ingot.framework.security.account.domain.model.enums.LockReason;
import com.ingot.cloud.security.api.model.enums.SecurityEventType;
import com.ingot.framework.security.account.domain.port.inbound.LockAccountUseCase;
import com.ingot.framework.security.account.domain.port.outbound.AccountLockSignalPort;
import com.ingot.framework.security.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.security.account.domain.port.outbound.SecurityEventPort;
import com.ingot.framework.security.account.domain.port.outbound.SessionRevocationPort;
import com.ingot.framework.security.account.domain.port.outbound.UserAccountPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link LockAccountUseCaseService} 边沿检测单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class LockAccountUseCaseServiceTest {

    private final UserAccountPort userAccountPort = mock(UserAccountPort.class);
    private final LockStatePort lockStatePort = mock(LockStatePort.class);
    private final SecurityEventPort securityEventPort = mock(SecurityEventPort.class);
    private final AccountLockSignalPort accountLockSignalPort = mock(AccountLockSignalPort.class);
    private final SessionRevocationPort sessionRevocationPort = mock(SessionRevocationPort.class);

    private LockAccountUseCaseService service;

    @BeforeEach
    void setUp() {
        service = new LockAccountUseCaseService(
                userAccountPort, lockStatePort, securityEventPort,
                accountLockSignalPort, sessionRevocationPort);
    }

    @Test
    void lockManually_alreadyLocked_isNoOp() {
        when(lockStatePort.findByUser(1L, UserTypeEnum.ADMIN))
                .thenReturn(Optional.of(LockState.builder().locked(true).build()));

        service.lockManually(LockAccountUseCase.LockCommand.builder()
                .userId(1L)
                .userType(UserTypeEnum.ADMIN)
                .reason(LockReason.MANUAL_LOCK)
                .reasonDetail("manual")
                .operatorId(9L)
                .operatorName("admin")
                .source(EventSource.IAM)
                .build());

        verify(lockStatePort, never()).updateLockStatus(
                anyLong(), any(), anyBoolean(), any(), any(), any(), any(), any());
        verify(userAccountPort, never()).updateLockStatus(anyLong(), any(), anyBoolean());
        verify(securityEventPort, never()).publishEvent(any());
        verify(sessionRevocationPort, never()).revokeUserSessions(anyLong(), any(), any());
    }

    @Test
    void lockManually_unlocked_publishesAccountLocked() {
        when(lockStatePort.findByUser(1L, UserTypeEnum.ADMIN)).thenReturn(Optional.empty());

        service.lockManually(LockAccountUseCase.LockCommand.builder()
                .userId(1L)
                .userType(UserTypeEnum.ADMIN)
                .reason(LockReason.MANUAL_LOCK)
                .reasonDetail("manual")
                .operatorId(9L)
                .operatorName("admin")
                .source(EventSource.IAM)
                .build());

        ArgumentCaptor<AccountSecurityEvent> captor = ArgumentCaptor.forClass(AccountSecurityEvent.class);
        verify(securityEventPort).publishEvent(captor.capture());
        assertEquals(SecurityEventType.ACCOUNT_LOCKED, captor.getValue().getEventType());
        verify(userAccountPort).updateLockStatus(1L, UserTypeEnum.ADMIN, true);
        verify(sessionRevocationPort).revokeUserSessions(1L, SessionRevokeReason.ACCOUNT_LOCKED, 9L);
    }

    @Test
    void lockAutomatically_alreadyLocked_isNoOp() {
        when(lockStatePort.findByUser(2L, UserTypeEnum.ADMIN))
                .thenReturn(Optional.of(LockState.builder().locked(true).build()));

        service.lockAutomatically(2L, UserTypeEnum.ADMIN, LockReason.LOGIN_FAIL_EXCEED, 30);

        verify(securityEventPort, never()).publishEvent(any());
        verify(lockStatePort, never()).updateLockStatus(
                anyLong(), any(), anyBoolean(), any(), any(), any(), any(), any());
    }

    @Test
    void lockAutomatically_transition_publishesOnce() {
        when(lockStatePort.findByUser(2L, UserTypeEnum.ADMIN))
                .thenReturn(Optional.of(LockState.builder().locked(false).build()));

        service.lockAutomatically(2L, UserTypeEnum.ADMIN, LockReason.LOGIN_FAIL_EXCEED, 30);

        ArgumentCaptor<AccountSecurityEvent> captor = ArgumentCaptor.forClass(AccountSecurityEvent.class);
        verify(securityEventPort).publishEvent(captor.capture());
        assertEquals(SecurityEventType.ACCOUNT_LOCKED, captor.getValue().getEventType());
        verify(userAccountPort).updateLockStatus(eq(2L), eq(UserTypeEnum.ADMIN), eq(true));
        // 系统自动锁定无操作者
        verify(sessionRevocationPort).revokeUserSessions(2L, SessionRevokeReason.ACCOUNT_LOCKED, null);
    }
}
