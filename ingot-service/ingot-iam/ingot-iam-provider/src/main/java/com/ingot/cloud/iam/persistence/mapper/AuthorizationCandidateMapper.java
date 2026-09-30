package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;
import java.util.List;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.projection.AuthorizationCandidateRow;
import com.ingot.cloud.iam.persistence.projection.AuthorizationRoleRow;
import com.ingot.framework.commons.model.iam.AuthorizationActionOption;
import org.apache.ibatis.annotations.*;

/**
 * <p>授权配置专用最小候选，分页与计数采用完全相同的可信筛选。</p>
 * @author jy
 * @since 1.0.0
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface AuthorizationCandidateMapper {
    /**
     * 查询一页候选。
     * @param query 可信查询
     * @return 当前页
     */
    @SelectProvider(type = AuthorizationCandidateSql.class, method = "page")
    List<AuthorizationCandidateRow> page(@Param("q") AuthorizationCandidateSql.Query query);
    /**
     * 计数同一候选集合。
     * @param query 可信查询
     * @return 可见总数
     */
    @SelectProvider(type = AuthorizationCandidateSql.class, method = "count")
    long count(@Param("q") AuthorizationCandidateSql.Query query);

    /**
     * 查询一页可分配角色或指定角色的可分配版本。
     * @param query 已通过身份与依据验证的层级查询
     * @return 最小角色树投影
     */
    @SelectProvider(type = AuthorizationCandidateSql.class, method = "rolePage")
    List<AuthorizationRoleRow> rolePage(@Param("q") AuthorizationCandidateSql.RoleQuery query);

    /**
     * 计数与层级列表相同的可见集合。
     * @param query 可信层级查询
     * @return 当前层可见总数
     */
    @SelectProvider(type = AuthorizationCandidateSql.class, method = "roleCount")
    long roleCount(@Param("q") AuthorizationCandidateSql.RoleQuery query);
    /**
     * 批量查询操作的应用及资源元数据。
     * @param ids 操作标识
     * @return 操作投影
     */
    @Select("""
            <script>SELECT a.id,a.name,a.code,a.application_id,app.name AS application_name,
                   a.resource_id,r.name AS resource_name,r.scope_capabilities
              FROM iam_action a JOIN iam_application app ON app.id=a.application_id
              JOIN iam_resource r ON r.id=a.resource_id
             WHERE app.domain='PLATFORM' AND app.enabled=TRUE AND r.enabled=TRUE AND a.enabled=TRUE
               AND a.id IN <foreach collection="ids" item="id" open="(" close=")" separator=",">#{id}</foreach>
             ORDER BY app.id,r.id,a.id</script>
            """)
    List<ActionRow> actions(@Param("ids") List<BigInteger> ids);
    /**
     * <p>操作元数据原始投影，范围能力按框架 JSON 契约解析。</p>
     * @param id 操作
     * @param name 名称
     * @param code 完整操作码
     * @param applicationId 应用
     * @param applicationName 应用名称
     * @param resourceId 资源
     * @param resourceName 资源名称
     * @param scopeCapabilities 范围能力 JSON
     * @author jy
     * @since 1.0.0
     */
    record ActionRow(BigInteger id, String name, String code, BigInteger applicationId, String applicationName,
                     BigInteger resourceId, String resourceName, String scopeCapabilities) { }
}
