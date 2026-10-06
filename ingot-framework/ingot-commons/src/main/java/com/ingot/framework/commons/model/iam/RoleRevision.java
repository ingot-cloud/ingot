package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * 传输不可变角色版本及原始定义，不持久化租户合成后的完整副本。
 * </p>
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
 * @param displayDeltas 列表读时相对上一版本的展示差异，含操作名称；写入不使用
 * @param resourceFieldPermissions 资源字段固定快照；未声明字段不授予权限
 * @param previousResourceFieldPermissions 版本历史比较时的前一版本字段快照
 */
@Schema(description = "传输不可变角色版本及原始定义，不持久化租户合成后的完整副本")
public record RoleRevision(
        @NotBlank @Schema(description = "版本 ID", requiredMode = Schema.RequiredMode.REQUIRED) String id,
        @NotBlank @Schema(description = "角色定义 ID", requiredMode = Schema.RequiredMode.REQUIRED) String roleId,
        @NotBlank @Schema(description = "版本序号，字符串传输", requiredMode = Schema.RequiredMode.REQUIRED) String revision,
        @NotNull @Schema(description = "角色来源种类", requiredMode = Schema.RequiredMode.REQUIRED) RoleKind kind,
        @Schema(description = "定制租户版本固定的共享基础版本 ID，其余为空") String baseRevisionId,
        @NotNull @Schema(description = "完整定义的逐操作范围；定制版本不复制基础授权",
                requiredMode = Schema.RequiredMode.REQUIRED) List<@NotNull @Valid ActionGrant> grants,
        @NotNull @Schema(description = "租户定制差异，其余为空集合",
                requiredMode = Schema.RequiredMode.REQUIRED) List<@NotNull @Valid RoleDelta> deltas,
        @NotNull @Schema(description = "命名参数定义，键及类型须一致",
                requiredMode = Schema.RequiredMode.REQUIRED) List<@NotNull @Valid RoleParameterDefinition> parameterDefinitions,
        @Valid @Schema(description = "定制元数据覆盖，其余为空") RoleMetadataOverrides metadataOverrides,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY) @Schema(
                description = "相对上一版本的展示差异，含操作名称；仅列表读填充") List<@NotNull @Valid RoleDisplayDelta> displayDeltas,
        @Schema(description = "资源 ID 到字段 key 的固定权限快照；历史版本可为空") Map<String, Map<String, @Valid FieldAccess>> resourceFieldPermissions,
        @Schema(description = "相对上一版本的字段比较基线；仅版本历史读填充") Map<String, Map<String, @Valid FieldAccess>> previousResourceFieldPermissions) {
    /** 保留字段快照读写构造契约。 */
    public RoleRevision(String id, String roleId, String revision, RoleKind kind, String baseRevisionId,
            List<ActionGrant> grants, List<RoleDelta> deltas, List<RoleParameterDefinition> parameterDefinitions,
            RoleMetadataOverrides metadataOverrides, List<RoleDisplayDelta> displayDeltas,
            Map<String, Map<String, FieldAccess>> resourceFieldPermissions) {
        this(id, roleId, revision, kind, baseRevisionId, grants, deltas, parameterDefinitions, metadataOverrides,
                displayDeltas, resourceFieldPermissions, null);
    }

    /** 兼容没有字段快照的历史版本。 */
    public RoleRevision(String id, String roleId, String revision, RoleKind kind, String baseRevisionId,
            List<ActionGrant> grants, List<RoleDelta> deltas, List<RoleParameterDefinition> parameterDefinitions,
            RoleMetadataOverrides metadataOverrides, List<RoleDisplayDelta> displayDeltas) {
        this(id, roleId, revision, kind, baseRevisionId, grants, deltas, parameterDefinitions, metadataOverrides,
                displayDeltas, null);
    }

    /**
     * 复制输入集合，防止校验与消费之间被外部修改；必填空引用由 Bean Validation 拒绝。
     */
    public RoleRevision {
        previousResourceFieldPermissions = ResourceFieldPermissions.copy(previousResourceFieldPermissions);
        resourceFieldPermissions = ResourceFieldPermissions.copy(resourceFieldPermissions);
        if (grants != null) {
            grants = Collections.unmodifiableList(new ArrayList<>(grants));
        }
        if (deltas != null) {
            deltas = Collections.unmodifiableList(new ArrayList<>(deltas));
        }
        if (parameterDefinitions != null) {
            parameterDefinitions = Collections.unmodifiableList(new ArrayList<>(parameterDefinitions));
        }
        if (displayDeltas != null) {
            displayDeltas = Collections.unmodifiableList(new ArrayList<>(displayDeltas));
        }
        else {
            displayDeltas = List.of();
        }
    }

    /**
     * 定制版本只能保存固定共享基础与差异，完整版本不携带差异。
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
     * @return 是否满足结构约束
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.validation.constraints.AssertTrue(message = "同版本操作与参数定义不得重复")
    @Schema(hidden = true)
    public boolean isDefinitionUnique() {
        if (grants == null || deltas == null || parameterDefinitions == null || grants.contains(null)
                || deltas.contains(null) || parameterDefinitions.contains(null)) {
            return true;
        }
        return grants.stream().map(ActionGrant::actionId).distinct().count() == grants.size()
                && deltas.stream().map(RoleDelta::actionId).distinct().count() == deltas.size()
                && parameterDefinitions.stream()
                    .map(RoleParameterDefinition::key)
                    .distinct()
                    .count() == parameterDefinitions.size();
    }
}
