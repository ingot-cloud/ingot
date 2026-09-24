package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * <p>整包创建时绑定到资源的精确操作，用临时 ID 供菜单引用。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param tempId 客户端临时 ID
 * @param code 操作码末段或完整码
 * @param name 操作名称
 */
@Schema(description = "整包创建时绑定到资源的精确操作，用临时 ID 供菜单引用")
public record ApplicationBundleAction(
        @NotBlank @Size(max = 64) @Schema(description = "客户端临时 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String tempId,
        @NotBlank @Size(max = 192) @Pattern(regexp = "[^*]+")
        @Schema(description = "操作码末段或完整码", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @NotBlank @Size(max = 128) @Schema(description = "操作名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name) {
}
