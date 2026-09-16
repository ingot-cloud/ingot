package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>描述当前身份可用的应用及导航展示信息。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 应用 ID
 * @param code 应用命名空间
 * @param name 应用名称
 * @param icon 图标引用，可空
 * @param sortOrder 展示顺序
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "描述当前身份可用的应用及导航展示信息")
public record ApplicationSummary(
        @NotBlank @Schema(description = "应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "应用命名空间", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @NotBlank @Schema(description = "应用名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
         @Schema(description = "图标引用，可空")
        String icon,
         @Schema(description = "展示顺序", requiredMode = Schema.RequiredMode.REQUIRED)
        int sortOrder) {
}
