package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamMemberDepartmentEntity;
import com.ingot.cloud.iam.persistence.projection.DepartmentMemberCount;
import com.ingot.framework.commons.model.iam.MemberStatus;
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

    /**
     * 按部门批量统计指定状态的直属成员，调用方保证 {@code departmentIds} 非空。
     *
     * @param tenantId 已授权租户 ID
     * @param departmentIds 当前页部门 ID
     * @param status 计入的成员状态
     * @return 有任职的部门计数；无成员的部门不出现
     */
    @Select("""
            <script>
            SELECT md.department_id AS departmentId, COUNT(*) AS memberCount
              FROM iam_member_department md
              JOIN iam_tenant_member m ON m.tenant_id = md.tenant_id AND m.id = md.member_id
             WHERE md.tenant_id = #{tenantId}
               AND m.status = #{status}
               AND md.department_id IN
            <foreach collection="departmentIds" item="id" open="(" separator="," close=")">#{id}</foreach>
             GROUP BY md.department_id
            </script>
            """)
    List<DepartmentMemberCount> countMembersByStatus(@Param("tenantId") BigInteger tenantId,
                                                     @Param("departmentIds") Collection<BigInteger> departmentIds,
                                                     @Param("status") MemberStatus status);
}
