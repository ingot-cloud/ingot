package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>描述服务端计算后的字段输出与编辑能力，不独立授予对象或写操作权限。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param visibility 字段可见程度
 * @param editable 是否允许编辑，非 FULL 必须为 false
 */
@Schema(description = "描述服务端计算后的字段输出与编辑能力，不独立授予对象或写操作权限")
public record FieldAccess(
        @NotNull @Schema(description = "字段可见程度", requiredMode = Schema.RequiredMode.REQUIRED)
        FieldVisibility visibility,
        @Schema(description = "是否允许编辑，非 FULL 必须为 false")
        boolean editable) {
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
