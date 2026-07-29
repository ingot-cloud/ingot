package com.ingot.framework.security.account.domain.port.outbound;

import com.ingot.framework.commons.model.security.UserTypeEnum;

import java.time.LocalDateTime;

/**
 * 用户凭证数据访问端口
 *
 * @author jymot
 * @since 2026-02-13
 */
public interface UserCredentialPort {

    /**
     * 获取密码哈希
     *
     * @param userId   用户ID
     * @param userType 用户类型
     * @return 密码哈希
     */
    String getPasswordHash(Long userId, UserTypeEnum userType);

    /**
     * 更新密码（带版本控制）
     * <p>
     * 一次 UPDATE 同时写入密码哈希、修改时间和强制改密标记，避免多次 SQL 往返。
     * </p>
     *
     * @param userId          用户ID
     * @param userType        用户类型
     * @param newPasswordHash 新密码哈希
     * @param changedAt       修改时间
     * @param expectedVersion 期望版本号
     * @param mustChangePwd   更新后是否标记为必须修改密码（true-管理员重置场景；false-用户自主修改场景）
     * @return true-更新成功，false-版本冲突
     */
    boolean updatePassword(Long userId, UserTypeEnum userType,
                           String newPasswordHash, LocalDateTime changedAt,
                           Long expectedVersion, boolean mustChangePwd);

    /**
     * 获取密码最后修改时间
     *
     * @param userId   用户ID
     * @param userType 用户类型
     * @return 最后修改时间
     */
    LocalDateTime getPasswordChangedAt(Long userId, UserTypeEnum userType);
}
