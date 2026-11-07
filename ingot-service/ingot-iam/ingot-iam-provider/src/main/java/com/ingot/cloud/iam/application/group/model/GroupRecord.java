package com.ingot.cloud.iam.application.group.model;

import com.ingot.framework.commons.model.iam.IamCountSerializer;
import com.ingot.framework.commons.model.iam.Selection;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回用户组元数据和可见成员来源，实际权限由分配与委派引擎另行计算。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 组 ID
 * @param name 组名称
 * @param description 说明，可空
 * @param selection 当前域内的直接成员及部门来源
 * @param selectionEditable 允许完整替换成员来源，仅在用户组管理接口返回
 * @param visibleMemberCount 仅有权部分的有效人数；无法披露时省略
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回用户组元数据和可见成员来源，实际权限由分配与委派引擎另行计算")
public record GroupRecord(
        @NotBlank @Schema(description = "组 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
        @NotBlank @Schema(description = "组名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
         @Schema(description = "说明，可空")
        String description,
        @NotNull @Valid @Schema(description = "当前域内的直接成员及部门来源", requiredMode = Schema.RequiredMode.REQUIRED)
        Selection selection,
         @PositiveOrZero @JsonSerialize(using = IamCountSerializer.class) @Schema(description = "仅有权部分的有效人数；无法披露时省略")
        Long visibleMemberCount,
        @Schema(description = "当前操作者可以完整替换成员来源") Boolean selectionEditable) {
}
