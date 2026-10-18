package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;
import java.util.List;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamMembershipSql;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamTenantGroupEntity;
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

    /**
     * 统计成员所属租户组：显式成员并入部门来源，上级部门只在连带下级时命中。
     *
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @return 去重后的组数
     */
    @Select(IamMembershipSql.DEPARTMENT_REACH + IamMembershipSql.MEMBER_GROUPS
            + " SELECT COUNT(*) FROM member_groups")
    long countGroupsOfMember(@Param("tenantId") BigInteger tenantId, @Param("memberId") BigInteger memberId);

    /**
     * 按组 ID 升序分页读取成员所属租户组，展开规则与 {@link #countGroupsOfMember} 一致。
     *
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @param limit 页大小
     * @param offset 偏移
     * @return 当前页组
     */
    @Select(IamMembershipSql.DEPARTMENT_REACH + IamMembershipSql.MEMBER_GROUPS + """
             SELECT g.id, g.tenant_id, g.name, g.description, g.version
               FROM iam_tenant_group g
               JOIN member_groups mg ON mg.id=g.id
              WHERE g.tenant_id=#{tenantId}
              ORDER BY g.id
              LIMIT #{limit} OFFSET #{offset}
            """)
    List<IamTenantGroupEntity> pageGroupsOfMember(@Param("tenantId") BigInteger tenantId,
                                                  @Param("memberId") BigInteger memberId,
                                                  @Param("limit") int limit, @Param("offset") int offset);
}
