package com.ingot.cloud.iam.authorization.snapshot;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;

import com.ingot.cloud.iam.api.model.dto.authorization.AuthorizationSnapshotDTO;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationSnapshotLoader;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;

/**
 * <p>IAM 进程内按当前认证成员从 {@link AuthorizationEvaluator} 组装授权快照，避免自调用 Feign。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Primary
@Service
@RequiredArgsConstructor
public class LocalAuthorizationSnapshotLoader implements AuthorizationSnapshotLoader {

    private final AuthorizationEvaluator evaluator;

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthorizationSnapshotDTO load(long tenantId, long userId) {
        return assemble(tenantId, userId);
    }

    /**
     * 按当前认证上下文组装快照；请求身份与上下文不一致时拒绝。
     *
     * @param requestedTenantId 可选租户；平台上下文对外为 {@code 0}
     * @param requestedUserId 可选账号；必须等于当前认证用户
     * @return 仅含精确 ACTION 的快照
     * @throws AuthorizationDeniedException 无认证上下文或身份不一致
     */
    public AuthorizationSnapshotDTO assemble(Long requestedTenantId, Long requestedUserId) {
        InUser user = SecurityAuthContext.getUser();
        AuthorizationContext context = user == null ? null : user.getAuthorizationContext();
        if (user == null || user.getId() == null || context == null) {
            throw new AuthorizationDeniedException("AuthorizationDenied");
        }
        String contextTenant = context.tenantId() == null ? "0" : context.tenantId();
        if (requestedTenantId != null && !Objects.equals(requestedTenantId.toString(), contextTenant)) {
            throw new AuthorizationDeniedException("AuthorizationDenied");
        }
        if (requestedUserId != null && !requestedUserId.equals(user.getId())) {
            throw new AuthorizationDeniedException("AuthorizationDenied");
        }
        AuthorizationEvaluator.AuthorizationView view = evaluator.evaluate(context);
        AuthorizationSnapshotDTO snapshot = new AuthorizationSnapshotDTO();
        snapshot.setTenantId(context.tenantId() == null ? 0L : Long.parseLong(context.tenantId()));
        snapshot.setUserId(user.getId());
        snapshot.setPermissionCodes(new LinkedHashSet<>(view.actionCodes()));
        snapshot.setSource("REMOTE");
        snapshot.setVersion(Instant.now().toEpochMilli());
        snapshot.setGeneratedAt(Instant.now());
        snapshot.setExpiresAt(view.expiresAt());
        return snapshot;
    }
}
