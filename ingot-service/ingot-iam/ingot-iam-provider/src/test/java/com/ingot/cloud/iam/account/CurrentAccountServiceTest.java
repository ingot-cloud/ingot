package com.ingot.cloud.iam.account;

import java.math.BigInteger;

import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.AccountQueryRepository;
import com.ingot.cloud.iam.persistence.AccountWriteRepository;
import com.ingot.cloud.iam.persistence.SessionRepository;
import com.ingot.cloud.iam.persistence.entity.IamAccountEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CurrentPasswordInput;
import com.ingot.framework.commons.model.iam.CurrentProfile;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.security.account.domain.port.inbound.ChangePasswordUseCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>验证本人资料读取当前账号，普通改密缺少旧密码时失败关闭。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class CurrentAccountServiceTest {
    private static final AuthorizationContext PLATFORM =
            new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001");

    @Test
    void profileReturnsCurrentAccountContacts() {
        IamAccess access = mock(IamAccess.class);
        AccountQueryRepository accounts = mock(AccountQueryRepository.class);
        SessionRepository sessions = mock(SessionRepository.class);
        when(access.requireCurrent()).thenReturn(new ActiveIdentity(PLATFORM, "0", "0", null));
        when(accounts.find(1L)).thenReturn(account());
        when(sessions.profile(PLATFORM)).thenReturn(new CurrentProfile("1001", "管理者", null));
        CurrentAccountService service = new CurrentAccountService(access, accounts, mock(AccountWriteRepository.class),
                sessions, mock(ChangePasswordUseCase.class));
        var profile = service.profile();
        assertEquals("1", profile.accountId());
        assertEquals("alice", profile.username());
        assertEquals("13800000000", profile.phone());
        assertEquals("1001", profile.member().memberId());
        assertEquals("0", profile.version());
    }

    @Test
    void passwordChangeWithoutOldPasswordFailsWhenNotForced() {
        IamAccess access = mock(IamAccess.class);
        AccountQueryRepository accounts = mock(AccountQueryRepository.class);
        when(access.requireCurrent()).thenReturn(new ActiveIdentity(PLATFORM, "0", "0", null));
        when(accounts.find(1L)).thenReturn(account());
        CurrentAccountService service = new CurrentAccountService(access, accounts, mock(AccountWriteRepository.class),
                mock(SessionRepository.class), mock(ChangePasswordUseCase.class));
        BizException failure = assertThrows(BizException.class,
                () -> service.updatePassword(new CurrentPasswordInput(null, "new-pass", "new-pass")));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), failure.getCode());
    }

    private static IamAccountEntity account() {
        IamAccountEntity row = new IamAccountEntity();
        row.setId(BigInteger.ONE);
        row.setUsername("alice");
        row.setPhone("13800000000");
        row.setEmail("alice@example.com");
        row.setMustChangePassword(false);
        row.setVersion(BigInteger.ZERO);
        return row;
    }
}
