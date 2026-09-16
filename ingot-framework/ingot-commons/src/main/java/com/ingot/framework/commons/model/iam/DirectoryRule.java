package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>描述通讯录查看者对应的允许或禁止目标，默认范围单独保存。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param effect 允许或禁止效果
 * @param viewerSelection 规则匹配的查看者
 * @param targetSelection 允许或禁止的目标
 */
@Schema(description = "描述通讯录查看者对应的允许或禁止目标，默认范围单独保存")
public record DirectoryRule(
        @NotNull @Schema(description = "允许或禁止效果", requiredMode = Schema.RequiredMode.REQUIRED)
        PolicyEffect effect,
        @NotNull @Valid @Schema(description = "规则匹配的查看者", requiredMode = Schema.RequiredMode.REQUIRED)
        Selection viewerSelection,
        @NotNull @Valid @Schema(description = "允许或禁止的目标", requiredMode = Schema.RequiredMode.REQUIRED)
        Selection targetSelection) {
}
