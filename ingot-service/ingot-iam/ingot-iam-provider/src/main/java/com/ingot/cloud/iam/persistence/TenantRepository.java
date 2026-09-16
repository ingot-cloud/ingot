package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.persistence.entity.IamTenantEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>持久化组织设置，租户身份操作始终绑定授权后的组织 ID。</p>
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class TenantRepository {
    private final IamTenantMapper tenants;
    private final IamTenantMemberMapper members;

    /**
     * 分页读取平台有权管理的未删除组织。
     * @param page 页码，从一开始
     * @param size 每页大小
     * @return 组织页及总数
     */
    public Page<IamTenantEntity> page(int page, int size) {
        return tenants.selectPage(new Page<>(page, size), Wrappers.<IamTenantEntity>lambdaQuery()
                .isNull(IamTenantEntity::getDeletedAt).orderByAsc(IamTenantEntity::getId));
    }

    /**
     * 读取未删除组织。
     * @param id 已授权组织 ID
     * @return 组织记录或空
     */
    public IamTenantEntity findActive(long id) {
        return tenants.selectOne(Wrappers.<IamTenantEntity>lambdaQuery()
                .eq(IamTenantEntity::getId, BigInteger.valueOf(id)).isNull(IamTenantEntity::getDeletedAt));
    }

    /**
     * 按 ID 集合读取未删除组织。
     *
     * @param ids 组织 ID
     * @return 未删除组织
     */
    public List<IamTenantEntity> listByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return tenants.selectList(Wrappers.<IamTenantEntity>lambdaQuery()
                .in(IamTenantEntity::getId, ids.stream().map(BigInteger::valueOf).toList())
                .isNull(IamTenantEntity::getDeletedAt)
                .orderByAsc(IamTenantEntity::getId));
    }

    /**
     * 在调用方事务中锁定未删除组织。
     * @param id 已授权组织 ID
     * @return 组织记录或空
     */
    public IamTenantEntity lockActive(long id) {
        return tenants.lockActive(BigInteger.valueOf(id));
    }

    /**
     * 按持锁读到的版本条件更新已锁定组织的设置并递增版本。
     * @param id 已授权组织 ID
     * @param name 组织名称
     * @param avatar 头像，可清空
     * @param enabled 启用状态，为空时保持原值
     * @param version 持锁读到的版本
     * @return 受影响行数；为 0 表示版本已被并发改写
     */
    public int update(long id, String name, String avatar, Boolean enabled, BigInteger version) {
        return tenants.update(Wrappers.<IamTenantEntity>lambdaUpdate()
                .eq(IamTenantEntity::getId, BigInteger.valueOf(id))
                .eq(IamTenantEntity::getVersion, version)
                .set(IamTenantEntity::getName, name).set(IamTenantEntity::getAvatar, avatar)
                .set(enabled != null, IamTenantEntity::getEnabled, enabled)
                .set(IamTenantEntity::getVersion, version.add(BigInteger.ONE)));
    }

    /**
     * 在调用方事务中锁定指定组织的成员行。
     * @param tenantId 已授权组织 ID
     * @param memberId 成员 ID
     * @return 锁定行，不存在时为空
     */
    public IamTenantMemberEntity lockMember(long tenantId, long memberId) {
        return members.lock(BigInteger.valueOf(tenantId), BigInteger.valueOf(memberId));
    }

    /**
     * 按持锁读到的版本条件转交所有者并递增版本。
     * @param tenantId 已授权组织 ID
     * @param memberId 已验证的有效成员 ID
     * @param version 持锁读到的版本
     * @return 受影响行数；为 0 表示版本已被并发改写
     */
    public int transferOwner(long tenantId, long memberId, BigInteger version) {
        return tenants.update(Wrappers.<IamTenantEntity>lambdaUpdate()
                .eq(IamTenantEntity::getId, BigInteger.valueOf(tenantId))
                .eq(IamTenantEntity::getVersion, version)
                .set(IamTenantEntity::getOwnerMemberId, BigInteger.valueOf(memberId))
                .set(IamTenantEntity::getVersion, version.add(BigInteger.ONE)));
    }
}
