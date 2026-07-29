package com.ingot.framework.security.account.domain.port.outbound;

import com.ingot.framework.security.account.domain.model.LockState;
import com.ingot.framework.security.account.domain.model.enums.LockType;
import com.ingot.framework.commons.model.security.UserTypeEnum;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


/**
 * 锁定状态数据访问端口（基于 account_lock_state 表）
 *
 * @author jymot
 * @since 2026-02-13
 */
public interface LockStatePort {

    /**
     * 根据用户ID查询锁定状态
     *
     * @param userId   用户ID
     * @param userType 用户类型
     * @return 锁定状态
     */
    Optional<LockState> findByUser(Long userId, UserTypeEnum userType);

    /**
     * 初始化锁定状态（用户注册时调用）
     *
     * @param userId   用户ID
     * @param userType 用户类型
     * @return 初始化的锁定状态
     */
    LockState initialize(Long userId, UserTypeEnum userType);

    /**
     * 原子递增登录失败计数
     * <p>
     * 实现必须保证操作的原子性（如使用 {@code INSERT ... ON DUPLICATE KEY UPDATE} 或等效的行锁机制），
     * 避免并发登录失败时出现计数丢失。
     * </p>
     *
     * @param userId   用户ID
     * @param userType 用户类型
     * @return 递增后的最新失败次数
     */
    int incrementFailCount(Long userId, UserTypeEnum userType);

    /**
     * 重置登录失败计数
     *
     * @param userId   用户ID
     * @param userType 用户类型
     */
    void resetFailCount(Long userId, UserTypeEnum userType);

    /**
     * 更新锁定状态
     *
     * @param userId            用户ID
     * @param userType          用户类型
     * @param locked            是否锁定
     * @param lockedUntil       锁定到期时间（NULL=永久）
     * @param reasonCode        锁定原因代码
     * @param lockType          锁定类型（{@link LockType#MANUAL} / {@link LockType#AUTO}），解锁时传 {@code null}
     * @param operatorId        操作人ID
     * @param operatorName      操作人姓名
     */
    void updateLockStatus(Long userId, UserTypeEnum userType,
                          boolean locked, LocalDateTime lockedUntil,
                          String reasonCode, LockType lockType,
                          Long operatorId, String operatorName);

    /**
     * 删除用户的锁定状态记录（账号注销时调用）
     *
     * @param userId   用户ID
     * @param userType 用户类型
     */
    void deleteByUser(Long userId, UserTypeEnum userType);

    /**
     * 游标分页查找过期的临时锁定
     * <p>
     * 使用 {@code id > afterId} 游标方式，避免大数据量下 OFFSET 效率退化问题。
     * 调用方应从 {@code afterId=0} 开始，每次将返回列表最后一条记录的 id 作为下次入参，
     * 直至返回列表数量小于 {@code pageSize} 为止。
     * </p>
     *
     * @param now      当前时间，locked_until &lt;= now 的记录视为过期
     * @param afterId  游标：仅返回 id &gt; afterId 的记录，首次调用传 0
     * @param pageSize 每页最大记录数
     * @return 当前游标位置后的一批过期锁定状态，按 id 升序
     */
    List<LockState> findExpiredLocksByPage(LocalDateTime now, Long afterId, int pageSize);
}
