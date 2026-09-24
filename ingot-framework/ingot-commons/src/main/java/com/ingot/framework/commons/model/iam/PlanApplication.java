package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>描述套餐已关联应用的展示内容，供详情直接回显，不必再按 ID 逐条查询应用。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 应用 ID
 * @param code 应用编码
 * @param name 应用名称
 * @param status 应用启停状态
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "描述套餐已关联应用的展示内容")
public record PlanApplication(
        @NotBlank @Schema(description = "应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "应用编码", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @NotBlank @Schema(description = "应用名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @NotNull @Schema(description = "应用启停状态", requiredMode = Schema.RequiredMode.REQUIRED)
        ConfigurationStatus status) {
}
