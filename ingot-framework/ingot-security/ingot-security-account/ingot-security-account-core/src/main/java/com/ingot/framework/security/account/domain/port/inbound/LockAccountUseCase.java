package com.ingot.framework.security.account.domain.port.inbound;

import com.ingot.framework.security.account.domain.model.enums.EventSource;
import com.ingot.framework.security.account.domain.model.enums.LockReason;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 锁定账号用例（入站端口）
 *
 * @author jymot
 * @since 2026-02-13
 */
public interface LockAccountUseCase {

    /**
     * 手动锁定账号（管理员操作）
     *
     * @param command 锁定命令
     */
    void lockManually(LockCommand command);

    /**
     * 自动锁定账号（策略触发）
     *
     * @param userId          用户ID
     * @param userType        用户类型
     * @param reason          锁定原因
     * @param durationMinutes 锁定时长（分钟），NULL=永久
     */
    void lockAutomatically(Long userId, UserTypeEnum userType, 
                           LockReason reason, Integer durationMinutes);

    /**
     * 锁定命令
     */
    @Value
    @Builder
    class LockCommand {
        /**
         * 用户ID
         */
        Long userId;

        /**
         * 用户类型
         */
        UserTypeEnum userType;

        /**
         * 锁定原因
         */
        LockReason reason;

        /**
         * 锁定原因详情
         */
        String reasonDetail;

        /**
         * 锁定到期时间（NULL=永久锁定）
         */
        LocalDateTime lockedUntil;

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
