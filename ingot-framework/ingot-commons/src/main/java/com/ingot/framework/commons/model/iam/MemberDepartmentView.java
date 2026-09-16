package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回操作者有权查看的成员部门关系，不包含隐藏关系统计。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 部门 ID
 * @param name 可见部门名称
 * @param primary 是否主部门
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回操作者有权查看的成员部门关系，不包含隐藏关系统计")
public record MemberDepartmentView(
        @NotBlank @Schema(description = "部门 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "可见部门名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
         @Schema(description = "是否主部门", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean primary) {
}
