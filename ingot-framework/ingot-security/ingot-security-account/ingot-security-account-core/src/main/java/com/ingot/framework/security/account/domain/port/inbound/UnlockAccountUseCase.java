package com.ingot.framework.security.account.domain.port.inbound;

import com.ingot.framework.security.account.domain.model.enums.EventSource;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.Builder;
import lombok.Value;

/**
 * 解锁账号用例（入站端口）
 *
 * @author jymot
 * @since 2026-02-13
 */
public interface UnlockAccountUseCase {

    /**
     * 手动解锁账号（管理员操作）
     *
     * @param command 解锁命令
     */
    void unlockManually(UnlockCommand command);

    /**
     * 自动解锁过期的临时锁定（定时任务调用）
     */
    void unlockExpired();

    /**
     * 解锁命令
     */
    @Value
    @Builder
    class UnlockCommand {
        /**
         * 用户ID
         */
        Long userId;

        /**
         * 用户类型
         */
        UserTypeEnum userType;

        /**
         * 解锁原因
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
