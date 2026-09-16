package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.identity.InitializationIdAllocator;
import com.ingot.cloud.iam.persistence.entity.IamAccountEntity;
import com.ingot.cloud.iam.persistence.mapper.IamAccountMapper;
import com.ingot.framework.security.account.domain.model.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>通过类型化条件维护全局账号自身字段，不承载锁定态持久化。</p>
 * @author jy
 * @since 1.0.0
 * @see com.ingot.framework.security.account.domain.port.outbound.LockStatePort
 */
@Repository
@RequiredArgsConstructor
public class AccountWriteRepository {
    private final IamAccountMapper accounts;
    private final InitializationIdAllocator ids;

    /**
     * 新建全局账号，标识由发号器分配并回填到入参。
     *
     * <p>凭证哈希、初始改密标记与改密时间由安全框架的注册用例决定，此处只负责持久化；
     * 锁定态不属于账号字段，由 {@code LockStatePort} 独立初始化。</p>
     *
     * @param account 待新建账号，登录名必填，方法返回后 {@code id} 被回填
     * @return 新建账号的全局标识
     * @throws org.springframework.dao.DuplicateKeyException 登录名已被占用
     */
    public long insert(UserAccount account) {
        long accountId = ids.nextId();
        LocalDateTime now = now();
        IamAccountEntity entity = new IamAccountEntity();
        entity.setId(BigInteger.valueOf(accountId));
        entity.setUsername(account.getUsername());
        entity.setPasswordHash(account.getPassword());
        entity.setPhone(account.getPhone());
        entity.setEmail(account.getEmail());
        entity.setEnabled(!Boolean.FALSE.equals(account.getEnabled()));
        entity.setMustChangePassword(Boolean.TRUE.equals(account.getMustChangePwd()));
        entity.setPasswordChangedAt(account.getPasswordChangedAt() == null
                ? now : account.getPasswordChangedAt());
        entity.setVersion(BigInteger.ZERO);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        accounts.insert(entity);
        account.setId(accountId);
        return accountId;
    }

    /**
     * 修改未删除账号的启用状态。
     * @param accountId 全局账号 ID
     * @param enabled 是否启用
     * @return 受影响记录数
     */
    public int updateStatus(Long accountId, boolean enabled) {
        return accounts.update(active(accountId).set(IamAccountEntity::getEnabled, enabled)
                .set(IamAccountEntity::getUpdatedAt, now()));
    }

    /**
     * 更新未删除账号最近登录时间。
     * @param accountId 全局账号 ID
     * @param loginAt 登录时间
     * @return 受影响记录数
     */
    public int updateLastLogin(Long accountId, LocalDateTime loginAt) {
        return accounts.update(active(accountId).set(IamAccountEntity::getLastLoginAt, loginAt)
                .set(IamAccountEntity::getUpdatedAt, now()));
    }

    /**
     * 软删除账号。
     * @param accountId 全局账号 ID
     * @return 受影响记录数
     */
    public int delete(Long accountId) {
        return accounts.update(active(accountId).set(IamAccountEntity::getDeletedAt, now()));
    }

    /**
     * 按期望版本更新账号，成功后版本加一；空字段显式写入。
     * @param account 待保存账号
     * @param expectedVersion 期望版本；为空时不匹配任何版本
     * @return 受影响记录数，零表示账号不可用或版本冲突
     */
    public int updateWithVersion(UserAccount account, Long expectedVersion) {
        if (expectedVersion == null) {
            return 0;
        }
        BigInteger version = BigInteger.valueOf(expectedVersion);
        return accounts.update(active(account.getId())
                .eq(IamAccountEntity::getVersion, version)
                .set(IamAccountEntity::getUsername, account.getUsername())
                .set(IamAccountEntity::getPasswordHash, account.getPassword())
                .set(IamAccountEntity::getPhone, account.getPhone())
                .set(IamAccountEntity::getEmail, account.getEmail())
                .set(IamAccountEntity::getEnabled, Boolean.TRUE.equals(account.getEnabled()))
                .set(IamAccountEntity::getMustChangePassword, Boolean.TRUE.equals(account.getMustChangePwd()))
                .set(IamAccountEntity::getPasswordChangedAt, account.getPasswordChangedAt())
                .set(IamAccountEntity::getVersion, version.add(BigInteger.ONE))
                .set(IamAccountEntity::getUpdatedAt, now()));
    }

    /**
     * 按期望版本更新凭证哈希、改密时间与强制改密标记，成功后版本加一。
     *
     * @param accountId 全局账号 ID
     * @param passwordHash 新凭证哈希
     * @param changedAt 改密时间
     * @param expectedVersion 期望版本
     * @param mustChangePassword 更新后是否必须改密
     * @return 受影响记录数，零表示账号不可用或版本冲突
     */
    public int updatePassword(long accountId, String passwordHash, LocalDateTime changedAt,
                              Long expectedVersion, boolean mustChangePassword) {
        if (expectedVersion == null) {
            return 0;
        }
        return accounts.update(active(accountId)
                .eq(IamAccountEntity::getVersion, BigInteger.valueOf(expectedVersion))
                .set(IamAccountEntity::getPasswordHash, passwordHash)
                .set(IamAccountEntity::getPasswordChangedAt, changedAt)
                .set(IamAccountEntity::getMustChangePassword, mustChangePassword)
                .set(IamAccountEntity::getVersion, BigInteger.valueOf(expectedVersion + 1))
                .set(IamAccountEntity::getUpdatedAt, now()));
    }

    /**
     * 按期望版本更新联系资料，成功后版本加一。
     *
     * @param accountId 全局账号 ID
     * @param phone 登录手机号，可空表示不修改
     * @param email 登录邮箱，可空表示不修改
     * @param expectedVersion 期望版本
     * @return 受影响记录数
     */
    public int updateContacts(long accountId, String phone, String email, Long expectedVersion) {
        if (expectedVersion == null) {
            return 0;
        }
        LambdaUpdateWrapper<IamAccountEntity> update = active(accountId)
                .eq(IamAccountEntity::getVersion, BigInteger.valueOf(expectedVersion))
                .set(IamAccountEntity::getVersion, BigInteger.valueOf(expectedVersion + 1))
                .set(IamAccountEntity::getUpdatedAt, now());
        if (phone != null) {
            update.set(IamAccountEntity::getPhone, phone.isBlank() ? null : phone);
        }
        if (email != null) {
            update.set(IamAccountEntity::getEmail, email.isBlank() ? null : email);
        }
        return accounts.update(update);
    }

    private LambdaUpdateWrapper<IamAccountEntity> active(Long accountId) {
        return Wrappers.<IamAccountEntity>lambdaUpdate()
                .eq(IamAccountEntity::getId, BigInteger.valueOf(accountId))
                .isNull(IamAccountEntity::getDeletedAt);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
