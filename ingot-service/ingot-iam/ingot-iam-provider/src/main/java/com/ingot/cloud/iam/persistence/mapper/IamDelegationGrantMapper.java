package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamDelegationGrantEntity;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_delegation_grant，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamDelegationGrantMapper extends BaseMapper<IamDelegationGrantEntity> {
    /**
     * 锁定平台域委派行，调用方须处于事务中。
     * @param id 委派 ID
     * @param domain 期望授权域
     * @return 命中的委派 ID，不存在时为空
     */
    @Select("SELECT id FROM iam_delegation_grant WHERE id=#{id} AND domain=#{domain} AND tenant_id IS NULL FOR UPDATE")
    BigInteger lockPlatform(@Param("id") BigInteger id, @Param("domain") AuthorizationDomain domain);

    /**
     * 锁定租户域委派行，调用方须处于事务中并提供可信 tenantId。
     * @param id 委派 ID
     * @param domain 期望授权域
     * @param tenantId 已授权租户 ID
     * @return 命中的委派 ID，不存在时为空
     */
    @Select("""
            SELECT id FROM iam_delegation_grant
             WHERE id=#{id} AND domain=#{domain} AND tenant_id=#{tenantId} FOR UPDATE
            """)
    BigInteger lockTenant(@Param("id") BigInteger id, @Param("domain") AuthorizationDomain domain,
                          @Param("tenantId") BigInteger tenantId);
}
