package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>声明角色版本中的命名范围参数及其绑定类型。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param key 命名参数键，在当前版本内唯一
 * @param kind 允许的绑定集合类型
 */
@Schema(description = "声明角色版本中的命名范围参数及其绑定类型")
public record RoleParameterDefinition(
        @NotBlank @Schema(description = "命名参数键，在当前版本内唯一", requiredMode = Schema.RequiredMode.REQUIRED)
        String key,
        @NotNull @Schema(description = "允许的绑定集合类型", requiredMode = Schema.RequiredMode.REQUIRED)
        ScopeBindingKind kind) {
}
