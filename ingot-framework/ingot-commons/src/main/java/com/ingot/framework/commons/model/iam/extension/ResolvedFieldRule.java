package com.ingot.framework.commons.model.iam.extension;

import java.util.List;
import jakarta.validation.constraints.*;
import com.ingot.framework.commons.model.iam.FieldAccess;

/**
 * <p>已匹配当前查看者的字段目标范围与限制。</p>
 *
 * @param fieldKey 字段键
 * @param scope 目标范围并集
 * @param access 字段访问限制
 * @author jy
 * @since 1.0.0
 */
public record ResolvedFieldRule(String fieldKey, List<ScopeCondition> scope, FieldAccess access) {
    /**
     * 防御复制。
     */
    public ResolvedFieldRule {
        scope = List.copyOf(scope);
    }
}
