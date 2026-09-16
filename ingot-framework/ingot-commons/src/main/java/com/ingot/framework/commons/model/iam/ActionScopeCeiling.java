package com.ingot.framework.commons.model.iam;

import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>限定委派允许的单个操作与范围，范围参数在该委派内绑定。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param actionId 操作 ID
 * @param scopes 该操作允许范围的并集
 * @param scopeBindings 委派范围参数，不接受未绑定的管理范围
 */
@Schema(description = "限定委派允许的单个操作与范围，范围参数在该委派内绑定")
public record ActionScopeCeiling(
        @NotBlank @Schema(description = "操作 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String actionId,
        @NotNull @Schema(description = "该操作允许范围的并集", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ScopeExpression> scopes,
        @NotNull @Schema(description = "委派范围参数，不接受未绑定的管理范围", requiredMode = Schema.RequiredMode.REQUIRED)
        Map<@NotBlank String, @NotNull @Valid ScopeBinding> scopeBindings) {

    /**
     * 复制输入集合，防止校验与消费之间被外部修改；必填空引用由 Bean Validation 拒绝。
     */
    public ActionScopeCeiling {
        if (scopes != null) {
            scopes = Collections.unmodifiableList(new ArrayList<>(scopes));
        }
        if (scopeBindings != null) {
            scopeBindings = Collections.unmodifiableMap(new LinkedHashMap<>(scopeBindings));
        }
    }
}
