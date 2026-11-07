package com.ingot.cloud.iam.infrastructure.persistence.mapper;

import java.math.BigInteger;
import java.util.List;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.infrastructure.persistence.IamPersistence;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamAudienceDepartmentEntity;
import com.ingot.cloud.iam.infrastructure.persistence.projection.AudienceBindingRows;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_audience_department，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamAudienceDepartmentMapper extends BaseMapper<IamAudienceDepartmentEntity> {

    /**
     * 按绑定行连接部门名称，含下级标记取自绑定行。
     *
     * @param tenantId 已授权租户 ID
     * @param applicationId 应用 ID
     * @return 按部门 ID 排序的绑定展示行
     */
    @Select("""
            SELECT ad.department_id AS id, d.name AS name, ad.include_descendants AS includeDescendants
              FROM iam_audience_department ad
              LEFT JOIN iam_department d ON d.id = ad.department_id AND d.tenant_id = ad.tenant_id
             WHERE ad.tenant_id = #{tenantId} AND ad.application_id = #{applicationId}
             ORDER BY ad.department_id
            """)
    List<AudienceBindingRows.Department> listBound(@Param("tenantId") BigInteger tenantId,
                                                   @Param("applicationId") BigInteger applicationId);
}
