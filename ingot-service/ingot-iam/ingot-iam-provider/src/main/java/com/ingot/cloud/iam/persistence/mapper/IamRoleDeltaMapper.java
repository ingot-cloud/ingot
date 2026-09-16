package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;
import java.util.List;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamRoleDeltaEntity;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_role_delta，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamRoleDeltaMapper extends BaseMapper<IamRoleDeltaEntity> {
    /**
     * 读取指定角色版本相对基础的单操作差异。
     *
     * @param revisionId 角色版本 ID
     * @return 差异列表
     */
    @Select("SELECT action_id,operation,scopes FROM iam_role_delta WHERE revision_id=#{id}")
    List<AuthorizationEvalRows.Delta> listByRevision(@Param("id") BigInteger revisionId);

    /**
     * 删除指定角色全部版本的差异行。
     * @param roleId 角色定义 ID
     * @return 删除行数
     */
    @Delete("DELETE FROM iam_role_delta WHERE revision_id IN (SELECT id FROM iam_role_revision WHERE role_id=#{roleId})")
    int deleteByRoleId(@Param("roleId") BigInteger roleId);
}
