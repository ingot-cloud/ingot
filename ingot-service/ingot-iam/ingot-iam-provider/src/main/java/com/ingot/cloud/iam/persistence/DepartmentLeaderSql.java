package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.framework.commons.model.iam.MemberStatus;

/**
 * <p>使用可信租户和类型化成员范围生成关联分页 SQL，不接受客户端 SQL 或列名。</p>
 * @author jy
 * @since 1.0.0
 */
public final class DepartmentLeaderSql {
    private static final String AVAILABLE = "(m.status=#{q.activeStatus} AND a.enabled=TRUE AND a.deleted_at IS NULL)";
    private static final String MEMBERS = " FROM iam_tenant_member m JOIN iam_account a ON a.id=m.account_id ";
    private static final String RELATION = " JOIN iam_department_leader l ON l.tenant_id=m.tenant_id AND l.member_id=m.id ";

    private DepartmentLeaderSql() { }

    /** 生成候选或已绑定分页；两者均在分页前应用成员范围。 */
    public static String page(Map<String, Object> parameters) {
        Query q = (Query) parameters.get("q");
        return "<script>SELECT m.id AS member_id,m.display_name,m.status,"
                + (q.candidates() ? "FALSE" : "l.is_primary") + " AS primary_flag," + AVAILABLE + " AS available"
                + source(q) + " ORDER BY " + (q.candidates() ? "m.id" : "l.is_primary DESC,m.id")
                + " LIMIT #{q.size} OFFSET #{q.offset}</script>";
    }

    /** 与分页使用相同过滤条件计算总数。 */
    public static String count(Map<String, Object> parameters) {
        return "<script>SELECT COUNT(*)" + source((Query) parameters.get("q")) + "</script>";
    }

    /** 批量汇总可见负责人，不把整个关系集合拉到内存。 */
    public static String summaries(Map<String, Object> parameters) {
        Query q = (Query) parameters.get("q");
        return "<script>SELECT l.department_id,COUNT(*) AS visible_count,"
                + "MAX(CASE WHEN l.is_primary THEN m.id END) AS member_id,"
                + "MAX(CASE WHEN l.is_primary THEN m.display_name END) AS display_name,"
                + "MAX(CASE WHEN l.is_primary THEN m.status END) AS status,"
                + "MAX(CASE WHEN l.is_primary THEN " + AVAILABLE + " ELSE FALSE END) AS available"
                + MEMBERS + RELATION + " WHERE m.tenant_id=#{q.tenantId} AND l.department_id IN "
                + "<foreach collection='q.departmentIds' item='department' open='(' close=')' separator=','>#{department}</foreach>"
                + " AND " + memberScope(q.scope()) + " GROUP BY l.department_id</script>";
    }

    private static String source(Query q) {
        String relation = "SELECT 1 FROM iam_department_leader bound WHERE bound.tenant_id=m.tenant_id "
                + "AND bound.department_id=#{q.departmentId} AND bound.member_id=m.id";
        return MEMBERS + (q.candidates() ? "" : RELATION)
                + " WHERE m.tenant_id=#{q.tenantId} AND " + memberScope(q.scope())
                + (q.candidates() ? " AND " + AVAILABLE + " AND NOT EXISTS(" + relation + ")"
                    : " AND l.department_id=#{q.departmentId}")
                + (q.browseDepartmentId() == null || q.keyword() != null ? "" : " AND EXISTS(SELECT 1 FROM iam_member_department md WHERE md.tenant_id=m.tenant_id AND md.member_id=m.id AND md.department_id=#{q.browseDepartmentId})")
                + (q.pickerBranch() == null ? "" : " AND " + q.pickerBranch().sql("m.id", "m.tenant_id", "#{q.pickerBranch.departmentId}"))
                + (q.keyword() == null ? "" : " AND m.display_name LIKE #{q.keyword} ESCAPE '!'");
    }

    private static String memberScope(ObjectScope scope) {
        if (scope.coversAll()) return "1=1";
        if (scope.coversNone()) return "1=0";
        StringBuilder sql = new StringBuilder("(");
        for (int index = 0; index < scope.clauses().size(); index++) {
            if (index > 0) sql.append(" OR ");
            var clause = scope.clauses().get(index);
            String path = "q.scope.clauses[" + index + "]";
            sql.append("(1=1");
            if (clause.requiredIds().isEmpty() && clause.departmentSets().isEmpty()) sql.append(" AND 1=0");
            if (!clause.requiredIds().isEmpty()) {
                sql.append(" AND m.id IN ").append(ids(path + ".requiredIds"));
            }
            for (int set = 0; set < clause.departmentSets().size(); set++) {
                if (clause.departmentSets().get(set).isEmpty()) {
                    sql.append(" AND 1=0");
                } else {
                    sql.append(" AND EXISTS(SELECT 1 FROM iam_member_department md WHERE md.tenant_id=m.tenant_id ")
                            .append("AND md.member_id=m.id AND md.department_id IN ")
                            .append(ids(path + ".departmentSets[" + set + "]")).append(')');
                }
            }
            sql.append(')');
        }
        return sql.append(')').toString();
    }

    private static String ids(String path) {
        return "<foreach collection='" + path + "' item='boundId' open='(' close=')' separator=','>#{boundId}</foreach>";
    }

    /**
     * <p>由服务端构造的 SQL 查询参数；scope 保留各条款交集，不接受客户端身份。</p>
     * @author jy
     * @since 1.0.0
     */
    public record Query(BigInteger tenantId, BigInteger departmentId, List<BigInteger> departmentIds,
                        ObjectScope scope, String keyword, int size, int offset, boolean candidates, BigInteger browseDepartmentId, MemberPickerBranch pickerBranch) {
        /** 兼容旧候选接口，选择器特有规则不改变旧列表。 */
        public Query(BigInteger tenantId, BigInteger departmentId, List<BigInteger> departmentIds, ObjectScope scope, String keyword, int size, int offset, boolean candidates, BigInteger browseDepartmentId) {
            this(tenantId, departmentId, departmentIds, scope, keyword, size, offset, candidates, browseDepartmentId, null);
        }
        /** 兼容既有关联分页；未传浏览部门时不额外收窄候选。 */
        public Query(BigInteger tenantId, BigInteger departmentId, List<BigInteger> departmentIds, ObjectScope scope, String keyword, int size, int offset, boolean candidates) {
            this(tenantId, departmentId, departmentIds, scope, keyword, size, offset, candidates, null);
        }
        /** 返回持久化层统一的有效成员字面量。 */
        public String activeStatus() { return MemberStatus.ACTIVE.getValue(); }
    }
}
