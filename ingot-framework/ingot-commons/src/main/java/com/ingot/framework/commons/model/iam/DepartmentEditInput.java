package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * <p>以同一版本原子编辑部门资料和负责人，按实际修改分别验证操作权限。</p>
 * @author jy
 * @since 1.0.0
 * @param expectedVersion 部门读取版本
 * @param department 本次修改的基本资料，未修改时省略
 * @param leaders 本次负责人差量，未修改时省略
 */
@Schema(description = "统一部门编辑，基本资料与负责人原子保存")
public record DepartmentEditInput(@NotBlank String expectedVersion,
                                  @Valid DepartmentDraft department, @Valid DepartmentLeaderChanges leaders) {
    /** 至少明确提供一个待编辑部分。 */
    @AssertTrue @JsonIgnore
    public boolean isChangeProvided() { return department != null || leaders != null; }
}
