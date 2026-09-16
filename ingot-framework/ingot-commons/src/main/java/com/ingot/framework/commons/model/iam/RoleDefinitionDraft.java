package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * <p>提交待发布的角色定义内容，不含服务器分配的版本标识。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param grants 完整定义的逐操作范围；定制差异版本必须为空
 * @param deltas 租户定制差异；完整版本必须为空
 * @param parameterDefinitions 命名参数定义，键及类型须一致
 * @param metadataOverrides 定制元数据覆盖，完整版本为空
 */
@Schema(description = "提交待发布的角色定义内容，不含服务器分配的版本标识")
public record RoleDefinitionDraft(
        @NotNull @Schema(description = "完整定义的逐操作范围；定制差异版本必须为空", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ActionGrant> grants,
        @NotNull @Schema(description = "租户定制差异；完整版本必须为空", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid RoleDelta> deltas,
        @NotNull @Schema(description = "命名参数定义，键及类型须一致", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid RoleParameterDefinition> parameterDefinitions,
        @Valid @Schema(description = "定制元数据覆盖，完整版本为空")
        RoleMetadataOverrides metadataOverrides) {

    /**
     * 复制输入集合，防止校验与消费之间被外部修改；必填空引用由 Bean Validation 拒绝。
     */
    public RoleDefinitionDraft {
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
     * 完整版本只保存授权，定制版本只保存差异，二者不能同时出现。
     *
     * @return 是否满足结构约束
     */
    @JsonIgnore
    @AssertTrue(message = "完整版本只保存授权，定制版本只保存差异")
    @Schema(hidden = true)
    public boolean isDefinitionShapeValid() {
        return grants == null || deltas == null || grants.isEmpty() || deltas.isEmpty();
    }

    /**
     * 同版本操作与参数定义不得重复。
     *
     * @return 是否满足结构约束
     */
    @JsonIgnore
    @AssertTrue(message = "同版本操作与参数定义不得重复")
    @Schema(hidden = true)
    public boolean isDefinitionUnique() {
        if (grants == null || deltas == null || parameterDefinitions == null
                || grants.contains(null) || deltas.contains(null) || parameterDefinitions.contains(null)) {
            return true;
        }
        return grants.stream().map(ActionGrant::actionId).distinct().count() == grants.size()
                && deltas.stream().map(RoleDelta::actionId).distinct().count() == deltas.size()
                && parameterDefinitions.stream().map(RoleParameterDefinition::key).distinct().count()
                == parameterDefinitions.size();
    }

    /**
     * 是否为固定基础版本上的差异定义。
     *
     * @return 仅包含差异且不含完整授权时为 true
     */
    @JsonIgnore
    public boolean customized() {
        return grants != null && grants.isEmpty() && deltas != null && !deltas.isEmpty();
    }
}
