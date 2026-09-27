package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>返回成员直接角色的标识与名称，不含用户组继承或带范围的授权。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 角色定义 ID
 * @param name 角色名称
 */
@Schema(description = "返回成员直接角色的标识与名称，不含用户组继承或带范围的授权")
public record MemberRoleView(
        @NotBlank @Schema(description = "角色定义 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name) {
}
