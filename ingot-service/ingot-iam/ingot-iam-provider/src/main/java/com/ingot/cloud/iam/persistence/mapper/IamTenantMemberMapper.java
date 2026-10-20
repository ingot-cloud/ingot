package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
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

}
