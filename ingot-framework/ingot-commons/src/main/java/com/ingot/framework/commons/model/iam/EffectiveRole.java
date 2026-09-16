package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回角色固定版本的合成定义及受限使用统计，不将合成副本作为持久化来源。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param role 角色元数据
 * @param revision 固定版本引用
 * @param grants 合成后的逐操作允许范围
 * @param origins 逐操作来源，含本角色移除项
 * @param parameterDefinitions 合成后参数定义
 * @param usage 允许披露的使用统计
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回角色固定版本的合成定义及受限使用统计，不将合成副本作为持久化来源")
public record EffectiveRole(
        @NotNull @Valid @Schema(description = "角色元数据", requiredMode = Schema.RequiredMode.REQUIRED)
        RoleSummary role,
        @NotNull @Valid @Schema(description = "固定版本引用", requiredMode = Schema.RequiredMode.REQUIRED)
        RoleRevisionRef revision,
        @NotNull @Schema(description = "合成后的逐操作允许范围", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ActionGrant> grants,
        @NotNull @Schema(description = "逐操作来源，含本角色移除项", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ActionOrigin> origins,
        @NotNull @Schema(description = "合成后参数定义", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid RoleParameterDefinition> parameterDefinitions,
        @NotNull @Valid @Schema(description = "允许披露的使用统计", requiredMode = Schema.RequiredMode.REQUIRED)
        UsageSummary usage) {

    /**
     * 复制输出集合，避免外部修改改变已经计算的响应视图；必填空引用由校验拒绝。
     */
    public EffectiveRole {
        if (grants != null) {
            grants = Collections.unmodifiableList(new ArrayList<>(grants));
        }
        if (origins != null) {
            origins = Collections.unmodifiableList(new ArrayList<>(origins));
        }
        if (parameterDefinitions != null) {
            parameterDefinitions = Collections.unmodifiableList(new ArrayList<>(parameterDefinitions));
        }
    }
}
