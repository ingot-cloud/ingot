package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import com.ingot.cloud.iam.assignment.PlatformScopeObjectResource;
import com.ingot.framework.commons.model.iam.AuthorizationCandidateKind;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.error.BizException;

/**
 * <p>授权候选 SQL；表、列及关联只由封闭服务端适配器决定，用户值全部绑定。</p>
 * @author jy
 * @since 1.0.0
 */
public final class AuthorizationCandidateSql {
    private static final String LIMIT = " ORDER BY x.id LIMIT #{q.size} OFFSET #{q.offset}";
    private static final String ROLE_BOUNDARY = "d.domain='" + AuthorizationDomain.PLATFORM.getValue()
            + "' AND d.tenant_id IS NULL AND d.enabled=TRUE";
    private static final String REVISION_BOUNDARY = "r.kind IN ('" + RoleKind.PLATFORM_CUSTOM.getValue()
            + "','" + RoleKind.SYSTEM.getValue() + "')";
    private AuthorizationCandidateSql() { }
    /**
     * 生成分页语句。
     * @param parameters Mapper 参数
     * @return 带绑定占位符 SQL
     */
    public static String page(Map<String, Object> parameters) {
        Query q = (Query) parameters.get("q");
        return "<script>SELECT x.id,x.name,x.kind,x.revision FROM (" + source(q) + ") x WHERE "
                + filters(q) + LIMIT + "</script>";
    }
    /**
     * 生成使用同一筛选的计数语句。
     * @param parameters Mapper 参数
     * @return 计数 SQL
     */
    public static String count(Map<String, Object> parameters) {
        Query q = (Query) parameters.get("q");
        return "<script>SELECT COUNT(*) FROM (" + source(q) + ") x WHERE " + filters(q) + "</script>";
    }

    /**
     * 生成角色树当前层分页 SQL；版本层按版本号倒序。
     * @param parameters Mapper 参数
     * @return 只含绑定参数与固定结构的分页 SQL
     */
    public static String rolePage(Map<String, Object> parameters) {
        RoleQuery q = (RoleQuery) parameters.get("q");
        String order = q.roleId() == null ? "x.id" : "x.revision DESC,x.id DESC";
        return "<script>SELECT x.id,x.role_id,x.name,x.kind,x.revision FROM (" + roleSource(q)
                + ") x WHERE " + roleFilters(q) + " ORDER BY " + order
                + " LIMIT #{q.size} OFFSET #{q.offset}</script>";
    }

    /**
     * 生成与角色树当前层分页完全相同的计数筛选。
     * @param parameters Mapper 参数
     * @return 当前层计数 SQL
     */
    public static String roleCount(Map<String, Object> parameters) {
        RoleQuery q = (RoleQuery) parameters.get("q");
        return "<script>SELECT COUNT(*) FROM (" + roleSource(q) + ") x WHERE "
                + roleFilters(q) + "</script>";
    }

    private static String roleSource(RoleQuery q) {
        String allowed = q.allowedRevisionIds() == null ? "" : q.allowedRevisionIds().isEmpty()
                ? " AND 1=0" : " AND r.id IN <foreach collection='q.allowedRevisionIds' item='id' "
                + "open='(' close=')' separator=','>#{id}</foreach>";
        if (q.roleId() == null) {
            return "SELECT d.id,d.id AS role_id,d.name,NULL AS kind,NULL AS revision FROM iam_role_definition d WHERE "
                    + ROLE_BOUNDARY + " AND EXISTS(SELECT 1 FROM iam_role_revision r WHERE r.role_id=d.id AND "
                    + REVISION_BOUNDARY + allowed + ")";
        }
        return "SELECT r.id,r.role_id,d.name,r.kind,r.revision FROM iam_role_revision r "
                + "JOIN iam_role_definition d ON d.id=r.role_id WHERE " + ROLE_BOUNDARY + " AND "
                + REVISION_BOUNDARY + " AND r.role_id=#{q.roleId}" + allowed;
    }

    private static String roleFilters(RoleQuery q) {
        String filter = "x.name LIKE #{q.keyword} ESCAPE '!'";
        return q.ids().isEmpty() ? filter : filter
                + " AND x.id IN <foreach collection='q.ids' item='id' open='(' close=')' separator=','>#{id}</foreach>";
    }

