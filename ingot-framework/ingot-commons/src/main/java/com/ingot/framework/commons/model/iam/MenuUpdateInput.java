package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>整体更新菜单配置并重验本应用操作引用。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 菜单读取版本
 * @param menu 待保存菜单内容
 */
@Schema(description = "整体更新菜单配置并重验本应用操作引用")
public record MenuUpdateInput(
        @NotBlank @Schema(description = "菜单读取版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @NotNull @Valid @Schema(description = "待保存菜单内容", requiredMode = Schema.RequiredMode.REQUIRED)
        MenuDraft menu) {
}
