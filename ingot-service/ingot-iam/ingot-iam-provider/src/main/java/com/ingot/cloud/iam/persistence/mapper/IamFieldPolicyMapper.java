package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamFieldPolicyEntity;
import com.ingot.framework.commons.model.iam.DefaultPolicyKind;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>映射iam_field_policy，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamFieldPolicyMapper extends BaseMapper<IamFieldPolicyEntity> {
    /**
     * 按租户插入或整体替换字段策略引用，并递增配置版本。
     *
     * @param tenantId 已授权租户 ID
     * @param revisionId 默认策略版本 ID
     * @param kind 固定为字段类别
     * @return 受影响行数
     */
    @Insert("""
            INSERT INTO iam_field_policy(tenant_id,default_revision_id,default_kind)
            VALUES (#{tenantId},#{revisionId},#{kind})
            ON DUPLICATE KEY UPDATE default_revision_id=#{revisionId},version=version+1
            """)
    int upsert(@Param("tenantId") BigInteger tenantId, @Param("revisionId") BigInteger revisionId,
               @Param("kind") DefaultPolicyKind kind);
}
