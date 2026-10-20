package com.ingot.framework.commons.model.iam;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>选择器当前页成员快照，虚拟根展示与真实任职关系分别表达。</p>
 * @author jy
 * @since 1.0.0
 * @param id 租户成员ID
 * @param name 按场景字段权限投影的姓名
 * @param status 成员状态
 * @param available 是否可新增选择
 * @param departments 真实且可披露的任职
 * @param implicitRoot 确实未绑定任何部门时可披露的根展示，不是持久化任职
 * @param avatar 当前候选已授权的头像时效链接，未设置或隐藏时省略
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "组织成员选择器候选")
public record OrganizationMemberPickerOption(@NotBlank String id, String name, @NotNull MemberStatus status, boolean available,
        @NotNull List<MemberDepartmentView> departments, DepartmentNavigationEntry implicitRoot, String avatar) {
    /** 兼容不附带头像的既有场景投影。 */
    public OrganizationMemberPickerOption(String id, String name, MemberStatus status, boolean available,
                                          List<MemberDepartmentView> departments, DepartmentNavigationEntry implicitRoot) {
        this(id, name, status, available, departments, implicitRoot, null);
    }
}
