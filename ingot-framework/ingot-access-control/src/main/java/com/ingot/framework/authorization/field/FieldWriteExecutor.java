package com.ingot.framework.authorization.field;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import com.ingot.framework.authorization.*;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.extension.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * <p>业务加载或锁定目标后，在同一事务内重新检查写权限和实际提交字段。</p>
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class FieldWriteExecutor {
    private final FieldPolicyProvider policies;

    /**
     * 最终门禁，不接受外部传入的预览决策；调用者随后完成同一事务内的更新。
     * @param resource 服务声明资源
     * @param action 精确写操作
     * @param target 已加载真实对象
     * @param submitted 实际提交的逻辑键，包括显式 null
     * @return 本次 fresh 决策
     */
    public AuthorizationDecision require(ResourceKey resource, String action, ScopeTarget target,
            Map<String, ?> submitted) {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("字段写校验必须在业务事务中执行");
        var decision = policies.write(resource, action);
        if (decision == null || decision.context() == null || !resource.equals(decision.resource()) || decision.expiresAt() == null
                || !Instant.now().isBefore(decision.expiresAt()))
            throw new SdkAuthorizationException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        var operation = decision.actions().get(action);
        if (operation == null || !operation.allowed() || target == null
                || !Objects.equals(decision.context().tenantId(), target.tenantId())
                || !ScopeRules.matches(operation.scope(), target))
            throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
        FieldPolicyProcessor.requireWritable(submitted,
                FieldPolicyProcessor.access(FieldPolicyProcessor.forAction(decision, action), target));
        return decision;
    }
}
