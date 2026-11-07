package com.ingot.cloud.iam.infrastructure.persistence.mapper;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.infrastructure.persistence.IamPersistence;
import com.ingot.cloud.iam.infrastructure.persistence.projection.AuditReferenceRows;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>按页批量读取授权审计涉及对象的名称，所有查询显式绑定可信管理域与租户。</p>
 *
 * @author jy
 * @since 1.0.0
 * @apiNote {@code tenantId} 为空表示平台域；调用方必须传入可信身份上的租户。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface AuditReferenceMapper {
    /** IN 列表片段。 */
    String IDS = "<foreach collection='ids' item='id' open='(' close=')' separator=','>#{id}</foreach>";
    /** 平台域角色归属：平台角色及平台维护的共享角色；租户域：共享角色或本租户私有角色。 */
    String ROLE_OWNER = "<choose><when test='tenantId == null'>d.tenant_id IS NULL</when>"
            + "<otherwise>d.domain='TENANT' AND (d.tenant_id IS NULL OR d.tenant_id=#{tenantId})</otherwise></choose>";

    /**
     * 读取用户组名称。
     *
     * @param tenantId 租户 ID；平台域为空
     * @param ids 用户组 ID
     * @return 存在的用户组
     */
    @Select("<script><choose><when test='tenantId == null'>SELECT id,name FROM iam_platform_group WHERE id IN " + IDS
            + "</when><otherwise>SELECT id,name FROM iam_tenant_group WHERE tenant_id=#{tenantId} AND id IN " + IDS
            + "</otherwise></choose></script>")
    List<AuditReferenceRows.Name> groups(@Param("tenantId") BigInteger tenantId, @Param("ids") Collection<BigInteger> ids);

    /**
     * 读取角色名称。
     *
     * @param tenantId 租户 ID；平台域为空
     * @param ids 角色 ID
     * @return 存在且归属可见的角色
     */
    @Select("<script>SELECT d.id,d.name FROM iam_role_definition d WHERE " + ROLE_OWNER + " AND d.id IN " + IDS
            + "</script>")
    List<AuditReferenceRows.Name> roles(@Param("tenantId") BigInteger tenantId, @Param("ids") Collection<BigInteger> ids);

    /**
     * 读取角色版本及所属角色名称。
     *
     * @param tenantId 租户 ID；平台域为空
     * @param ids 版本 ID
     * @return 存在且归属可见的版本
     */
    @Select("<script>SELECT r.id,d.name AS role_name,r.revision,r.version_name FROM iam_role_revision r "
            + "JOIN iam_role_definition d ON d.id=r.role_id WHERE " + ROLE_OWNER + " AND r.id IN " + IDS + "</script>")
    List<AuditReferenceRows.Revision> revisions(@Param("tenantId") BigInteger tenantId,
                                                @Param("ids") Collection<BigInteger> ids);

    /**
     * 读取角色分配的角色与接收对象；平台分配经固定版本找到角色。
     *
     * @param tenantId 租户 ID；平台域为空
     * @param ids 分配 ID
     * @return 存在的分配
     */
    @Select("<script>SELECT a.id,COALESCE(a.role_id,rv.role_id) AS role_id,a.subject_type,"
            + "COALESCE(a.tenant_member_id,a.platform_member_id) AS member_id,"
            + "COALESCE(a.tenant_group_id,a.platform_group_id) AS group_id "
            + "FROM iam_role_assignment a LEFT JOIN iam_role_revision rv ON rv.id=a.revision_id WHERE "
            + "<choose><when test='tenantId == null'>a.domain='PLATFORM' AND a.tenant_id IS NULL</when>"
            + "<otherwise>a.domain='TENANT' AND a.tenant_id=#{tenantId}</otherwise></choose> AND a.id IN " + IDS
            + "</script>")
    List<AuditReferenceRows.Assignment> assignments(@Param("tenantId") BigInteger tenantId,
                                                    @Param("ids") Collection<BigInteger> ids);

    /**
     * 读取委派管理员。
     *
     * @param tenantId 租户 ID；平台域为空
     * @param ids 委派 ID
     * @return 存在的委派
     */
    @Select("<script>SELECT id,COALESCE(tenant_administrator_id,platform_administrator_id) AS member_id "
            + "FROM iam_delegation_grant WHERE "
            + "<choose><when test='tenantId == null'>domain='PLATFORM' AND tenant_id IS NULL</when>"
            + "<otherwise>domain='TENANT' AND tenant_id=#{tenantId}</otherwise></choose> AND id IN " + IDS
            + "</script>")
    List<AuditReferenceRows.Delegation> delegations(@Param("tenantId") BigInteger tenantId,
                                                    @Param("ids") Collection<BigInteger> ids);

    /**
     * 读取角色版本应用任务。
     *
     * @param tenantId 租户 ID；平台域为空
     * @param ids 任务 ID
     * @return 角色归属可见的任务
     */
    @Select("<script>SELECT t.id,t.role_id,t.target_revision_id AS revision_id FROM iam_role_application_task t "
            + "JOIN iam_role_definition d ON d.id=t.role_id WHERE " + ROLE_OWNER + " AND t.id IN " + IDS + "</script>")
    List<AuditReferenceRows.Task> tasks(@Param("tenantId") BigInteger tenantId, @Param("ids") Collection<BigInteger> ids);

    /**
     * 读取未删除组织名称。
     *
     * @param ids 组织 ID
     * @return 未删除的组织
     */
    @Select("<script>SELECT id,name FROM iam_tenant WHERE deleted_at IS NULL AND id IN " + IDS + "</script>")
    List<AuditReferenceRows.Name> tenants(@Param("ids") Collection<BigInteger> ids);

    /**
     * 读取未删除账号用户名，仅平台域使用。
     *
     * @param ids 账号 ID
     * @return 未删除的账号
     */
    @Select("<script>SELECT id,username AS name FROM iam_account WHERE deleted_at IS NULL AND id IN " + IDS
            + "</script>")
    List<AuditReferenceRows.Name> accounts(@Param("ids") Collection<BigInteger> ids);

    /**
     * 把账号映射为当前租户内的成员。
     *
     * @param tenantId 租户 ID
     * @param ids 账号 ID
     * @return 账号与成员对应
     */
    @Select("<script>SELECT account_id,id AS member_id FROM iam_tenant_member WHERE tenant_id=#{tenantId} "
            + "AND account_id IN " + IDS + "</script>")
    List<AuditReferenceRows.AccountMember> tenantAccountMembers(@Param("tenantId") BigInteger tenantId,
                                                                @Param("ids") Collection<BigInteger> ids);

    /**
     * 读取当前管理域的应用名称。
     *
     * @param domain 管理域字面量
     * @param ids 应用 ID
     * @return 存在的应用
     */
    @Select("<script>SELECT id,name FROM iam_application WHERE domain=#{domain} AND id IN " + IDS + "</script>")
    List<AuditReferenceRows.Name> applications(@Param("domain") String domain, @Param("ids") Collection<BigInteger> ids);
}
