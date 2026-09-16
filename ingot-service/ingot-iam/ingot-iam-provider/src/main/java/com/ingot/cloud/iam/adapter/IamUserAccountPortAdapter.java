package com.ingot.cloud.iam.adapter;

import java.time.LocalDateTime;
import java.util.Optional;

import com.ingot.cloud.iam.identity.AccountCredentialRepository;
import com.ingot.cloud.iam.persistence.AccountWriteRepository;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.model.UserAccount;
import com.ingot.framework.security.account.domain.port.outbound.UserAccountPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>把账号安全端口接到新模型 {@code iam_account}，失败关闭且不回退旧用户表。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class IamUserAccountPortAdapter implements UserAccountPort {
    private final AccountCredentialRepository accounts;
    private final AccountWriteRepository accountWrites;

    /**
     * {@inheritDoc}
     *
     * <p>新建账号只写新模型 {@code iam_account}，标识由 IAM 发号器分配。锁定态、密码历史与
     * 过期记录仍由调用用例通过安全框架端口初始化，此处不代为写入。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAccount save(UserAccount account) {
        accountWrites.insert(account);
        return account;
    }

    /** {@inheritDoc} */
    @Override
    public Optional<UserAccount> findById(Long userId, UserTypeEnum userType) {
        return accounts.findById(userId).map(this::toModel);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<UserAccount> findByUsername(String username, UserTypeEnum userType) {
        return accounts.findByLogin(username)
                .filter(account -> username.equals(account.username()))
                .map(this::toModel);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<UserAccount> findByPhone(String phone, UserTypeEnum userType) {
        return accounts.findByLogin(phone)
                .filter(account -> phone.equals(account.phone()))
                .map(this::toModel);
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsByUsername(String username, UserTypeEnum userType) {
        return findByUsername(username, userType).isPresent();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, UserTypeEnum userType, boolean enabled) {
        accountWrites.updateStatus(userId, enabled);
    }

    /**
     * {@inheritDoc}
     *
     * <p>新模型账号没有锁定冗余列，锁定事实唯一保存在安全框架的
     * {@link com.ingot.framework.security.account.domain.port.outbound.LockStatePort} 中。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLockStatus(Long userId, UserTypeEnum userType, boolean locked) {
        // 锁定态不写入 iam_account。
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastLogin(Long userId, UserTypeEnum userType,
                                LocalDateTime loginAt, String loginIp) {
        accountWrites.updateLastLogin(userId, loginAt);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, UserTypeEnum userType) {
        accountWrites.delete(userId);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWithVersion(UserAccount account, Long expectedVersion) {
        return accountWrites.updateWithVersion(account, expectedVersion) > 0;
    }

    private UserAccount toModel(AccountCredentialRepository.AccountCredentials account) {
        return UserAccount.builder()
                .id(account.id())
                .userType(UserTypeEnum.ADMIN)
                .username(account.username())
                .password(account.passwordHash())
                .nickname(account.username())
                .phone(account.phone())
                .email(account.email())
                .mustChangePwd(account.mustChangePassword())
                .passwordChangedAt(account.passwordChangedAt())
                .enabled(account.enabled())
                .locked(accounts.locked(account.id()))
                .lastLoginAt(account.lastLoginAt())
                .version(account.version())
                .createdAt(account.createdAt())
                .updatedAt(account.updatedAt())
                .build();
    }
}
