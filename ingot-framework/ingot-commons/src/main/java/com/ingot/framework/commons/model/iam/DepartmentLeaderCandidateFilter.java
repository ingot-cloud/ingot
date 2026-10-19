package com.ingot.framework.commons.model.iam;

import com.ingot.framework.commons.annotation.field.FieldBinding;
import com.ingot.framework.commons.annotation.field.FieldUse;
import com.ingot.framework.commons.annotation.field.PublicField;

/**
 * <p>既有部门负责人候选的完整参数分类，不扩大成员或浏览部门的可见范围。</p>
 * @param keyword 显示名关键词，沿用原文字段检索资格
 * @param browseDepartmentId 浏览部门，目标部门由路径参数指定
 * @param page 候选页码
 * @param pageSize 候选页大小
 * @author jy
 * @since 1.0.0
 */
public record DepartmentLeaderCandidateFilter(
        @FieldBinding(key = MemberFieldKey.VALUE_DISPLAY_NAME, uses = FieldUse.FILTER) String keyword,
        @PublicField String browseDepartmentId,
        @PublicField Integer page,
        @PublicField Integer pageSize) { }
