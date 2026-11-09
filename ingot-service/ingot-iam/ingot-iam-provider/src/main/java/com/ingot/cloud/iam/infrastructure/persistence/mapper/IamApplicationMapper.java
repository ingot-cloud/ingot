package com.ingot.cloud.iam.infrastructure.persistence.mapper;

import java.math.BigInteger;
import java.util.List;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.infrastructure.persistence.IamPersistence;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamApplicationEntity;
import com.ingot.cloud.iam.infrastructure.persistence.projection.ActionCatalogRow;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_application，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamApplicationMapper extends BaseMapper<IamApplicationEntity> {
    /**
     * 锁定指定域的启用应用，调用方须处于事务中。
     * @param id 应用 ID
     * @param domain 期望授权域
     * @return 命中的应用 ID，不存在时为空
     */
    @Select("SELECT id FROM iam_application WHERE id=#{id} AND domain=#{domain} AND enabled=TRUE FOR UPDATE")
    BigInteger lockEnabled(@Param("id") BigInteger id, @Param("domain") AuthorizationDomain domain);

    /**
     * 锁定应用目录行，调用方须处于事务中。
     * @param id 应用 ID
     * @return 命中行；不存在时为空
     */
    @Select("SELECT id,code,domain,name,baseline,enabled,version FROM iam_application WHERE id=#{id} FOR UPDATE")
    IamApplicationEntity lockRow(@Param("id") BigInteger id);

    /**
     * 一次读取应用存在性和有序操作目录；应用存在但目录为空时仍返回应用行。
     *
     * @param id 已通过入口访问检查的应用 ID
     * @param enabledOnly 是否仅返回启用资源及操作
     * @return 按资源 ID 和操作 ID 排序的目录行
     */
    @Select("""
        <script>
        SELECT app.id AS application_id, app.code AS application_code, app.name AS application_name,
               r.id AS resource_id, r.code AS resource_code, r.name AS resource_name,
               a.id AS action_id, a.code AS action_code, a.name AS action_name, a.enabled AS action_enabled
        FROM iam_application app
        LEFT JOIN iam_resource r ON r.application_id = app.id
        <if test="enabledOnly">AND r.enabled = TRUE</if>
        LEFT JOIN iam_action a ON a.application_id = r.application_id AND a.resource_id = r.id
        <if test="enabledOnly">AND a.enabled = TRUE</if>
        WHERE app.id = #{id}
        ORDER BY r.id, a.id
        </script>
        """)
    List<ActionCatalogRow> selectActionCatalog(@Param("id") BigInteger id, @Param("enabledOnly") boolean enabledOnly);
}
