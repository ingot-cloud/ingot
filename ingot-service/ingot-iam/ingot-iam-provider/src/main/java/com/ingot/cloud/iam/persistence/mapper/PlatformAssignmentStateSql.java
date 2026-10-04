package com.ingot.cloud.iam.persistence.mapper;

/**
 * <p>统一平台分配展示与需要来源校验的分页筛选 SQL。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class PlatformAssignmentStateSql {

    /**
     * 平台分配 ra、角色定义 d 的来源有效性；保留 MyBatis script 的 XML 转义。
     */
    public static final String SOURCE_VALID = """
            CASE WHEN d.enabled=FALSE THEN FALSE WHEN ra.delegation_grant_id IS NULL THEN TRUE
            ELSE EXISTS(SELECT 1 FROM iam_delegation_grant dg
              WHERE dg.id=ra.delegation_grant_id AND dg.status='ACTIVE'
                AND (dg.valid_from IS NULL OR dg.valid_from&lt;=CURRENT_TIMESTAMP)
                AND (dg.valid_until IS NULL OR dg.valid_until&gt;CURRENT_TIMESTAMP)
                AND (dg.valid_from IS NULL OR ra.valid_from&gt;=dg.valid_from)
                AND (dg.valid_until IS NULL OR ra.valid_until&lt;=dg.valid_until OR (ra.valid_until IS NULL AND dg.assignment_duration_mode='UNLIMITED'))
                AND (dg.assignment_duration_mode='UNLIMITED' OR (ra.valid_until IS NOT NULL AND TIMESTAMPDIFF(MICROSECOND,ra.valid_from,ra.valid_until)&lt;=dg.max_assignment_duration_seconds*1000000+FLOOR(dg.max_assignment_duration_nanos/1000)))
                AND EXISTS(SELECT 1 FROM iam_delegation_role_revision dr WHERE dr.delegation_id=dg.id
                  AND dr.revision_id=ra.revision_id)
                AND ((ra.subject_type='MEMBER' AND ra.platform_member_id&lt;&gt;dg.platform_administrator_id AND EXISTS(SELECT 1 FROM iam_delegation_recipient_member rm
                  WHERE rm.delegation_id=dg.id AND rm.platform_member_id=ra.platform_member_id))
                OR (ra.subject_type='GROUP' AND NOT EXISTS(SELECT 1 FROM iam_platform_group_member self_member
                  WHERE self_member.group_id=ra.platform_group_id AND self_member.member_id=dg.platform_administrator_id)
                  AND EXISTS(SELECT 1 FROM iam_platform_group_member gm
                  WHERE gm.group_id=ra.platform_group_id)
                  AND NOT EXISTS(SELECT 1 FROM iam_platform_group_member gm
                    WHERE gm.group_id=ra.platform_group_id AND NOT EXISTS(
                      SELECT 1 FROM iam_delegation_recipient_member rm
                       WHERE rm.delegation_id=dg.id AND rm.platform_member_id=gm.member_id))))) END
            """;

    /**
     * 来源有效性分页条件，绑定布尔值；仅用于未撤销且未到期的分配，不引入展示关联。
     */
    public static final String SOURCE_FILTER = ("EXISTS (SELECT 1 FROM iam_role_revision r "
            + "JOIN iam_role_definition d ON d.id=r.role_id "
            + "WHERE r.id=iam_role_assignment.revision_id AND (" + SOURCE_VALID + ")={0})")
            .replace("ra.", "iam_role_assignment.").replace("&lt;", "<").replace("&gt;", ">");

    private PlatformAssignmentStateSql() {
    }
}
