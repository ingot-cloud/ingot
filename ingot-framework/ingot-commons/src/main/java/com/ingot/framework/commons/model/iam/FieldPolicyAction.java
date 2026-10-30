package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>字段策略可选的精确动作。</p>
 *
 * @param code 精确操作码
 * @param name 动作名称
 * @author jy
 * @since 1.0.0
 */
@Schema(description = "字段策略可选的精确动作")
public record FieldPolicyAction(
        @NotBlank @Schema(description = "精确操作码", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @NotBlank @Schema(description = "动作名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name) {
}
