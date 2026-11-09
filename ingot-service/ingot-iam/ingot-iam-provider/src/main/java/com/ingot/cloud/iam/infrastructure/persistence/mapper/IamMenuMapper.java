package com.ingot.cloud.iam.infrastructure.persistence.mapper;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.infrastructure.persistence.IamPersistence;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamMenuEntity;
import com.ingot.cloud.iam.infrastructure.persistence.projection.SessionMenuRow;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_menu，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamMenuMapper extends BaseMapper<IamMenuEntity> {
    /**
     * 锁定指定应用下的菜单行，调用方须处于事务中。
     * @param applicationId 所属应用 ID
     * @param id 菜单 ID
     * @return 命中行；不存在时为空
     */
    @Select("SELECT name,version,hidden,is_cache,props,route_params FROM iam_menu WHERE application_id=#{applicationId} AND id=#{id} FOR UPDATE")
    IamMenuEntity lockRow(@Param("applicationId") BigInteger applicationId, @Param("id") BigInteger id);

    /**
     * 批量读取可访问应用的启用菜单及操作绑定。停用操作仍作为准入条件。
     *
     * @param domain 当前授权域
     * @param applicationIds 已通过访问检查的非空应用 ID 集合
     * @return 菜单和绑定行，无绑定时仍返回菜单
     */
    @Select("""
        <script>
        SELECT m.id, m.application_id, m.parent_id, m.name, m.path, m.view_path, m.route_name,
               m.icon, m.kind, m.match_mode, m.access_mode, m.sort_order, m.hidden, m.is_cache,
               m.props, m.route_params, a.code AS required_action_code
        FROM iam_application app
        JOIN iam_menu m ON m.application_id = app.id AND m.enabled = TRUE
        LEFT JOIN iam_menu_action b ON b.application_id = m.application_id AND b.menu_id = m.id
        LEFT JOIN iam_action a ON a.application_id = b.application_id AND a.id = b.action_id
        WHERE app.domain = #{domain} AND app.enabled = TRUE AND app.id IN
        <foreach collection="applicationIds" item="id" open="(" separator="," close=")">#{id}</foreach>
        </script>
        """)
    List<SessionMenuRow> selectForSession(@Param("domain") AuthorizationDomain domain,
                                         @Param("applicationIds") Collection<Long> applicationIds);
}
