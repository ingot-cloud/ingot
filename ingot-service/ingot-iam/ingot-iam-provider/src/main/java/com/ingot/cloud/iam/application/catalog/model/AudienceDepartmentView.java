package com.ingot.cloud.iam.application.catalog.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>展示当前应用已绑定的一个部门，含下级标记来自绑定行。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 部门 ID
 * @param name 部门名称；缺失或空白时为 {@link AudienceView#RESTRICTED_NAME}
 * @param includeDescendants 是否包含下级部门
 */
@Schema(description = "展示当前应用已绑定的一个部门")
public record AudienceDepartmentView(
        @NotBlank @Schema(description = "部门 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "部门名称；缺失时为信息受限", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @NotNull @Schema(description = "是否包含下级部门", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean includeDescendants) {
}
