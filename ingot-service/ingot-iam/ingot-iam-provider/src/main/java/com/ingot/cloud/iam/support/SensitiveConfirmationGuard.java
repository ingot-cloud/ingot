package com.ingot.cloud.iam.support;

import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.SensitiveConfirmation;
import com.ingot.framework.commons.model.iam.SensitiveConfirmationKind;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.ConfirmPasswordFailedException;
import com.ingot.framework.security.account.domain.port.inbound.ConfirmPasswordUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * <p>在敏感写改库之前确认当前账号，不暴露独立验密 HTTP。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SensitiveConfirmationGuard {
    private final ConfirmPasswordUseCase confirmPassword;

    /**
     * 校验内嵌确认；缺字段、未实现种类或口令错误一律失败关闭。
     *
     * @param actor 已鉴权当前身份
     * @param confirmation 写请求内嵌确认
     */
    public void require(ActiveIdentity actor, SensitiveConfirmation confirmation) {
        if (confirmation == null || confirmation.kind() == null || !StringUtils.hasText(confirmation.secret())) {
            throw new BizException(IamReasonCode.STEP_UP_FAILED);
        }
        if (confirmation.kind() != SensitiveConfirmationKind.LOGIN_PASSWORD) {
            throw new BizException(IamReasonCode.STEP_UP_FAILED);
        }
        try {
            confirmPassword.confirm(ConfirmPasswordUseCase.ConfirmPasswordCommand.builder()
                    .userId(IamIds.require(actor.context().accountId()))
                    .userType(UserTypeEnum.ADMIN)
                    .password(confirmation.secret())
                    .build());
        } catch (ConfirmPasswordFailedException ex) {
            throw new BizException(IamReasonCode.STEP_UP_FAILED);
        }
    }
}
