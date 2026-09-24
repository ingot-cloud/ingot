package com.ingot.framework.security.account.domain.service;

import java.util.Optional;

import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.ConfirmPasswordFailedException;
import com.ingot.framework.security.account.domain.model.LockoutPolicy;
import com.ingot.framework.security.account.domain.model.UserAccount;
import com.ingot.framework.security.account.domain.model.enums.LockReason;
import com.ingot.framework.security.account.domain.port.inbound.ConfirmPasswordUseCase;
import com.ingot.framework.security.account.domain.port.inbound.LockAccountUseCase;
import com.ingot.framework.security.account.domain.port.outbound.LockStatePort;
import com.ingot.framework.security.account.domain.port.outbound.SecurityEventPort;
import com.ingot.framework.security.account.domain.port.outbound.UserAccountPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ConfirmPasswordUseCaseService} 口令确认与失败锁定。
 *
 * @author jy
 * @since 1.0.0
 */
class ConfirmPasswordUseCaseServiceTest {
    private final UserAccountPort userAccountPort = mock(UserAccountPort.class);
    private final LockStatePort lockStatePort = mock(LockStatePort.class);
    private final SecurityEventPort securityEventPort = mock(SecurityEventPort.class);
    private final LockAccountUseCase lockAccountUseCase = mock(LockAccountUseCase.class);
    private final AccountLockoutPolicyLoader lockoutPolicyLoader = mock(AccountLockoutPolicyLoader.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private ConfirmPasswordUseCaseService service;

    @BeforeEach
    void setUp() {
        service = new ConfirmPasswordUseCaseService(userAccountPort, lockStatePort, securityEventPort,
                lockAccountUseCase, lockoutPolicyLoader, passwordEncoder);
        when(lockoutPolicyLoader.getLockoutPolicy(UserTypeEnum.ADMIN)).thenReturn(new LockoutPolicy(
                UserTypeEnum.ADMIN, true, 5, 30, 15, 3));
        when(userAccountPort.findById(1L, UserTypeEnum.ADMIN)).thenReturn(Optional.of(UserAccount.builder()
                .id(1L)
                .userType(UserTypeEnum.ADMIN)
                .username("admin")
                .password("hash")
                .build()));
    }

    @Test
    void confirm_matches_resetsFailCount() {
        when(passwordEncoder.matches("ok", "hash")).thenReturn(true);

        service.confirm(ConfirmPasswordUseCase.ConfirmPasswordCommand.builder()
                .userId(1L)
                .userType(UserTypeEnum.ADMIN)
                .password("ok")
                .build());

        verify(lockStatePort).resetFailCount(1L, UserTypeEnum.ADMIN);
        verify(lockStatePort, never()).incrementFailCount(anyLong(), any());
        verify(securityEventPort, never()).publishEvent(any());
    }

    @Test
    void confirm_mismatch_incrementsFailCount() {
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
        when(lockStatePort.findByUser(1L, UserTypeEnum.ADMIN)).thenReturn(Optional.empty());
        when(lockStatePort.incrementFailCount(1L, UserTypeEnum.ADMIN)).thenReturn(1);

        assertThrows(ConfirmPasswordFailedException.class, () -> service.confirm(
                ConfirmPasswordUseCase.ConfirmPasswordCommand.builder()
                        .userId(1L)
                        .userType(UserTypeEnum.ADMIN)
                        .password("wrong")
                        .build()));

        verify(lockStatePort).incrementFailCount(1L, UserTypeEnum.ADMIN);
        verify(lockStatePort, never()).resetFailCount(anyLong(), any());
        verify(lockAccountUseCase, never()).lockAutomatically(anyLong(), any(), any(), anyInt());
    }

    @Test
    void confirm_mismatchAtThreshold_locksAutomatically() {
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
        when(lockStatePort.findByUser(1L, UserTypeEnum.ADMIN)).thenReturn(Optional.empty());
        when(lockStatePort.incrementFailCount(1L, UserTypeEnum.ADMIN)).thenReturn(5);

        assertThrows(ConfirmPasswordFailedException.class, () -> service.confirm(
                ConfirmPasswordUseCase.ConfirmPasswordCommand.builder()
                        .userId(1L)
                        .userType(UserTypeEnum.ADMIN)
                        .password("wrong")
                        .build()));

        verify(lockAccountUseCase).lockAutomatically(1L, UserTypeEnum.ADMIN, LockReason.LOGIN_FAIL_EXCEED, 30);
    }
}
