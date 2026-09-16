package com.ingot.framework.commons.model.iam;

import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>传输不可变角色版本及原始定义，不持久化租户合成后的完整副本。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 版本 ID
 * @param roleId 角色定义 ID
 * @param revision 版本序号，字符串传输
 * @param kind 角色来源种类
 * @param baseRevisionId 定制租户版本固定的共享基础版本 ID，其余为空
 * @param grants 完整定义的逐操作范围；定制版本不复制基础授权
 * @param deltas 租户定制差异，其余为空集合
 * @param parameterDefinitions 命名参数定义，键及类型须一致
 * @param metadataOverrides 定制元数据覆盖，其余为空
 */
@Schema(description = "传输不可变角色版本及原始定义，不持久化租户合成后的完整副本")
public record RoleRevision(
        @NotBlank @Schema(description = "版本 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "角色定义 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String roleId,
        @NotBlank @Schema(description = "版本序号，字符串传输", requiredMode = Schema.RequiredMode.REQUIRED)
        String revision,
        @NotNull @Schema(description = "角色来源种类", requiredMode = Schema.RequiredMode.REQUIRED)
        RoleKind kind,
        @Schema(description = "定制租户版本固定的共享基础版本 ID，其余为空")
        String baseRevisionId,
        @NotNull @Schema(description = "完整定义的逐操作范围；定制版本不复制基础授权", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ActionGrant> grants,
        @NotNull @Schema(description = "租户定制差异，其余为空集合", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid RoleDelta> deltas,
        @NotNull @Schema(description = "命名参数定义，键及类型须一致", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid RoleParameterDefinition> parameterDefinitions,
        @Valid @Schema(description = "定制元数据覆盖，其余为空")
        RoleMetadataOverrides metadataOverrides) {

    /**
     * 复制输入集合，防止校验与消费之间被外部修改；必填空引用由 Bean Validation 拒绝。
     */
    public RoleRevision {
        if (grants != null) {
            grants = Collections.unmodifiableList(new ArrayList<>(grants));
        }
        if (deltas != null) {
            deltas = Collections.unmodifiableList(new ArrayList<>(deltas));
        }
        if (parameterDefinitions != null) {
            parameterDefinitions = Collections.unmodifiableList(new ArrayList<>(parameterDefinitions));
        }
    }

    /**
     * 定制版本只能保存固定共享基础与差异，完整版本不携带差异。
     *
     * @return 是否满足结构约束
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.validation.constraints.AssertTrue(message = "定制版本只能保存固定共享基础与差异，完整版本不携带差异")
    @Schema(hidden = true)
    public boolean isDefinitionShapeValid() {
        if (grants == null || deltas == null) {
            return true;
        }
        if (baseRevisionId != null) {
            return !baseRevisionId.isBlank() && kind == RoleKind.TENANT_CUSTOM && grants.isEmpty();
        }
        return deltas.isEmpty();
    }

    /**
     * 同版本操作与参数定义不得重复。
     *
     * @return 是否满足结构约束
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.validation.constraints.AssertTrue(message = "同版本操作与参数定义不得重复")
    @Schema(hidden = true)
    public boolean isDefinitionUnique() {
        if (grants == null || deltas == null || parameterDefinitions == null
                || grants.contains(null) || deltas.contains(null) || parameterDefinitions.contains(null)) {
            return true;
        }
        return grants.stream().map(ActionGrant::actionId).distinct().count() == grants.size()
                && deltas.stream().map(RoleDelta::actionId).distinct().count() == deltas.size()
                && parameterDefinitions.stream().map(RoleParameterDefinition::key).distinct().count() == parameterDefinitions.size();
    }
}
