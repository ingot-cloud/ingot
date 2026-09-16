package com.ingot.framework.commons.model.iam;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <p>提交部门名称与树位置，不携带成员计数。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param parentId 父部门 ID，根节点为空
 * @param name 部门名称
 * @param sortOrder 展示顺序
 */
@Schema(description = "提交部门名称与树位置，不携带成员计数")
public record DepartmentDraft(
        @Schema(description = "父部门 ID，根节点为空")
        String parentId,
        @NotBlank @Size(max = 128) @Schema(description = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "展示顺序", requiredMode = Schema.RequiredMode.REQUIRED)
        int sortOrder) {
}
