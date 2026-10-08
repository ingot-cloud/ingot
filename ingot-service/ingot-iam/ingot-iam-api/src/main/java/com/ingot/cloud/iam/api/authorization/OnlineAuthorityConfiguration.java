package com.ingot.cloud.iam.api.authorization;

import com.ingot.cloud.iam.api.EnableAPIConfiguration;
import com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationSnapshotRequest;
import com.ingot.cloud.iam.api.rpc.RemoteIamAuthorizationService;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.security.oauth2.server.resource.access.expression.TrustedAuthoritySource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * <p>为未接入数据范围 SDK 的 Auth/BFF 装配在线准入，已有 IAM 本地或 SDK 来源优先。</p>
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration(after = EnableAPIConfiguration.class)
public class OnlineAuthorityConfiguration {
    /**
     * 按当前可信身份查询最新账号状态与授权，不跨请求缓存。
     * @param remote 可信内部快照 RPC
     * @return 在线授权来源
     */
    @Bean
    @ConditionalOnBean(RemoteIamAuthorizationService.class)
    @ConditionalOnMissingBean(TrustedAuthoritySource.class)
    public TrustedAuthoritySource iamOnlineTrustedAuthoritySource(RemoteIamAuthorizationService remote) {
        return new OnlineSnapshotAuthoritySource((tenant, user) -> {
            var request = new AuthorizationSnapshotRequest();
            request.setTenantId(tenant);
            request.setUserId(user);
            var response = remote.snapshot(request);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
            }
            return response.getData();
        });
    }
}
