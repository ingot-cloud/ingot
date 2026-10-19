package com.ingot.framework.commons.model.iam;

import com.ingot.framework.commons.annotation.field.FieldBinding;
import com.ingot.framework.commons.annotation.field.FieldUse;
import com.ingot.framework.commons.annotation.field.PublicField;

/**
 * <p>组织选人浏览的完整参数分类；公开控制参数仍由业务校验场景、范围及分页。</p>
 * @param keyword 显示名关键词，沿用原文字段检索资格
 * @param scene 候选资格来源
 * @param departmentId 当前浏览部门，与维护的目标部门区分
 * @param targetDepartmentId 负责人场景维护的部门
 * @param delegationGrantId 委派场景的来源授权
 * @param page 人员页码
 * @param pageSize 人员页大小
 * @param departmentPage 部门页码
 * @param departmentPageSize 部门页大小
 * @author jy
 * @since 1.0.0
 */
public record OrganizationMemberBrowseFilter(
        @FieldBinding(key = MemberFieldKey.VALUE_DISPLAY_NAME, uses = FieldUse.FILTER) String keyword,
        @PublicField OrganizationMemberPickerScene scene,
        @PublicField String departmentId,
        @PublicField String targetDepartmentId,
        @PublicField String delegationGrantId,
        @PublicField Integer page,
        @PublicField Integer pageSize,
        @PublicField Integer departmentPage,
        @PublicField Integer departmentPageSize) { }
