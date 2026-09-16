package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>提交租户成员的目标部门关系，不修改部门资料或全局账号。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 当前租户部门 ID
 * @param primary 是否主部门；同一次提交最多一个
 */
@Schema(description = "提交租户成员的目标部门关系，不修改部门资料或全局账号")
public record MemberDepartmentBinding(
        @NotBlank @Schema(description = "当前租户部门 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @Schema(description = "是否主部门；同一次提交最多一个", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean primary) {
}
