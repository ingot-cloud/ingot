package com.ingot.framework.commons.model.iam;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * <p>一次浏览同时返回部门分支和场景候选，两者独立分页。</p>
 * @author jy
 * @since 1.0.0
 * @param navigationAvailable 是否允许浏览部门
 * @param navigation 可披露的分支及面包屑，无权限时为空导航
 * @param members 当前场景可选成员分页
 */
@Schema(description = "组织成员选择器浏览结果")
public record OrganizationMemberPickerPage(boolean navigationAvailable, @NotNull DepartmentNavigationPage navigation,
        @NotNull PageResponse<OrganizationMemberPickerOption> members) { }
