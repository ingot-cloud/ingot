package com.ingot.framework.commons.model.iam.extension;

import java.util.Map;
import java.time.Instant;
import com.ingot.framework.commons.model.iam.AuthorizationContext;

/**
 * <p>
 * 同一身份与求值版本的资源授权，不携带敏感字段原值。
 * </p>
 *
 * @param resource 完整资源键
 * @param context 从认证恢复并验证的身份
 * @param actions 精确操作结论
 * @param version 求值版本指纹
 * @param expiresAt UTC授权截止
 * @author jy
 * @since 1.0.0
 */
public record AuthorizationDecision(ResourceKey resource, AuthorizationContext context,
        Map<String, ActionDecision> actions, String version, Instant expiresAt) {
    /**
     * 复制操作结论。
     */
    public AuthorizationDecision {
        actions = Map.copyOf(actions);
        if (expiresAt != null) {
            for (var action : actions.values()) {
                if (action.fields() != null) expiresAt = FieldPolicyLifetime.earliest(expiresAt, action.fields().expiresAt());
            }
        }
    }
}
