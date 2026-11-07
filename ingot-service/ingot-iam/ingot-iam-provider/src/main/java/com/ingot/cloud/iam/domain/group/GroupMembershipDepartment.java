package com.ingot.cloud.iam.domain.group;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>标出令成员进入租户组的一条部门规则，名称仅在操作者可见时填写。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 组上配置的部门 ID
 * @param name 可见部门名称；无权查看时省略
 * @param includeDescendants 该规则是否连带下级
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "令成员进入租户组的部门规则；名称仅在可见时填写")
public record GroupMembershipDepartment(
        @NotBlank @Schema(description = "组上配置的部门 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @Schema(description = "可见部门名称；无权查看时省略")
        String name,
        @Schema(description = "该规则是否连带下级", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean includeDescendants) {
}
