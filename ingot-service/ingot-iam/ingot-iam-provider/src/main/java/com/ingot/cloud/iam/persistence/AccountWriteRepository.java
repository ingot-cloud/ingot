package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.persistence.entity.IamAccountEntity;
import com.ingot.cloud.iam.persistence.entity.IamAccountLockStateEntity;
import com.ingot.cloud.iam.persistence.mapper.IamAccountMapper;
import com.ingot.cloud.iam.persistence.mapper.IamAccountLockStateMapper;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.model.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>通过类型化条件维护全局账号及既有账号锁定状态。</p>
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class AccountWriteRepository {
    private final IamAccountMapper accounts;
    private final IamAccountLockStateMapper locks;

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
     * 更新或创建管理账号锁定状态，由调用方事务管理并发冲突。
     * @param accountId 全局账号 ID
     * @param locked 是否锁定
     */
    public void upsertLock(Long accountId, boolean locked) {
        String userType = UserTypeEnum.ADMIN.getValue();
        int updated = locks.update(Wrappers.<IamAccountLockStateEntity>lambdaUpdate()
                .eq(IamAccountLockStateEntity::getUserId, accountId)
                .eq(IamAccountLockStateEntity::getUserType, userType)
                .set(IamAccountLockStateEntity::getLocked, locked)
                .set(IamAccountLockStateEntity::getUpdatedAt, now()));
        if (updated == 0) {
            IamAccountLockStateEntity state = new IamAccountLockStateEntity();
            state.setUserId(accountId);
            state.setUserType(userType);
            state.setLocked(locked);
            locks.insert(state);
        }
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
