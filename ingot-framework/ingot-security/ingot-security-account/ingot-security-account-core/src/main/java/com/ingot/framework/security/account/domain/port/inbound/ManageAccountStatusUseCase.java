package com.ingot.framework.security.account.domain.port.inbound;

import com.ingot.framework.security.account.domain.model.enums.EventSource;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.Builder;
import lombok.Value;

/**
 * 账号状态管理用例（入站端口）
 *
 * @author jymot
 * @since 2026-02-13
 */
public interface ManageAccountStatusUseCase {

    /**
     * 启用账号
     *
     * @param command 启用命令
     */
    void enableAccount(StatusCommand command);

    /**
     * 禁用账号
     *
     * @param command 禁用命令
     */
    void disableAccount(StatusCommand command);

    /**
     * 账号状态命令
     */
    @Value
    @Builder
    class StatusCommand {
        /**
         * 用户ID
         */
        Long userId;

        /**
         * 用户类型
         */
        UserTypeEnum userType;

        /**
         * 目标状态（true-启用 false-禁用）
         */
        Boolean targetStatus;

        /**
         * 原因
         */
        String reason;

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
}
