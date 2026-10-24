package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;
import java.util.List;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamAudienceMemberEntity;
import com.ingot.cloud.iam.persistence.projection.AudienceBindingRows;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_audience_member，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamAudienceMemberMapper extends BaseMapper<IamAudienceMemberEntity> {

    /**
     * 按绑定行连接成员资料，目标缺失时仍返回绑定 ID。
     *
     * @param tenantId 已授权租户 ID
     * @param applicationId 应用 ID
     * @return 按成员 ID 排序的绑定展示行
     */
    @Select("""
            SELECT am.member_id AS id, m.display_name AS name, m.avatar AS avatar
              FROM iam_audience_member am
              LEFT JOIN iam_tenant_member m ON m.id = am.member_id AND m.tenant_id = am.tenant_id
             WHERE am.tenant_id = #{tenantId} AND am.application_id = #{applicationId}
             ORDER BY am.member_id
            """)
    List<AudienceBindingRows.Member> listBound(@Param("tenantId") BigInteger tenantId,
                                               @Param("applicationId") BigInteger applicationId);
}
