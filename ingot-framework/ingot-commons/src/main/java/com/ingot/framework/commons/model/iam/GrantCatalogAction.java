package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>授权选择目录中的启用操作，供角色权限树勾选。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 操作 ID
 * @param code 全局唯一精确操作码
 * @param name 操作名称
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "授权选择目录中的启用操作")
public record GrantCatalogAction(
        @NotBlank @Schema(description = "操作 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "全局唯一精确操作码", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @NotBlank @Schema(description = "操作名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name) {
}
