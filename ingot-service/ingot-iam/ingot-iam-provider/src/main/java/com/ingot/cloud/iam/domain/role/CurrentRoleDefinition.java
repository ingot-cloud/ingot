package com.ingot.cloud.iam.domain.role;

import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.ResourceFieldDefinition;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>保存完整当前操作、资源参数和字段快照，不承载共享继承或业务版本。</p>
 * @author jy
 * @since 1.0.0
 * @param grants 完整操作范围
 * @param parameterDefinitions 资源参数定义
 * @param resourceFieldPermissions 当前角色字段快照，按资源 ID 索引；省略时按空快照处理
 */
@Schema(description = "保存完整当前操作、资源参数和字段快照，不承载共享继承或业务版本", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CurrentRoleDefinition(
        @NotNull List<@NotNull @Valid ActionGrant> grants,
        @NotNull List<@NotNull @Valid CurrentRoleParameter> parameterDefinitions,
        @NotNull Map<String, @NotNull @Valid ResourceFieldDefinition> resourceFieldPermissions) {

    /**
     * 兼容尚未声明字段快照的调用，空快照与显式快照由持久化层区分。
     * @param grants 完整操作范围
     * @param parameterDefinitions 资源参数定义
     */
    public CurrentRoleDefinition(List<ActionGrant> grants, List<CurrentRoleParameter> parameterDefinitions) {
        this(grants, parameterDefinitions, Map.of());
    }

    /** 拒绝未知属性，避免旧版本请求被静默解释为其他契约。 */
    @JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("当前授权契约不支持字段：" + field);
    }

    /** 固定集合快照，避免验证和使用之间被外部修改。 */
    public CurrentRoleDefinition {
        if (grants != null) grants = List.copyOf(grants);
        if (parameterDefinitions != null) parameterDefinitions = List.copyOf(parameterDefinitions);
        resourceFieldPermissions = resourceFieldPermissions == null ? Map.of() : Map.copyOf(resourceFieldPermissions);
    }
}
