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
    @Select("SELECT id,tenant_id AS tenantId,status,version FROM iam_tenant_member WHERE tenant_id=#{tenantId} AND id=#{id} FOR UPDATE")
    IamTenantMemberEntity lock(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id);
}
