package com.ingot.cloud.iam.persistence.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamTenantEntity;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.math.BigInteger;

/**
 * <p>映射iam_tenant，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamTenantMapper extends BaseMapper<IamTenantEntity> {
    /**
     * 锁定未删除的指定组织，调用方须处于事务中。
     * @param id 已通过授权检查的组织 ID
     * @return 组织记录，不存在时为空
     */
    @Select("SELECT id,name,avatar,owner_member_id AS ownerMemberId,enabled,version FROM iam_tenant WHERE id=#{id} AND deleted_at IS NULL FOR UPDATE")
    IamTenantEntity lockActive(@Param("id") BigInteger id);

    /**
     * 按 ID 锁定组织行，不附加删除过滤，供成员写事务读取所有者。
     * @param id 组织 ID
     * @return 组织记录，不存在时为空
     */
    @Select("SELECT id,owner_member_id AS ownerMemberId FROM iam_tenant WHERE id=#{id} FOR UPDATE")
    IamTenantEntity lock(@Param("id") BigInteger id);

    /**
     * 读取组织当前版本，不附加其它列以免夹具缺列。
     * @param id 组织 ID
     * @return 版本；不存在时为空
     */
    @Select("SELECT version FROM iam_tenant WHERE id=#{id}")
    BigInteger currentVersion(@Param("id") BigInteger id);
}

