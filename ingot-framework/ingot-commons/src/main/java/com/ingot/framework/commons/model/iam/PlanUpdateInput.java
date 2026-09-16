package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>整体替换套餐应用清单，不影响已开通租户。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 套餐读取版本
 * @param plan 待保存套餐内容
 */
@Schema(description = "整体替换套餐应用清单，不影响已开通租户")
public record PlanUpdateInput(
        @NotBlank @Schema(description = "套餐读取版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @NotNull @Valid @Schema(description = "待保存套餐内容", requiredMode = Schema.RequiredMode.REQUIRED)
        PlanDraft plan) {
}
