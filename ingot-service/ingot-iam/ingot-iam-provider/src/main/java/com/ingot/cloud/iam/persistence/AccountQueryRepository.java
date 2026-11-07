package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.persistence.entity.IamAccountEntity;
import com.ingot.cloud.iam.persistence.mapper.IamAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>查询未删除的全局账号，按已编译对象范围过滤，不返回组织关系。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class AccountQueryRepository {
    private static final String PLATFORM_MEMBERSHIP =
            "SELECT 1 FROM iam_platform_member pm WHERE pm.account_id = iam_account.id";
    private final IamAccountMapper accounts;

    /**
     * 分页列出未删除账号。
     *
     * @param scope 已编译范围
     * @param page 从 1 开始的页码
     * @param size 页大小
     * @return 账号页
     */
    public Page<IamAccountEntity> page(ObjectScope scope, int page, int size) {
        return accounts.selectPage(new Page<>(page, size), pageQuery(scope));
    }

    /**
     * 在读取与查找范围内分页查询可加入平台的账号。
     *
     * @param readScope 账号读取范围
     * @param lookupScope 账号查找范围
     * @param page 从 1 开始的页码
     * @param size 页大小
     * @return 排除所有已有平台成员资格后的账号页与总数
     */
    public Page<IamAccountEntity> pageWithoutPlatformMembers(ObjectScope readScope, ObjectScope lookupScope,
                                                            int page, int size) {
        LambdaQueryWrapper<IamAccountEntity> wrapper = pageQuery(readScope).notExists(PLATFORM_MEMBERSHIP);
        ObjectScopeSql.restrictAccounts(wrapper, lookupScope);
        return accounts.selectPage(new Page<>(page, size), wrapper);
    }

    private LambdaQueryWrapper<IamAccountEntity> pageQuery(ObjectScope scope) {
        LambdaQueryWrapper<IamAccountEntity> wrapper = Wrappers.<IamAccountEntity>lambdaQuery()
                .isNull(IamAccountEntity::getDeletedAt)
                .orderByAsc(IamAccountEntity::getId);
        ObjectScopeSql.restrictAccounts(wrapper, scope);
        return wrapper;
    }

    /**
     * 读取未删除账号。
     *
     * @param accountId 全局账号 ID
     * @return 账号或空
     */
    public IamAccountEntity find(long accountId) {
        return accounts.selectOne(Wrappers.<IamAccountEntity>lambdaQuery()
                .eq(IamAccountEntity::getId, BigInteger.valueOf(accountId))
                .isNull(IamAccountEntity::getDeletedAt));
    }

    /**
     * 按 ID 集合读取未删除账号，保持输入顺序之外的升序。
     *
     * @param ids 账号 ID
     * @return 未删除账号
     */
    public List<IamAccountEntity> listByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<BigInteger> keys = ids.stream().map(BigInteger::valueOf).toList();
        return accounts.selectList(Wrappers.<IamAccountEntity>lambdaQuery()
                .in(IamAccountEntity::getId, keys)
                .isNull(IamAccountEntity::getDeletedAt)
                .orderByAsc(IamAccountEntity::getId));
    }

    /**
     * 按登录名精确查找未删除账号。
     *
     * @param username 登录名
     * @return 账号或空
     */
    public IamAccountEntity findByUsername(String username) {
        return accounts.selectOne(Wrappers.<IamAccountEntity>lambdaQuery()
                .eq(IamAccountEntity::getUsername, username)
                .isNull(IamAccountEntity::getDeletedAt));
    }

    /**
     * 按手机号精确查找未删除账号。
     *
     * @param phone 手机号
     * @return 账号或空
     */
    public IamAccountEntity findByPhone(String phone) {
        return accounts.selectOne(Wrappers.<IamAccountEntity>lambdaQuery()
                .eq(IamAccountEntity::getPhone, phone)
                .isNull(IamAccountEntity::getDeletedAt));
    }

    /**
     * 按手机号列出全部未删除账号，用于发现一对多冲突。
     *
     * @param phone 手机号
     * @return 匹配账号，可能为空
     */
    public List<IamAccountEntity> listByPhone(String phone) {
        return accounts.selectList(Wrappers.<IamAccountEntity>lambdaQuery()
                .eq(IamAccountEntity::getPhone, phone)
                .isNull(IamAccountEntity::getDeletedAt)
                .orderByAsc(IamAccountEntity::getId));
    }

    /**
     * 按邮箱精确查找未删除账号。
     *
     * @param email 邮箱
     * @return 账号或空
     */
    public IamAccountEntity findByEmail(String email) {
        return accounts.selectOne(Wrappers.<IamAccountEntity>lambdaQuery()
                .eq(IamAccountEntity::getEmail, email)
                .isNull(IamAccountEntity::getDeletedAt));
    }

    /**
     * 在调用方事务中锁定未删除账号。
     *
     * @param accountId 全局账号 ID
     * @return 锁定行，不存在时为空
     */
    public IamAccountEntity lock(long accountId) {
        return accounts.lock(BigInteger.valueOf(accountId));
    }
}
