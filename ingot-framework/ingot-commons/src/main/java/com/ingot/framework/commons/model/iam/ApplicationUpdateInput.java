package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <p>更新应用展示信息与基础开通标记，不能改写命名空间或管理域。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 应用读取版本
 * @param name 应用名称
 * @param description 说明，可空
 * @param icon 图标，可空
 * @param sortOrder 展示顺序
 * @param baseline 是否在组织初始化时开通；仅租户域可为 true，由服务按已保存域校验
 */
@Schema(description = "更新应用展示信息与基础开通标记，不能改写命名空间或管理域")
public record ApplicationUpdateInput(
        @NotBlank @Schema(description = "应用读取版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @NotBlank @Size(max = 128) @Schema(description = "应用名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "说明，可空")
        String description,
        @Schema(description = "图标，可空")
        String icon,
        @Schema(description = "展示顺序", requiredMode = Schema.RequiredMode.REQUIRED)
        int sortOrder,
        @Schema(description = "是否在组织初始化时开通；仅租户域可为 true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean baseline) {
}
