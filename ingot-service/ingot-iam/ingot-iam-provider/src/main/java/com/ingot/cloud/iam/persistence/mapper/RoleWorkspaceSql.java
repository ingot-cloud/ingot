package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import com.ingot.framework.commons.model.iam.SubjectType;

/**
 * <p>角色关联查询在 SQL 内复用有效来源、归属和对象边界，再去重、计数和分页。</p>
 * @author jy
 * @since 1.0.0
 */
public final class RoleWorkspaceSql {
    private RoleWorkspaceSql() { }
    /**
     * 生成主体聚合分页；列表和总数使用相同的关系子查询。
     * @param params 可信查询参数
     * @return 绑定参数 SQL
     */
    public static String page(Map<String, Object> params) {
        Query q = (Query) params.get("q");
        return "<script>SELECT x.id,x.name,JSON_ARRAYAGG(x.revision_number) AS revisions,"
                + "JSON_ARRAYAGG(x.subject_type) AS source_types,COUNT(DISTINCT x.assignment_id) AS source_count "
                + "FROM (" + relations(q) + ") x GROUP BY x.id,x.name ORDER BY x.id LIMIT #{q.size} OFFSET #{q.offset}</script>";
    }
    /**
     * 按相同关系边界统计去重主体数。
     * @param params 可信查询参数
     * @return 绑定参数 SQL
     */
    public static String count(Map<String, Object> params) {
        return "<script>SELECT COUNT(DISTINCT x.id) FROM (" + relations((Query) params.get("q")) + ") x</script>";
    }
    /**
     * 分页读取一个成员的真实来源分配，组继承仍按组对象范围过滤。
     * @param params 可信查询参数
     * @return 绑定参数 SQL
     */
    public static String sources(Map<String, Object> params) {
        return "<script>SELECT ra.* FROM iam_role_assignment ra WHERE ra.id IN (SELECT x.assignment_id FROM ("
                + relations((Query) params.get("q")) + ") x) ORDER BY ra.id LIMIT #{q.size} OFFSET #{q.offset}</script>";
    }
    /**
     * 统计成员可见的真实有效分配来源数。
     * @param params 可信查询参数
     * @return 绑定参数 SQL
     */
    public static String sourceCount(Map<String, Object> params) {
        return "<script>SELECT COUNT(DISTINCT x.assignment_id) FROM (" + relations((Query) params.get("q")) + ") x</script>";
    }
    private static String relations(Query q) {
        String base = " FROM iam_role_assignment ra JOIN iam_role_revision r ON r.id=ra.revision_id "
                + "JOIN iam_role_definition definition ON definition.id=r.role_id AND definition.domain='PLATFORM' AND definition.tenant_id IS NULL "
                + "LEFT JOIN iam_delegation_grant d ON d.id=ra.delegation_grant_id ";
        String assignmentVisibility = q.visibleAssignmentIds() == null ? "" : q.visibleAssignmentIds().isEmpty() ? " AND 1=0 "
                : " AND ra.id IN <foreach collection='q.visibleAssignmentIds' item='id' open='(' close=')' separator=','>#{id}</foreach> ";
        String where = " WHERE r.role_id=#{q.roleId} AND definition.enabled=TRUE AND ra.domain='PLATFORM' AND ra.tenant_id IS NULL "
                + "AND ra.status='ACTIVE' AND ra.valid_from&lt;=CURRENT_TIMESTAMP "
                + "AND (ra.valid_until IS NULL OR ra.valid_until&gt;CURRENT_TIMESTAMP) " + assignmentVisibility
                + (q.revisionId() == null ? "" : "AND ra.revision_id=#{q.revisionId} ")
                + (q.ownerId() == null ? "" : "AND d.platform_administrator_id=#{q.ownerId} ")
                + "AND (ra.delegation_grant_id IS NULL OR (d.domain='PLATFORM' AND d.tenant_id IS NULL AND d.status='ACTIVE' "
                + "AND (d.valid_from IS NULL OR d.valid_from&lt;=CURRENT_TIMESTAMP) "
                + "AND (d.valid_until IS NULL OR d.valid_until&gt;CURRENT_TIMESTAMP) "
                + "AND (d.valid_from IS NULL OR ra.valid_from&gt;=d.valid_from) "
                + "AND (d.valid_until IS NULL OR ra.valid_until&lt;=d.valid_until OR (ra.valid_until IS NULL AND d.assignment_duration_mode='UNLIMITED')) "
                + "AND (d.assignment_duration_mode='UNLIMITED' OR (ra.valid_until IS NOT NULL "
                + "AND TIMESTAMPDIFF(MICROSECOND,ra.valid_from,ra.valid_until)&lt;=d.max_assignment_duration_seconds*1000000+FLOOR(d.max_assignment_duration_nanos/1000))) "
                + "AND EXISTS(SELECT 1 FROM iam_delegation_role_revision dr WHERE dr.delegation_id=d.id AND dr.revision_id=ra.revision_id) "
                + "AND ((ra.subject_type='MEMBER' AND ra.platform_member_id&lt;&gt;d.platform_administrator_id "
                + "AND EXISTS(SELECT 1 FROM iam_delegation_recipient_member rm WHERE rm.delegation_id=d.id AND rm.platform_member_id=ra.platform_member_id)) "
                + "OR (ra.subject_type='GROUP' AND EXISTS(SELECT 1 FROM iam_platform_group_member valid_gm WHERE valid_gm.group_id=ra.platform_group_id) "
                + "AND NOT EXISTS(SELECT 1 FROM iam_platform_group_member valid_gm LEFT JOIN iam_delegation_recipient_member rm "
                + "ON rm.delegation_id=d.id AND rm.platform_member_id=valid_gm.member_id WHERE valid_gm.group_id=ra.platform_group_id "
                + "AND (rm.platform_member_id IS NULL OR valid_gm.member_id=d.platform_administrator_id)))))) ";
        String columns = ",r.revision AS revision_number,ra.subject_type,ra.id AS assignment_id";
        String groupVisibility = q.visibleGroupIds() == null ? "" : q.visibleGroupIds().isEmpty() ? " AND 1=0 "
                : " AND ra.platform_group_id IN <foreach collection='q.visibleGroupIds' item='id' open='(' close=')' separator=','>#{id}</foreach> ";
        if (q.subjectType() == SubjectType.GROUP) {
            return "SELECT g.id,g.name" + columns + base + "JOIN iam_platform_group g ON g.id=ra.platform_group_id "
                    + where + "AND ra.subject_type='GROUP' AND g.name LIKE #{q.keyword} ESCAPE '!' " + groupVisibility;
        }
        String member = q.memberId() == null ? "" : " AND m.id=#{q.memberId} ";
        String direct = "SELECT m.id,m.display_name AS name" + columns + base
                + "JOIN iam_platform_member m ON m.id=ra.platform_member_id " + where + "AND ra.subject_type='MEMBER' AND m.status='ACTIVE' "
                + "AND m.display_name LIKE #{q.keyword} ESCAPE '!' " + member;
        String group = "SELECT m.id,m.display_name AS name" + columns + base
                + "JOIN iam_platform_group_member gm ON gm.group_id=ra.platform_group_id JOIN iam_platform_member m ON m.id=gm.member_id "
                + where + "AND ra.subject_type='GROUP' AND m.status='ACTIVE' AND m.display_name LIKE #{q.keyword} ESCAPE '!' "
                + member + groupVisibility;
        return direct + " UNION ALL " + group;
    }
    /**
     * <p>服务端校验后的角色、固定版本、来源所有权和组可见范围。</p>
     * @param roleId 可信可见角色
     * @param revisionId 可选固定版本，须属于角色
     * @param ownerId 受限管理员 ID；治理读取资格为空
     * @param visibleGroupIds 可见组对象，null 全部，空集合无可见组
     * @param subjectType 接收主体页种类
     * @param memberId 查询实际来源时的成员 ID
     * @param keyword 转义包含搜索
     * @param offset 分页偏移
     * @param size 页大小
     * @param visibleAssignmentIds 治理读取允许的分配对象，null 表示全部
     * @author jy
     * @since 1.0.0
     */
    public record Query(BigInteger roleId, BigInteger revisionId, BigInteger ownerId,
            List<BigInteger> visibleGroupIds, SubjectType subjectType, BigInteger memberId,
            String keyword, int offset, int size, List<BigInteger> visibleAssignmentIds) { }

}
