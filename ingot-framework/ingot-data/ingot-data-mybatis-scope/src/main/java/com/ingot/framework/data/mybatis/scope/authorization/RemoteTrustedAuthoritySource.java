package com.ingot.framework.data.mybatis.scope.authorization;

import com.ingot.cloud.iam.api.authorization.OnlineSnapshotAuthoritySource;

/**
 * <p>SDK 在线准入复用 IAM API 的账号状态与权限校验，不依赖数据范围热缓存。</p>
 * @author jy
 * @since 1.0.0
 */
public class RemoteTrustedAuthoritySource extends OnlineSnapshotAuthoritySource {
    /** @param loader 可信在线快照加载器 */
    public RemoteTrustedAuthoritySource(AuthorizationSnapshotLoader loader) {
        super(loader::load);
    }
}
