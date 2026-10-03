package com.ingot.cloud.iam.persistence.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import java.math.BigInteger;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;

/**
 * <p>租户范围对象候选；所有对象查询明确绑定当前租户。</p>
 * @author jy
 * @since 1.0.0
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface TenantScopeCandidateMapper {
    /**
     * 查询角色操作所属的可信应用和资源编码。
     * @param ids 角色版本合成后的操作标识
     * @return 所属资源
     */
    @Select("""
            <script>SELECT a.id,app.code AS application_code,r.code AS resource_code,r.name AS resource_name
              FROM iam_action a JOIN iam_application app ON app.id=a.application_id
              JOIN iam_resource r ON r.id=a.resource_id
             WHERE app.domain='TENANT' AND app.enabled=TRUE AND r.enabled=TRUE AND a.enabled=TRUE
               AND a.id IN <foreach collection="ids" item="id" open="(" close=")" separator=",">#{id}</foreach></script>
            """)
    List<ActionResource> actions(@Param("ids") List<BigInteger> ids);

    /**
     * 查询一页当前租户可用对象。
     * @param query 已解析的固定对象适配器和可信租户
     * @return 候选
     */
    @SelectProvider(type = TenantScopeCandidateSql.class, method = "page")
    List<Candidate> page(@Param("q") TenantScopeCandidateSql.Query query);

    /**
     * 计算相同筛选的候选数。
     * @param query 已解析的固定对象适配器和可信租户
     * @return 总数
     */
    @SelectProvider(type = TenantScopeCandidateSql.class, method = "count")
    long count(@Param("q") TenantScopeCandidateSql.Query query);

    /**
     * 为当前页已过滤部门批量读取同租户祖先名称路径。
     * @param tenantId 可信租户 ID
     * @param ids 当前页部门 ID
     * @return 名称路径
     */
    @Select("""
            <script>WITH RECURSIVE candidate_ancestors(leaf_id,parent_id,label_path,depth) AS (
              SELECT d.id AS leaf_id,d.parent_id,CAST(d.name AS CHAR(4096)) AS label_path,0 AS depth
                FROM iam_department d WHERE d.tenant_id=#{tenantId}
                 AND d.id IN <foreach collection="ids" item="id" open="(" close=")" separator=",">#{id}</foreach>
              UNION ALL
              SELECT n.leaf_id,p.parent_id,CONCAT(p.name,' / ',n.label_path),n.depth+1
                FROM candidate_ancestors n JOIN iam_department p ON p.id=n.parent_id AND p.tenant_id=#{tenantId}
               WHERE n.depth&lt;32
            )
            SELECT leaf_id,label_path AS ancestor_path FROM (
              SELECT leaf_id,label_path,ROW_NUMBER() OVER (PARTITION BY leaf_id ORDER BY depth DESC) AS rn
                FROM candidate_ancestors
            ) ranked WHERE rn=1</script>
            """)
    List<TreePath> departmentPaths(@Param("tenantId") long tenantId, @Param("ids") List<BigInteger> ids);

    /** 操作资源投影。 */
    record ActionResource(BigInteger id, String applicationCode, String resourceCode, String resourceName) { }
    /** 对象名称投影。 */
    record Candidate(BigInteger id, String name, BigInteger parentId, Boolean hasChildren) { }
    /** 已可见部门的名称路径。 */
    record TreePath(BigInteger leafId, String ancestorPath) { }
}
