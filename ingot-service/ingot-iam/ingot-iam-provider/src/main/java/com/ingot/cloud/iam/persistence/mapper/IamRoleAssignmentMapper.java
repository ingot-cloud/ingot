package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;
import java.util.List;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamMembershipSql;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.GrantStatus;
import com.ingot.framework.commons.model.iam.SubjectType;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_role_assignment，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamRoleAssignmentMapper extends BaseMapper<IamRoleAssignmentEntity> {
    /**
     * 读取平台成员当前有效的直接分配。
     * <p>来源委派的分配持续核对委派状态、有效期、角色版本白名单与接收成员名单，任一不再成立即不返回。</p>
     *
     * @param domain 授权域
     * @param status 分配状态
     * @param subjectType 主体类型
     * @param memberId 平台成员 ID
     * @return 版本、绑定、委派来源与有效期截止
     */
    @Select("""
            SELECT ra.revision_id,ra.scope_bindings,ra.delegation_grant_id,ra.valid_until,
                   (SELECT d.valid_until FROM iam_delegation_grant d
                     WHERE d.id=ra.delegation_grant_id) AS delegation_valid_until,
                   ra.id AS assignment_id,ra.revision_kind,COALESCE(ra.platform_group_id,ra.tenant_group_id) AS group_id
              FROM iam_role_assignment ra
             WHERE ra.domain=#{domain} AND ra.status=#{status} AND ra.subject_type=#{subjectType}
               AND ra.platform_member_id=#{memberId} AND ra.tenant_id IS NULL
               AND ra.valid_from<=CURRENT_TIMESTAMP
               AND (ra.valid_until IS NULL OR ra.valid_until>CURRENT_TIMESTAMP)
               AND (ra.delegation_grant_id IS NULL OR EXISTS (
                    SELECT 1 FROM iam_delegation_grant d
                     WHERE d.id=ra.delegation_grant_id AND d.status=#{status}
                       AND (d.valid_from IS NULL OR d.valid_from<=CURRENT_TIMESTAMP)
                       AND (d.valid_until IS NULL OR d.valid_until>CURRENT_TIMESTAMP)
            """ + IamMembershipSql.AND_REVISION_STILL_ALLOWED + IamMembershipSql.AND_PLATFORM_DURATION_ALLOWED + IamMembershipSql.AND_PLATFORM_RECIPIENT_REACHED + """
                       ))
            """)
    List<AuthorizationEvalRows.Assignment> listPlatformDirect(@Param("domain") AuthorizationDomain domain,
                                                              @Param("status") GrantStatus status,
                                                              @Param("subjectType") SubjectType subjectType,
                                                              @Param("memberId") BigInteger memberId);

    /**
     * 读取租户成员当前有效的直接分配。
     * <p>来源委派的分配持续核对委派状态、有效期、角色版本白名单与接收人；接收部门按任职关系与连带下级判定。</p>
     *
     * @param domain 授权域
     * @param status 分配状态
     * @param subjectType 主体类型
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @return 版本、绑定、委派来源与有效期截止
     */
    @Select(IamMembershipSql.DEPARTMENT_REACH + """
            SELECT ra.revision_id,ra.scope_bindings,ra.delegation_grant_id,ra.valid_until,
                   (SELECT d.valid_until FROM iam_delegation_grant d
                     WHERE d.id=ra.delegation_grant_id) AS delegation_valid_until,
                   ra.id AS assignment_id,ra.revision_kind,COALESCE(ra.platform_group_id,ra.tenant_group_id) AS group_id
              FROM iam_role_assignment ra
             WHERE ra.domain=#{domain} AND ra.status=#{status} AND ra.subject_type=#{subjectType}
               AND ra.tenant_member_id=#{memberId} AND ra.tenant_id=#{tenantId}
               AND ra.valid_from<=CURRENT_TIMESTAMP
               AND (ra.valid_until IS NULL OR ra.valid_until>CURRENT_TIMESTAMP)
               AND (ra.delegation_grant_id IS NULL OR EXISTS (
                    SELECT 1 FROM iam_delegation_grant d
                     WHERE d.id=ra.delegation_grant_id AND d.status=#{status}
                       AND (d.valid_from IS NULL OR d.valid_from<=CURRENT_TIMESTAMP)
                       AND (d.valid_until IS NULL OR d.valid_until>CURRENT_TIMESTAMP)
            """ + IamMembershipSql.AND_REVISION_STILL_ALLOWED + IamMembershipSql.AND_TENANT_RECIPIENT_REACHED + """
                       ))
            """)
    List<AuthorizationEvalRows.Assignment> listTenantDirect(@Param("domain") AuthorizationDomain domain,
                                                            @Param("status") GrantStatus status,
                                                            @Param("subjectType") SubjectType subjectType,
                                                            @Param("tenantId") BigInteger tenantId,
                                                            @Param("memberId") BigInteger memberId);

    /**
     * 读取平台组成员当前有效的组主体分配。
     * <p>来源委派的分配按当前成员重新核对接收名单，组被扩大不会让名单外成员受益。</p>
     *
     * @param domain 授权域
     * @param status 分配状态
     * @param subjectType 主体类型
     * @param memberId 平台成员 ID
     * @return 版本、绑定、委派来源与有效期截止
     */
    @Select("""
            SELECT ra.revision_id,ra.scope_bindings,ra.delegation_grant_id,ra.valid_until,
                   (SELECT d.valid_until FROM iam_delegation_grant d
                     WHERE d.id=ra.delegation_grant_id) AS delegation_valid_until,
                   ra.id AS assignment_id,ra.revision_kind,COALESCE(ra.platform_group_id,ra.tenant_group_id) AS group_id
              FROM iam_role_assignment ra
              JOIN iam_platform_group_member gm ON gm.group_id=ra.platform_group_id
             WHERE ra.domain=#{domain} AND ra.status=#{status} AND ra.subject_type=#{subjectType}
               AND gm.member_id=#{memberId} AND ra.tenant_id IS NULL
               AND ra.valid_from<=CURRENT_TIMESTAMP
               AND (ra.valid_until IS NULL OR ra.valid_until>CURRENT_TIMESTAMP)
               AND (ra.delegation_grant_id IS NULL OR EXISTS (
                    SELECT 1 FROM iam_delegation_grant d
                     WHERE d.id=ra.delegation_grant_id AND d.status=#{status}
                       AND (d.valid_from IS NULL OR d.valid_from<=CURRENT_TIMESTAMP)
                       AND (d.valid_until IS NULL OR d.valid_until>CURRENT_TIMESTAMP)
            """ + IamMembershipSql.AND_REVISION_STILL_ALLOWED + IamMembershipSql.AND_PLATFORM_DURATION_ALLOWED + IamMembershipSql.AND_PLATFORM_GROUP_RECIPIENTS_REACHED + """
                       ))
            """)
    List<AuthorizationEvalRows.Assignment> listPlatformGroup(@Param("domain") AuthorizationDomain domain,
                                                             @Param("status") GrantStatus status,
                                                             @Param("subjectType") SubjectType subjectType,
                                                             @Param("memberId") BigInteger memberId);

    /**
     * 读取当前成员所属租户组的有效组主体分配，组成员含显式成员与部门来源。
     * <p>来源委派的分配按当前成员重新核对角色版本白名单与接收人，组或部门被扩大不会让名单外成员受益。</p>
     *
     * @param domain 授权域
     * @param status 分配状态
     * @param subjectType 主体类型
     * @param tenantId 已授权租户 ID
     * @param memberId 租户成员 ID
     * @return 版本、绑定、委派来源与有效期截止
     */
    @Select(IamMembershipSql.DEPARTMENT_REACH + IamMembershipSql.MEMBER_GROUPS + """
            SELECT ra.revision_id,ra.scope_bindings,ra.delegation_grant_id,ra.valid_until,
                   (SELECT d.valid_until FROM iam_delegation_grant d
                     WHERE d.id=ra.delegation_grant_id) AS delegation_valid_until,
                   ra.id AS assignment_id,ra.revision_kind,COALESCE(ra.platform_group_id,ra.tenant_group_id) AS group_id
              FROM iam_role_assignment ra
             WHERE ra.domain=#{domain} AND ra.status=#{status} AND ra.subject_type=#{subjectType}
               AND ra.tenant_group_id """ + IamMembershipSql.IN_MEMBER_GROUPS + """
               AND ra.tenant_id=#{tenantId}
               AND ra.valid_from<=CURRENT_TIMESTAMP
               AND (ra.valid_until IS NULL OR ra.valid_until>CURRENT_TIMESTAMP)
               AND (ra.delegation_grant_id IS NULL OR EXISTS (
                    SELECT 1 FROM iam_delegation_grant d
                     WHERE d.id=ra.delegation_grant_id AND d.status=#{status}
                       AND (d.valid_from IS NULL OR d.valid_from<=CURRENT_TIMESTAMP)
                       AND (d.valid_until IS NULL OR d.valid_until>CURRENT_TIMESTAMP)
            """ + IamMembershipSql.AND_REVISION_STILL_ALLOWED + IamMembershipSql.AND_TENANT_RECIPIENT_REACHED + """
                       ))
            """)
    List<AuthorizationEvalRows.Assignment> listTenantGroup(@Param("domain") AuthorizationDomain domain,
                                                           @Param("status") GrantStatus status,
                                                           @Param("subjectType") SubjectType subjectType,
                                                           @Param("tenantId") BigInteger tenantId,
                                                           @Param("memberId") BigInteger memberId);

    /**
     * 锁定平台域分配行，调用方须处于事务中。
     * @param id 分配 ID
     * @param domain 期望授权域
     * @return 命中行；不存在时为空
     */
    @Select("""
            SELECT id,revision_id,revision_kind,scope_bindings,status,version,source,subject_type,
                   platform_member_id,platform_group_id,tenant_member_id,tenant_group_id,delegation_grant_id,
                   valid_from,valid_until
              FROM iam_role_assignment WHERE id=#{id} AND domain=#{domain} AND tenant_id IS NULL FOR UPDATE
            """)
    IamRoleAssignmentEntity lockPlatform(@Param("id") BigInteger id, @Param("domain") AuthorizationDomain domain);

    /**
     * 锁定租户域分配行，调用方须处于事务中并提供可信 tenantId。
     * @param id 分配 ID
     * @param domain 期望授权域
     * @param tenantId 已授权租户 ID
     * @return 命中行；不存在时为空
     */
    @Select("""
            SELECT id,revision_id,revision_kind,scope_bindings,status,version,source,subject_type,
                   platform_member_id,platform_group_id,tenant_member_id,tenant_group_id,delegation_grant_id,
                   valid_from,valid_until
              FROM iam_role_assignment WHERE id=#{id} AND domain=#{domain} AND tenant_id=#{tenantId} FOR UPDATE
            """)
    IamRoleAssignmentEntity lockTenant(@Param("id") BigInteger id, @Param("domain") AuthorizationDomain domain,
                                       @Param("tenantId") BigInteger tenantId);

    /**
     * 一次批量关联平台分配标签及最早创建审计，避免逐行读取授权人。
     * @param ids 当前可披露页 ID
     * @return 显示信息
     */
    @Select("""
            <script>SELECT ra.id,COALESCE(m.display_name,g.name) AS subject_name,
                   d.name AS role_name,r.revision AS revision_number,au.actor_member_id AS author_id,
                   author.display_name AS author_name,
            """ + PlatformAssignmentStateSql.SOURCE_VALID + """
                   AS source_valid
              FROM iam_role_assignment ra JOIN iam_role_revision r ON r.id=ra.revision_id
              JOIN iam_role_definition d ON d.id=r.role_id
              LEFT JOIN iam_platform_member m ON m.id=ra.platform_member_id
              LEFT JOIN iam_platform_group g ON g.id=ra.platform_group_id
              LEFT JOIN iam_authorization_audit au ON au.assignment_id=ra.id AND au.change_type='CREATE'
               AND NOT EXISTS(SELECT 1 FROM iam_authorization_audit older WHERE older.assignment_id=ra.id
                    AND older.change_type='CREATE' AND (older.occurred_at&lt;au.occurred_at
                    OR (older.occurred_at=au.occurred_at AND older.id&lt;au.id)))
              LEFT JOIN iam_platform_member author ON author.id=au.actor_member_id
             WHERE ra.domain='PLATFORM' AND ra.tenant_id IS NULL
               AND ra.id IN <foreach collection="ids" item="id" open="(" close=")" separator=",">#{id}</foreach>
            </script>
            """)
    List<com.ingot.cloud.iam.persistence.projection.AssignmentPresentation> presentation(
            @Param("ids") List<BigInteger> ids);

    /**
     * 本人有效平台委派，只提供分配入口，不产生业务授权。
     * @param memberId 可信平台成员
     * @return 有效委派
     */
    @Select("""
            SELECT d.* FROM iam_delegation_grant d
             WHERE d.domain='PLATFORM' AND d.tenant_id IS NULL AND d.status='ACTIVE'
               AND d.platform_administrator_id=#{memberId}
               AND (d.valid_from IS NULL OR d.valid_from<=CURRENT_TIMESTAMP)
               AND (d.valid_until IS NULL OR d.valid_until>CURRENT_TIMESTAMP)
             ORDER BY d.id
            """)
    List<com.ingot.cloud.iam.persistence.entity.IamDelegationGrantEntity> effectivePlatformDelegations(
            @Param("memberId") BigInteger memberId);

    /**
     * 平台写入的首个锁，统一委派、分配与组成员变更顺序。
     * @return 治理应用行
     */
    @Select(com.ingot.cloud.iam.persistence.IamAuthorizationSql.PLATFORM_WRITE_LOCK)
    BigInteger lockPlatformAuthorization();

    /**
     * 统计引用指定角色任一版本的授权条数。
     * @param roleId 角色定义 ID
     * @return 命中条数
     */
    @Select("""
            SELECT COUNT(*) FROM iam_role_assignment a JOIN iam_role_revision r ON r.id=a.revision_id
             WHERE r.role_id=#{roleId}
            """)
    long countByRoleId(@Param("roleId") BigInteger roleId);
}
