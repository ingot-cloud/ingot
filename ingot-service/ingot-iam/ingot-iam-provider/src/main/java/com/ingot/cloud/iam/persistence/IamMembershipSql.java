package com.ingot.cloud.iam.persistence;

/**
 * <p>集中定义按部门任职展开成员归属的递归 SQL 片段，保证授权求值、开通判定、人数统计与委派接收人使用同一套规则。</p>
 *
 * <p>组、人群和委派接收人都可以按部门授予，部门规则可声明是否连带下级。成员归属不做物化，
 * 每次求值都从任职关系与部门树推导，任职变化立即生效。片段是编译期常量，
 * 供 {@code @Select} 拼接；使用时必须绑定 {@code tenantId} 与 {@code memberId}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class IamMembershipSql {
    /**
     * 成员视角的部门公共表 {@code member_reach}：本人任职部门及其全部上级。
     *
     * <p>{@code direct} 标记该部门是否为成员本人的任职部门，部门规则命中条件为
     * {@code direct OR include_descendants}：本人任职部门总是命中，上级部门只在规则连带下级时命中。
     * 必须作为整段 SQL 的开头，后续可追加 {@link #MEMBER_GROUPS}。</p>
     */
    public static final String DEPARTMENT_REACH = """
            WITH RECURSIVE member_reach (id, direct) AS (
                SELECT md.department_id,TRUE FROM iam_member_department md
                 WHERE md.tenant_id=#{tenantId} AND md.member_id=#{memberId}
                UNION
                SELECT d.parent_id,FALSE FROM member_reach r
                  JOIN iam_department d ON d.tenant_id=#{tenantId} AND d.id=r.id
                 WHERE d.parent_id IS NOT NULL
            )
            """;
    /**
     * 成员所属租户组的公共表 {@code member_groups}：显式成员并入按部门授予的组。
     *
     * <p>以逗号开头，只能紧跟 {@link #DEPARTMENT_REACH} 使用。</p>
     */
    public static final String MEMBER_GROUPS = """
            , member_groups (id) AS (
                SELECT gm.group_id FROM iam_tenant_group_member gm
                 WHERE gm.tenant_id=#{tenantId} AND gm.member_id=#{memberId}
                UNION
                SELECT gd.group_id FROM iam_tenant_group_department gd
                  JOIN member_reach r ON r.id=gd.department_id
                 WHERE gd.tenant_id=#{tenantId} AND (r.direct OR gd.include_descendants)
            )
            """;
    /**
     * 成员所属租户组的判定条件，配合 {@link #MEMBER_GROUPS} 使用。
     *
     * <p>以空格开头：文本块会去掉行尾空白，拼接处不能依赖前一段的尾随空格。</p>
     */
    public static final String IN_MEMBER_GROUPS = " IN (SELECT id FROM member_groups)";
    /**
     * 委派接收人判定条件：接收成员名单直接命中，或接收部门按 {@link #DEPARTMENT_REACH} 命中任职关系。
     *
     * <p>以 {@code AND} 开头，供求值 SQL 在已定位委派行别名 {@code d} 后追加；
     * 只适用于租户域，平台委派没有部门维度。</p>
     */
    public static final String AND_TENANT_RECIPIENT_REACHED = """
             AND (EXISTS (
                    SELECT 1 FROM iam_delegation_recipient_member rm
                     WHERE rm.delegation_id=d.id AND rm.tenant_id=#{tenantId}
                       AND rm.tenant_member_id=#{memberId})
                  OR EXISTS (
                    SELECT 1 FROM iam_delegation_recipient_department rd
                      JOIN member_reach r ON r.id=rd.department_id
                     WHERE rd.delegation_id=d.id AND rd.tenant_id=#{tenantId}
                       AND (r.direct OR rd.include_descendants)))
            """;
    /**
     * 平台委派接收人判定条件：只按接收成员名单命中。
     *
     * <p>以 {@code AND} 开头，供求值 SQL 在已定位委派行别名 {@code d} 后追加。</p>
     */
    public static final String AND_PLATFORM_RECIPIENT_REACHED = """
             AND EXISTS (
                    SELECT 1 FROM iam_delegation_recipient_member rm
                     WHERE rm.delegation_id=d.id AND rm.platform_member_id=#{memberId})
            """;
    /**
     * 派生授权的角色版本仍在委派白名单内的判定条件。
     *
     * <p>以 {@code AND} 开头，供求值 SQL 在已定位委派行别名 {@code d} 与分配行别名 {@code ra} 后追加。</p>
     */
    public static final String AND_REVISION_STILL_ALLOWED = """
             AND EXISTS (
                    SELECT 1 FROM iam_delegation_role_revision dr
                     WHERE dr.delegation_id=d.id AND dr.revision_id=ra.revision_id)
            """;

    /**
     * 组视角的有效成员集合：显式成员并入部门来源成员，连带下级的部门规则沿部门树向下展开后去重。
     *
     * <p>整段是完整的 {@code SELECT member_id ...}，可直接使用或包在 {@code SELECT COUNT(*) FROM (...)} 中；
     * 使用时必须绑定 {@code tenantId} 与 {@code groupId}。</p>
     */
    public static final String GROUP_MEMBERSHIP = """
            WITH RECURSIVE group_reach (id, deep) AS (
                SELECT gd.department_id,gd.include_descendants FROM iam_tenant_group_department gd
                 WHERE gd.tenant_id=#{tenantId} AND gd.group_id=#{groupId}
                UNION
                SELECT d.id,TRUE FROM group_reach g
                  JOIN iam_department d ON d.tenant_id=#{tenantId} AND d.parent_id=g.id
                 WHERE g.deep
            )
            SELECT gm.member_id FROM iam_tenant_group_member gm
             WHERE gm.tenant_id=#{tenantId} AND gm.group_id=#{groupId}
            UNION
            SELECT md.member_id FROM iam_member_department md
              JOIN group_reach g ON g.id=md.department_id
             WHERE md.tenant_id=#{tenantId}
            """;

    private IamMembershipSql() { }
}
