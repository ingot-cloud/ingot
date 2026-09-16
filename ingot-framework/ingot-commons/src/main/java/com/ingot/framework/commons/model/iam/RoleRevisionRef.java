package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>引用明确来源的不可变角色版本，服务端还须校验域、状态及可分配性。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param kind 角色版本来源种类
 * @param id 角色版本 ID，不是角色定义 ID
 */
@Schema(description = "引用明确来源的不可变角色版本，服务端还须校验域、状态及可分配性")
public record RoleRevisionRef(
        @NotNull @Schema(description = "角色版本来源种类", requiredMode = Schema.RequiredMode.REQUIRED)
        RoleKind kind,
        @NotBlank @Schema(description = "角色版本 ID，不是角色定义 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id) {
}
