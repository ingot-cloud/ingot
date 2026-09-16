package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamMembershipSql;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamDelegationRecipientDepartmentEntity;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_delegation_recipient_department，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamDelegationRecipientDepartmentMapper extends BaseMapper<IamDelegationRecipientDepartmentEntity> {
    /**
     * 统计成员是否通过委派接收部门命中任职关系。
     * <p>本人任职部门总是命中，上级部门只在该接收规则连带下级时命中，与组、人群使用同一套展开规则。</p>
     * @param id 委派 ID
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @return 命中条数
     */
    @Select(IamMembershipSql.DEPARTMENT_REACH + """
            SELECT COUNT(*) FROM iam_delegation_recipient_department d
              JOIN member_reach r ON r.id=d.department_id
             WHERE d.delegation_id=#{id} AND d.tenant_id=#{tenantId}
               AND (r.direct OR d.include_descendants)
            """)
    long countMemberInRecipientDepartments(@Param("id") BigInteger id, @Param("tenantId") BigInteger tenantId,
                                           @Param("memberId") BigInteger memberId);
}
