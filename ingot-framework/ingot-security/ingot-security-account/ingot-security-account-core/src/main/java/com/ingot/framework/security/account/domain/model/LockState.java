package com.ingot.framework.security.account.domain.model;

import com.ingot.framework.security.account.domain.model.enums.LockType;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账号锁定状态领域模型
 *
 * @author jymot
 * @since 2026-02-13
 */
@Data
@Builder
public class LockState {
    /**
     * ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户类型
     */
    private UserTypeEnum userType;

    /**
     * 是否锁定
     */
    private Boolean locked;

    /**
     * 锁定类型
     */
    private LockType lockType;

    /**
     * 锁定原因代码
     */
    private String lockReasonCode;

    /**
     * 锁定原因详情
     */
    private String lockReasonDetail;

    /**
     * 锁定时间
     */
    private LocalDateTime lockedAt;

    /**
     * 锁定到期时间（NULL表示永久锁定）
     */
    private LocalDateTime lockedUntil;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 连续失败次数
     */
    private Integer failedLoginCount;

    /**
     * 最后失败时间
     */
    private LocalDateTime lastFailedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 检查是否已锁定
     */
    public boolean isLocked() {
        return Boolean.TRUE.equals(locked);
    }

    /**
     * 检查是否为临时锁定（有到期时间）
     */
    public boolean isTemporaryLock() {
        return isLocked() && lockedUntil != null;
    }

    /**
     * 检查是否为永久锁定
     */
    public boolean isPermanentLock() {
        return isLocked() && lockedUntil == null;
    }

    /**
     * 检查锁定是否已过期
     */
    public boolean isLockExpired() {
        if (!isLocked() || lockedUntil == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(lockedUntil);
    }
}
