package com.ingot.framework.commons.model.iam;

import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>描述场景、字段、查看者和目标范围对应的字段限制。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param scenario 后台或通讯录场景
 * @param fieldKey 资源目录已注册的字段键
 * @param viewerSelection 规则匹配的查看者
 * @param targetScope 目标范围项的并集，不独立开放对象
 * @param scopeBindings 目标范围的命名参数值
 * @param visibility 字段可见程度
 * @param editable 编辑上限，仍受对象、操作和平台规则限制
 */
@Schema(description = "描述场景、字段、查看者和目标范围对应的字段限制")
public record FieldRule(
        @NotNull @Schema(description = "后台或通讯录场景", requiredMode = Schema.RequiredMode.REQUIRED)
        PolicyScenario scenario,
        @NotBlank @Schema(description = "资源目录已注册的字段键", requiredMode = Schema.RequiredMode.REQUIRED)
        String fieldKey,
        @NotNull @Valid @Schema(description = "规则匹配的查看者", requiredMode = Schema.RequiredMode.REQUIRED)
        Selection viewerSelection,
        @NotNull @Schema(description = "目标范围项的并集，不独立开放对象", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid ScopeExpression> targetScope,
        @NotNull @Schema(description = "目标范围的命名参数值", requiredMode = Schema.RequiredMode.REQUIRED)
        Map<@NotBlank String, @NotNull @Valid ScopeBinding> scopeBindings,
        @NotNull @Schema(description = "字段可见程度", requiredMode = Schema.RequiredMode.REQUIRED)
        FieldVisibility visibility,
        @Schema(description = "编辑上限，仍受对象、操作和平台规则限制")
        boolean editable) {

    /**
     * 复制输入集合，防止校验与消费之间被外部修改；必填空引用由 Bean Validation 拒绝。
     */
    public FieldRule {
        if (targetScope != null) {
            targetScope = Collections.unmodifiableList(new ArrayList<>(targetScope));
        }
        if (scopeBindings != null) {
            scopeBindings = Collections.unmodifiableMap(new LinkedHashMap<>(scopeBindings));
        }
    }

    /**
     * 可编辑字段必须完整可见。
     *
     * @return 是否满足结构约束
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.validation.constraints.AssertTrue(message = "可编辑字段必须完整可见")
    @Schema(hidden = true)
    public boolean isEditableVisibilityValid() {
        return !editable || visibility == FieldVisibility.FULL;
    }
}
