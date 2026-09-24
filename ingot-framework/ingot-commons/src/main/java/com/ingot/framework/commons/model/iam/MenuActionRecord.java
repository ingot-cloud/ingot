package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>菜单已绑定的精确操作，带资源名称，供详情回显。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 操作 ID
 * @param code 全局唯一精确操作码
 * @param name 操作名称
 * @param resourceId 所属资源 ID
 * @param resourceCode 资源编码
 * @param resourceName 资源名称
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "菜单已绑定的精确操作，带资源名称，供详情回显")
public record MenuActionRecord(
        @NotBlank @Schema(description = "操作 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "全局唯一精确操作码", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @NotBlank @Schema(description = "操作名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @NotBlank @Schema(description = "所属资源 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String resourceId,
        @NotBlank @Schema(description = "资源编码", requiredMode = Schema.RequiredMode.REQUIRED)
        String resourceCode,
        @NotBlank @Schema(description = "资源名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String resourceName) {
}
