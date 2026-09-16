package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;
import java.util.List;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamMembershipSql;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamTenantGroupMemberEntity;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_tenant_group_member，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamTenantGroupMemberMapper extends BaseMapper<IamTenantGroupMemberEntity> {
    /**
     * 统计组的有效成员数：显式成员并入部门来源成员，连带下级的部门规则沿部门树向下展开后去重。
     *
     * @param tenantId 已授权租户 ID
     * @param groupId 组 ID
     * @return 去重后的成员数
     */
    @Select("SELECT COUNT(*) FROM (" + IamMembershipSql.GROUP_MEMBERSHIP + ") membership")
    long countMembership(@Param("tenantId") BigInteger tenantId, @Param("groupId") BigInteger groupId);

    /**
     * 列出组的有效成员，展开规则与 {@link #countMembership} 一致。
     *
     * @param tenantId 已授权租户 ID
     * @param groupId 组 ID
     * @return 去重后的成员 ID
     */
    @Select(IamMembershipSql.GROUP_MEMBERSHIP)
    List<BigInteger> listMembership(@Param("tenantId") BigInteger tenantId, @Param("groupId") BigInteger groupId);
}
