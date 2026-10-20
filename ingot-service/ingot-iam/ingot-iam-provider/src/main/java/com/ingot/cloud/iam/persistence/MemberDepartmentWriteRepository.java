package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.Map;
import java.util.TreeMap;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.persistence.entity.IamMemberDepartmentEntity;
import com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>在调用方已持有组织和成员锁的事务内按完整关系键差量保存任职，不承担授权校验。</p>
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class MemberDepartmentWriteRepository {
    private final IamMemberDepartmentMapper relations;

    /**
     * 保留未变关系，只增删差异并切换主标记；空目标表示清空。
     * @param tenantId 已授权且持锁的租户
     * @param memberId 已验证归属且持锁的成员
     * @param departments 完整目标关系，null等同空集合
     */
    public void replace(long tenantId, long memberId, Map<String, Boolean> departments) {
        BigInteger tenant = BigInteger.valueOf(tenantId);
        BigInteger member = BigInteger.valueOf(memberId);
        Map<BigInteger, Boolean> before = new TreeMap<>();
        relations.lockByMember(tenant, member).forEach(row ->
                before.put(row.getDepartmentId(), Boolean.TRUE.equals(row.getIsPrimary())));
        Map<BigInteger, Boolean> after = new TreeMap<>();
        if (departments != null) {
            departments.forEach((department, primary) -> {
                if (after.putIfAbsent(new BigInteger(department), Boolean.TRUE.equals(primary)) != null) {
                    throw new BizException(IamReasonCode.INVALID_ARGUMENT);
                }
            });
        }
        before.forEach((department, primary) -> {
            if (!after.containsKey(department)) {
                relations.delete(Wrappers.<IamMemberDepartmentEntity>lambdaQuery()
                        .eq(IamMemberDepartmentEntity::getTenantId, tenant)
                        .eq(IamMemberDepartmentEntity::getMemberId, member)
                        .eq(IamMemberDepartmentEntity::getDepartmentId, department));
            } else if (primary && !after.get(department)) {
                setPrimary(tenant, member, department, false);
            }
        });
        after.forEach((department, primary) -> {
            if (!before.containsKey(department)) {
                var row = new IamMemberDepartmentEntity();
                row.setTenantId(tenant);
                row.setMemberId(member);
                row.setDepartmentId(department);
                row.setIsPrimary(primary);
                relations.insert(row);
            }
        });
        after.forEach((department, primary) -> {
            if (primary && before.containsKey(department) && !Boolean.TRUE.equals(before.get(department))) {
                setPrimary(tenant, member, department, true);
            }
        });
    }

    private void setPrimary(BigInteger tenant, BigInteger member, BigInteger department, boolean primary) {
        relations.update(Wrappers.<IamMemberDepartmentEntity>lambdaUpdate()
                .eq(IamMemberDepartmentEntity::getTenantId, tenant)
                .eq(IamMemberDepartmentEntity::getMemberId, member)
                .eq(IamMemberDepartmentEntity::getDepartmentId, department)
                .set(IamMemberDepartmentEntity::getIsPrimary, primary));
    }
}
