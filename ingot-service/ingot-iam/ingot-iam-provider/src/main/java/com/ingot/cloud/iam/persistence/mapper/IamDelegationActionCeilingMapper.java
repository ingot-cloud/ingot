package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;
import java.util.List;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamDelegationActionCeilingEntity;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_delegation_action_ceiling，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamDelegationActionCeilingMapper extends BaseMapper<IamDelegationActionCeilingEntity> {
    /**
     * 读取委派派生授权的操作范围上限。
     *
     * @param delegationId 委派 ID
     * @return 操作、范围与绑定
     */
    @Select("SELECT action_id,scopes,scope_bindings FROM iam_delegation_action_ceiling WHERE delegation_id=#{id}")
    List<AuthorizationEvalRows.Ceiling> listByDelegation(@Param("id") BigInteger delegationId);
}
