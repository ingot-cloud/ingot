package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>标记操作相对固定基础版本的来源，移除操作不成为全局拒绝。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param actionId 操作 ID
 * @param origin 基础、新增、移除或替换来源
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "标记操作相对固定基础版本的来源，移除操作不成为全局拒绝")
public record ActionOrigin(
        @NotBlank @Schema(description = "操作 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String actionId,
        @NotNull @Schema(description = "基础、新增、移除或替换来源", requiredMode = Schema.RequiredMode.REQUIRED)
        RoleOrigin origin) {
}
