package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>整体更新部门资料并重验子树循环。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 部门读取版本
 * @param department 待保存部门内容
 */
@Schema(description = "整体更新部门资料并重验子树循环")
public record DepartmentUpdateInput(
        @NotBlank @Schema(description = "部门读取版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @NotNull @Valid @Schema(description = "待保存部门内容", requiredMode = Schema.RequiredMode.REQUIRED)
        DepartmentDraft department) {
}
