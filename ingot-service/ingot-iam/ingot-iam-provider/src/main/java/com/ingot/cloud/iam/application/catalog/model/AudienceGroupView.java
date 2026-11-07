package com.ingot.cloud.iam.application.catalog.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>展示当前应用已绑定的一个用户组。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 用户组 ID
 * @param name 组名称；缺失或空白时为 {@link AudienceView#RESTRICTED_NAME}
 */
@Schema(description = "展示当前应用已绑定的一个用户组")
public record AudienceGroupView(
        @NotBlank @Schema(description = "用户组 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "组名称；缺失时为信息受限", requiredMode = Schema.RequiredMode.REQUIRED)
        String name) {
}
