package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <p>更新当前组织可编辑设置，所有者转交使用独立命令。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 组织设置读取版本
 * @param name 组织名称
 * @param avatar 组织头像，可空
 */
@Schema(description = "更新当前组织可编辑设置，所有者转交使用独立命令")
public record TenantSettingsInput(
        @NotBlank @Schema(description = "组织设置读取版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @NotBlank @Size(max = 128) @Schema(description = "组织名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "组织头像，可空")
        String avatar) {
}
