package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;
import java.util.List;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamRoleAssignmentEntity;
import org.apache.ibatis.annotations.*;

/**
 * <p>显式限定平台角色有效关联和可披露组关系的查询映射。</p>
 * @author jy
 * @since 1.0.0
 */
@Mapper
@InterceptorIgnore(tenantLine=IamPersistence.EXPLICIT_BOUNDARY, dataPermission=IamPersistence.EXPLICIT_BOUNDARY)
public interface RoleWorkspaceMapper {
    /**
     * 读取 SQL 去重后的主体页。
     * @param query 可信关系边界和分页
     * @return 当前页主体摘要
     */
    @SelectProvider(type=RoleWorkspaceSql.class, method="page")
    List<Row> page(@Param("q") RoleWorkspaceSql.Query query);
    /**
     * 统计相同边界下的去重主体。
     * @param query 可信关系边界
     * @return 可见主体总数
     */
    @SelectProvider(type=RoleWorkspaceSql.class, method="count")
    long count(@Param("q") RoleWorkspaceSql.Query query);
    /**
     * 分页读取成员的可披露有效来源。
     * @param query 可信成员关系边界和分页
     * @return 真实分配实体页
     */
    @SelectProvider(type=RoleWorkspaceSql.class, method="sources")
    List<IamRoleAssignmentEntity> sources(@Param("q") RoleWorkspaceSql.Query query);
    /**
     * 统计成员的可披露有效来源。
     * @param query 可信成员关系边界
     * @return 可见有效分配总数
     */
    @SelectProvider(type=RoleWorkspaceSql.class, method="sourceCount")
    long sourceCount(@Param("q") RoleWorkspaceSql.Query query);
    /**
     * <p>内部聚合行，仅当前页摘要在应用层转换。</p>
     * @param id 主体 ID
     * @param name 名称
     * @param revisions JSON 聚合版本号，服务层去重
     * @param sourceTypes JSON 聚合来源类型，服务层去重
     * @param sourceCount 可见来源数
     * @author jy
     * @since 1.0.0
     */
    record Row(BigInteger id, String name, String revisions, String sourceTypes, long sourceCount) { }
}
