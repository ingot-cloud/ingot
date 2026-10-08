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
     * 读取可信成员对应账号的即时改密状态，供求值器在缓存及管理员捷径前收紧资格。
     * @param domain 可信管理域
     * @param tenantId 租户域必填
     * @param memberId 当前可信成员
     * @return 账号改密状态；身份不存在时为空
     */
    @Select("""
        <script>
        SELECT a.must_change_password FROM iam_account a
        <choose>
          <when test="domain.name() == 'PLATFORM'">
            JOIN iam_platform_member m ON m.account_id=a.id
            WHERE m.id=#{memberId} AND m.status='ACTIVE'
          </when>
          <otherwise>
            JOIN iam_tenant_member m ON m.account_id=a.id
            WHERE m.id=#{memberId} AND m.tenant_id=#{tenantId} AND m.status='ACTIVE'
          </otherwise>
        </choose>
        AND a.enabled=TRUE AND a.deleted_at IS NULL
        </script>
        """)
    Boolean passwordChangeRequired(@Param("domain") AuthorizationDomain domain,
            @Param("tenantId") BigInteger tenantId, @Param("memberId") BigInteger memberId);

    /** 成员当前有效角色关系，组继承只披露角色摘要。 */
    String MEMBER_BOUND_RELATION = """
        FROM iam_role_assignment ra
        JOIN iam_role_revision r ON r.id=ra.revision_id AND r.kind=ra.revision_kind
        JOIN iam_role_definition d ON d.id=r.role_id AND d.domain='PLATFORM' AND d.tenant_id IS NULL
        WHERE ra.domain='PLATFORM' AND ra.tenant_id IS NULL AND ra.status='ACTIVE'
          AND ra.valid_from&lt;=CURRENT_TIMESTAMP AND (ra.valid_until IS NULL OR ra.valid_until&gt;CURRENT_TIMESTAMP)
          AND EXISTS(SELECT 1 FROM iam_platform_member m WHERE m.id=#{memberId} AND m.status='ACTIVE')
          AND ((ra.subject_type='MEMBER' AND ra.platform_member_id=#{memberId})
            OR (ra.subject_type='GROUP' AND EXISTS(SELECT 1 FROM iam_platform_group_member gm
                  WHERE gm.group_id=ra.platform_group_id AND gm.member_id=#{memberId})))
          AND (
        """ + PlatformAssignmentStateSql.SOURCE_VALID + ")=TRUE ";

    /**
     * 分页当前有效角色/固定版本，以关系 SQL 聚合而非回查父列表。
     * @param memberId 已通过成员查看范围校验的目标
     * @param offset 偏移
     * @param size 页大小
     * @return 不披露组信息的摘要投影
     */
    @Select("<script>SELECT r.role_id,d.name,r.id AS revision_id,r.kind,r.revision AS revision_number,"
            + "SUM(ra.subject_type='MEMBER') AS direct_count,SUM(ra.subject_type='GROUP') AS group_count "
            + MEMBER_BOUND_RELATION + " GROUP BY r.role_id,d.name,r.id,r.kind,r.revision ORDER BY r.role_id,r.id"
            + " LIMIT #{size} OFFSET #{offset}</script>")
    List<com.ingot.cloud.iam.persistence.projection.MemberBoundRoleRow> memberBoundRoles(
            @Param("memberId") BigInteger memberId, @Param("offset") int offset, @Param("size") int size);

    /**
     * 按相同有效关系计算固定版本数量。
     * @param memberId 已校验成员
     * @return 总数
     */
    @Select("<script>SELECT COUNT(DISTINCT r.id) " + MEMBER_BOUND_RELATION + "</script>")
    long countMemberBoundRoles(@Param("memberId") BigInteger memberId);

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

    /**
     * 读取真实系统超管直接分配，同时检查平台成员和账号可用性。
     * @param memberId 平台成员
     * @param code 服务器保留的系统角色编码
     * @return 有效来源；不包含组或委派
     */
    @Select("""
            SELECT ra.revision_id,ra.scope_bindings,ra.delegation_grant_id,ra.valid_until,
                   NULL AS delegation_valid_until,ra.id AS assignment_id,ra.revision_kind,NULL AS group_id
              FROM iam_role_assignment ra
              JOIN iam_role_revision rv ON rv.id=ra.revision_id AND rv.kind='SYSTEM'
              JOIN iam_role_definition r ON r.id=rv.role_id AND r.kind='SYSTEM' AND r.domain='PLATFORM'
              JOIN iam_platform_member m ON m.id=ra.platform_member_id AND m.status='ACTIVE'
              JOIN iam_account ac ON ac.id=m.account_id AND ac.enabled=TRUE AND ac.deleted_at IS NULL
             WHERE ra.domain='PLATFORM' AND ra.tenant_id IS NULL AND ra.status='ACTIVE'
               AND ra.subject_type='MEMBER' AND ra.delegation_grant_id IS NULL AND ra.revision_kind='SYSTEM'
               AND r.code=#{code} AND r.enabled=TRUE AND r.tenant_id IS NULL AND ra.platform_member_id=#{memberId}
               AND ra.valid_from<=CURRENT_TIMESTAMP AND (ra.valid_until IS NULL OR ra.valid_until>CURRENT_TIMESTAMP)
               AND NOT EXISTS (SELECT 1 FROM account_lock_state ls WHERE ls.user_id=ac.id AND ls.user_type='0'
                 AND ls.locked=TRUE AND (ls.locked_until IS NULL OR ls.locked_until>CURRENT_TIMESTAMP))
             ORDER BY ra.id
            """)
    List<AuthorizationEvalRows.Assignment> platformAdministratorAssignments(@Param("memberId") BigInteger memberId,
            @Param("code") String code);

    /**
     * 在平台授权串行锁内检查仍可用的长期超管数量，读本事务写入后的状态。
     * @param code 服务器保留编码
     * @return 不重复的长期可用成员数
     */
    @Select("""
            SELECT COUNT(DISTINCT m.id) FROM iam_role_assignment ra
              JOIN iam_role_revision rv ON rv.id=ra.revision_id AND rv.kind='SYSTEM'
              JOIN iam_role_definition r ON r.id=rv.role_id AND r.kind='SYSTEM' AND r.domain='PLATFORM'
              JOIN iam_platform_member m ON m.id=ra.platform_member_id AND m.status='ACTIVE'
              JOIN iam_account ac ON ac.id=m.account_id AND ac.enabled=TRUE AND ac.deleted_at IS NULL
             WHERE ra.domain='PLATFORM' AND ra.tenant_id IS NULL AND ra.status='ACTIVE'
               AND ra.subject_type='MEMBER' AND ra.delegation_grant_id IS NULL AND ra.revision_kind='SYSTEM'
               AND r.code=#{code} AND r.enabled=TRUE AND r.tenant_id IS NULL
               AND ra.valid_from<=CURRENT_TIMESTAMP AND ra.valid_until IS NULL
               AND NOT EXISTS (SELECT 1 FROM account_lock_state ls WHERE ls.user_id=ac.id AND ls.user_type='0'
                 AND ls.locked=TRUE AND (ls.locked_until IS NULL OR ls.locked_until>CURRENT_TIMESTAMP))
            """)
    long availablePermanentAdministrators(@Param("code") String code);
}
