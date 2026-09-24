package com.ingot.cloud.iam.support;

import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.SensitiveConfirmation;
import com.ingot.framework.commons.model.iam.SensitiveConfirmationKind;
import com.ingot.framework.security.account.domain.ConfirmPasswordFailedException;
import com.ingot.framework.security.account.domain.port.inbound.ConfirmPasswordUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * {@link SensitiveConfirmationGuard} 缺字段、未实现种类与口令失败均关闭。
 *
 * @author jy
 * @since 1.0.0
 */
class SensitiveConfirmationGuardTest {
    private ConfirmPasswordUseCase passwords;
    private SensitiveConfirmationGuard guard;
    private ActiveIdentity actor;

    @BeforeEach
    void setUp() {
        passwords = mock(ConfirmPasswordUseCase.class);
        guard = new SensitiveConfirmationGuard(passwords);
        actor = new ActiveIdentity(
                new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001"), "0", "0", null);
    }

    @Test
    void require_missingConfirmation_failsClosed() {
        BizException denied = assertThrows(BizException.class, () -> guard.require(actor, null));
        assertEquals(IamReasonCode.STEP_UP_FAILED.getCode(), denied.getCode());
    }

    @Test
    void require_operationPassword_failsClosed() {
        BizException denied = assertThrows(BizException.class, () -> guard.require(actor,
                new SensitiveConfirmation(SensitiveConfirmationKind.OPERATION_PASSWORD, "secret")));
        assertEquals(IamReasonCode.STEP_UP_FAILED.getCode(), denied.getCode());
    }

    @Test
    void require_wrongPassword_failsClosed() {
        doThrow(new ConfirmPasswordFailedException()).when(passwords).confirm(any());
        BizException denied = assertThrows(BizException.class, () -> guard.require(actor,
                new SensitiveConfirmation(SensitiveConfirmationKind.LOGIN_PASSWORD, "wrong")));
        assertEquals(IamReasonCode.STEP_UP_FAILED.getCode(), denied.getCode());
    }

    @Test
    void require_loginPassword_delegatesToConfirm() {
        guard.require(actor, new SensitiveConfirmation(SensitiveConfirmationKind.LOGIN_PASSWORD, "ok"));
        verify(passwords).confirm(any());
    }
}
