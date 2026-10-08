package com.ingot.framework.security.account.domain.service;

import java.util.Optional;

import com.ingot.framework.commons.model.security.SessionRevokeReason;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.model.UserAccount;
import com.ingot.framework.security.account.domain.model.enums.EventSource;
import com.ingot.framework.security.account.domain.port.inbound.ChangePasswordUseCase;
import com.ingot.framework.security.account.domain.port.outbound.*;
import com.ingot.framework.security.credential.service.CredentialSecurityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <p>强制改密提交后才撤销会话，失败及回滚不能解除会话限制。</p>
 * @author jy
 * @since 1.0.0
 */
class ChangePasswordUseCaseServiceTest {
    private final UserAccountPort accounts = mock(UserAccountPort.class);
    private final UserCredentialPort credentials = mock(UserCredentialPort.class);
    private final SecurityEventPort events = mock(SecurityEventPort.class);
    private final SessionRevocationPort sessions = mock(SessionRevocationPort.class);
    private final CredentialSecurityService security = mock(CredentialSecurityService.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final ChangePasswordUseCaseService service = new ChangePasswordUseCaseService(
            accounts, credentials, events, sessions, security, encoder);

    @AfterEach void clear() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test void successfulChangeRevokesOnlyAfterCommit() {
        prepare(true);
        begin();
        service.forceChangePassword(command());
        verifyNoInteractions(sessions);
        verify(credentials).updatePassword(eq(1L), eq(UserTypeEnum.ADMIN), eq("hash"), any(), eq(0L), eq(false));
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(sessions).revokeUserSessions(1L, SessionRevokeReason.PASSWORD_CHANGED, 1L);
    }

    @Test void rollbackOrVersionFailureDoesNotRevokeSessions() {
        prepare(true);
        begin();
        service.forceChangePassword(command());
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verifyNoInteractions(sessions);
        clear();
        prepare(false);
        begin();
        assertThrows(RuntimeException.class, () -> service.forceChangePassword(command()));
        verifyNoInteractions(sessions);
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }

    @Test void rejectedCredentialDoesNotChangePasswordOrRevoke() {
        prepare(true);
        doThrow(new IllegalArgumentException("weak password")).when(security).validate(any());
        assertThrows(IllegalArgumentException.class, () -> service.forceChangePassword(command()));
        verifyNoInteractions(credentials, sessions, events);
    }

    private void prepare(boolean updated) {
        when(accounts.findById(1L, UserTypeEnum.ADMIN)).thenReturn(Optional.of(
                UserAccount.builder().id(1L).username("test").version(0L).build()));
        when(encoder.encode("new-password")).thenReturn("hash");
        when(credentials.updatePassword(eq(1L), eq(UserTypeEnum.ADMIN), eq("hash"), any(), eq(0L), eq(false))).thenReturn(updated);
    }

    private void begin() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    private ChangePasswordUseCase.ForceChangePasswordCommand command() {
        return ChangePasswordUseCase.ForceChangePasswordCommand.builder().userId(1L).userType(UserTypeEnum.ADMIN)
                .newPassword("new-password").source(EventSource.IAM).build();
    }
}
