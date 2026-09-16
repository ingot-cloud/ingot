package com.ingot.framework.data.mybatis.scope.authorization;

import com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationSnapshotDTO;

/**
 * <p>按租户与用户加载有效授权快照。IAM 进程内走本地解析，其它服务走内部 RPC。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface AuthorizationSnapshotLoader {

    /**
     * 加载指定租户成员的授权快照。
     *
     * @param tenantId 租户 ID
     * @param userId   用户 ID
     * @return 快照；合法空授权返回空集合字段，不得返回 {@code null} 冒充故障
     */
    AuthorizationSnapshotDTO load(long tenantId, long userId);
}
