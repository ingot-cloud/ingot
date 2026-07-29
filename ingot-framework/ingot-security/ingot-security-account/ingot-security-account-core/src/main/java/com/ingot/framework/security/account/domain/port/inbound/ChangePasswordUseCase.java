package com.ingot.framework.security.account.domain.port.inbound;

import com.ingot.framework.security.account.domain.model.enums.EventSource;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.Builder;
import lombok.Value;

/**
 * 修改密码用例（入站端口）
 *
 * @author jymot
 * @since 2026-02-13
 */
public interface ChangePasswordUseCase {

    /**
     * 修改密码（自助）
     *
     * @param command 修改密码命令
     */
    void changePassword(ChangePasswordCommand command);

    /**
     * 重置密码（管理员操作）
     *
     * @param command 重置密码命令
     */
    void resetPassword(ResetPasswordCommand command);

    /**
     * 强制修改密码
     *
     * @param command 初始化命令
     */
    void forceChangePassword(ForceChangePasswordCommand command);

    /**
     * 修改密码命令
     */
    @Value
    @Builder
    class ChangePasswordCommand {
        /**
         * 用户ID
         */
        Long userId;

        /**
         * 用户类型
         */
        UserTypeEnum userType;

        /**
         * 旧密码
         */
        String oldPassword;

        /**
         * 新密码
         */
        String newPassword;

        /**
         * 确认密码
         */
        String confirmPassword;
    }

    /**
     * 重置密码命令
     */
    @Value
    @Builder
    class ResetPasswordCommand {
        /**
         * 用户ID
         */
        Long userId;

        /**
         * 用户类型
         */
        UserTypeEnum userType;

        /**
         * 新密码
         */
        String newPassword;

        /**
         * 操作人ID
         */
        Long operatorId;

        /**
         * 操作人姓名
         */
        String operatorName;

        /**
         * 操作来源（PMS 管理端 / MEMBER 会员端 等）
         */
        EventSource source;
    }

    @Value
    @Builder
    class ForceChangePasswordCommand {
        /**
         * 用户ID
         */
        Long userId;

        /**
         * 用户类型
         */
        UserTypeEnum userType;

        /**
         * 新密码
         */
        String newPassword;

        /**
         * 操作来源（PMS 管理端 / MEMBER 会员端 等）
         */
        EventSource source;
    }
}
