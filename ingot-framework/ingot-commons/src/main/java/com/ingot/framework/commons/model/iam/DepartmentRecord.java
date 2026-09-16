package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>返回当前域内可见部门或必要导航骨架，骨架不携带成员数量。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 部门 ID
 * @param parentId 父部门 ID，根节点为空
 * @param name 可见部门名称
 * @param sortOrder 展示顺序
 * @param navigationOnly 是否仅作为必要祖先导航骨架
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "返回当前域内可见部门或必要导航骨架，骨架不携带成员数量")
public record DepartmentRecord(
        @NotBlank @Schema(description = "部门 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String id,
         @Schema(description = "父部门 ID，根节点为空")
        String parentId,
        @NotBlank @Schema(description = "可见部门名称", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
         @Schema(description = "展示顺序", requiredMode = Schema.RequiredMode.REQUIRED)
        int sortOrder,
         @Schema(description = "是否仅作为必要祖先导航骨架", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean navigationOnly) {
}
