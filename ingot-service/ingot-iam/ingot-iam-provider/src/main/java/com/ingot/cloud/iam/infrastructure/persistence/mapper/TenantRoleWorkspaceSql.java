package com.ingot.cloud.iam.infrastructure.persistence.mapper;

import java.math.BigInteger;
import java.util.List;
import com.ingot.cloud.iam.domain.assignment.SubjectType;

/**
 * <p>租户角色工作区在 SQL 内展开当前有效分配和组继承，再去重分页。</p>
 *
 * <p>不接受业务版本。组继承同时包含显式成员和按部门展开的成员。
 * 委派来源在此校验状态、时间窗、时长和角色白名单。接收人是否仍落在委派人群内，
 * 以及管理员账号是否仍然有效，由分配详情计算有效状态，不在本聚合里重复展开。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class TenantRoleWorkspaceSql {
    private TenantRoleWorkspaceSql() { }

    /**
     * 生成去重后的成员或用户组页。
     *
     * @param params 可信查询参数
     * @return 绑定参数 SQL
     */
    public static String page(java.util.Map<String, Object> params) {
        Query query = (Query) params.get("q");
        return "<script>" + people(query) + "SELECT x.id,x.name,JSON_ARRAYAGG(x.subject_type) AS source_types,"
                + "COUNT(DISTINCT x.assignment_id) AS source_count FROM (" + relations(query) + ") x "
                + "GROUP BY x.id,x.name ORDER BY x.id LIMIT #{q.size} OFFSET #{q.offset}</script>";
    }

    /**
     * 按相同关系边界统计去重主体。
     *
     * @param params 可信查询参数
     * @return 绑定参数 SQL
     */
    public static String count(java.util.Map<String, Object> params) {
        Query query = (Query) params.get("q");
        return "<script>" + people(query) + "SELECT COUNT(DISTINCT x.id) FROM (" + relations(query) + ") x</script>";
    }

    /**
     * 分页读取一名成员的真实有效来源分配。
     *
     * @param params 可信查询参数
     * @return 绑定参数 SQL
     */
    public static String sources(java.util.Map<String, Object> params) {
        Query query = (Query) params.get("q");
        return "<script>" + people(query) + "SELECT " + com.ingot.cloud.iam.infrastructure.persistence.TenantAdministrationSql.ASSIGNMENT_COLUMNS
                + com.ingot.cloud.iam.infrastructure.persistence.TenantAdministrationSql.ASSIGNMENT_FROM
                + " WHERE ra.id IN (SELECT x.assignment_id FROM (" + relations(query) + ") x) "
                + "ORDER BY ra.id LIMIT #{q.size} OFFSET #{q.offset}</script>";
    }

    /**
     * 统计一名成员的可见有效来源数。
     *
     * @param params 可信查询参数
     * @return 绑定参数 SQL
     */
    public static String sourceCount(java.util.Map<String, Object> params) {
        Query query = (Query) params.get("q");
        return "<script>" + people(query) + "SELECT COUNT(DISTINCT x.assignment_id) FROM ("
                + relations(query) + ") x</script>";
    }

    private static String people(Query query) {
        if (query.subjectType() == SubjectType.GROUP && query.memberId() == null) {
            return "";
        }
        return "WITH RECURSIVE group_reach(group_id,id,deep) AS ("
                + "SELECT group_id,department_id,include_descendants FROM iam_tenant_group_department WHERE tenant_id=#{q.tenantId} "
                + "UNION SELECT r.group_id,d.id,TRUE FROM group_reach r JOIN iam_department d "
                + "ON d.tenant_id=#{q.tenantId} AND d.parent_id=r.id WHERE r.deep"
                + "), group_people(group_id,member_id) AS ("
                + "SELECT group_id,member_id FROM iam_tenant_group_member WHERE tenant_id=#{q.tenantId} "
                + "UNION SELECT gr.group_id,md.member_id FROM group_reach gr "
                + "JOIN iam_member_department md ON md.tenant_id=#{q.tenantId} AND md.department_id=gr.id"
                + ") ";
    }

    private static String relations(Query query) {
        String columns = ",ra.subject_type,ra.id AS assignment_id";
        String where = " WHERE ra.domain='TENANT' AND ra.tenant_id=#{q.tenantId} AND ra.role_id=#{q.roleId} "
                + "AND d.enabled=TRUE AND ra.status='ACTIVE' AND ra.valid_from&lt;=CURRENT_TIMESTAMP(6) "
                + "AND (ra.valid_until IS NULL OR ra.valid_until&gt;CURRENT_TIMESTAMP(6)) "
                + idFilter("visibleAssignmentIds", "ra.id")
                + idFilter("sourceIds", "ra.delegation_grant_id")
                + "AND (ra.delegation_grant_id IS NULL OR (dl.domain='TENANT' AND dl.tenant_id=#{q.tenantId} "
                + "AND dl.status='ACTIVE' AND (dl.valid_from IS NULL OR dl.valid_from&lt;=CURRENT_TIMESTAMP(6)) "
                + "AND (dl.valid_until IS NULL OR dl.valid_until&gt;CURRENT_TIMESTAMP(6)) "
                + "AND ra.valid_until IS NOT NULL "
                + "AND (dl.valid_from IS NULL OR ra.valid_from&gt;=dl.valid_from) "
                + "AND (dl.valid_until IS NULL OR ra.valid_until&lt;=dl.valid_until) "
                + "AND TIMESTAMPDIFF(MICROSECOND,ra.valid_from,ra.valid_until)&lt;=dl.max_assignment_duration_seconds*1000000"
                + "+FLOOR(dl.max_assignment_duration_nanos/1000) "
                + "AND EXISTS(SELECT 1 FROM iam_delegation_role dr WHERE dr.delegation_id=dl.id AND dr.role_id=ra.role_id))) ";
        String base = " FROM iam_role_assignment ra JOIN iam_role_definition d ON d.id=ra.role_id "
                + "LEFT JOIN iam_delegation_grant dl ON dl.id=ra.delegation_grant_id ";
        String groupVisibility = idFilter("visibleGroupIds", "ra.tenant_group_id");
        if (query.subjectType() == SubjectType.GROUP && query.memberId() == null) {
            return "SELECT g.id,g.name" + columns + base
                    + "JOIN iam_tenant_group g ON g.tenant_id=ra.tenant_id AND g.id=ra.tenant_group_id "
                    + where + "AND ra.subject_type='GROUP' AND g.name LIKE #{q.keyword} ESCAPE '!' " + groupVisibility;
        }
        String member = query.memberId() == null ? "" : " AND m.id=#{q.memberId} ";
        String direct = "SELECT m.id,m.display_name AS name" + columns + base
                + "JOIN iam_tenant_member m ON m.tenant_id=ra.tenant_id AND m.id=ra.tenant_member_id "
                + "JOIN iam_account a ON a.id=m.account_id "
                + where + "AND ra.subject_type='MEMBER' AND m.status='ACTIVE' AND a.enabled=TRUE AND a.deleted_at IS NULL "
                + "AND m.display_name LIKE #{q.keyword} ESCAPE '!' " + member;
        String inherited = "SELECT m.id,m.display_name AS name" + columns + base
                + "JOIN group_people gp ON gp.group_id=ra.tenant_group_id "
                + "JOIN iam_tenant_member m ON m.tenant_id=ra.tenant_id AND m.id=gp.member_id "
                + "JOIN iam_account a ON a.id=m.account_id "
                + where + "AND ra.subject_type='GROUP' AND m.status='ACTIVE' AND a.enabled=TRUE AND a.deleted_at IS NULL "
                + "AND m.display_name LIKE #{q.keyword} ESCAPE '!' " + member + groupVisibility;
        return direct + " UNION ALL " + inherited;
    }

    private static String idFilter(String field, String column) {
        return "<choose><when test='q." + field + " == null'></when>"
                + "<when test='q." + field + ".isEmpty()'> AND 1=0 </when><otherwise> AND " + column
                + " IN <foreach collection='q." + field + "' item='id' open='(' close=')' separator=','>#{id}</foreach>"
                + "</otherwise></choose>";
    }

    /**
     * <p>服务端校验后的租户、角色、来源和组可见范围。</p>
     *
     * @param tenantId 当前组织
     * @param roleId 已确认可见的角色
     * @param memberId 查看来源时的成员；主体页为空
     * @param subjectType 成员或用户组
     * @param visibleAssignmentIds 治理读取允许的分配，空集合表示无可见对象，{@code null} 表示全部
     * @param sourceIds 受限管理员本人委派；治理读取时为 {@code null}
     * @param visibleGroupIds 可见用户组，空集合表示不能展开继承，{@code null} 表示全部
     * @param keyword 已转义的包含匹配
     * @param offset 分页偏移
     * @param size 页大小
     * @author jy
     * @since 1.0.0
     */
    public record Query(BigInteger tenantId, BigInteger roleId, BigInteger memberId, SubjectType subjectType,
            List<BigInteger> visibleAssignmentIds, List<BigInteger> sourceIds, List<BigInteger> visibleGroupIds,
            String keyword, int offset, int size) { }
}
