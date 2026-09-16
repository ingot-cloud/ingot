package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamPlatformGroupEntity;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_platform_group，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamPlatformGroupMapper extends BaseMapper<IamPlatformGroupEntity> {
    /**
     * 锁定平台用户组，调用方须处于事务中。
     * @param id 组 ID
     * @return 命中行；不存在时为空
     */
    @Select("SELECT id,name,description,version FROM iam_platform_group WHERE id=#{id} FOR UPDATE")
    IamPlatformGroupEntity lockRow(@Param("id") BigInteger id);
}
