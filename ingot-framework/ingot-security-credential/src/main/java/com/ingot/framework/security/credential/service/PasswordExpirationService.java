package com.ingot.framework.security.credential.service;

import com.ingot.framework.security.credential.model.domain.PasswordExpiration;

/**
 * 密码过期服务接口（通用）
 * <p>各业务服务需要实现此接口来管理自己的密码过期信息</p>
 *
 * @author jymot
 * @since 2026-01-23
 */
public interface PasswordExpirationService {

    /**
     * 获取用户的密码过期信息
     *
     * @param userId 用户ID
     * @return 密码过期信息，不存在则返回null
     */
    PasswordExpiration getByUserId(Long userId);

    /**
     * 初始化用户的密码过期信息
     *
     * @param userId       用户ID
     * @param maxDays      密码有效天数
     * @param forceChange  是否强制修改
     * @param graceLogins  宽限登录次数
     */
    void initExpiration(Long userId, int maxDays, boolean forceChange, int graceLogins);

    /**
     * 更新密码修改时间（重置过期时间）
     *
     * @param userId  用户ID
     * @param maxDays 密码有效天数
     */
    void updateLastChanged(Long userId, int maxDays);

    /**
     * 减少宽限登录次数
     *
     * @param userId 用户ID
     * @return 剩余宽限登录次数
     */
    int decrementGraceLogin(Long userId);

    /**
     * 检查密码是否过期
     *
     * @param userId 用户ID
     * @return true-已过期，false-未过期
     */
    boolean isExpired(Long userId);

    /**
     * 检查密码是否需要提醒
     *
     * @param userId         用户ID
     * @param warningDaysBefore 提前N天提醒
     * @return true-需要提醒，false-不需要提醒
     */
    boolean needsWarning(Long userId, int warningDaysBefore);

    /**
     * 更新下次提醒时间
     *
     * @param userId 用户ID
     */
    void updateNextWarning(Long userId);

    /**
     * 删除用户的密码过期信息
     *
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);
}
