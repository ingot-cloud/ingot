package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>强制清除应用及其全部关联，确认必须与清除出现在同一次请求。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 应用读取版本
 * @param confirmation 当前账号身份确认
 */
@Schema(description = "强制清除应用及其全部关联，确认必须与清除出现在同一次请求")
public record ApplicationPurgeInput(
        @NotBlank @Schema(description = "应用读取版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @NotNull @Valid @Schema(description = "当前账号身份确认", requiredMode = Schema.RequiredMode.REQUIRED)
        SensitiveConfirmation confirmation) {
}
