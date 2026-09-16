package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回应用目录配置，不把启用状态等同于业务操作授权。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 应用 ID
 * @param code 应用命名空间
 * @param domain 应用所属管理域
 * @param name 应用名称
 * @param description 说明，可空
 * @param icon 图标，可空
 * @param sortOrder 展示顺序
 * @param baseline 是否在组织初始化时开通；仅租户域可为 true
 * @param status 全局启停状态
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回应用目录配置，不把启用状态等同于业务操作授权")
public record ApplicationRecord(
        @NotBlank @Schema(description = "应用 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "应用命名空间", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @NotNull @Schema(description = "应用所属管理域", requiredMode = Schema.RequiredMode.REQUIRED)
        AuthorizationDomain domain,
        @NotBlank @Schema(description = "应用名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
         @Schema(description = "说明，可空")
        String description,
         @Schema(description = "图标，可空")
        String icon,
         @Schema(description = "展示顺序", requiredMode = Schema.RequiredMode.REQUIRED)
        int sortOrder,
        @Schema(description = "是否在组织初始化时开通；仅租户域可为 true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean baseline,
        @NotNull @Schema(description = "全局启停状态", requiredMode = Schema.RequiredMode.REQUIRED)
        ConfigurationStatus status) {
}
