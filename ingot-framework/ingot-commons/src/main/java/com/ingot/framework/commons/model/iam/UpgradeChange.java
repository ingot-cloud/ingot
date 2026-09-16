package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>描述共享基础版本的单个变化，用稳定键关联冲突处理。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param key 稳定变化键
 * @param actionId 相关操作 ID，参数级变化时可空
 * @param kind 变化种类
 * @param oldScopes 旧基础范围，无对应操作时为空
 * @param newScopes 新基础范围，无对应操作时为空
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "描述共享基础版本的单个变化，用稳定键关联冲突处理")
public record UpgradeChange(
        @NotBlank @Schema(description = "稳定变化键", requiredMode = Schema.RequiredMode.REQUIRED)
        String key,
         @Schema(description = "相关操作 ID，参数级变化时可空")
        String actionId,
        @NotNull @Schema(description = "变化种类", requiredMode = Schema.RequiredMode.REQUIRED)
        UpgradeChangeKind kind,
        @NotNull @Schema(description = "旧基础范围，无对应操作时为空", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ScopeExpression> oldScopes,
        @NotNull @Schema(description = "新基础范围，无对应操作时为空", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ScopeExpression> newScopes) {

    /**
     * 复制输出集合，避免外部修改改变已经计算的响应视图；必填空引用由校验拒绝。
     */
    public UpgradeChange {
        if (oldScopes != null) {
            oldScopes = Collections.unmodifiableList(new ArrayList<>(oldScopes));
        }
        if (newScopes != null) {
            newScopes = Collections.unmodifiableList(new ArrayList<>(newScopes));
        }
    }
}
