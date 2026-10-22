package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;
import java.util.List;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.projection.TenantLiveRows;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

/**
 * <p>显式限定组织角色当前有效关联的查询映射，不读取业务版本。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface TenantRoleWorkspaceMapper {
    /**
     * 读取去重后的成员或用户组页。
     *
     * @param query 可信关系边界和分页
     * @return 当前页主体摘要
     */
    @SelectProvider(type = TenantRoleWorkspaceSql.class, method = "page")
    List<Row> page(@Param("q") TenantRoleWorkspaceSql.Query query);

    /**
     * 统计相同边界下的去重主体。
     *
     * @param query 可信关系边界
     * @return 可见主体总数
     */
    @SelectProvider(type = TenantRoleWorkspaceSql.class, method = "count")
    long count(@Param("q") TenantRoleWorkspaceSql.Query query);

    /**
     * 分页读取成员的可披露有效来源。
     *
     * @param query 可信成员关系边界和分页
     * @return 真实分配投影
     */
    @SelectProvider(type = TenantRoleWorkspaceSql.class, method = "sources")
    List<TenantLiveRows.Assignment> sources(@Param("q") TenantRoleWorkspaceSql.Query query);

    /**
     * 统计成员的可披露有效来源。
     *
     * @param query 可信成员关系边界
     * @return 可见有效分配总数
     */
    @SelectProvider(type = TenantRoleWorkspaceSql.class, method = "sourceCount")
    long sourceCount(@Param("q") TenantRoleWorkspaceSql.Query query);

    /**
     * <p>工作区聚合行，来源类型在服务层去重。</p>
     *
     * @param id 主体 ID
     * @param name 显示名称
     * @param sourceTypes JSON 聚合来源类型
     * @param sourceCount 可见来源数
     * @author jy
     * @since 1.0.0
     */
    record Row(BigInteger id, String name, String sourceTypes, long sourceCount) { }
}
