package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>版本历史相对上一版本的展示差异，含操作名称，不作为写入模型。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param actionId 被调整的操作 ID
 * @param actionName 操作名称；目录缺失时为空
 * @param operation 新增、移除或替换范围
 * @param scopes 新增或替换的范围；移除为空集合
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "版本历史展示差异，含操作名称")
public record RoleDisplayDelta(
        @NotBlank @Schema(description = "操作 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String actionId,
        @Schema(description = "操作名称；目录缺失时为空")
        String actionName,
        @NotNull @Schema(description = "差异操作", requiredMode = Schema.RequiredMode.REQUIRED)
        RoleDeltaOperation operation,
        @Schema(description = "仅新增或替换使用；省略时为空集合")
        List<@NotNull @Valid ScopeExpression> scopes) {

    /**
     * 复制范围集合；移除操作省略范围时规范化为空集合。
     */
    public RoleDisplayDelta {
        if (scopes != null) {
            scopes = Collections.unmodifiableList(new ArrayList<>(scopes));
        } else if (operation == RoleDeltaOperation.REMOVE) {
            scopes = List.of();
        }
    }
}
