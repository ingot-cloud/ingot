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
 * @param versionName 版本展示名称，不参与权限身份
 * @param remark 版本备注
 * @param sourceRevisionId 同角色编辑来源
 * @param definition 待发布完整或差异定义
 */
@Schema(description = "发布新的不可变角色版本，不自动升级既有授权")
public record RolePublishInput(
        @NotBlank @Schema(description = "角色定义读取版本；提交时比较并重新鉴权", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @NotNull @Valid @Schema(description = "待发布完整或差异定义", requiredMode = Schema.RequiredMode.REQUIRED)
        RoleDefinitionDraft definition,
        @jakarta.validation.constraints.Size(max = 128) @Schema(description = "版本名称，缺省显示vN") String versionName,
        @jakarta.validation.constraints.Size(max = 512) @Schema(description = "版本备注") String remark,
        @Schema(description = "编辑来源，须为同角色固定版本") String sourceRevisionId) {
    /** 保留仅发布权限的调用方式。 */
    public RolePublishInput(String expectedVersion, RoleDefinitionDraft definition) {
        this(expectedVersion, definition, null, null, null);
    }

}
