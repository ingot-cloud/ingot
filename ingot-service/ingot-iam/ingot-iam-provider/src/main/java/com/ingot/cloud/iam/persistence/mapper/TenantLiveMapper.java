package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamMembershipSql;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.TenantLiveSql;
import com.ingot.cloud.iam.persistence.projection.TenantLiveRows;
import com.ingot.framework.commons.model.iam.*;
import org.apache.ibatis.annotations.*;

/**
 * <p>以显式租户归属访问当前角色和资源委派，不复用平台固定版本SQL。</p>
 * @author jy
 * @since 1.0.0
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface TenantLiveMapper {
    /** 核对当前组织和成员仍有效，并读取拥有者事实。 */
    @Select("""
        SELECT t.owner_member_id,t.version,m.version AS member_version,a.version AS account_version FROM iam_tenant t
          JOIN iam_tenant_member m ON m.tenant_id=t.id
          JOIN iam_account a ON a.id=m.account_id
         WHERE t.id=#{tenantId} AND m.id=#{memberId} AND t.enabled=TRUE AND t.deleted_at IS NULL
           AND m.status='ACTIVE' AND a.enabled=TRUE AND a.deleted_at IS NULL
        """)
    TenantLiveRows.Owner owner(@Param("tenantId") BigInteger tenantId, @Param("memberId") BigInteger memberId);

    /** 批量读取策略、开通、人群及目录变更标记；不缓存可变事实。 */
    @Select("""
        SELECT 'directory' AS category,CONCAT(COUNT(*),':',COALESCE(SUM(version),0),':',COALESCE(MAX(default_revision_id),0)) AS token FROM iam_directory_policy WHERE tenant_id=#{tenantId}
        UNION ALL SELECT 'field',CONCAT(COUNT(*),':',COALESCE(SUM(version),0),':',COALESCE(MAX(default_revision_id),0)) FROM iam_field_policy WHERE tenant_id=#{tenantId}
        UNION ALL SELECT 'defaults',CONCAT(COUNT(*),':',COALESCE(SUM(revision),0),':',COALESCE(MAX(id),0)) FROM iam_default_policy_revision
        UNION ALL SELECT 'departments',CONCAT(COUNT(*),':',COALESCE(SUM(version),0),':',COALESCE(MAX(id),0)) FROM iam_department WHERE tenant_id=#{tenantId}
        UNION ALL SELECT 'groups',CONCAT(COUNT(*),':',COALESCE(SUM(version),0),':',COALESCE(MAX(id),0)) FROM iam_tenant_group WHERE tenant_id=#{tenantId}
        UNION ALL SELECT 'members',CONCAT(COUNT(*),':',COALESCE(SUM(version),0),':',COALESCE(MAX(id),0)) FROM iam_tenant_member WHERE tenant_id=#{tenantId}
        UNION ALL SELECT 'entitlements',CONCAT(COUNT(*),':',COALESCE(SUM(version),0),':',COALESCE(MAX(id),0)) FROM iam_tenant_app_entitlement WHERE tenant_id=#{tenantId}
        UNION ALL SELECT 'audiences',CONCAT(COUNT(*),':',COALESCE(SUM(version),0),':',COALESCE(MAX(application_id),0)) FROM iam_app_audience WHERE tenant_id=#{tenantId}
        UNION ALL SELECT 'menus',CONCAT(m.id,':',m.version) FROM iam_menu m JOIN iam_application a ON a.id=m.application_id WHERE a.domain='TENANT' ORDER BY category,token
        """)
    List<TenantLiveRows.Dependency> dependencies(@Param("tenantId") BigInteger tenantId);

    /** 锁定组织，治理写入按同一顺序重验身份。 */
    @Select("SELECT owner_member_id,version FROM iam_tenant WHERE id=#{tenantId} AND enabled=TRUE AND deleted_at IS NULL FOR UPDATE")
    TenantLiveRows.Owner lockTenant(@Param("tenantId") BigInteger tenantId);

    /** 读取当前租户可见角色。 */
    @Select("SELECT " + TenantLiveSql.ROLE_COLUMNS + " FROM iam_role_definition d WHERE d.id=#{id} AND " + TenantLiveSql.ROLE_VISIBLE)
    TenantLiveRows.Role role(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id);

    /** 一次加载当前分配涉及的角色头，仍限定租户归属。 */
    @Select("<script>SELECT " + TenantLiveSql.ROLE_COLUMNS + " FROM iam_role_definition d WHERE " + TenantLiveSql.ROLE_VISIBLE + " AND d.id IN <foreach collection='ids' item='id' open='(' close=')' separator=','>#{id}</foreach> ORDER BY d.id</script>")
    List<TenantLiveRows.Role> roleHeads(@Param("tenantId") BigInteger tenantId,@Param("ids") List<BigInteger> ids);
    /** 按可信角色头一次读取完整快照权限。 */
    @Select("<script>SELECT revision_id AS snapshot_id,action_id,scopes FROM iam_role_grant WHERE revision_id IN <foreach collection='ids' item='id' open='(' close=')' separator=','>#{id}</foreach> ORDER BY revision_id,action_id</script>")
    List<TenantLiveRows.SnapshotGrant> snapshotGrants(@Param("ids") List<BigInteger> ids);
    /** 按可信角色头一次读取全部资源参数。 */
    @Select("<script>SELECT revision_id AS snapshot_id,parameter_key AS `key`,binding_kind AS kind,resource_id FROM iam_role_parameter WHERE revision_id IN <foreach collection='ids' item='id' open='(' close=')' separator=','>#{id}</foreach> ORDER BY revision_id,parameter_key</script>")
    List<TenantLiveRows.SnapshotParameter> snapshotParameters(@Param("ids") List<BigInteger> ids);

    /** 锁定一个租户域角色，写入方仍校验共享或私有归属。 */
    @Select("SELECT " + TenantLiveSql.ROLE_COLUMNS + " FROM iam_role_definition d WHERE d.id=#{id} AND d.domain='TENANT' FOR UPDATE")
    TenantLiveRows.Role lockRole(@Param("id") BigInteger id);

    /** 按当前组织可见角色筛选再计数。 */
    @Select("""
        <script>SELECT COUNT(*) FROM iam_role_definition d WHERE
        """ + TenantLiveSql.ROLE_VISIBLE + """
        <if test="visible != null"><choose><when test="visible.isEmpty()"> AND 1=0</when><otherwise> AND d.id IN <foreach collection='visible' item='v' open='(' close=')' separator=','>#{v}</foreach></otherwise></choose></if>
        <if test="shared"> AND d.kind='SHARED'</if>
        <if test="kind != null"> AND d.kind=#{kind}</if>
        <if test="name != null"> AND d.name LIKE #{name}</if>
        <if test="enabled != null"> AND d.enabled=#{enabled}</if></script>
        """)
    long roleCount(@Param("tenantId") BigInteger tenantId, @Param("shared") boolean shared,
                   @Param("kind") RoleKind kind, @Param("name") String name, @Param("enabled") Boolean enabled,@Param("visible") List<BigInteger> visible);

    /** 分页查询角色，不加载全部租户目录。 */
    @Select("<script>SELECT " + TenantLiveSql.ROLE_COLUMNS + " FROM iam_role_definition d WHERE " + TenantLiveSql.ROLE_VISIBLE + """
        <if test="visible != null"><choose><when test="visible.isEmpty()"> AND 1=0</when><otherwise> AND d.id IN <foreach collection='visible' item='v' open='(' close=')' separator=','>#{v}</foreach></otherwise></choose></if>
        <if test="shared"> AND d.kind='SHARED'</if>
        <if test="kind != null"> AND d.kind=#{kind}</if>
        <if test="name != null"> AND d.name LIKE #{name}</if>
        <if test="enabled != null"> AND d.enabled=#{enabled}</if>
        ORDER BY d.id LIMIT #{limit} OFFSET #{offset}</script>
        """)
    List<TenantLiveRows.Role> roles(@Param("tenantId") BigInteger tenantId, @Param("shared") boolean shared,
                                  @Param("kind") RoleKind kind, @Param("name") String name,
                                  @Param("enabled") Boolean enabled, @Param("limit") int limit, @Param("offset") int offset,@Param("visible") List<BigInteger> visible);

    /** 使完整快照成为当前权限，配置锁条件必须成立。 */
    @Update("UPDATE iam_role_definition SET current_revision_id=#{snapshot},version=version+1 WHERE id=#{id} AND version=#{version} AND domain='TENANT'")
    int current(@Param("id") BigInteger id, @Param("snapshot") BigInteger snapshot, @Param("version") BigInteger version);

    /** 写入当前角色基本信息和启停。 */
    @Update("UPDATE iam_role_definition SET name=#{name},description=#{description},group_name=#{groupName},enabled=#{enabled},version=version+1 WHERE id=#{id} AND version=#{version} AND domain='TENANT'")
    int profile(@Param("id") BigInteger id, @Param("version") BigInteger version, @Param("name") String name,
                @Param("description") String description, @Param("groupName") String groupName, @Param("enabled") boolean enabled);

    /** 给快照参数保存稳定资源归属。 */
    @Update("UPDATE iam_role_parameter SET resource_id=#{resourceId} WHERE revision_id=#{snapshot} AND parameter_key=#{key}")
    int parameterResource(@Param("snapshot") BigInteger snapshot, @Param("key") String key, @Param("resourceId") BigInteger resourceId);

    /** 查询完整快照的资源参数。 */
    @Select("SELECT parameter_key AS `key`,binding_kind AS kind,resource_id FROM iam_role_parameter WHERE revision_id=#{snapshot} ORDER BY parameter_key")
    List<TenantLiveRows.Parameter> parameters(@Param("snapshot") BigInteger snapshot);

    /** 已使用过的参数身份不可跨资源或改型，即使当前定义已删除该键。 */
    @Select("SELECT DISTINCT p.parameter_key AS `key`,p.binding_kind AS kind,p.resource_id FROM iam_role_parameter p JOIN iam_role_revision r ON r.id=p.revision_id WHERE r.role_id=#{id} ORDER BY p.parameter_key,p.resource_id")
    List<TenantLiveRows.Parameter> historicParameters(@Param("id") BigInteger id);
    /** 核对同一租户归属内的稳定编码唯一性。 */
    @Select("SELECT COUNT(*) FROM iam_role_definition WHERE domain='TENANT' AND code=#{code} AND tenant_key=COALESCE(#{tenantId},0)")
    long roleCodeExists(@Param("tenantId") BigInteger tenantId,@Param("code") String code);

    /** 批量加载操作实际能力。 */
    @Select("<script>SELECT " + TenantLiveSql.ACTION_COLUMNS + " FROM iam_action a JOIN iam_application app ON app.id=a.application_id JOIN iam_resource r ON r.id=a.resource_id AND r.application_id=app.id WHERE app.domain='TENANT' AND a.id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> ORDER BY a.id</script>")
    List<TenantLiveRows.Action> actions(@Param("ids") List<BigInteger> ids);

    /** 读取拥有者有效开通应用，不受普通人群限制。 */
    @Select("""
        SELECT app.id,app.code,app.name,app.icon,app.sort_order,
               CASE WHEN COUNT(*)>COUNT(e.valid_until) THEN NULL ELSE MIN(e.valid_until) END AS expires_at
          FROM iam_application app JOIN iam_tenant_app_entitlement e ON e.application_id=app.id
         WHERE app.domain='TENANT' AND app.enabled=TRUE AND e.tenant_id=#{tenantId} AND e.enabled=TRUE
           AND (e.valid_from IS NULL OR e.valid_from<=CURRENT_TIMESTAMP(6))
           AND (e.valid_until IS NULL OR e.valid_until>CURRENT_TIMESTAMP(6))
         GROUP BY app.id,app.code,app.name,app.icon,app.sort_order ORDER BY app.sort_order,app.id
        """)
    List<TenantLiveRows.Application> applications(@Param("tenantId") BigInteger tenantId);

    /** 为拥有者批量展开已开通启用操作。 */
    @Select("SELECT " + TenantLiveSql.ACTION_COLUMNS + """
         FROM iam_action a JOIN iam_application app ON app.id=a.application_id
         JOIN iam_resource r ON r.id=a.resource_id AND r.application_id=app.id
        WHERE app.domain='TENANT' AND app.enabled=TRUE AND r.enabled=TRUE AND a.enabled=TRUE
          AND EXISTS(SELECT 1 FROM iam_tenant_app_entitlement e WHERE e.tenant_id=#{tenantId}
            AND e.application_id=app.id AND e.enabled=TRUE
            AND (e.valid_from IS NULL OR e.valid_from<=CURRENT_TIMESTAMP(6))
            AND (e.valid_until IS NULL OR e.valid_until>CURRENT_TIMESTAMP(6))) ORDER BY a.id
        """)
    List<TenantLiveRows.Action> ownerActions(@Param("tenantId") BigInteger tenantId);

    /** 批量展开当前成员有效直接和组分配，委派来源持续检查。 */
    @Select(IamMembershipSql.DEPARTMENT_REACH + IamMembershipSql.MEMBER_GROUPS + "SELECT " + TenantLiveSql.ASSIGNMENT_COLUMNS + TenantLiveSql.ASSIGNMENT_FROM + """
        WHERE ra.domain='TENANT' AND ra.tenant_id=#{tenantId} AND ra.status='ACTIVE' AND d.enabled=TRUE
          AND ((ra.tenant_member_id=#{memberId} AND ra.subject_type='MEMBER')
            OR (ra.subject_type='GROUP' AND ra.tenant_group_id IN (SELECT id FROM member_groups)))
          AND (d.kind='SHARED' AND d.tenant_id IS NULL OR d.kind='TENANT_CUSTOM' AND d.tenant_id=#{tenantId})
          AND ra.valid_from<=CURRENT_TIMESTAMP(6) AND (ra.valid_until IS NULL OR ra.valid_until>CURRENT_TIMESTAMP(6))
          AND (ra.delegation_grant_id IS NULL OR EXISTS(
            SELECT 1 FROM iam_delegation_grant dl WHERE dl.id=ra.delegation_grant_id AND dl.tenant_id=#{tenantId}
              AND dl.domain='TENANT' AND dl.status='ACTIVE' AND dl.tenant_administrator_id!=#{memberId}
              AND EXISTS(SELECT 1 FROM iam_tenant_member administrator JOIN iam_account account ON account.id=administrator.account_id WHERE administrator.id=dl.tenant_administrator_id AND administrator.tenant_id=#{tenantId} AND administrator.status='ACTIVE' AND account.enabled=TRUE AND account.deleted_at IS NULL)
              AND (dl.valid_from IS NULL OR dl.valid_from<=CURRENT_TIMESTAMP(6))
              AND (dl.valid_until IS NULL OR dl.valid_until>CURRENT_TIMESTAMP(6))
              AND (dl.valid_from IS NULL OR ra.valid_from>=dl.valid_from)
              AND (dl.valid_until IS NULL OR ra.valid_until<=dl.valid_until)
              AND ra.valid_until IS NOT NULL
              AND TIMESTAMPDIFF(MICROSECOND,ra.valid_from,ra.valid_until)<=dl.max_assignment_duration_seconds*1000000+FLOOR(dl.max_assignment_duration_nanos/1000)
              AND EXISTS(SELECT 1 FROM iam_delegation_role dr WHERE dr.delegation_id=dl.id AND dr.role_id=ra.role_id)
              AND (EXISTS(SELECT 1 FROM iam_delegation_recipient_member rm WHERE rm.delegation_id=dl.id AND rm.tenant_id=#{tenantId} AND rm.tenant_member_id=#{memberId})
                OR EXISTS(SELECT 1 FROM iam_delegation_recipient_department rd JOIN member_reach mr ON mr.id=rd.department_id WHERE rd.delegation_id=dl.id AND rd.tenant_id=#{tenantId} AND (mr.direct OR rd.include_descendants)))))
        ORDER BY ra.id
        """)
    List<TenantLiveRows.Assignment> effectiveAssignments(@Param("tenantId") BigInteger tenantId, @Param("memberId") BigInteger memberId);

    /** 一次验证整个组展开都在同一来源内，不逐成员往返数据库。 */
    @Select(com.ingot.cloud.iam.persistence.TenantLiveOptionSql.REACH + "SELECT (EXISTS(SELECT 1 FROM group_members gm WHERE gm.group_id=#{groupId}) AND NOT EXISTS(SELECT 1 FROM group_members gm WHERE gm.group_id=#{groupId} AND (gm.member_id=#{q.memberId} OR gm.member_id NOT IN(SELECT id FROM recipients))))")
    boolean groupWithinSource(@Param("q") com.ingot.cloud.iam.persistence.TenantLiveOptionSql.Query query,@Param("groupId") BigInteger groupId);

    /** 读取一条组织委派。 */
    @Select("SELECT " + TenantLiveSql.DELEGATION_COLUMNS + " FROM iam_delegation_grant dl LEFT JOIN iam_tenant_member m ON m.tenant_id=dl.tenant_id AND m.id=dl.tenant_administrator_id WHERE dl.domain='TENANT' AND dl.tenant_id=#{tenantId} AND dl.id=#{id}")
    TenantLiveRows.Delegation delegation(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id);

    /** 锁定组织委派并在写入时重验。 */
    @Select("SELECT " + TenantLiveSql.DELEGATION_COLUMNS + " FROM iam_delegation_grant dl LEFT JOIN iam_tenant_member m ON m.tenant_id=dl.tenant_id AND m.id=dl.tenant_administrator_id WHERE dl.domain='TENANT' AND dl.tenant_id=#{tenantId} AND dl.id=#{id} FOR UPDATE")
    TenantLiveRows.Delegation lockDelegation(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id);

    /** 仅返回本人当前有效来源。 */
    @Select("SELECT " + TenantLiveSql.DELEGATION_COLUMNS + " FROM iam_delegation_grant dl JOIN iam_tenant_member m ON m.tenant_id=dl.tenant_id AND m.id=dl.tenant_administrator_id WHERE dl.domain='TENANT' AND dl.tenant_id=#{tenantId} AND dl.tenant_administrator_id=#{memberId} AND m.status='ACTIVE' AND dl.status='ACTIVE' AND (dl.valid_from IS NULL OR dl.valid_from<=CURRENT_TIMESTAMP(6)) AND (dl.valid_until IS NULL OR dl.valid_until>CURRENT_TIMESTAMP(6)) ORDER BY dl.id")
    List<TenantLiveRows.Delegation> mine(@Param("tenantId") BigInteger tenantId, @Param("memberId") BigInteger memberId);

    /** 读取委派的角色身份白名单。 */
    @Select("SELECT role_id FROM iam_delegation_role WHERE delegation_id=#{id} ORDER BY role_id")
    List<BigInteger> delegatedRoles(@Param("id") BigInteger id);

    /** 读取资源上限，不按操作重复存储。 */
    @Select("SELECT resource_id,scopes,scope_bindings FROM iam_delegation_resource_ceiling WHERE delegation_id=#{id} ORDER BY resource_id")
    List<TenantLiveRows.Ceiling> ceilings(@Param("id") BigInteger id);

    /** 检查当前接收成员在完整委派人群内。 */
    @Select(IamMembershipSql.DEPARTMENT_REACH + """
        SELECT COUNT(*) FROM iam_delegation_grant dl WHERE dl.id=#{id} AND dl.tenant_id=#{tenantId}
          AND dl.tenant_administrator_id!=#{memberId}
          AND (EXISTS(SELECT 1 FROM iam_delegation_recipient_member rm WHERE rm.delegation_id=dl.id AND rm.tenant_id=#{tenantId} AND rm.tenant_member_id=#{memberId})
            OR EXISTS(SELECT 1 FROM iam_delegation_recipient_department rd JOIN member_reach mr ON mr.id=rd.department_id WHERE rd.delegation_id=dl.id AND rd.tenant_id=#{tenantId} AND (mr.direct OR rd.include_descendants)))
        """)
    long recipient(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id, @Param("memberId") BigInteger memberId);

    /** 检查对象属于当前组织且成员有效。 */
    @Select("SELECT COUNT(*) FROM iam_tenant_member WHERE tenant_id=#{tenantId} AND id=#{id} AND status='ACTIVE'")
    long member(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id);

    /** 查询角色真实授权引用数量，供删除及影响预览。 */
    @Select("SELECT COUNT(*) FROM iam_role_assignment WHERE domain='TENANT' AND role_id=#{id}")
    long roleAssignments(@Param("id") BigInteger id);

    /** 查询角色委派引用数量。 */
    @Select("SELECT COUNT(*) FROM iam_delegation_role WHERE role_id=#{id}")
    long roleDelegations(@Param("id") BigInteger id);

    /** 在角色行锁内分配下一个内部快照序号。 */
    @Select("SELECT COALESCE(MAX(revision),0)+1 FROM iam_role_revision WHERE role_id=#{id}")
    BigInteger nextRevision(@Param("id") BigInteger id);

    /** 删除未引用角色之前解除本角色当前指针。 */
    @Update("UPDATE iam_role_definition SET current_revision_id=NULL WHERE id=#{id} AND domain='TENANT'")
    int clearCurrent(@Param("id") BigInteger id);

    /** 分页操作候选，参数在服务器目录中确定。 */
    @Select("<script>SELECT " + TenantLiveSql.ACTION_COLUMNS + """
         FROM iam_action a JOIN iam_application app ON app.id=a.application_id
         JOIN iam_resource r ON r.id=a.resource_id AND r.application_id=app.id
        WHERE app.domain='TENANT' AND app.id=#{applicationId} AND app.enabled=TRUE AND r.enabled=TRUE AND a.enabled=TRUE
          AND EXISTS(SELECT 1 FROM iam_tenant_app_entitlement e WHERE e.tenant_id=#{tenantId} AND e.application_id=app.id AND e.enabled=TRUE AND (e.valid_from IS NULL OR e.valid_from&lt;=CURRENT_TIMESTAMP(6)) AND (e.valid_until IS NULL OR e.valid_until>CURRENT_TIMESTAMP(6)))
          <if test="keyword != null"> AND a.name LIKE #{keyword}</if>
          <if test="ids != null and !ids.isEmpty()"> AND a.id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></if>
        ORDER BY r.id,a.id LIMIT #{limit} OFFSET #{offset}</script>
        """)
    List<TenantLiveRows.Action> catalog(@Param("tenantId") BigInteger tenantId, @Param("applicationId") BigInteger applicationId,
                                      @Param("keyword") String keyword, @Param("ids") List<BigInteger> ids,
                                      @Param("limit") int limit, @Param("offset") int offset);

    /** 在相同目录和筛选边界内计数操作候选。 */
    @Select("""
        <script>SELECT COUNT(*) FROM iam_action a JOIN iam_application app ON app.id=a.application_id
         JOIN iam_resource r ON r.id=a.resource_id AND r.application_id=app.id
        WHERE app.domain='TENANT' AND app.id=#{applicationId} AND app.enabled=TRUE AND r.enabled=TRUE AND a.enabled=TRUE
          AND EXISTS(SELECT 1 FROM iam_tenant_app_entitlement e WHERE e.tenant_id=#{tenantId} AND e.application_id=app.id AND e.enabled=TRUE AND (e.valid_from IS NULL OR e.valid_from&lt;=CURRENT_TIMESTAMP(6)) AND (e.valid_until IS NULL OR e.valid_until>CURRENT_TIMESTAMP(6)))
          <if test="keyword != null"> AND a.name LIKE #{keyword}</if>
          <if test="ids != null and !ids.isEmpty()"> AND a.id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></if></script>
        """)
    long catalogCount(@Param("tenantId") BigInteger tenantId, @Param("applicationId") BigInteger applicationId,
                      @Param("keyword") String keyword, @Param("ids") List<BigInteger> ids);
    /** 读取一条同组织当前角色分配。 */
    @Select("SELECT " + TenantLiveSql.ASSIGNMENT_COLUMNS + TenantLiveSql.ASSIGNMENT_FROM + " WHERE ra.domain='TENANT' AND ra.tenant_id=#{tenantId} AND ra.id=#{id}")
    TenantLiveRows.Assignment assignment(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id);

    /** 锁定一条同组织分配。 */
    @Select("SELECT " + TenantLiveSql.ASSIGNMENT_COLUMNS + TenantLiveSql.ASSIGNMENT_FROM + " WHERE ra.domain='TENANT' AND ra.tenant_id=#{tenantId} AND ra.id=#{id} FOR UPDATE")
    TenantLiveRows.Assignment lockAssignment(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id);

    /** 当前分配列表先过滤可见ID、来源和搜索，再分页。 */
    @Select("<script>SELECT " + TenantLiveSql.ASSIGNMENT_COLUMNS + TenantLiveSql.ASSIGNMENT_FROM + TenantLiveSql.ASSIGNMENT_FILTER + " ORDER BY ra.id LIMIT #{limit} OFFSET #{offset}</script>")
    List<TenantLiveRows.Assignment> assignments(@Param("tenantId") BigInteger tenantId, @Param("visible") List<BigInteger> visible,
        @Param("sources") List<BigInteger> sources, @Param("type") SubjectType type, @Param("keyword") String keyword,
        @Param("limit") int limit, @Param("offset") int offset);

    /** 与列表使用同一可见条件计数。 */
    @Select("<script>SELECT COUNT(*) " + TenantLiveSql.ASSIGNMENT_FROM + TenantLiveSql.ASSIGNMENT_FILTER + "</script>")
    long assignmentsCount(@Param("tenantId") BigInteger tenantId, @Param("visible") List<BigInteger> visible,
        @Param("sources") List<BigInteger> sources, @Param("type") SubjectType type, @Param("keyword") String keyword);

    /** 保存租户当前角色引用，平台版本字段不参与执行。 */
    @Insert("""
        INSERT INTO iam_role_assignment(id,domain,tenant_id,subject_type,tenant_member_id,tenant_group_id,
            role_id,role_kind,role_tenant_key,scope_bindings,delegation_grant_id,valid_from,valid_until,status,source,version)
        VALUES(#{row.id},'TENANT',#{row.tenantId},#{row.subjectType},#{row.memberId},#{row.groupId},#{row.roleId},#{row.roleKind},
            #{roleTenantKey},#{row.scopeBindings},#{row.delegationGrantId},#{row.validFrom},#{row.validUntil},'ACTIVE','MANUAL',0)
        """)
    int insertAssignment(@Param("row") TenantLiveRows.Assignment row, @Param("roleTenantKey") BigInteger roleTenantKey);

    /** 并发锁下更新完整分配。 */
    @Update("""
        UPDATE iam_role_assignment SET subject_type=#{row.subjectType},tenant_member_id=#{row.memberId},tenant_group_id=#{row.groupId},
            role_id=#{row.roleId},role_kind=#{row.roleKind},role_tenant_key=#{roleTenantKey},scope_bindings=#{row.scopeBindings},
            delegation_grant_id=#{row.delegationGrantId},valid_from=#{row.validFrom},valid_until=#{row.validUntil},version=version+1
         WHERE domain='TENANT' AND tenant_id=#{row.tenantId} AND id=#{row.id} AND version=#{row.version} AND status='ACTIVE'
        """)
    int updateAssignment(@Param("row") TenantLiveRows.Assignment row, @Param("roleTenantKey") BigInteger roleTenantKey);

    /** 撤销保留分配历史。 */
    @Update("UPDATE iam_role_assignment SET status='REVOKED',version=version+1 WHERE tenant_id=#{tenantId} AND domain='TENANT' AND id=#{id} AND version=#{version}")
    int revokeAssignment(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id, @Param("version") BigInteger version);

    /** 读取组实际成员（显式和部门归属去重）。 */
    @Select(IamMembershipSql.GROUP_MEMBERSHIP)
    List<BigInteger> groupMembers(@Param("tenantId") BigInteger tenantId, @Param("groupId") BigInteger groupId);

    /** 核对组织组归属。 */
    @Select("SELECT COUNT(*) FROM iam_tenant_group WHERE tenant_id=#{tenantId} AND id=#{id}")
    long groupExists(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id);

    /** 核对部门归属。 */
    @Select("SELECT COUNT(*) FROM iam_department WHERE tenant_id=#{tenantId} AND id=#{id}")
    long departmentExists(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id);

    /** 读取完整接收人群的成员部分。 */
    @Select("SELECT tenant_member_id FROM iam_delegation_recipient_member WHERE domain='TENANT' AND tenant_id=#{tenantId} AND delegation_id=#{id} ORDER BY tenant_member_id")
    List<BigInteger> recipientMembers(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id);

    /** 读取完整接收人群的部门部分。 */
    @Select("SELECT CAST(department_id AS CHAR) AS id,include_descendants FROM iam_delegation_recipient_department WHERE tenant_id=#{tenantId} AND delegation_id=#{id} ORDER BY department_id")
    List<DepartmentSelection> recipientDepartments(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id);

    /** 接收人群写入在同一来源事务内。 */
    @Insert("INSERT INTO iam_delegation_recipient_member(delegation_id,domain,tenant_id,tenant_member_id) VALUES(#{id},'TENANT',#{tenantId},#{memberId})")
    int recipientMember(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id, @Param("memberId") BigInteger memberId);

    /** 接收部门写入。 */
    @Insert("INSERT INTO iam_delegation_recipient_department(delegation_id,tenant_id,department_id,include_descendants) VALUES(#{id},#{tenantId},#{departmentId},#{descendants})")
    int recipientDepartment(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id, @Param("departmentId") BigInteger departmentId, @Param("descendants") boolean descendants);

    /** 保存资源上限。 */
    @Insert("INSERT INTO iam_delegation_resource_ceiling(delegation_id,resource_id,scopes,scope_bindings) VALUES(#{id},#{resourceId},#{scopes},#{bindings})")
    int ceiling(@Param("id") BigInteger id, @Param("resourceId") BigInteger resourceId, @Param("scopes") String scopes, @Param("bindings") String bindings);

    /** 保存角色身份白名单。 */
    @Insert("INSERT INTO iam_delegation_role(delegation_id,tenant_id,domain,role_id,role_kind,role_tenant_key) VALUES(#{id},#{tenantId},'TENANT',#{role.id},#{role.kind},#{roleTenantKey})")
    int allowedRole(@Param("id") BigInteger id, @Param("tenantId") BigInteger tenantId, @Param("role") TenantLiveRows.Role role, @Param("roleTenantKey") BigInteger roleTenantKey);

    /** 替换委派前删除原有资源上限。 */
    @Delete("DELETE FROM iam_delegation_resource_ceiling WHERE delegation_id=#{id}")
    int clearCeilings(@Param("id") BigInteger id);
    /** 替换委派前删除原有白名单。 */
    @Delete("DELETE FROM iam_delegation_role WHERE delegation_id=#{id}")
    int clearAllowedRoles(@Param("id") BigInteger id);
    /** 替换同组织接收成员。 */
    @Delete("DELETE FROM iam_delegation_recipient_member WHERE domain='TENANT' AND tenant_id=#{tenantId} AND delegation_id=#{id}")
    int clearRecipientMembers(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id);
    /** 替换同组织接收部门。 */
    @Delete("DELETE FROM iam_delegation_recipient_department WHERE tenant_id=#{tenantId} AND delegation_id=#{id}")
    int clearRecipientDepartments(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id);

    /** 创建有限期限组织委派。 */
    @Insert("""
        INSERT INTO iam_delegation_grant(id,domain,tenant_id,tenant_administrator_id,valid_from,valid_until,
            assignment_duration_mode,max_assignment_duration_seconds,max_assignment_duration_nanos,status,version)
        VALUES(#{row.id},'TENANT',#{row.tenantId},#{row.administratorMemberId},#{row.validFrom},#{row.validUntil},'LIMITED',#{row.durationSeconds},#{row.durationNanos},'ACTIVE',0)
        """)
    int insertDelegation(@Param("row") TenantLiveRows.Delegation row);

    /** 并发锁下替换委派期限及管理员。 */
    @Update("""
        UPDATE iam_delegation_grant SET tenant_administrator_id=#{row.administratorMemberId},valid_from=#{row.validFrom},
            valid_until=#{row.validUntil},assignment_duration_mode='LIMITED',max_assignment_duration_seconds=#{row.durationSeconds},
            max_assignment_duration_nanos=#{row.durationNanos},version=version+1
         WHERE domain='TENANT' AND tenant_id=#{row.tenantId} AND id=#{row.id} AND version=#{row.version} AND status='ACTIVE'
        """)
    int updateDelegation(@Param("row") TenantLiveRows.Delegation row);

    /** 撤销同组织委派。 */
    @Update("UPDATE iam_delegation_grant SET status='REVOKED',version=version+1 WHERE domain='TENANT' AND tenant_id=#{tenantId} AND id=#{id} AND version=#{version}")
    int revokeDelegation(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id, @Param("version") BigInteger version);

    /** 过滤治理对象范围后分页委派。 */
    @Select("<script>SELECT " + TenantLiveSql.DELEGATION_COLUMNS + " FROM iam_delegation_grant dl LEFT JOIN iam_tenant_member m ON m.id=dl.tenant_administrator_id AND m.tenant_id=dl.tenant_id " + TenantLiveSql.DELEGATION_FILTER + " ORDER BY dl.id LIMIT #{limit} OFFSET #{offset}</script>")
    List<TenantLiveRows.Delegation> delegations(@Param("tenantId") BigInteger tenantId, @Param("visible") List<BigInteger> visible, @Param("keyword") String keyword, @Param("limit") int limit, @Param("offset") int offset);

    /** 相同范围计数委派。 */
    @Select("<script>SELECT COUNT(*) FROM iam_delegation_grant dl LEFT JOIN iam_tenant_member m ON m.id=dl.tenant_administrator_id AND m.tenant_id=dl.tenant_id " + TenantLiveSql.DELEGATION_FILTER + "</script>")
    long delegationsCount(@Param("tenantId") BigInteger tenantId, @Param("visible") List<BigInteger> visible, @Param("keyword") String keyword);

    /** 受限角色、成员、组和资源候选。 */
    @SelectProvider(type=com.ingot.cloud.iam.persistence.TenantLiveOptionSql.class,method="page")
    List<com.ingot.cloud.iam.persistence.TenantLiveOptionSql.Option> options(@Param("q") com.ingot.cloud.iam.persistence.TenantLiveOptionSql.Query query);
    /** 在同一限制内计数选择器。 */
    @SelectProvider(type=com.ingot.cloud.iam.persistence.TenantLiveOptionSql.class,method="count")
    long optionsCount(@Param("q") com.ingot.cloud.iam.persistence.TenantLiveOptionSql.Query query);

    /** 查询租户域真实资源编码。 */
    @Select("SELECT r.id,app.code AS application_code,r.code AS resource_code,r.name AS resource_name FROM iam_resource r JOIN iam_application app ON app.id=r.application_id WHERE r.id=#{id} AND app.domain='TENANT' AND app.enabled=TRUE AND r.enabled=TRUE")
    TenantScopeCandidateMapper.ActionResource resource(@Param("id") BigInteger id);

    /** 验证全部接收成员和账号当前有效且属于本组织。 */
    @Select("<script>SELECT COUNT(*) FROM iam_tenant_member m JOIN iam_account a ON a.id=m.account_id WHERE m.tenant_id=#{tenantId} AND m.status='ACTIVE' AND a.enabled=TRUE AND a.deleted_at IS NULL AND m.id IN <foreach collection='ids' item='id' open='(' close=')' separator=','>#{id}</foreach></script>")
    long members(@Param("tenantId") BigInteger tenantId,@Param("ids") List<BigInteger> ids);

    /** 只查询本组织当前可用角色的指定资源参数，不拿大页模拟全集。 */
    @Select("SELECT DISTINCT p.parameter_key AS `key`,p.binding_kind AS kind,p.resource_id FROM iam_role_parameter p JOIN iam_role_definition d ON d.current_revision_id=p.revision_id WHERE " + TenantLiveSql.ROLE_VISIBLE + " AND d.enabled=TRUE AND p.resource_id=#{resourceId} AND p.parameter_key=#{key}")
    List<TenantLiveRows.Parameter> currentParameters(@Param("tenantId") BigInteger tenantId,@Param("resourceId") BigInteger resourceId,@Param("key") String key);
    /** 读取实际资源支持范围能力。 */
    @Select("SELECT r.scope_capabilities FROM iam_resource r JOIN iam_application app ON app.id=r.application_id WHERE r.id=#{id} AND r.enabled=TRUE AND app.enabled=TRUE AND app.domain='TENANT'")
    String resourceScopeKinds(@Param("id") BigInteger id);

    /** 资源必须属于当前组织有效开通应用。 */
    @Select("""
        SELECT COUNT(*) FROM iam_resource r JOIN iam_application app ON app.id=r.application_id
         WHERE r.id=#{id} AND r.enabled=TRUE AND app.enabled=TRUE AND app.domain='TENANT'
          AND EXISTS(SELECT 1 FROM iam_tenant_app_entitlement e WHERE e.tenant_id=#{tenantId} AND e.application_id=app.id AND e.enabled=TRUE
           AND (e.valid_from IS NULL OR e.valid_from<=CURRENT_TIMESTAMP(6)) AND (e.valid_until IS NULL OR e.valid_until>CURRENT_TIMESTAMP(6)))
        """)
    long openResource(@Param("tenantId") BigInteger tenantId,@Param("id") BigInteger id);

    /** 部门上限对应的实际成员，不读取其他组织对象。 */
    @Select("<script>SELECT DISTINCT m.id FROM iam_tenant_member m JOIN iam_member_department md ON md.member_id=m.id AND md.tenant_id=m.tenant_id WHERE m.tenant_id=#{tenantId} AND m.status='ACTIVE' AND md.department_id IN <foreach collection='ids' item='id' open='(' close=')' separator=','>#{id}</foreach></script>")
    List<BigInteger> membersInDepartments(@Param("tenantId") BigInteger tenantId,@Param("ids") List<BigInteger> ids);
    /** 批量读取成员任职，用于相对SELF/本部门的委派范围。 */
    @Select("<script>SELECT DISTINCT department_id FROM iam_member_department WHERE tenant_id=#{tenantId} AND member_id IN <foreach collection='ids' item='id' open='(' close=')' separator=','>#{id}</foreach></script>")
    List<BigInteger> memberDepartments(@Param("tenantId") BigInteger tenantId,@Param("ids") List<BigInteger> ids);
    /** 来源接收人集合只用于资源上限编译，不向客户端披露全集。 */
    @Select(com.ingot.cloud.iam.persistence.TenantLiveOptionSql.REACH + " SELECT m.id FROM iam_tenant_member m WHERE m.tenant_id=#{q.tenantId} AND m.status='ACTIVE' AND m.id!=#{q.memberId} AND m.id IN(SELECT id FROM recipients)")
    List<BigInteger> scopeRecipients(@Param("q") com.ingot.cloud.iam.persistence.TenantLiveOptionSql.Query query);

}
