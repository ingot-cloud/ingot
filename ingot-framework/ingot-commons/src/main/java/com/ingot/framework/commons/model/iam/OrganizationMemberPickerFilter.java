package com.ingot.framework.commons.model.iam;

import java.util.List;
import com.ingot.framework.commons.annotation.field.FieldBinding;
import com.ingot.framework.commons.annotation.field.FieldUse;
import com.ingot.framework.commons.annotation.field.PublicField;

/**
 * <p>通用成员候选的完整参数分类，不改变部门、已知ID与字段读取的既有交集。</p>
 * @param keyword 显示名关键词，沿用原文字段检索资格
 * @param departmentId 浏览部门
 * @param ids 已知成员ID回显集合，服务层继续重验可见范围
 * @param page 人员页码
 * @param pageSize 人员页大小
 * @author jy
 * @since 1.0.0
 */
public record OrganizationMemberPickerFilter(
        @FieldBinding(key = MemberFieldKey.VALUE_DISPLAY_NAME, uses = FieldUse.FILTER) String keyword,
        @PublicField String departmentId,
        @PublicField List<String> ids,
        @PublicField Integer page,
        @PublicField Integer pageSize) { }
