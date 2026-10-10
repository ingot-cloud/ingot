package com.ingot.framework.commons.model.iam;

import java.util.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>描述资源字段允许配置的可见性与编辑能力，不能代替对象级计算结果。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param key 已注册字段键
 * @param label 字段展示名
 * @param visibilities 支持的可见程度
 * @param editable 资源是否支持编辑此字段
 * @param filterable 完整可见时是否支持筛选
 * @param mask 当前资源版本使用的文本脱敏规则
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "描述资源字段允许配置的可见性与编辑能力，不能代替对象级计算结果")
public record FieldCapability(
        @NotBlank @Schema(description = "已注册字段键", requiredMode = Schema.RequiredMode.REQUIRED)
        String key,
        @NotBlank @Schema(description = "字段展示名", requiredMode = Schema.RequiredMode.REQUIRED)
        String label,
        @NotNull @Schema(description = "支持的可见程度", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull FieldVisibility> visibilities,
         @Schema(description = "资源是否支持编辑此字段", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean editable,
         @Schema(description = "完整可见时是否支持筛选", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean filterable,
        @Valid @Schema(description = "文本脱敏规则，支持 MASKED 时必填") MaskSpec mask) {

    /**
     * 复制输出集合，避免外部修改改变已经计算的响应视图；必填空引用由校验拒绝。
     */
    public FieldCapability {
        if (visibilities != null) {
            visibilities = Collections.unmodifiableList(new ArrayList<>(visibilities));
            if (visibilities.isEmpty() || new HashSet<>(visibilities).size() != visibilities.size()
                    || visibilities.contains(FieldVisibility.MASKED) != (mask != null))
                throw new IllegalArgumentException("字段可见性和脱敏规则不一致");
        }
    }
}
