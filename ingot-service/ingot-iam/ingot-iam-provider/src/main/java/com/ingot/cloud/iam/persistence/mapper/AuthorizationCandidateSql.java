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
    private static final String PLATFORM_DOMAIN = AuthorizationDomain.PLATFORM.getValue();
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
        String resourceName = q.kind() == AuthorizationCandidateKind.ACTION
                ? ",x.resource_name" : ",NULL AS resource_name";
        boolean menu = q.kind() == AuthorizationCandidateKind.OBJECT
                && q.objectResource() != null
                && PlatformScopeObjectResource.find(q.objectResource()) == PlatformScopeObjectResource.MENU;
        String treeColumns = menu ? ",x.parent_id,x.has_children"
                : ",NULL AS parent_id,FALSE AS has_children";
        return "<script>SELECT x.id,x.name,x.kind,x.revision" + resourceName + treeColumns
                + " FROM (" + source(q) + ") x WHERE "
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
        String sql = q.kind() == AuthorizationCandidateKind.ACTION
                ? "(x.name LIKE #{q.keyword} ESCAPE '!' OR x.resource_name LIKE #{q.keyword} ESCAPE '!')"
                : "x.name LIKE #{q.keyword} ESCAPE '!'";
        if (!q.ids().isEmpty()) {
            sql += " AND x.id IN <foreach collection='q.ids' item='id' open='(' close=')' separator=','>#{id}</foreach>";
        }
        if (q.allowedIds() != null) {
            sql += q.allowedIds().isEmpty() ? " AND 1=0" :
                    " AND x.id IN <foreach collection='q.allowedIds' item='id' open='(' close=')' separator=','>#{id}</foreach>";
        }
        if (q.tree() && q.kind() == AuthorizationCandidateKind.OBJECT
                && PlatformScopeObjectResource.find(q.objectResource()) == PlatformScopeObjectResource.MENU
                && q.ids().isEmpty() && "%".equals(q.keyword())) {
            sql += q.parentId() == null ? " AND x.parent_id IS NULL" : " AND x.parent_id=#{q.parentId}";
        }
        if (q.excludeMemberId() != null && q.kind() == AuthorizationCandidateKind.MEMBER) {
            sql += " AND x.id&lt;&gt;#{q.excludeMemberId}";
        }
        if (q.selectedDelegationId() != null) {
            sql += switch (q.kind()) {
                case MEMBER -> " AND EXISTS(SELECT 1 FROM iam_delegation_recipient_member selected WHERE "
                        + "selected.delegation_id=#{q.selectedDelegationId} AND selected.platform_member_id=x.id)";
                case ROLE_REVISION -> " AND EXISTS(SELECT 1 FROM iam_delegation_role_revision selected WHERE "
                        + "selected.delegation_id=#{q.selectedDelegationId} AND selected.revision_id=x.id)";
                case OBJECT -> " AND EXISTS(SELECT 1 FROM iam_delegation_action_ceiling selected "
                        + "JOIN JSON_TABLE(selected.scope_bindings, '$.*.ids[*]' COLUMNS(object_id DECIMAL(20,0) PATH '$')) bound "
                        + "WHERE selected.delegation_id=#{q.selectedDelegationId} AND selected.action_id=#{q.selectedActionId} "
                        + "AND bound.object_id=x.id)";
                default -> throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            };
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
            case ACTION -> "SELECT a.id,a.name,NULL AS kind,NULL AS revision,r.name AS resource_name FROM iam_action a JOIN iam_application app ON app.id=a.application_id "
                    + "JOIN iam_resource r ON r.id=a.resource_id WHERE app.domain='PLATFORM' AND app.enabled=TRUE AND a.enabled=TRUE AND r.enabled=TRUE"
                    + (q.applicationId() == null ? "" : " AND app.id=#{q.applicationId}");
            case OBJECT -> objectSource(q.objectResource());
        };
    }
    private static String groupFilter(Query q) {
        String recipient = q.delegationId() == null ? "" : " OR NOT EXISTS(SELECT 1 FROM iam_delegation_recipient_member rm "
                + "WHERE rm.delegation_id=#{q.delegationId} AND rm.platform_member_id=gm.member_id)";
        String self = q.delegationId() == null ? "" : " AND NOT EXISTS(SELECT 1 FROM iam_platform_group_member self_member "
                + "JOIN iam_delegation_grant source ON source.id=#{q.delegationId} WHERE self_member.group_id=g.id "
                + "AND self_member.member_id=source.platform_administrator_id)";
        return " WHERE EXISTS(SELECT 1 FROM iam_platform_group_member gm WHERE gm.group_id=g.id) " + self
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
            case APPLICATION -> { table="iam_application"; label="name"; where=" WHERE domain='" + PLATFORM_DOMAIN + "'"; }
            case RESOURCE -> { table="iam_resource"; label="name"; where=platformApplication(); }
            case ACTION -> { table="iam_action"; label="name"; where=platformApplication(); }
            case MENU -> {
                return "SELECT m.id,m.name,NULL AS kind,NULL AS revision,m.parent_id,"
                        + "EXISTS(SELECT 1 FROM iam_menu child WHERE child.parent_id=m.id) AS has_children "
                        + "FROM iam_menu m" + platformApplication();
            }
            case ROLE -> { table="iam_role_definition"; label="name"; where=" WHERE domain='PLATFORM'"; }
            case SHARED_ROLE -> { table="iam_role_definition"; label="name"; where=" WHERE kind='SHARED' AND domain='" + PLATFORM_DOMAIN + "'"; }
            case PLAN -> { table="iam_plan"; label="name"; }
            case ASSIGNMENT -> { table="iam_role_assignment"; label="CAST(id AS CHAR)"; where=" WHERE domain='PLATFORM'"; }
            case DELEGATION -> { table="iam_delegation_grant"; label="CAST(id AS CHAR)"; where=" WHERE domain='PLATFORM'"; }
            default -> throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return "SELECT id," + label + " AS name,NULL AS kind,NULL AS revision FROM " + table + where;
    }

    private static String platformApplication() {
        return " WHERE application_id IN (SELECT id FROM iam_application WHERE domain='" + PLATFORM_DOMAIN + "')";
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
     * @param excludeMemberId 接收候选排除的管理员
     * @param selectedDelegationId 可信现有委派，按真实已选关系分页
     * @param selectedActionId 对象已选关系所属操作
     * @author jy
     * @since 1.0.0
     */
    public record Query(AuthorizationCandidateKind kind, BigInteger memberId, BigInteger delegationId,
                        BigInteger applicationId, String objectResource, String keyword, List<BigInteger> ids,
                        List<BigInteger> allowedIds, int offset, int size, boolean tree,
                        BigInteger parentId, BigInteger excludeMemberId, BigInteger selectedDelegationId,
                        BigInteger selectedActionId) {
        /** 保持普通树候选查询的构造契约。 */
        public Query(AuthorizationCandidateKind kind, BigInteger memberId, BigInteger delegationId,
                BigInteger applicationId, String objectResource, String keyword, List<BigInteger> ids,
                List<BigInteger> allowedIds, int offset, int size, boolean tree, BigInteger parentId) {
            this(kind, memberId, delegationId, applicationId, objectResource, keyword, ids,
                    allowedIds, offset, size, tree, parentId, null, null, null);
        }
        /**
         * 保持普通列表与对象校验查询构造契约。
         */
        public Query(AuthorizationCandidateKind kind, BigInteger memberId, BigInteger delegationId,
                BigInteger applicationId, String objectResource, String keyword, List<BigInteger> ids,
                List<BigInteger> allowedIds, int offset, int size) {
            this(kind, memberId, delegationId, applicationId, objectResource, keyword, ids,
                    allowedIds, offset, size, false, null);
        }
    }
}
