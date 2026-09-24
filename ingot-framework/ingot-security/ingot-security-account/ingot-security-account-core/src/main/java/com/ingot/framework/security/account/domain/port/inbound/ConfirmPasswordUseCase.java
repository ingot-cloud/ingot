package com.ingot.framework.security.account.domain.port.inbound;

import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.Builder;
import lombok.Value;

/**
 * 确认当前账号口令，不签发令牌、不记录登录成功。
 *
 * @author jy
 * @since 1.0.0
 */
public interface ConfirmPasswordUseCase {

    /**
     * 校验当前账号口令；失败抛出 {@link com.ingot.framework.security.account.domain.ConfirmPasswordFailedException}。
     *
     * @param command 账号与口令
     */
    void confirm(ConfirmPasswordCommand command);

    /**
     * 确认口令命令。
     */
    @Value
    @Builder
    class ConfirmPasswordCommand {
        /**
         * 用户 ID。
         */
        Long userId;

        /**
         * 用户类型。
         */
        UserTypeEnum userType;

        /**
         * 待确认口令。
         */
        String password;
    }
}
