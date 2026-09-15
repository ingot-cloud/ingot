package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamDefaultPolicyRevisionEntity;
import com.ingot.framework.commons.model.iam.DefaultPolicyKind;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_default_policy_revision，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamDefaultPolicyRevisionMapper extends BaseMapper<IamDefaultPolicyRevisionEntity> {
    /**
     * 锁定指定类别的默认策略版本，调用方须处于事务中。
     * @param id 策略版本 ID
     * @param kind 期望策略类别
     * @return 命中的版本 ID，不存在时为空
     */
    @Select("SELECT id FROM iam_default_policy_revision WHERE id=#{id} AND kind=#{kind} FOR UPDATE")
    BigInteger lockKind(@Param("id") BigInteger id, @Param("kind") DefaultPolicyKind kind);
}
