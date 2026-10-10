package com.ingot.framework.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.extension.AuthorizationDecision;
import com.ingot.framework.commons.model.iam.extension.AuthorizationRequest;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import com.ingot.framework.commons.model.iam.extension.ScopeTarget;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * 通用操作与对象执行门禁，不提供发号或猜测业务归属。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class AuthorizationAccess {

    private final AuthorizationClient client;

    /**
     * 精确操作准入。
     * @param resource 服务器声明资源
     * @param action 精确编码
     * @return 同次求值
     */
    public AuthorizationDecision require(ResourceKey resource, String action) {
        var result = client.evaluate(new AuthorizationRequest(resource, List.of(action)));
        requireDecision(result, action);
        FieldPolicyProcessor.forAction(result, action);
        return result;
    }

    /** 读取交互能力，写操作最终仍须 require 或 FieldWriteExecutor。 */
    public AuthorizationDecision preview(ResourceKey resource, String action) {
        var result = client.preview(new AuthorizationRequest(resource, List.of(action)));
        requireDecision(result, action);
        if (!resource.equals(result.resource())) throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
        FieldPolicyProcessor.forAction(result, action);
        return result;
    }

    /**
     * 使用同一有效决策检查目标。
     * @param decision 有效决策
     * @param action 精确编码
     * @param target 实际实体目标
     */
    public void requireTarget(AuthorizationDecision decision, String action, ScopeTarget target) {
        requireDecision(decision, action);
        if (target == null || !Objects.equals(decision.context().tenantId(), target.tenantId())
                || !ScopeRules.matches(decision.actions().get(action).scope(), target)) {
            throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
        }
    }

    private static void requireDecision(AuthorizationDecision value, String action) {
        if (value == null || value.expiresAt() == null || !Instant.now().isBefore(value.expiresAt()))
            throw new SdkAuthorizationException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        if (!value.actions().containsKey(action) || !value.actions().get(action).allowed())
            throw new SdkAuthorizationException(IamReasonCode.ACTION_DENIED);
    }

}
