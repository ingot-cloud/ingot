package com.ingot.cloud.iam.persistence.mapper;

import com.ingot.cloud.iam.assignment.TenantScopeObjectResource;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * <p>固定的租户对象适配器 SQL；查询值只通过 MyBatis 参数绑定。</p>
 * @author jy
 * @since 1.0.0
 */
public final class TenantScopeCandidateSql {
    private static final String TENANT_DOMAIN = AuthorizationDomain.TENANT.getValue();
    private TenantScopeCandidateSql() { }

    /**
     * 生成分页 SQL。
     * @param parameters MyBatis 参数
     * @return 分页查询
     */
    public static String page(Map<String, Object> parameters) {
        Query q = (Query) parameters.get("q");
        String treeColumns = q.resource() == TenantScopeObjectResource.DEPARTMENT
                ? ",x.parent_id,x.has_children" : ",NULL AS parent_id,FALSE AS has_children";
        return "<script>SELECT x.id,x.name" + treeColumns + " FROM (" + source(q.resource()) + ") x WHERE "
                + filter(q) + " ORDER BY x.id LIMIT #{q.size} OFFSET #{q.offset}</script>";
    }

    /**
     * 生成与分页相同筛选的计数 SQL。
     * @param parameters MyBatis 参数
     * @return 计数查询
     */
    public static String count(Map<String, Object> parameters) {
        Query q = (Query) parameters.get("q");
        return "<script>SELECT COUNT(*) FROM (" + source(q.resource()) + ") x WHERE " + filter(q) + "</script>";
    }

    private static String source(TenantScopeObjectResource resource) {
        return switch (resource) {
            case DEPARTMENT -> "SELECT d.id,d.name,d.parent_id,"
                    + "EXISTS(SELECT 1 FROM iam_department child WHERE child.parent_id=d.id"
                    + " AND child.tenant_id=d.tenant_id) AS has_children "
                    + "FROM iam_department d WHERE d.tenant_id=#{q.tenantId}";
            case MEMBER, DIRECTORY -> "SELECT id,display_name AS name FROM iam_tenant_member "
                    + "WHERE tenant_id=#{q.tenantId} AND status='ACTIVE'";
            case GROUP -> "SELECT id,name FROM iam_tenant_group WHERE tenant_id=#{q.tenantId}";
            case APPLICATION -> "SELECT app.id,app.name FROM iam_application app "
                    + "JOIN iam_tenant_app_entitlement e ON e.application_id=app.id "
                    + "WHERE e.tenant_id=#{q.tenantId} AND e.enabled=TRUE AND app.enabled=TRUE "
                    + "AND (e.valid_from IS NULL OR e.valid_from&lt;=CURRENT_TIMESTAMP) "
                    + "AND (e.valid_until IS NULL OR e.valid_until&gt;CURRENT_TIMESTAMP)";
            case ROLE -> "SELECT id,name FROM iam_role_definition WHERE domain='" + TENANT_DOMAIN + "' "
                    + "AND tenant_id=#{q.tenantId} AND enabled=TRUE";
            case ASSIGNMENT -> "SELECT id,CAST(id AS CHAR) AS name FROM iam_role_assignment "
                    + "WHERE domain='" + TENANT_DOMAIN + "' AND tenant_id=#{q.tenantId}";
            case DELEGATION -> "SELECT id,CAST(id AS CHAR) AS name FROM iam_delegation_grant "
                    + "WHERE domain='" + TENANT_DOMAIN + "' AND tenant_id=#{q.tenantId}";
        };
    }

    private static String filter(Query q) {
        String sql = "x.name LIKE #{q.keyword} ESCAPE '!'";
        if (!q.ids().isEmpty()) {
            sql += " AND x.id IN <foreach collection='q.ids' item='id' open='(' close=')' separator=','>#{id}</foreach>";
        } else if (q.tree() && q.resource() == TenantScopeObjectResource.DEPARTMENT
                && "%".equals(q.keyword())) {
            sql += q.parentId() == null ? " AND x.parent_id IS NULL" : " AND x.parent_id=#{q.parentId}";
        }
        return sql;
    }

    /**
     * <p>可信服务端解析后的范围候选查询。</p>
     * @param resource 固定适配器资源
     * @param tenantId 可信当前租户
     * @param keyword 已转义的匹配表达式
     * @param ids 已选回显标识
     * @param offset 偏移
     * @param size 页大小
     * @param tree 是否分页当前树分支
     * @param parentId 父节点，根分支为空
     * @author jy
     * @since 1.0.0
     */
    public record Query(TenantScopeObjectResource resource, long tenantId, String keyword,
            List<BigInteger> ids, int offset, int size, boolean tree, BigInteger parentId) {
        /**
         * 保持普通候选与提交校验查询构造契约。
         */
        public Query(TenantScopeObjectResource resource, long tenantId, String keyword,
                List<BigInteger> ids, int offset, int size) {
            this(resource, tenantId, keyword, ids, offset, size, false, null);
        }
    }
}
