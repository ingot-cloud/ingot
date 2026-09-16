package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>只返回操作者可获知的授权来源，不能披露的标识省略并给出概括说明。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param assignmentId 可披露授权 ID，可空
 * @param delegationId 可披露委派 ID，可空
 * @param roleRevisionRef 可披露角色版本，可空
 * @param summary 安全概括说明
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "只返回操作者可获知的授权来源，不能披露的标识省略并给出概括说明")
public record DecisionSource(
         @Schema(description = "可披露授权 ID，可空")
        String assignmentId,
         @Schema(description = "可披露委派 ID，可空")
        String delegationId,
        @Valid @Schema(description = "可披露角色版本，可空")
        RoleRevisionRef roleRevisionRef,
        @NotBlank @Schema(description = "安全概括说明", requiredMode = Schema.RequiredMode.REQUIRED)
        String summary) {
}
