package com.ingot.cloud.iam.infrastructure.persistence;

/**
 * <p>按当前租户和可见范围读取分配分页，批量验证页内委派状态和接收范围。</p>
 * @author jy
 * @since 1.0.0
 */
public final class TenantAssignmentReadSql {
    private TenantAssignmentReadSql() { }

    // 全范围页使用覆盖索引顺扫；对象及委派范围可选择各自的索引。
    private static final String SCAN_ASSIGNMENTS = """
            SELECT <if test="visible == null and sources == null">
                /*+ INDEX(ra idx_iam_assignment_tenant_list) NO_SKIP_SCAN(ra) */
            </if>
            """;

    // 复合外键保证当前角色存在，旧撤销历史可以没有 role_id。
    private static final String LIST_FILTER = """
            WHERE ra.domain='TENANT' AND ra.tenant_id=#{tenantId} AND ra.role_id IS NOT NULL
            <if test="visible != null"><choose><when test="visible.isEmpty()"> AND ra.id IN (NULL)</when><otherwise> AND ra.id IN
             <foreach collection="visible" item="id" open="(" close=")" separator=",">#{id}</foreach></otherwise></choose></if>
            <if test="sources != null"><choose><when test="sources.isEmpty()"> AND ra.delegation_grant_id IN (NULL)</when><otherwise> AND ra.delegation_grant_id IN
             <foreach collection="sources" item="id" open="(" close=")" separator=",">#{id}</foreach></otherwise></choose></if>
            <if test="type != null"> AND ra.subject_type=#{type}</if>
            <if test="keyword != null"> AND (
                ra.tenant_member_id IN (SELECT id FROM iam_tenant_member
                    WHERE tenant_id=#{tenantId} AND display_name LIKE #{keyword} ESCAPE '!')
                OR ra.tenant_group_id IN (SELECT id FROM iam_tenant_group
                    WHERE tenant_id=#{tenantId} AND name LIKE #{keyword} ESCAPE '!')
                OR ra.role_id IN (SELECT id FROM iam_role_definition
                    WHERE domain='TENANT' AND tenant_key IN (0,#{tenantId}) AND name LIKE #{keyword} ESCAPE '!')
            )</if>
            """;

    /** 先读取可见分配 ID，再读取页内详情和显示名称。 */
    public static final String PAGE = """
            <script>SELECT /*+ JOIN_PREFIX(page_ids, ra) */
            """ + TenantAdministrationSql.ASSIGNMENT_COLUMNS + " FROM (" + SCAN_ASSIGNMENTS + "ra.id"
            + TenantAdministrationSql.ASSIGNMENT_BASE + LIST_FILTER + """
            ORDER BY ra.id LIMIT #{limit} OFFSET #{offset}) page_ids
            JOIN iam_role_assignment ra ON ra.id=page_ids.id
            """ + TenantAdministrationSql.ASSIGNMENT_JOINS + " ORDER BY page_ids.id</script>";

    /** 按取页时的同一可见范围及搜索条件计数。 */
    public static final String COUNT = "<script>" + SCAN_ASSIGNMENTS + "COUNT(*)"
            + TenantAdministrationSql.ASSIGNMENT_BASE + LIST_FILTER + "</script>";

