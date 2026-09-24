package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>返回套餐选择器所需的最小标识，不附带应用清单。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 套餐 ID
 * @param name 套餐名称
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回套餐选择器所需的最小标识，不附带应用清单")
public record PlanSummary(
        @NotBlank @Schema(description = "套餐 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "套餐名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name) {
}
