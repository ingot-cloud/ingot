package com.ingot.framework.commons.model.iam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>当前查看者对所属资源精确操作的字段能力规则，不使用目标对象范围。</p>
 * @param actionCode 精确操作
 * @param fieldKey 逻辑字段
 * @param viewerSelection 查看者选择
 * @param operations 编辑及筛选能力
 * @author jy
 * @since 1.0.0
 */
public record FieldOperationRule(@NotBlank String actionCode, @NotBlank String fieldKey,
        @NotNull @Valid Selection viewerSelection, @NotNull @Valid FieldOperations operations) {
}
