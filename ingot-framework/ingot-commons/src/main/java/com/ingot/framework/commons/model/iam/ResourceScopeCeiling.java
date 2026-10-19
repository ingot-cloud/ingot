package com.ingot.framework.commons.model.iam;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>限定委派的资源范围，同资源新操作复用此上限。</p>
 * @author jy
 * @since 1.0.0
 * @param resourceId 实际资源ID
 * @param scopes 范围表达式并集
 * @param scopeBindings 资源范围参数绑定
 */
@Schema(description = "限定委派的资源范围，同资源新操作复用此上限", additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record ResourceScopeCeiling(
        @NotBlank String resourceId,
        @NotNull List<@NotNull @Valid ScopeExpression> scopes,
        @NotNull Map<@NotBlank String, @NotNull @Valid ScopeBinding> scopeBindings) {

    /** 拒绝退出版本字段和未知输入，避免旧契约被静默解释。 */
    @JsonAnySetter
    public void rejectUnknown(String field, Object value) { throw new IllegalArgumentException("当前授权契约不支持字段：" + field); }

    /** 固定授权集合，避免调用方在验证后修改内容。 */
    public ResourceScopeCeiling {
        if (scopes != null) scopes = List.copyOf(scopes);
        if (scopeBindings != null) scopeBindings = Map.copyOf(scopeBindings);
    }
}
