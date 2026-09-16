package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamRoleDefinitionEntity;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_role_definition，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamRoleDefinitionMapper extends BaseMapper<IamRoleDefinitionEntity> {
    /**
     * 锁定共享角色定义，调用方须处于事务中。
     * @param id 角色 ID
     * @param kind 共享角色种类
     * @return 命中行；不存在时为空
     */
    @Select("""
            SELECT id,name,kind,enabled,version FROM iam_role_definition
             WHERE id=#{id} AND kind=#{kind} AND tenant_id IS NULL FOR UPDATE
            """)
    IamRoleDefinitionEntity lockShared(@Param("id") BigInteger id, @Param("kind") RoleKind kind);

    /**
     * 锁定平台目录可见的角色定义，调用方须处于事务中。
     * @param id 角色 ID
     * @param domain 接口管理域
     * @param systemKind 系统角色种类
     * @param platformCustomKind 平台自定义种类
     * @return 命中行；不存在时为空
     */
    @Select("""
            SELECT id,name,kind,enabled,version FROM iam_role_definition
             WHERE id=#{id} AND ((kind=#{systemKind} AND domain=#{domain}) OR kind=#{platformCustomKind}) FOR UPDATE
            """)
    IamRoleDefinitionEntity lockPlatform(@Param("id") BigInteger id, @Param("domain") AuthorizationDomain domain,
                                         @Param("systemKind") RoleKind systemKind,
                                         @Param("platformCustomKind") RoleKind platformCustomKind);

    /**
     * 锁定租户目录可见的角色定义，调用方须处于事务中并绑定可信 tenantId。
     * @param id 角色 ID
     * @param domain 接口管理域
     * @param tenantId 已授权租户 ID
     * @param systemKind 系统角色种类
     * @param sharedKind 共享角色种类
     * @param tenantCustomKind 租户自定义种类
     * @return 命中行；不存在时为空
     */
    @Select("""
            SELECT id,name,kind,enabled,version FROM iam_role_definition
             WHERE id=#{id} AND ((kind=#{systemKind} AND domain=#{domain} AND tenant_id IS NULL)
                OR kind=#{sharedKind} OR (kind=#{tenantCustomKind} AND tenant_id=#{tenantId})) FOR UPDATE
            """)
    IamRoleDefinitionEntity lockTenant(@Param("id") BigInteger id, @Param("domain") AuthorizationDomain domain,
                                       @Param("tenantId") BigInteger tenantId, @Param("systemKind") RoleKind systemKind,
                                       @Param("sharedKind") RoleKind sharedKind,
                                       @Param("tenantCustomKind") RoleKind tenantCustomKind);
}
