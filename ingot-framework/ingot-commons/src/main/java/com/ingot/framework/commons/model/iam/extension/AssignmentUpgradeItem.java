package com.ingot.framework.commons.model.iam.extension;

import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import com.ingot.framework.commons.model.iam.ScopeBinding;

/**
 * <p>一条保留主体、期限和来源的升级草稿。</p>
 *
 * @param id 既有分配ID
 * @param expectedVersion 当前分配版本
 * @param scopeBindings 范围绑定，省略按语义复用
 * @author jy
 * @since 1.0.0
 */
public record AssignmentUpgradeItem(@NotBlank String id, @NotBlank String expectedVersion,
        Map<String, @Valid ScopeBinding> scopeBindings) {
    /**
     * 复制非空草稿。
     */
    public AssignmentUpgradeItem {
        scopeBindings = scopeBindings == null ? null : Map.copyOf(scopeBindings);
    }
}
