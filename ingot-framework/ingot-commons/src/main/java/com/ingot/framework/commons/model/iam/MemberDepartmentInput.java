package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>整体替换租户成员任职，只校验受影响的原部门与目标部门。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 成员读取版本
 * @param departments 完整目标关系，允许空集合表示无部门
 */
@Schema(description = "整体替换租户成员任职，只校验受影响的原部门与目标部门")
public record MemberDepartmentInput(
        @NotBlank @Schema(description = "成员读取版本", requiredMode = Schema.RequiredMode.REQUIRED)
        String expectedVersion,
        @NotNull @Schema(description = "完整目标关系，允许空集合表示无部门", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid MemberDepartmentBinding> departments) {

    /**
     * 复制目标关系。
     */
    public MemberDepartmentInput {
        if (departments != null) {
            departments = Collections.unmodifiableList(new ArrayList<>(departments));
        }
    }

    /**
     * 部门不能重复，且最多一个主部门。
     *
     * @return 是否满足结构约束
     */
    @JsonIgnore
    @AssertTrue(message = "部门不能重复且最多一个主部门")
    @Schema(hidden = true)
    public boolean isDepartmentShapeValid() {
        return MemberCreateInput.uniqueDepartments(departments);
    }
}
