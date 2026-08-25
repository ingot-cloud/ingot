package com.ingot.framework.security.account.domain.service;

import java.util.Optional;

import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.model.AccountSecurityEvent;
import com.ingot.framework.security.account.domain.model.LockState;
import com.ingot.framework.security.account.domain.model.LockoutPolicy;
import com.ingot.cloud.security.api.model.enums.SecurityEventType;
import com.ingot.framework.security.account.domain.port.inbound.LockAccountUseCase;
import com.ingot.framework.security.account.domain.port.inbound.RecordLoginUseCase;
import com.ingot.framework.security.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.security.account.domain.port.outbound.SecurityEventPort;
import com.ingot.framework.security.account.domain.port.outbound.UserAccountPort;
import com.ingot.framework.security.credential.service.CredentialSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RecordLoginUseCaseService} 锁定后短路与 LOGIN_FAILURE 电平语义单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class RecordLoginUseCaseServiceTest {

    private final UserAccountPort userAccountPort = mock(UserAccountPort.class);
    private final LockStatePort lockStatePort = mock(LockStatePort.class);
    private final SecurityEventPort securityEventPort = mock(SecurityEventPort.class);
    private final LockAccountUseCase lockAccountUseCase = mock(LockAccountUseCase.class);
    private final AccountLockoutPolicyLoader lockoutPolicyLoader = mock(AccountLockoutPolicyLoader.class);
    private final CredentialSecurityService credentialSecurityService = mock(CredentialSecurityService.class);

    private RecordLoginUseCaseService service;

    @BeforeEach
    void setUp() {
        service = new RecordLoginUseCaseService(
                userAccountPort,
                lockStatePort,
                securityEventPort,
                lockAccountUseCase,
                lockoutPolicyLoader,
                credentialSecurityService);
        when(lockoutPolicyLoader.getLockoutPolicy(UserTypeEnum.ADMIN)).thenReturn(new LockoutPolicy(
                UserTypeEnum.ADMIN, true, 5, 30, 15, 3));
    }

    @Test
    void recordFailure_alreadyLocked_publishesLoginFailureOnly() {
        when(lockStatePort.findByUser(1L, UserTypeEnum.ADMIN))
                .thenReturn(Optional.of(LockState.builder().locked(true).build()));

        service.recordFailure(RecordLoginUseCase.LoginCommand.builder()
                .userId(1L)
                .userType(UserTypeEnum.ADMIN)
                .username("admin")
                .clientIp("127.0.0.1")
                .failureReason("bad credentials")
                .build());

        ArgumentCaptor<AccountSecurityEvent> captor = ArgumentCaptor.forClass(AccountSecurityEvent.class);
        verify(securityEventPort).publishEvent(captor.capture());
        assertEquals(SecurityEventType.LOGIN_FAILURE, captor.getValue().getEventType());
        verify(lockStatePort, never()).incrementFailCount(anyLong(), any());
        verify(lockAccountUseCase, never()).lockAutomatically(anyLong(), any(), any(), anyInt());
    }

    @Test
    void recordFailure_thresholdReached_triggersLockAutomatically() {
        when(lockStatePort.findByUser(1L, UserTypeEnum.ADMIN))
                .thenReturn(Optional.of(LockState.builder().locked(false).build()));
        when(lockStatePort.incrementFailCount(1L, UserTypeEnum.ADMIN)).thenReturn(5);

        service.recordFailure(RecordLoginUseCase.LoginCommand.builder()
                .userId(1L)
                .userType(UserTypeEnum.ADMIN)
                .username("admin")
                .clientIp("127.0.0.1")
                .failureReason("bad credentials")
                .build());

        ArgumentCaptor<AccountSecurityEvent> captor = ArgumentCaptor.forClass(AccountSecurityEvent.class);
        verify(securityEventPort).publishEvent(captor.capture());
        assertEquals(SecurityEventType.LOGIN_FAILURE, captor.getValue().getEventType());
        verify(lockAccountUseCase).lockAutomatically(
                1L, UserTypeEnum.ADMIN,
                com.ingot.framework.security.account.domain.model.enums.LockReason.LOGIN_FAIL_EXCEED,
                30);
    }
}
