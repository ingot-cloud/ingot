package com.ingot.cloud.iam.persistence;

/**
 * <p>集中租户当前角色查询的列和边界，所有动态值使用具名参数。</p>
 * @author jy
 * @since 1.0.0
 */
public final class TenantLiveSql {
    /** 当前角色列顺序与只读投影一致。 */
    public static final String ROLE_COLUMNS = "d.id,d.tenant_id,d.kind,d.code,d.name,d.description,d.group_name,d.enabled,d.version,d.current_revision_id";
    /** 组织可见的共享或私有角色。 */
    public static final String ROLE_VISIBLE = "d.domain='TENANT' AND ((d.kind='SHARED' AND d.tenant_id IS NULL) OR (d.kind='TENANT_CUSTOM' AND d.tenant_id=#{tenantId}))";
    /** 操作目录真实能力。 */
    public static final String ACTION_COLUMNS = "a.id AS action_id,a.code AS action_code,a.name AS action_name,app.id AS application_id,app.name AS application_name,r.id AS resource_id,r.name AS resource_name,(a.enabled AND app.enabled AND r.enabled) AS enabled,r.scope_capabilities,a.version AS action_version,app.version AS application_version,r.version AS resource_version";
    /** 当前分配及主体展示。 */
    public static final String ASSIGNMENT_COLUMNS = "ra.id,ra.tenant_id,ra.subject_type,ra.tenant_member_id AS member_id,ra.tenant_group_id AS group_id,ra.role_id,ra.role_kind,d.current_revision_id,ra.scope_bindings,ra.delegation_grant_id,ra.valid_from,ra.valid_until,ra.status,ra.source,ra.version,ra.created_at,COALESCE(m.display_name,g.name) AS subject_name,d.name AS role_name";
    /** 分配真实关联表。 */
    public static final String ASSIGNMENT_FROM = " FROM iam_role_assignment ra JOIN iam_role_definition d ON d.id=ra.role_id LEFT JOIN iam_tenant_member m ON m.tenant_id=ra.tenant_id AND m.id=ra.tenant_member_id LEFT JOIN iam_tenant_group g ON g.tenant_id=ra.tenant_id AND g.id=ra.tenant_group_id ";
    /** 委派列。 */
    public static final String DELEGATION_COLUMNS = "dl.id,dl.tenant_id,dl.tenant_administrator_id AS administrator_member_id,dl.valid_from,dl.valid_until,dl.max_assignment_duration_seconds AS duration_seconds,dl.max_assignment_duration_nanos AS duration_nanos,dl.status,dl.version,m.display_name AS administrator_name";

    /** 当前分配可见范围和来源条件；空集合恒拒绝。 */
    public static final String ASSIGNMENT_FILTER = """
        WHERE ra.domain='TENANT' AND ra.tenant_id=#{tenantId}
        <if test="visible != null"><choose><when test="visible.isEmpty()"> AND 1=0</when><otherwise> AND ra.id IN
         <foreach collection='visible' item='v' open='(' close=')' separator=','>#{v}</foreach></otherwise></choose></if>
        <if test="sources != null"><choose><when test="sources.isEmpty()"> AND 1=0</when><otherwise> AND ra.delegation_grant_id IN
         <foreach collection='sources' item='v' open='(' close=')' separator=','>#{v}</foreach></otherwise></choose></if>
        <if test="type != null"> AND ra.subject_type=#{type}</if>
        <if test="keyword != null"> AND (m.display_name LIKE #{keyword} ESCAPE '!' OR g.name LIKE #{keyword} ESCAPE '!' OR d.name LIKE #{keyword} ESCAPE '!')</if>
        """;
    /** 委派治理对象可见范围。 */
    public static final String DELEGATION_FILTER = """
        WHERE dl.domain='TENANT' AND dl.tenant_id=#{tenantId}
        <if test="visible != null"><choose><when test="visible.isEmpty()"> AND 1=0</when><otherwise> AND dl.id IN
         <foreach collection='visible' item='v' open='(' close=')' separator=','>#{v}</foreach></otherwise></choose></if>
        <if test="keyword != null"> AND m.display_name LIKE #{keyword} ESCAPE '!'</if>
        """;
    private TenantLiveSql() { }
}
