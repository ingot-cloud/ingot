package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamTenantGroupEntity;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_tenant_group，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamTenantGroupMapper extends BaseMapper<IamTenantGroupEntity> {
    /**
     * 锁定当前租户用户组，调用方须处于事务中并绑定可信 tenantId。
     * @param tenantId 已授权租户 ID
     * @param id 组 ID
     * @return 命中行；不存在时为空
     */
    @Select("""
            SELECT id,name,description,version FROM iam_tenant_group
             WHERE tenant_id=#{tenantId} AND id=#{id} FOR UPDATE
            """)
    IamTenantGroupEntity lockRow(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id);
}
