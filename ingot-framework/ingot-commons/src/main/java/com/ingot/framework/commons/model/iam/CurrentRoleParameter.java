package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * <p>声明当前角色资源参数，身份由资源、类型和稳定键共同确定。</p>
 * @author jy
 * @since 1.0.0
 * @param key 稳定参数键
 * @param kind 绑定类型
 * @param resourceId 归属资源ID
 */
@Schema(description = "声明当前角色资源参数，身份由资源、类型和稳定键共同确定", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CurrentRoleParameter(
        @NotBlank String key,
        @NotNull ScopeBindingKind kind,
        @NotBlank String resourceId) {

    /** 拒绝退出字段及未知属性，避免旧版本请求被静默解释为当前角色。 */
    @JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("当前授权契约不支持字段：" + field);
    }
}
