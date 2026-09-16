package com.ingot.framework.security.account.domain.port.outbound;

import java.time.LocalDateTime;
import java.util.Optional;

import com.ingot.framework.security.account.domain.model.UserAccount;
import com.ingot.framework.commons.model.security.UserTypeEnum;

/**
 * 用户账号数据访问端口（出站端口）
 * IAM 和 Member 各自实现此接口
 *
 * @author jymot
 * @since 2026-02-13
 */
public interface UserAccountPort {

    /**
     * 保存/创建用户账号
     *
     * @param account 用户账号
     * @return 保存后的用户账号
     */
    UserAccount save(UserAccount account);

    /**
     * 根据ID查询用户
     *
     * @param userId   用户ID
     * @param userType 用户类型
     * @return 用户账号
     */
    Optional<UserAccount> findById(Long userId, UserTypeEnum userType);

    /**
     * 根据用户名查询
     *
     * @param username 用户名
     * @param userType 用户类型
     * @return 用户账号
     */
    Optional<UserAccount> findByUsername(String username, UserTypeEnum userType);

    /**
     * 根据手机号查询
     *
     * @param phone    手机号
     * @param userType 用户类型
     * @return 用户账号
     */
    Optional<UserAccount> findByPhone(String phone, UserTypeEnum userType);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @param userType 用户类型
     * @return true-存在，false-不存在
     */
    boolean existsByUsername(String username, UserTypeEnum userType);

    /**
     * 更新账号启用状态
     *
     * @param userId   用户ID
     * @param userType 用户类型
     * @param enabled  启用状态（true-启用 false-禁用）
     */
    void updateStatus(Long userId, UserTypeEnum userType, boolean enabled);

    /**
     * 更新账号锁定状态（冗余字段）
     *
     * @param userId   用户ID
     * @param userType 用户类型
     * @param locked   锁定状态（true-锁定 false-正常）
     */
    void updateLockStatus(Long userId, UserTypeEnum userType, boolean locked);

    /**
     * 更新最后登录信息
     *
     * @param userId   用户ID
     * @param userType 用户类型
     * @param loginAt  登录时间
     * @param loginIp  登录IP
     */
    void updateLastLogin(Long userId, UserTypeEnum userType,
                         LocalDateTime loginAt, String loginIp);

    /**
     * 乐观锁更新（带版本控制）
     *
     * @param account         用户账号
     * @param expectedVersion 期望版本号
     * @return true-更新成功，false-版本冲突
     */
    boolean updateWithVersion(UserAccount account, Long expectedVersion);

    /**
     * 删除账号（软删除）
     *
     * @param userId   用户ID
     * @param userType 用户类型
     */
    void delete(Long userId, UserTypeEnum userType);
}
