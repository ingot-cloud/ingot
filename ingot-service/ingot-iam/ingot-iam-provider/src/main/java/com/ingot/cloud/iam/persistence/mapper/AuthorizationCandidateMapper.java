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
                   app.code AS application_code,a.resource_id,r.name AS resource_name,
                   r.code AS resource_code,r.scope_capabilities
              FROM iam_action a JOIN iam_application app ON app.id=a.application_id
              JOIN iam_resource r ON r.id=a.resource_id AND r.application_id=a.application_id
             WHERE app.domain='PLATFORM' AND app.enabled=TRUE AND r.enabled=TRUE AND a.enabled=TRUE
               AND a.id IN <foreach collection="ids" item="id" open="(" close=")" separator=",">#{id}</foreach>
             ORDER BY app.id,r.id,a.id</script>
            """)
    List<ActionRow> actions(@Param("ids") List<BigInteger> ids);

    /** 精确编码批量关联目录，不从码格式推断资源。 @param codes 精确编码 @return 可信元数据 */
    @Select("""
        <script>SELECT a.code,app.domain,app.code AS application_code,r.code AS resource_code
        FROM iam_action a JOIN iam_application app ON app.id=a.application_id
        JOIN iam_resource r ON r.id=a.resource_id AND r.application_id=app.id
        WHERE a.enabled=TRUE AND app.enabled=TRUE AND r.enabled=TRUE AND a.code IN
        <foreach collection="codes" item="code" open="(" close=")" separator=",">#{code}</foreach></script>
        """)
    List<ActionMetadata> actionMetadata(@Param("codes") List<String> codes);
    /** <p>服务器目录元数据。</p> @param code 操作 @param domain 域 @param applicationCode 应用 @param resourceCode 资源 @author jy @since 1.0.0 */
    record ActionMetadata(String code,com.ingot.framework.commons.model.iam.AuthorizationDomain domain,String applicationCode,String resourceCode) { }

    /**
     * 为已过滤的菜单候选批量读取祖先路径，不用于扩大候选集合。
     * @param ids 当前页可披露菜单 ID
     * @return 每个菜单的名称路径
     */
    @Select("""
            <script>WITH RECURSIVE candidate_ancestors(leaf_id,parent_id,application_id,label_path,depth) AS (
              SELECT m.id AS leaf_id,m.parent_id,m.application_id,
                     CAST(m.name AS CHAR(4096)) AS label_path,0 AS depth
                FROM iam_menu m
               WHERE m.id IN <foreach collection="ids" item="id" open="(" close=")" separator=",">#{id}</foreach>
              UNION ALL
              SELECT n.leaf_id,p.parent_id,n.application_id,
                     CONCAT(p.name,' / ',n.label_path),n.depth+1
                FROM candidate_ancestors n JOIN iam_menu p ON p.id=n.parent_id AND p.application_id=n.application_id
               WHERE n.depth&lt;32
            )
            SELECT leaf_id, label_path AS ancestor_path FROM (
              SELECT leaf_id,label_path,ROW_NUMBER() OVER (PARTITION BY leaf_id ORDER BY depth DESC) AS rn
                FROM candidate_ancestors
            ) ranked WHERE rn=1</script>
            """)
    List<TreePath> menuPaths(@Param("ids") List<BigInteger> ids);

    /**
     * 仅为受限菜单树展开可见叶节点的祖先；祖先仍不获得选择资格。
     * @param ids 已通过委派上限验证的菜单 ID
     * @return 叶节点及其同应用祖先 ID
     */
    @Select("""
            <script>WITH RECURSIVE menu_ancestors(id,parent_id,application_id,depth) AS (
              SELECT m.id,m.parent_id,m.application_id,0
                FROM iam_menu m JOIN iam_application a ON a.id=m.application_id
               WHERE a.domain='PLATFORM' AND m.id IN
                 <foreach collection="ids" item="id" open="(" close=")" separator=",">#{id}</foreach>
              UNION ALL
              SELECT p.id,p.parent_id,p.application_id,n.depth+1
                FROM menu_ancestors n JOIN iam_menu p ON p.id=n.parent_id AND p.application_id=n.application_id
               WHERE n.depth&lt;32
            ) SELECT DISTINCT id FROM menu_ancestors</script>
            """)
    List<BigInteger> menuAncestorIds(@Param("ids") List<BigInteger> ids);

    /** 已可见节点的名称路径。 */
    record TreePath(BigInteger leafId, String ancestorPath) { }
    /**
     * <p>操作元数据原始投影，范围能力按框架 JSON 契约解析。</p>
     * @param id 操作
     * @param name 名称
     * @param code 完整操作码
     * @param applicationId 应用
     * @param applicationName 应用名称
     * @param applicationCode 实际关联应用的编码，用于对象适配
     * @param resourceId 资源
     * @param resourceName 资源名称
     * @param resourceCode 实际关联资源的编码，用于对象适配
     * @param scopeCapabilities 范围能力 JSON
     * @author jy
     * @since 1.0.0
     */
    record ActionRow(BigInteger id, String name, String code, BigInteger applicationId, String applicationName,
                     String applicationCode, BigInteger resourceId, String resourceName, String resourceCode,
                     String scopeCapabilities) { }
}
