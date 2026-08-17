package com.ingot.framework.security.account.domain.port.outbound;

import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.model.AccountLockSignal;

/**
 * <p>账号锁定信号出站端口：锁定/解锁 transition 后同步 Redis 双 Key，边缘层只读查询。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface AccountLockSignalPort {

    /**
     * 写入 uid / name 双 Key（fail-open：Redis 异常不抛出）。
     *
     * @param signal 锁定信号
     */
    void writeLocked(AccountLockSignal signal);

    /**
     * 删除 uid / name 双 Key（fail-open）。
     *
     * @param signal 至少包含 userId、userType；username 可选
     */
    void clearLocked(AccountLockSignal signal);

    /**
     * 按用户 ID 判断是否处于锁定信号有效期内。
     *
     * @param userType 用户类型
     * @param userId   用户 ID
     * @return {@code true} 表示 Redis 中存在有效锁定信号；Redis 不可用时返回 {@code false}（fail-open）
     */
    boolean isLockedByUserId(UserTypeEnum userType, Long userId);

    /**
     * 按用户名判断是否处于锁定信号有效期内。
     *
     * @param userType 用户类型
     * @param username 用户名
     * @return {@code true} 表示 Redis 中存在有效锁定信号；Redis 不可用时返回 {@code false}（fail-open）
     */
    boolean isLockedByUsername(UserTypeEnum userType, String username);
}
