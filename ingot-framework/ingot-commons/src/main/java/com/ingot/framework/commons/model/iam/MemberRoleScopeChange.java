package com.ingot.framework.commons.model.iam;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>调整成员直接分配的具体对象，保持固定版本和有效期。</p>
 * @author jy
 * @since 1.0.0
 * @param assignmentId 分配 ID
 * @param expectedVersion 读取的分配版本
 * @param scopeBindings 完整范围参数
 */
@Schema(description = "调整成员直接分配的具体对象，保持固定版本和有效期")
public record MemberRoleScopeChange(
        @Schema(description = "分配 ID") @NotBlank String assignmentId,
        @Schema(description = "读取的分配版本") @NotBlank String expectedVersion,
        @Schema(description = "完整范围参数") @NotNull Map<@NotBlank String, @NotNull @Valid ScopeBinding> scopeBindings) {
    /** 固定范围草稿。 */
    public MemberRoleScopeChange {
        scopeBindings = scopeBindings == null ? null : Map.copyOf(scopeBindings);
    }
}
