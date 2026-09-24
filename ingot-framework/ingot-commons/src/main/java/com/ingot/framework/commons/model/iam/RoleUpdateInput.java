package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>更新角色名称、说明、分组与启停；名称为空时只改启停，不改编码和已发布版本。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 读取时的配置版本
 * @param name 角色名称；空白时只改启停
 * @param description 说明，可空
 * @param groupName 仅展示分组，可空
 * @param status 启停状态
 */
@Schema(description = "更新角色名称、说明、分组与启停，不改编码和已发布版本")
public record RoleUpdateInput(
        @NotBlank @Schema(description = "读取时的配置版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @Schema(description = "角色名称；空白时只改启停")
        String name,
        @Schema(description = "说明，可空")
        String description,
        @Schema(description = "仅展示分组，可空")
        String groupName,
        @NotNull @Schema(description = "启停状态", requiredMode = Schema.RequiredMode.REQUIRED)
        ConfigurationStatus status) {
}
