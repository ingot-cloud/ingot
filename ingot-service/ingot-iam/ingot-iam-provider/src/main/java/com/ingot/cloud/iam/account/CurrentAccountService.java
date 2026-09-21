package com.ingot.cloud.iam.account;

import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.AccountQueryRepository;
import com.ingot.cloud.iam.persistence.AccountWriteRepository;
import com.ingot.cloud.iam.persistence.SessionRepository;
import com.ingot.cloud.iam.persistence.entity.IamAccountEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.framework.commons.constants.PermissionConstants;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AccountSelfProfile;
import com.ingot.framework.commons.model.iam.AccountSelfProfileInput;
import com.ingot.framework.commons.model.iam.CurrentPasswordInput;
import com.ingot.framework.commons.model.iam.CurrentProfile;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.model.enums.EventSource;
import com.ingot.framework.security.account.domain.port.inbound.ChangePasswordUseCase;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>维护当前认证账号的本人资料与改密，禁止提交其他账号 ID。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class CurrentAccountService {
    private final IamAccess access;
    private final AccountQueryRepository accounts;
    private final AccountWriteRepository writes;
    private final SessionRepository sessions;
    private final ChangePasswordUseCase passwords;

    /**
     * 读取当前认证账号联系资料与成员最小资料。
     *
     * @return 本人资料
     */
    public AccountSelfProfile profile() {
        ActiveIdentity actor = access.requireCurrent();
        IamAccountEntity row = requireAccount(actor);
        CurrentProfile member = sessions.profile(actor.context());
        if (member == null) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        return profile(row, member);
    }

    /**
     * 更新当前认证账号联系资料。
     *
     * @param input 联系资料
     * @return 更新后的本人资料
     */
    @Transactional(rollbackFor = Exception.class)
    public AccountSelfProfile updateProfile(AccountSelfProfileInput input) {
        ActiveIdentity actor = access.requireCurrent();
        IamAccountEntity row = requireAccount(actor);
        IamIds.requireVersion(input.expectedVersion(), version(row));
        if (writes.updateContacts(row.getId().longValueExact(), input.phone(), input.email(),
                row.getVersion().longValueExact()) != 1) {
            throw new BizException(IamReasonCode.REVISION_CONFLICT);
        }
        return profile();
    }

    /**
     * 修改当前认证账号密码；初始改密走强制改密用例。
     *
     * @param input 新旧密码
     */
    public void updatePassword(CurrentPasswordInput input) {
        ActiveIdentity actor = access.requireCurrent();
        IamAccountEntity row = requireAccount(actor);
        long accountId = row.getId().longValueExact();
        InUser user = SecurityAuthContext.getUser();
        boolean force = Boolean.TRUE.equals(row.getMustChangePassword())
                || (user != null && user.getAuthorities().stream()
                .anyMatch(authority -> PermissionConstants.INIT_PASSWORD.equals(authority.getAuthority())));
        if (force) {
            passwords.forceChangePassword(ChangePasswordUseCase.ForceChangePasswordCommand.builder()
                    .userId(accountId)
                    .userType(UserTypeEnum.ADMIN)
                    .newPassword(input.newPassword())
                    .source(EventSource.IAM)
                    .build());
            return;
        }
        if (input.oldPassword() == null || input.oldPassword().isBlank()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        passwords.changePassword(ChangePasswordUseCase.ChangePasswordCommand.builder()
                .userId(accountId)
                .userType(UserTypeEnum.ADMIN)
                .oldPassword(input.oldPassword())
                .newPassword(input.newPassword())
                .confirmPassword(input.confirmPassword())
                .build());
    }

    private IamAccountEntity requireAccount(ActiveIdentity actor) {
        IamAccountEntity row = accounts.find(IamIds.require(actor.context().accountId()));
        if (row == null) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        return row;
    }

    private static AccountSelfProfile profile(IamAccountEntity row, CurrentProfile member) {
        return new AccountSelfProfile(row.getId().toString(), row.getUsername(), row.getPhone(), row.getEmail(),
                Boolean.TRUE.equals(row.getMustChangePassword()), member, version(row));
    }

    private static String version(IamAccountEntity row) {
        return row.getVersion() == null ? "0" : row.getVersion().toString();
    }
}
