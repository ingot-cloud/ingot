package com.ingot.cloud.iam.infrastructure.persistence.mapper;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamTenantMemberEntity;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.infrastructure.persistence.IamMembershipSql;
import com.ingot.cloud.iam.infrastructure.persistence.IamPersistence;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamTenantGroupEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamTenantGroupMemberEntity;
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
    /** 当前关联页和总数共享的组范围条件，以 AND 开头；null 表示全量，可由动态 where 标签省略。 */
    String GROUP_SCOPE = """
            <if test="groupIds != null">
              AND mg.id IN
              <foreach collection="groupIds" item="groupId" open="(" separator="," close=")">#{groupId}</foreach>
            </if>
            """;
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
     * @param groupIds 已编译的可见组 ID，null 表示全量
     * @return 去重后的组数
     */
    @Select("<script>" + IamMembershipSql.DEPARTMENT_REACH + IamMembershipSql.MEMBER_GROUPS
            + " SELECT COUNT(*) FROM member_groups mg <where>" + GROUP_SCOPE + "</where></script>")
    long countGroupsOfMember(@Param("tenantId") BigInteger tenantId, @Param("memberId") BigInteger memberId,
                             @Param("groupIds") Set<BigInteger> groupIds);

    /**
     * 按组 ID 升序分页读取成员所属租户组，展开规则与 {@link #countGroupsOfMember} 一致。
     *
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @param groupIds 已编译的可见组 ID，null 表示全量
     * @param limit 页大小
     * @param offset 偏移
     * @return 当前页组
     */
    @Select("<script>" + IamMembershipSql.DEPARTMENT_REACH + IamMembershipSql.MEMBER_GROUPS + """
             SELECT g.id, g.tenant_id, g.name, g.description, g.version
               FROM iam_tenant_group g
               JOIN member_groups mg ON mg.id=g.id
              WHERE g.tenant_id=#{tenantId}
            """ + GROUP_SCOPE + """
              ORDER BY g.id
              LIMIT #{limit} OFFSET #{offset}
            </script>""")
    List<IamTenantGroupEntity> pageGroupsOfMember(@Param("tenantId") BigInteger tenantId,
                                                  @Param("memberId") BigInteger memberId,
                                                  @Param("groupIds") Set<BigInteger> groupIds,
                                                  @Param("limit") int limit, @Param("offset") int offset);
    /**
     * 查询当前页各组内符合成员读取范围的直接关联。
     *
     * @param groupIds 已分页的组 ID
     * @param tenantId 当前可信租户
     * @param scope 按成员表编译的读取范围
     * @return 组与可见直接成员关联
     */
    @Select("""
            <script>
            SELECT link.group_id, link.member_id FROM iam_tenant_group_member link
            JOIN iam_tenant_member ON iam_tenant_member.id=link.member_id
              AND iam_tenant_member.tenant_id=link.tenant_id AND link.tenant_id=#{tenantId}
              AND link.group_id IN
              <foreach collection="pageGroupIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            ${ew.customSqlSegment}
            ORDER BY link.group_id, link.member_id
            </script>
            """)
    List<IamTenantGroupMemberEntity> visibleGroupMembers(
            @Param("pageGroupIds") List<BigInteger> groupIds,
            @Param("tenantId") BigInteger tenantId,
            @Param(Constants.WRAPPER) Wrapper<IamTenantMemberEntity> scope);
}
