package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>表示租户选择器中的单个部门及其明确的下级包含规则。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 当前租户部门 ID
 * @param includeDescendants 是否包含下级，未指定时为 false
 */
@Schema(description = "租户部门选择项；平台域不支持")
public record DepartmentSelection(
        @NotBlank @Schema(description = "部门 ID", requiredMode = Schema.RequiredMode.REQUIRED) String id,
        @Schema(description = "是否包含下级，默认 false") boolean includeDescendants) {
}
