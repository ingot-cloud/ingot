package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回需要明确处置的升级冲突，不自动选择新旧定义。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param key 稳定冲突键
 * @param actionId 相关操作 ID，可空
 * @param reasonCode 冲突原因码
 * @param message 安全中文说明
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回需要明确处置的升级冲突，不自动选择新旧定义")
public record UpgradeConflict(
        @NotBlank @Schema(description = "稳定冲突键", requiredMode = Schema.RequiredMode.REQUIRED)
        String key,
         @Schema(description = "相关操作 ID，可空")
        String actionId,
        @NotNull @Schema(description = "冲突原因码", requiredMode = Schema.RequiredMode.REQUIRED)
        IamReasonCode reasonCode,
        @NotBlank @Schema(description = "安全中文说明", requiredMode = Schema.RequiredMode.REQUIRED)
        String message) {
}
