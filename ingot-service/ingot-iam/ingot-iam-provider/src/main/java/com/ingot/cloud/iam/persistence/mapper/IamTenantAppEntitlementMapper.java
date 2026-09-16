package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamMembershipSql;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamTenantAppEntitlementEntity;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.framework.commons.model.iam.AudienceKind;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_tenant_app_entitlement，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamTenantAppEntitlementMapper extends BaseMapper<IamTenantAppEntitlementEntity> {
    /**
     * 统计组织在当前时钟下对指定应用的有效开通行数。
     *
     * @param tenantId 已授权租户 ID
     * @param applicationId 应用 ID
     * @return 命中条数
     */
    @Select("""
            SELECT COUNT(*) FROM iam_tenant_app_entitlement
             WHERE tenant_id=#{tenantId} AND application_id=#{applicationId} AND enabled=TRUE
               AND (valid_from IS NULL OR valid_from<=CURRENT_TIMESTAMP)
               AND (valid_until IS NULL OR valid_until>CURRENT_TIMESTAMP)
            """)
    long countEntitled(@Param("tenantId") BigInteger tenantId, @Param("applicationId") BigInteger applicationId);

    /**
     * 统计当前租户开通且成员落入人群时的命中数与最近开通截止，不读取开通表主键。
     *
     * @param tenantId 已授权租户 ID
     * @param applicationId 应用 ID
     * @param memberId 租户成员 ID
     * @param allKind 全组织人群种类
     * @return 命中条数与最近截止；存在不限期开通时截止为空
     */
    @Select(IamMembershipSql.DEPARTMENT_REACH + IamMembershipSql.MEMBER_GROUPS + """
            SELECT COUNT(*) AS hits,
                   CASE WHEN COUNT(*)>COUNT(e.valid_until) THEN NULL ELSE MIN(e.valid_until) END AS earliest_expiry
              FROM iam_tenant_app_entitlement e
              JOIN iam_app_audience aud ON aud.tenant_id=e.tenant_id AND aud.application_id=e.application_id
             WHERE e.tenant_id=#{tenantId} AND e.application_id=#{applicationId} AND e.enabled=TRUE
               AND aud.enabled=TRUE
               AND (e.valid_from IS NULL OR e.valid_from<=CURRENT_TIMESTAMP)
               AND (e.valid_until IS NULL OR e.valid_until>CURRENT_TIMESTAMP)
               AND (aud.audience_kind=#{allKind}
                    OR EXISTS (
                        SELECT 1 FROM iam_audience_member m
                         WHERE m.tenant_id=e.tenant_id AND m.application_id=e.application_id
                           AND m.member_id=#{memberId})
                    OR EXISTS (
                        SELECT 1 FROM iam_audience_group g
                         WHERE g.tenant_id=e.tenant_id AND g.application_id=e.application_id
                           AND g.group_id """ + IamMembershipSql.IN_MEMBER_GROUPS + """
                        )
                    OR EXISTS (
                        SELECT 1 FROM iam_audience_department d
                          JOIN member_reach r ON r.id=d.department_id
                         WHERE d.tenant_id=e.tenant_id AND d.application_id=e.application_id
                           AND (r.direct OR d.include_descendants)))
            """)
    AuthorizationEvalRows.Entitlement entitlementForMember(@Param("tenantId") BigInteger tenantId,
                                                           @Param("applicationId") BigInteger applicationId,
                                                           @Param("memberId") BigInteger memberId,
                                                           @Param("allKind") AudienceKind allKind);
}
