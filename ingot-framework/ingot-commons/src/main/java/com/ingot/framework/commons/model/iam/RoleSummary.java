package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回角色目录元数据，区分固定共享、系统及两域自定义来源。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 角色 ID
 * @param code 角色编码
 * @param name 角色名称
 * @param description 说明，可空
 * @param groupName 仅展示的分组，可空
 * @param kind 角色来源
 * @param status 整体启停状态，约束所有旧版本
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回角色目录元数据，区分固定共享、系统及两域自定义来源")
public record RoleSummary(
        @NotBlank @Schema(description = "角色 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "角色编码", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @NotBlank @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
         @Schema(description = "说明，可空")
        String description,
         @Schema(description = "仅展示的分组，可空")
        String groupName,
        @NotNull @Schema(description = "角色来源", requiredMode = Schema.RequiredMode.REQUIRED)
        RoleKind kind,
        @NotNull @Schema(description = "整体启停状态，约束所有旧版本", requiredMode = Schema.RequiredMode.REQUIRED)
        ConfigurationStatus status) {
}