    /** 当前页仍符合委派时间、角色与接收人约束的分配。 */
    public static final String ELIGIBLE_DELEGATED = """
            <script>
            WITH RECURSIVE eligible (id, version, subject_type, member_id, group_id, source_id, administrator_id) AS (
                SELECT ra.id, ra.version, ra.subject_type, ra.tenant_member_id, ra.tenant_group_id, dl.id, dl.tenant_administrator_id
                  FROM iam_role_assignment ra
                  JOIN iam_delegation_grant dl
                    ON dl.domain=ra.domain AND dl.tenant_id=ra.tenant_id AND dl.id=ra.delegation_grant_id
                  JOIN iam_tenant_member administrator
                    ON administrator.tenant_id=dl.tenant_id AND administrator.id=dl.tenant_administrator_id
                   AND administrator.status='ACTIVE'
                  JOIN iam_delegation_role allowed ON allowed.delegation_id=dl.id AND allowed.role_id=ra.role_id
                 WHERE ra.domain='TENANT' AND ra.tenant_id=#{tenantId} AND dl.status='ACTIVE'
                   AND ra.id IN <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
                   AND (dl.valid_from IS NULL OR (dl.valid_from&lt;=#{now} AND dl.valid_from&lt;=ra.valid_from))
                   AND (dl.valid_until IS NULL OR (dl.valid_until>#{now} AND dl.valid_until>=ra.valid_until))
                   AND ra.valid_until IS NOT NULL
                   AND TIMESTAMPDIFF(MICROSECOND,ra.valid_from,ra.valid_until)
                       &lt;= dl.max_assignment_duration_seconds*1000000+FLOOR(dl.max_assignment_duration_nanos/1000)
            ), recipient_reach (source_id, department_id, deep) AS (
                SELECT rd.delegation_id, rd.department_id, rd.include_descendants
                  FROM iam_delegation_recipient_department rd
                 WHERE rd.tenant_id=#{tenantId} AND rd.delegation_id IN (SELECT source_id FROM eligible)
                UNION SELECT r.source_id,d.id,TRUE FROM recipient_reach r
                  JOIN iam_department d ON d.tenant_id=#{tenantId} AND d.parent_id=r.department_id WHERE r.deep
            ), recipients (source_id, member_id) AS (
                SELECT rm.delegation_id,rm.tenant_member_id FROM iam_delegation_recipient_member rm
                 WHERE rm.tenant_id=#{tenantId} AND rm.domain='TENANT'
                   AND rm.delegation_id IN (SELECT source_id FROM eligible)
                UNION SELECT r.source_id,md.member_id FROM recipient_reach r
                  JOIN iam_member_department md ON md.tenant_id=#{tenantId} AND md.department_id=r.department_id
            ), group_reach (group_id, department_id, deep) AS (
                SELECT gd.group_id,gd.department_id,gd.include_descendants FROM iam_tenant_group_department gd
                 WHERE gd.tenant_id=#{tenantId} AND gd.group_id IN (SELECT group_id FROM eligible)
                UNION SELECT r.group_id,d.id,TRUE FROM group_reach r
                  JOIN iam_department d ON d.tenant_id=#{tenantId} AND d.parent_id=r.department_id WHERE r.deep
            ), group_members (group_id, member_id) AS (
                SELECT gm.group_id,gm.member_id FROM iam_tenant_group_member gm
                 WHERE gm.tenant_id=#{tenantId} AND gm.group_id IN (SELECT group_id FROM eligible)
                UNION SELECT r.group_id,md.member_id FROM group_reach r
                  JOIN iam_member_department md ON md.tenant_id=#{tenantId} AND md.department_id=r.department_id
            )
            SELECT e.id,e.version FROM eligible e WHERE
                (e.subject_type='MEMBER' AND e.member_id!=e.administrator_id
                 AND EXISTS (SELECT 1 FROM recipients r WHERE r.source_id=e.source_id AND r.member_id=e.member_id))
                OR (e.subject_type='GROUP'
                    AND EXISTS (SELECT 1 FROM group_members gm WHERE gm.group_id=e.group_id)
                    AND NOT EXISTS (
                        SELECT 1 FROM group_members gm WHERE gm.group_id=e.group_id
                           AND (gm.member_id=e.administrator_id OR NOT EXISTS (
                               SELECT 1 FROM recipients r WHERE r.source_id=e.source_id AND r.member_id=gm.member_id))))
            </script>
            """;

    /** 当前页委派来源持有的资源上限标识。 */
    public static final String CEILING_RESOURCES = """
            <script>
            SELECT c.delegation_id,c.resource_id FROM iam_delegation_resource_ceiling c
              JOIN iam_delegation_grant d ON d.id=c.delegation_id
             WHERE d.domain='TENANT' AND d.tenant_id=#{tenantId}
               AND c.delegation_id IN <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """;
}
