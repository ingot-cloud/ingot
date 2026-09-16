package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>发布新的不可变角色版本，不自动升级既有授权。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 角色定义读取版本；提交时比较并重新鉴权
 * @param definition 待发布完整或差异定义
 */
@Schema(description = "发布新的不可变角色版本，不自动升级既有授权")
public record RolePublishInput(
        @NotBlank @Schema(description = "角色定义读取版本；提交时比较并重新鉴权", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @NotNull @Valid @Schema(description = "待发布完整或差异定义", requiredMode = Schema.RequiredMode.REQUIRED)
        RoleDefinitionDraft definition) {
}
