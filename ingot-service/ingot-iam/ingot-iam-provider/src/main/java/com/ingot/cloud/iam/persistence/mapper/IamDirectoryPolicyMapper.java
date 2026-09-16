package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamDirectoryPolicyEntity;
import com.ingot.framework.commons.model.iam.DefaultPolicyKind;
import com.ingot.framework.commons.model.iam.DirectoryDefaultScope;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>映射iam_directory_policy，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamDirectoryPolicyMapper extends BaseMapper<IamDirectoryPolicyEntity> {
    /**
     * 按租户插入或整体替换通讯录策略引用，并递增配置版本。
     *
     * @param tenantId 已授权租户 ID
     * @param revisionId 默认策略版本 ID
     * @param kind 固定为通讯录类别
     * @param scope 默认范围；继承固定版本时为空
     * @param selectorId SELECTED 时的选择器；其余为空
     * @return 受影响行数
     */
    @Insert("""
            INSERT INTO iam_directory_policy(tenant_id,default_revision_id,default_kind,default_scope,default_selector_id)
            VALUES (#{tenantId},#{revisionId},#{kind},#{scope},#{selectorId})
            ON DUPLICATE KEY UPDATE default_revision_id=#{revisionId},default_scope=#{scope},
              default_selector_id=#{selectorId},version=version+1
            """)
    int upsert(@Param("tenantId") BigInteger tenantId, @Param("revisionId") BigInteger revisionId,
               @Param("kind") DefaultPolicyKind kind, @Param("scope") DirectoryDefaultScope scope,
               @Param("selectorId") BigInteger selectorId);
}
