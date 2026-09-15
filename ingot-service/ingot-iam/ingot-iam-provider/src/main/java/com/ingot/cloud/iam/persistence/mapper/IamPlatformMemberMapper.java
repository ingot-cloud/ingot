package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamPlatformMemberEntity;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_platform_member，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamPlatformMemberMapper extends BaseMapper<IamPlatformMemberEntity> {
    /**
     * 锁定平台成员资格，调用方须处于事务中。
     * @param id 平台成员 ID
     * @return 成员记录，不存在时为空
     */
    @Select("SELECT id,status,version FROM iam_platform_member WHERE id=#{id} FOR UPDATE")
    IamPlatformMemberEntity lock(@Param("id") BigInteger id);
}
