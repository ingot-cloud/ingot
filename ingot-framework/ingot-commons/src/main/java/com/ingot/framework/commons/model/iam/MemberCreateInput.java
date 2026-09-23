package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <p>把已有全局账号关联为当前域成员，不创建或改写登录凭证。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param accountId 已存在的全局账号 ID
 * @param displayName 当前域显示名，可空
 * @param avatar 当前域头像，可空
 * @param departments 租户任职；平台必须为空且最多一个主部门
 */
@Schema(description = "把已有全局账号关联为当前域成员，不创建或改写登录凭证")
public record MemberCreateInput(
        @NotBlank @Schema(description = "已存在的全局账号 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        String accountId,
        @Schema(description = "当前域显示名，可空")
        String displayName,
        @Schema(description = "当前域头像，可空")
        String avatar,
        @NotNull @Schema(description = "租户任职；平台必须为空且最多一个主部门", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotNull @Valid MemberDepartmentBinding> departments) {

    /**
     * 规范化任职集合。
     */
    public MemberCreateInput {
        if (departments != null) {
            departments = Collections.unmodifiableList(new ArrayList<>(departments));
        }
    }

    /**
     * 部门不能重复，且最多一个主部门。
     *
     * @return 是否满足结构约束，跨域合法性由服务按路径校验
     */
    @JsonIgnore
    @AssertTrue(message = "部门不能重复且最多一个主部门")
    @Schema(hidden = true)
    public boolean isDepartmentShapeValid() {
        return uniqueDepartments(departments);
    }

    static boolean uniqueDepartments(List<MemberDepartmentBinding> departments) {
        if (departments == null || departments.contains(null)) {
            return true;
        }
        var ids = new HashSet<String>();
        int primary = 0;
        for (MemberDepartmentBinding department : departments) {
            if (!ids.add(department.id()) || (department.primary() && ++primary > 1)) {
                return false;
            }
        }
        return true;
    }
}
