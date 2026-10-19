package com.ingot.framework.commons.model.iam;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>保存完整当前操作及资源参数，不承载共享继承或业务版本。</p>
 * @author jy
 * @since 1.0.0
 * @param grants 完整操作范围
 * @param parameterDefinitions 资源参数定义
 */
@Schema(description = "保存完整当前操作及资源参数，不承载共享继承或业务版本", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CurrentRoleDefinition(
        @NotNull List<@NotNull @Valid ActionGrant> grants,
        @NotNull List<@NotNull @Valid CurrentRoleParameter> parameterDefinitions) {

    /** 拒绝退出字段及未知属性，避免旧版本请求被静默解释为当前角色。 */
    @JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("当前授权契约不支持字段：" + field);
    }

    /** 固定集合快照，避免验证和使用之间被外部修改。 */
    public CurrentRoleDefinition {
        if (grants != null) grants = List.copyOf(grants);
        if (parameterDefinitions != null) parameterDefinitions = List.copyOf(parameterDefinitions);
    }
}
