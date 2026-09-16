package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.persistence.entity.IamDelegationRecipientMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.IamDelegationRecipientDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamDelegationRecipientMemberMapper;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>判断成员是否仍在委派接收人范围内，供授权写入与组编辑共用同一套口径。</p>
 *
 * <p>接收名单命中即成立；租户委派还可按接收部门命中任职关系，本人任职部门总是命中，
 * 上级部门只在该规则连带下级时命中。平台委派没有部门维度。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class DelegationRecipientRepository {
    private final IamDelegationRecipientMemberMapper members;
    private final IamDelegationRecipientDepartmentMapper departments;

    /**
     * 判断成员是否被委派接收范围覆盖。
     *
     * @param domain 授权域
     * @param tenantId 租户域必填，平台域忽略
     * @param delegationId 委派 ID
     * @param memberId 当前域成员 ID
     * @return 覆盖时为 true
     */
    public boolean reaches(AuthorizationDomain domain, Long tenantId, long delegationId, long memberId) {
        if (domain == AuthorizationDomain.PLATFORM) {
            return members.selectCount(Wrappers.<IamDelegationRecipientMemberEntity>lambdaQuery()
                    .eq(IamDelegationRecipientMemberEntity::getDelegationId, BigInteger.valueOf(delegationId))
                    .eq(IamDelegationRecipientMemberEntity::getPlatformMemberId, BigInteger.valueOf(memberId))) > 0;
        }
        BigInteger tenant = BigInteger.valueOf(tenantId);
        boolean listed = members.selectCount(Wrappers.<IamDelegationRecipientMemberEntity>lambdaQuery()
                .eq(IamDelegationRecipientMemberEntity::getDelegationId, BigInteger.valueOf(delegationId))
                .eq(IamDelegationRecipientMemberEntity::getTenantId, tenant)
                .eq(IamDelegationRecipientMemberEntity::getTenantMemberId, BigInteger.valueOf(memberId))) > 0;
        return listed || departments.countMemberInRecipientDepartments(BigInteger.valueOf(delegationId), tenant,
                BigInteger.valueOf(memberId)) > 0;
    }
}
