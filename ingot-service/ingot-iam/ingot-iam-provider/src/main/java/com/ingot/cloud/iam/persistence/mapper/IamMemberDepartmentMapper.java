package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;
import java.util.List;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamMemberDepartmentEntity;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_member_department，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamMemberDepartmentMapper extends BaseMapper<IamMemberDepartmentEntity> {
    /**
     * 锁定当前租户成员的任职关系，调用方须处于事务中。
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @return 任职记录
     */
    @Select("SELECT tenant_id AS tenantId,member_id AS memberId,department_id AS departmentId,is_primary AS isPrimary FROM iam_member_department WHERE tenant_id=#{tenantId} AND member_id=#{memberId} FOR UPDATE")
    List<IamMemberDepartmentEntity> lockByMember(@Param("tenantId") BigInteger tenantId,
                                                 @Param("memberId") BigInteger memberId);
}
