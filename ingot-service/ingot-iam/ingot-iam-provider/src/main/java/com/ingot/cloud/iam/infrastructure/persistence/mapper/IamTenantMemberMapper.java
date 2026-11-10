package com.ingot.cloud.iam.infrastructure.persistence.mapper;

import java.math.BigInteger;
import java.util.List;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.infrastructure.persistence.IamPersistence;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamTenantMemberEntity;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_tenant_member，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamTenantMemberMapper extends BaseMapper<IamTenantMemberEntity> {
    /**
     * 锁定当前租户成员，调用方须处于事务中并提供可信 tenantId。
     * @param tenantId 已授权租户 ID
     * @param id 租户成员 ID
     * @return 成员记录，不存在时为空
     */
    @Select("SELECT m.id,m.tenant_id AS tenantId,m.status,m.version FROM iam_tenant_member m JOIN iam_account a ON a.id=m.account_id WHERE m.tenant_id=#{tenantId} AND m.id=#{id} AND a.enabled=TRUE AND a.deleted_at IS NULL FOR UPDATE")
    IamTenantMemberEntity lock(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id);

    /**
     * 锁定当前租户成员，包含已移出，不要求账号当前启用。
     *
     * @param tenantId 已授权租户 ID
     * @param id 租户成员 ID
     * @return 成员记录，不存在时为空
     */
    @Select("""
            SELECT m.id,m.tenant_id AS tenantId,m.account_id AS accountId,m.display_name AS displayName,
                   m.avatar,m.phone,m.email,m.status,m.version
              FROM iam_tenant_member m
             WHERE m.tenant_id=#{tenantId} AND m.id=#{id}
             FOR UPDATE
            """)
    IamTenantMemberEntity lockAny(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id);

    /**
     * 按账号锁定当前租户成员资格，包含已移出。
     *
     * @param tenantId 已授权租户 ID
     * @param accountId 全局账号 ID
     * @return 成员记录，不存在时为空
     */
    @Select("""
            SELECT m.id,m.tenant_id AS tenantId,m.account_id AS accountId,m.display_name AS displayName,
                   m.avatar,m.phone,m.email,m.status,m.version
              FROM iam_tenant_member m
             WHERE m.tenant_id=#{tenantId} AND m.account_id=#{accountId}
             FOR UPDATE
            """)
    IamTenantMemberEntity lockByAccount(@Param("tenantId") BigInteger tenantId, @Param("accountId") BigInteger accountId);
    /** 验证本组织真实拥有者与账号当前有效；不使用客户端标志。 */
    @org.apache.ibatis.annotations.Select("""
        SELECT COUNT(*) FROM iam_tenant t JOIN iam_tenant_member m ON m.tenant_id=t.id AND m.id=t.owner_member_id
          JOIN iam_account a ON a.id=m.account_id
         WHERE t.id=#{tenantId} AND m.id=#{memberId} AND t.enabled=TRUE AND t.deleted_at IS NULL
           AND m.status='ACTIVE' AND a.enabled=TRUE AND a.deleted_at IS NULL
        """)
    long currentOwner(@org.apache.ibatis.annotations.Param("tenantId") BigInteger tenantId,
                      @org.apache.ibatis.annotations.Param("memberId") BigInteger memberId);


    /**
     * 一次统计当前页各组的有效成员，在关联内去重后应用同一成员读取范围。
     * @param tenantId 当前可信租户
     * @param groupIds 已分页的组 ID
     * @param scope 已编译的成员读取范围
     * @return 有可见成员的组及人数
     */
    @Select("""
            <script>
            WITH RECURSIVE reach (group_id, department_id, deep) AS (
                SELECT group_id, department_id, include_descendants
                  FROM iam_tenant_group_department
                 WHERE tenant_id=#{tenantId} AND group_id IN
                   <foreach collection="pageGroupIds" item="id" open="(" separator="," close=")">#{id}</foreach>
                UNION
                SELECT r.group_id, d.id, TRUE FROM reach r
                  JOIN iam_department d ON d.tenant_id=#{tenantId} AND d.parent_id=r.department_id
                 WHERE r.deep
            ), membership (group_id, member_id) AS (
                SELECT group_id, member_id FROM iam_tenant_group_member
                 WHERE tenant_id=#{tenantId} AND group_id IN
                   <foreach collection="pageGroupIds" item="id" open="(" separator="," close=")">#{id}</foreach>
                UNION
                SELECT r.group_id, md.member_id FROM reach r
                  JOIN iam_member_department md ON md.tenant_id=#{tenantId} AND md.department_id=r.department_id
            )
            SELECT membership.group_id AS groupId, COUNT(*) AS memberCount
              FROM membership
              JOIN iam_tenant_member ON iam_tenant_member.tenant_id=#{tenantId}
                                    AND iam_tenant_member.id=membership.member_id
            ${ew.customSqlSegment}
             GROUP BY membership.group_id
            </script>
            """)
    List<EffectiveGroupCount> countEffectiveGroups(@Param("tenantId") BigInteger tenantId,
            @Param("pageGroupIds") List<BigInteger> groupIds, @Param(Constants.WRAPPER) Wrapper<IamTenantMemberEntity> scope);

    /**
     * 可见组成员聚合行。
     * @author jy
     * @since 1.0.0
     */
    record EffectiveGroupCount(BigInteger groupId, long memberCount) { }
}
