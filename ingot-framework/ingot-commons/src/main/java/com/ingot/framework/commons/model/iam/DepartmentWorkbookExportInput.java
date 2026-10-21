package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * <p>登记部门更新导出任务；空部门列表表示当前可见的全部部门。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 组织版本
 * @param departmentIds 部分部门 ID；空或省略表示全部可见部门，服务端展开下级
 */
@Schema(description = "登记部门更新导出任务；空部门列表表示当前可见的全部部门")
public record DepartmentWorkbookExportInput(
        @NotBlank @Schema(description = "组织版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @Schema(description = "部分部门 ID；空或省略表示全部可见部门，服务端展开下级")
        List<String> departmentIds) {

    /**
     * 复制部门 ID 列表。
     */
    public DepartmentWorkbookExportInput {
        departmentIds = departmentIds == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(departmentIds));
    }
}
