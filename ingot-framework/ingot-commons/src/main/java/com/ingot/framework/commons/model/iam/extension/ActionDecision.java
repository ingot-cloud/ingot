package com.ingot.framework.commons.model.iam.extension;

import java.util.List;

/**
 * <p>
 * 精确操作的准入及同一次求值对象范围。
 * </p>
 *
 * @param allowed 能否进入操作
 * @param governed 是否存在独立治理来源
 * @param scope 范围并集；空为无对象
 * @param fields 本精确操作的字段授权；新模型不得缺失
 * @author jy
 * @since 1.0.0
 */
public record ActionDecision(boolean allowed, boolean governed, List<ScopeCondition> scope,
        @jakarta.validation.constraints.NotNull FieldPolicyDecision fields) {
    /**
     * 复制条款。
     */
    public ActionDecision {
        scope = scope == null ? List.of() : List.copyOf(scope);
    }
}
