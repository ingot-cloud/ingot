package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_department，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamDepartmentMapper extends BaseMapper<IamDepartmentEntity> {
    /**
     * 锁定当前租户部门，调用方须处于事务中并提供可信 tenantId。
     * @param tenantId 已授权租户 ID
     * @param id 部门 ID
     * @return 命中的部门 ID，不存在时为空
     */
    @Select("SELECT id FROM iam_department WHERE tenant_id=#{tenantId} AND id=#{id} FOR UPDATE")
    BigInteger lock(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id);

    /**
     * 锁定并读取当前租户部门行，调用方须处于事务中。
     * @param tenantId 已授权租户 ID
     * @param id 部门 ID
     * @return 部门记录，不存在时为空
     */
    @Select("SELECT id,tenant_id AS tenantId,parent_id AS parentId,name,sort_order AS sortOrder,version FROM iam_department WHERE tenant_id=#{tenantId} AND id=#{id} FOR UPDATE")
    IamDepartmentEntity lockRow(@Param("tenantId") BigInteger tenantId, @Param("id") BigInteger id);
}
