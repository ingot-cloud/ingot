package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>创建自定义或共享角色并提交首个不可变版本，不能创建系统治理角色。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param code 所属域内唯一编码
 * @param name 角色名称
 * @param description 说明，可空
 * @param groupName 仅展示分组，可空
 * @param kind 允许 SHARED、PLATFORM_CUSTOM 或 TENANT_CUSTOM
 * @param baseRevisionId 租户定制时固定的共享基础版本；其余必须为空
 * @param definition 首个版本定义
 */
@Schema(description = "创建自定义或共享角色并提交首个不可变版本，不能创建系统治理角色")
public record RoleCreateInput(
        @NotBlank @Schema(description = "所属域内唯一编码", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,
        @NotBlank @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "说明，可空")
        String description,
        @Schema(description = "仅展示分组，可空")
        String groupName,
        @NotNull @Schema(description = "允许 SHARED、PLATFORM_CUSTOM 或 TENANT_CUSTOM", requiredMode = Schema.RequiredMode.REQUIRED)
        RoleKind kind,
        @Schema(description = "租户定制时固定的共享基础版本；其余必须为空")
        String baseRevisionId,
        @NotNull @Valid @Schema(description = "首个版本定义", requiredMode = Schema.RequiredMode.REQUIRED)
        RoleDefinitionDraft definition) {

    /**
     * 系统治理角色不经此命令创建。
     *
     * @return 种类是否允许由管理接口创建
     */
    @JsonIgnore
    @AssertTrue(message = "系统治理角色不能通过管理接口创建")
    @Schema(hidden = true)
    public boolean isCreatableKind() {
        return kind != RoleKind.SYSTEM;
    }

    /**
     * 仅租户定制可绑定共享基础；绑定后必须提交差异定义。
     *
     * @return 基础版本与定义形状是否匹配
     */
    @JsonIgnore
    @AssertTrue(message = "仅租户定制可绑定共享基础，且必须提交差异定义")
    @Schema(hidden = true)
    public boolean isBaseCompatible() {
        boolean customized = baseRevisionId != null && !baseRevisionId.isBlank();
        if (kind == null || definition == null || definition.grants() == null || definition.deltas() == null) {
            return true;
        }
        if (customized) {
            return kind == RoleKind.TENANT_CUSTOM && definition.grants().isEmpty();
        }
        return definition.deltas().isEmpty();
    }
}
