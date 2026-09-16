package com.ingot.cloud.iam.persistence.mapper;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ingot.cloud.iam.persistence.IamPersistence;
import com.ingot.cloud.iam.persistence.entity.IamActionEntity;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.framework.data.mybatis.common.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>映射iam_action，由 IAM Repository 显式限定归属，不应用旧租户及权限语义。</p>
 * @author jy
 * @since 1.0.0
 * @apiNote 仅持久化 Repository 使用，租户操作必须在 Wrapper 或具名 SQL 中绑定可信 tenantId。
 */
@Mapper
@InterceptorIgnore(tenantLine = IamPersistence.EXPLICIT_BOUNDARY, dataPermission = IamPersistence.EXPLICIT_BOUNDARY)
public interface IamActionMapper extends BaseMapper<IamActionEntity> {
    /**
     * 锁定指定应用下的操作行，调用方须处于事务中。
     * @param applicationId 所属应用 ID
     * @param id 操作 ID
     * @return 命中行；不存在时为空
     */
    @Select("SELECT name,enabled,version FROM iam_action WHERE application_id=#{applicationId} AND id=#{id} FOR UPDATE")
    IamActionEntity lockRow(@Param("applicationId") BigInteger applicationId, @Param("id") BigInteger id);

    /**
     * 读取操作码及所属应用的启用与域；只选择夹具与求值所需列。
     *
     * @param id 操作 ID
     * @return 操作投影；不存在时为空
     */
    @Select("""
            SELECT a.code,a.enabled,app.domain,app.enabled AS app_enabled,app.id AS application_id
              FROM iam_action a JOIN iam_application app ON app.id=a.application_id
             WHERE a.id=#{id}
            """)
    List<AuthorizationEvalRows.Action> listWithApplication(@Param("id") BigInteger id);

    /**
     * 读取多个操作的引用前提：自身与应用/资源启停、应用授权域及资源范围能力。
     *
     * @param ids 操作 ID，不得为空集合
     * @return 命中操作的能力投影；未命中的 ID 不出现
     */
    @Select("""
            <script>
            SELECT a.id,a.enabled,app.domain,app.enabled AS app_enabled,r.enabled AS resource_enabled,
                   r.scope_capabilities
              FROM iam_action a
              JOIN iam_application app ON app.id=a.application_id
              JOIN iam_resource r ON r.id=a.resource_id
             WHERE a.id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<AuthorizationEvalRows.Capability> listCapabilities(@Param("ids") Collection<BigInteger> ids);
}
