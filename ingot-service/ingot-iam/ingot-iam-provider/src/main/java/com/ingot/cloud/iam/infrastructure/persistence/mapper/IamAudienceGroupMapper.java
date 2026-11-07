package com.ingot.cloud.iam.infrastructure.persistence.mapper;

import java.math.BigInteger;
import java.util.List;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.infrastructure.persistence.IamPersistence;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamAudienceGroupEntity;
import com.ingot.cloud.iam.infrastructure.persistence.projection.AudienceBindingRows;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_audience_group，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamAudienceGroupMapper extends BaseMapper<IamAudienceGroupEntity> {

    /**
     * 按绑定行连接用户组名称，目标缺失时仍返回绑定 ID。
     *
     * @param tenantId 已授权租户 ID
     * @param applicationId 应用 ID
     * @return 按组 ID 排序的绑定展示行
     */
    @Select("""
            SELECT ag.group_id AS id, g.name AS name
              FROM iam_audience_group ag
              LEFT JOIN iam_tenant_group g ON g.id = ag.group_id AND g.tenant_id = ag.tenant_id
             WHERE ag.tenant_id = #{tenantId} AND ag.application_id = #{applicationId}
             ORDER BY ag.group_id
            """)
    List<AudienceBindingRows.Group> listBound(@Param("tenantId") BigInteger tenantId,
                                              @Param("applicationId") BigInteger applicationId);
}
