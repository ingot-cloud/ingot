package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.*;
import com.ingot.framework.commons.model.iam.TenantAssignmentOptionKind;

/**
 * <p>受限分配候选的具名SQL，先按本人来源过滤，再分页和回显。</p>
 * @author jy
 * @since 1.0.0
 */
public final class TenantLiveOptionSql {
    private TenantLiveOptionSql() { }
    /** 同组织接收人和组展开，所有参数必须来自可信来源。 */
    public static final String REACH = """
        WITH RECURSIVE recipient_reach(id,deep) AS (
            SELECT department_id,include_descendants FROM iam_delegation_recipient_department WHERE tenant_id=#{q.tenantId} AND delegation_id=#{q.sourceId}
            UNION SELECT d.id,TRUE FROM recipient_reach r JOIN iam_department d ON d.tenant_id=#{q.tenantId} AND d.parent_id=r.id WHERE r.deep
        ), recipients(id) AS (
            SELECT tenant_member_id FROM iam_delegation_recipient_member WHERE tenant_id=#{q.tenantId} AND delegation_id=#{q.sourceId} AND domain='TENANT'
            UNION SELECT md.member_id FROM iam_member_department md JOIN recipient_reach r ON r.id=md.department_id WHERE md.tenant_id=#{q.tenantId}
        ), group_reach(group_id,id,deep) AS (
            SELECT group_id,department_id,include_descendants FROM iam_tenant_group_department WHERE tenant_id=#{q.tenantId}
            UNION SELECT r.group_id,d.id,TRUE FROM group_reach r JOIN iam_department d ON d.tenant_id=#{q.tenantId} AND d.parent_id=r.id WHERE r.deep
        ), group_members(group_id,member_id) AS (
            SELECT group_id,member_id FROM iam_tenant_group_member WHERE tenant_id=#{q.tenantId}
            UNION SELECT r.group_id,md.member_id FROM group_reach r JOIN iam_member_department md ON md.tenant_id=#{q.tenantId} AND md.department_id=r.id
        )
        """;
    /** 分页查询。 */
    public static String page(Map<String,Object> parameters) { return sql((Query)parameters.get("q"), false); }
    /** 同范围计数。 */
    public static String count(Map<String,Object> parameters) { return sql((Query)parameters.get("q"), true); }
    private static String sql(Query q, boolean count) {
        String source = switch(q.kind()) {
            case ROLE -> "SELECT d.id,d.name,d.kind AS role_kind FROM iam_role_definition d WHERE " + TenantLiveSql.ROLE_VISIBLE.replace("#{tenantId}","#{q.tenantId}") + " AND d.enabled=TRUE AND d.current_revision_id IS NOT NULL" + (q.sourceId()==null ? "" : " AND EXISTS(SELECT 1 FROM iam_delegation_role dr WHERE dr.delegation_id=#{q.sourceId} AND dr.role_id=d.id)");
            case MEMBER -> "SELECT m.id,m.display_name AS name,NULL AS role_kind FROM iam_tenant_member m JOIN iam_account a ON a.id=m.account_id WHERE m.tenant_id=#{q.tenantId} AND m.status='ACTIVE' AND a.enabled=TRUE AND a.deleted_at IS NULL" + (q.sourceId()==null ? "" : " AND m.id IN(SELECT id FROM recipients) AND m.id!=#{q.memberId}");
            case GROUP -> "SELECT g.id,g.name,NULL AS role_kind FROM iam_tenant_group g WHERE g.tenant_id=#{q.tenantId}" + (q.sourceId()==null ? "" : " AND EXISTS(SELECT 1 FROM group_members gm WHERE gm.group_id=g.id) AND NOT EXISTS(SELECT 1 FROM group_members gm WHERE gm.group_id=g.id AND (gm.member_id=#{q.memberId} OR gm.member_id NOT IN(SELECT id FROM recipients)))");
            case APPLICATION -> "SELECT app.id,app.name,NULL AS role_kind FROM iam_application app WHERE app.domain='TENANT' AND app.enabled=TRUE AND EXISTS(SELECT 1 FROM iam_tenant_app_entitlement e WHERE e.tenant_id=#{q.tenantId} AND e.application_id=app.id AND e.enabled=TRUE AND (e.valid_from IS NULL OR e.valid_from&lt;=CURRENT_TIMESTAMP(6)) AND (e.valid_until IS NULL OR e.valid_until>CURRENT_TIMESTAMP(6)))";
            case DEPARTMENT -> "SELECT d.id,d.name,NULL AS role_kind FROM iam_department d WHERE d.tenant_id=#{q.tenantId}";
            case RESOURCE -> "SELECT r.id,r.name,NULL AS role_kind FROM iam_resource r JOIN iam_application app ON app.id=r.application_id WHERE app.domain='TENANT' AND app.enabled=TRUE AND r.enabled=TRUE AND EXISTS(SELECT 1 FROM iam_tenant_app_entitlement e WHERE e.tenant_id=#{q.tenantId} AND e.application_id=app.id AND e.enabled=TRUE AND (e.valid_from IS NULL OR e.valid_from&lt;=CURRENT_TIMESTAMP(6)) AND (e.valid_until IS NULL OR e.valid_until>CURRENT_TIMESTAMP(6)))" + (q.sourceId()==null ? "" : " AND EXISTS(SELECT 1 FROM iam_delegation_resource_ceiling rc WHERE rc.delegation_id=#{q.sourceId} AND rc.resource_id=r.id)");
        };
        String filter=" x.name LIKE #{q.keyword} ESCAPE '!'";
        if(!q.ids().isEmpty()) filter += " AND x.id IN <foreach collection='q.ids' item='id' open='(' close=')' separator=','>#{id}</foreach>";
        return "<script>" + (q.sourceId()==null ? "" : REACH) + (count ? "SELECT COUNT(*)" : "SELECT x.id,x.name,x.role_kind") + " FROM ("+source+") x WHERE"+filter+(count ? "" : " ORDER BY x.id LIMIT #{q.size} OFFSET #{q.offset}")+"</script>";
    }
    /** <p>可信租户和本人来源解析后的选择器参数。</p> @author jy @since 1.0.0 */
    public record Query(BigInteger tenantId,BigInteger memberId,BigInteger sourceId,TenantAssignmentOptionKind kind,
        String keyword,List<BigInteger> ids,int size,int offset) { }
    /** <p>当前角色和主体候选。</p> @author jy @since 1.0.0 */
    public record Option(BigInteger id,String name,com.ingot.framework.commons.model.iam.RoleKind roleKind) { }
}
