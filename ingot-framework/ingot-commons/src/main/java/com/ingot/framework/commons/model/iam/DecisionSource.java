package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>
 * 只返回操作者可获知的授权来源，不能披露的标识省略并给出概括说明。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 * @param assignmentId 可披露授权 ID，可空
 * @param delegationId 可披露委派 ID，可空
 * @param roleRevisionRef 可披露角色版本，可空
 * @param summary 安全概括说明
 * @param resourceFieldPermissions 可披露固定版本字段快照；历史版本为 null
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "只返回操作者可获知的授权来源，不能披露的标识省略并给出概括说明")
public record DecisionSource(@Schema(description = "可披露授权 ID，可空") String assignmentId,
        @Schema(description = "可披露委派 ID，可空") String delegationId,
        @Valid @Schema(description = "可披露角色版本，可空") RoleRevisionRef roleRevisionRef,
        @NotBlank @Schema(description = "安全概括说明", requiredMode = Schema.RequiredMode.REQUIRED) String summary,
        @Schema(description = "可披露固定版本字段快照；空引用表示历史安全默认") java.util.Map<String, java.util.Map<String, FieldAccess>> resourceFieldPermissions) {
    /** 兼容历史来源结论。 */
    public DecisionSource(String assignmentId, String delegationId, RoleRevisionRef roleRevisionRef, String summary) {
        this(assignmentId, delegationId, roleRevisionRef, summary, null);
    }

    /** 复制来源字段快照。 */
    public DecisionSource {
        resourceFieldPermissions = ResourceFieldPermissions.copy(resourceFieldPermissions);
    }
}
