package com.ingot.framework.security.account.domain.model;

import java.time.LocalDateTime;

import com.ingot.framework.commons.model.security.UserTypeEnum;

/**
 * <p>账号锁定信号，用于在 Redis 双 Key 上同步锁定态供边缘层短路读取。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param userId      用户 ID
 * @param userType    用户类型
 * @param username    用户名（name key 写入必需；缺失时仅写 uid key）
 * @param lockedUntil 锁定到期时间；{@code null} 表示永久锁定
 */
public record AccountLockSignal(
        Long userId,
        UserTypeEnum userType,
        String username,
        LocalDateTime lockedUntil
) {
}
