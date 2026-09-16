package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>承载固定基础角色版本的单操作差异，基础存在性及差异冲突由角色服务校验。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param actionId 被调整的操作 ID，使用字符串传输
 * @param operation 新增、移除或替换范围，必填
 * @param scopes 新增或替换的范围；移除不携带范围，序列化为非空集合
 */
@Schema(description = "共享角色的单操作差异；同版本同操作最多一项")
public record RoleDelta(
        @NotBlank @Schema(description = "操作 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String actionId,
        @NotNull @Schema(description = "差异操作", requiredMode = Schema.RequiredMode.REQUIRED)
        RoleDeltaOperation operation,
        @Schema(description = "仅新增或替换使用；省略时为空集合")
        List<@NotNull @Valid ScopeExpression> scopes) {

    /**
     * 将移除操作省略的范围规范化为空集合；其余操作保留缺失状态供校验，并复制非空集合。
     */
    public RoleDelta {
        if (scopes != null) {
            scopes = Collections.unmodifiableList(new ArrayList<>(scopes));
        } else if (operation == RoleDeltaOperation.REMOVE) {
            scopes = List.of();
        }
    }

    /**
     * 校验移除不携带范围、新增和替换必须显式提供范围；基础存在性另行校验。
     *
     * @return 差异操作与范围字段是否匹配
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.validation.constraints.AssertTrue(message = "差异操作与范围字段不匹配")
    @Schema(hidden = true)
    public boolean isStructurallyValid() {
        return operation == null || (scopes != null
                && (operation != RoleDeltaOperation.REMOVE || scopes.isEmpty()));
    }
}
