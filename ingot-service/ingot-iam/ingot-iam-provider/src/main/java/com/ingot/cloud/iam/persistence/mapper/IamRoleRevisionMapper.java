package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;
import java.util.List;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.IamRoleRevisionJoin;
import com.ingot.cloud.iam.persistence.entity.IamRoleRevisionEntity;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_role_revision，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamRoleRevisionMapper extends BaseMapper<IamRoleRevisionEntity> {
    /**
     * 锁定租户域启用的系统角色版本，调用方须处于事务中。
     * @param id 角色版本 ID
     * @param kind 期望角色种类
     * @param domain 期望授权域
     * @return 命中的版本 ID，不存在时为空
     */
    @Select("""
            SELECT r.id FROM iam_role_revision r JOIN iam_role_definition d ON d.id=r.role_id
             WHERE r.id=#{id} AND r.kind=#{kind} AND d.kind=#{kind} AND d.domain=#{domain}
               AND d.tenant_id IS NULL AND d.enabled=TRUE FOR UPDATE
            """)
    BigInteger lockTenantSystem(@Param("id") BigInteger id, @Param("kind") RoleKind kind,
                                @Param("domain") AuthorizationDomain domain);

    /**
     * 读取启用角色定义上的版本及其基础版本。
     *
     * @param id 角色版本 ID
     * @return 命中行；定义停用时为空
     */
    @Select("""
            SELECT r.id,r.base_revision_id FROM iam_role_revision r
              JOIN iam_role_definition d ON d.id=r.role_id
             WHERE r.id=#{id} AND d.enabled=TRUE
            """)
    List<AuthorizationEvalRows.Revision> listEnabled(@Param("id") BigInteger id);

    /**
     * 读取角色版本及其定义的域、租户和启停状态。
     * @param id 角色版本 ID
     * @return 联查投影，不存在时为空
     */
    @Select("""
            SELECT r.id,r.kind,d.enabled,d.domain,d.tenant_id FROM iam_role_revision r
              JOIN iam_role_definition d ON d.id=r.role_id
             WHERE r.id=#{id}
            """)
    IamRoleRevisionJoin findWithDefinition(@Param("id") BigInteger id);
}
