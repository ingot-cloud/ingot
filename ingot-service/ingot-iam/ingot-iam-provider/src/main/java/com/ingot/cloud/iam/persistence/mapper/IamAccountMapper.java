package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamAccountEntity;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_account，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamAccountMapper extends BaseMapper<IamAccountEntity> {
    /**
     * 锁定启用且未删除的账号，调用方须处于事务中。
     * @param id 已验证的全局账号 ID
     * @return 命中的账号 ID，不存在时为空
     */
    @Select("SELECT id FROM iam_account WHERE id=#{id} AND enabled=TRUE AND deleted_at IS NULL FOR UPDATE")
    BigInteger lockActive(@Param("id") BigInteger id);
}
