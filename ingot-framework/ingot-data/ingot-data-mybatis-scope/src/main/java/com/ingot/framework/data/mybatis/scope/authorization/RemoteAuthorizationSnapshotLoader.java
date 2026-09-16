package com.ingot.framework.data.mybatis.scope.authorization;

import com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationSnapshotRequest;
import com.ingot.cloud.iam.api.rpc.RemoteIamAuthorizationService;
import com.ingot.framework.cache.spi.RemoteUnavailableException;
import com.ingot.framework.commons.model.support.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>经 IAM 内部接口拉取授权快照；RPC 失败视为远端不可用，不得 fail-open。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteAuthorizationSnapshotLoader implements AuthorizationSnapshotLoader {

    private final RemoteIamAuthorizationService remoteIamAuthorizationService;

    @Override
    public AuthorizationSnapshotDTO load(long tenantId, long userId) {
        AuthorizationSnapshotRequest request = new AuthorizationSnapshotRequest();
        request.setTenantId(tenantId);
        request.setUserId(userId);
        try {
            R<AuthorizationSnapshotDTO> response = remoteIamAuthorizationService.snapshot(request);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new RemoteUnavailableException("authorization snapshot rpc failed");
            }
            return response.getData();
        } catch (RemoteUnavailableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("[AuthorizationSnapshot] remote load failed tenantId={} userId={}", tenantId, userId, ex);
            throw new RemoteUnavailableException("authorization snapshot rpc error", ex);
        }
    }
}