    /**
     * <p>服务端验证后的角色树层级查询，白名单仅作用于实际版本标识。</p>
     * @param roleId 指定角色时查询子版本；为空查询角色根层
     * @param keyword 已转义的角色名称包含匹配
     * @param ids 当前层少量已选节点
     * @param allowedRevisionIds 委派允许的版本；null 表示直接分配不额外收窄
     * @param offset 分页偏移
     * @param size 页大小
     * @author jy
     * @since 1.0.0
     */
    public record RoleQuery(BigInteger roleId, String keyword, List<BigInteger> ids,
            List<BigInteger> allowedRevisionIds, int offset, int size) { }
    private static String filters(Query q) {
        String sql = "x.name LIKE #{q.keyword} ESCAPE '!'";
        if (!q.ids().isEmpty()) {
            sql += " AND x.id IN <foreach collection='q.ids' item='id' open='(' close=')' separator=','>#{id}</foreach>";
        }
        if (q.allowedIds() != null) {
            sql += q.allowedIds().isEmpty() ? " AND 1=0" :
                    " AND x.id IN <foreach collection='q.allowedIds' item='id' open='(' close=')' separator=','>#{id}</foreach>";
        }
        return sql;
    }
    private static String source(Query q) {
        return switch (q.kind()) {
            case DELEGATION -> "SELECT id,CAST(id AS CHAR) AS name,NULL AS kind,NULL AS revision FROM iam_delegation_grant "
                    + "WHERE domain='PLATFORM' AND tenant_id IS NULL AND platform_administrator_id=#{q.memberId} "
                    + "AND status='ACTIVE' AND (valid_from IS NULL OR valid_from&lt;=CURRENT_TIMESTAMP) "
                    + "AND (valid_until IS NULL OR valid_until&gt;CURRENT_TIMESTAMP)";
            case ROLE_REVISION -> "SELECT r.id,d.name,r.kind,r.revision FROM iam_role_revision r "
                    + "JOIN iam_role_definition d ON d.id=r.role_id WHERE d.domain='PLATFORM' AND d.tenant_id IS NULL "
                    + "AND d.enabled=TRUE AND r.kind IN ('PLATFORM_CUSTOM','SYSTEM')";
            case MEMBER -> "SELECT id,display_name AS name,NULL AS kind,NULL AS revision FROM iam_platform_member WHERE status='ACTIVE'";
            case GROUP -> "SELECT g.id,g.name,NULL AS kind,NULL AS revision FROM iam_platform_group g" + groupFilter(q);
            case APPLICATION -> "SELECT id,name,NULL AS kind,NULL AS revision FROM iam_application WHERE domain='PLATFORM' AND enabled=TRUE";
            case ACTION -> "SELECT a.id,a.name,NULL AS kind,NULL AS revision FROM iam_action a JOIN iam_application app ON app.id=a.application_id "
                    + "JOIN iam_resource r ON r.id=a.resource_id WHERE app.domain='PLATFORM' AND app.enabled=TRUE AND a.enabled=TRUE AND r.enabled=TRUE"
                    + (q.applicationId() == null ? "" : " AND app.id=#{q.applicationId}");
            case OBJECT -> objectSource(q.objectResource());
        };
    }
    private static String groupFilter(Query q) {
        String recipient = q.delegationId() == null ? "" : " OR NOT EXISTS(SELECT 1 FROM iam_delegation_recipient_member rm "
                + "WHERE rm.delegation_id=#{q.delegationId} AND rm.platform_member_id=gm.member_id)";
        return " WHERE EXISTS(SELECT 1 FROM iam_platform_group_member gm WHERE gm.group_id=g.id) "
                + "AND NOT EXISTS(SELECT 1 FROM iam_platform_group_member gm LEFT JOIN iam_platform_member m ON m.id=gm.member_id "
                + "WHERE gm.group_id=g.id AND (m.id IS NULL OR m.status&lt;&gt;'ACTIVE'" + recipient + "))";
    }
    private static String objectSource(String resource) {
        // These identifiers are the same IDs used by ResourceAccess and the protected CRUD service.
        String table; String label; String where = "";
        PlatformScopeObjectResource adapter = PlatformScopeObjectResource.find(resource);
        if (adapter == null) { throw new BizException(IamReasonCode.INVALID_ARGUMENT); }
        switch (adapter) {
            case MEMBER -> { table="iam_platform_member"; label="display_name"; where=" WHERE status='ACTIVE'"; }
            case GROUP -> { table="iam_platform_group"; label="name"; }
            case ENTITLEMENT, TENANT -> { table="iam_tenant"; label="name"; where=" WHERE deleted_at IS NULL"; }
            case ACCOUNT -> { table="iam_account"; label="username"; where=" WHERE deleted_at IS NULL"; }
            case APPLICATION -> { table="iam_application"; label="name"; }
            case RESOURCE -> { table="iam_resource"; label="name"; }
            case ACTION -> { table="iam_action"; label="name"; }
            case MENU -> { table="iam_menu"; label="name"; }
            case ROLE -> { table="iam_role_definition"; label="name"; where=" WHERE domain='PLATFORM'"; }
            case SHARED_ROLE -> { table="iam_role_definition"; label="name"; where=" WHERE kind='SHARED'"; }
            case PLAN -> { table="iam_plan"; label="name"; }
            case ASSIGNMENT -> { table="iam_role_assignment"; label="CAST(id AS CHAR)"; where=" WHERE domain='PLATFORM'"; }
            case DELEGATION -> { table="iam_delegation_grant"; label="CAST(id AS CHAR)"; where=" WHERE domain='PLATFORM'"; }
            default -> throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return "SELECT id," + label + " AS name,NULL AS kind,NULL AS revision FROM " + table + where;
    }
    /**
     * <p>已由服务端验证的候选查询，不接收用户 SQL、表名或列名。</p>
     * @param kind 候选种类
     * @param memberId 可信当前成员
     * @param delegationId 已验证的授权依据
     * @param applicationId 诊断应用
     * @param objectResource 注册资源适配器
     * @param keyword 转义后的包含匹配
     * @param ids 已选标识回显
     * @param allowedIds 当前依据范围，null 表示未额外收窄
     * @param offset 分页偏移
     * @param size 每页上限
     * @author jy
     * @since 1.0.0
     */
    public record Query(AuthorizationCandidateKind kind, BigInteger memberId, BigInteger delegationId,
                        BigInteger applicationId, String objectResource, String keyword, List<BigInteger> ids,
                        List<BigInteger> allowedIds, int offset, int size) { }
}
