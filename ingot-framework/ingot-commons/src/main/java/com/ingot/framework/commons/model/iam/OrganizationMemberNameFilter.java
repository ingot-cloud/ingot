package com.ingot.framework.commons.model.iam;

import com.ingot.framework.commons.annotation.field.FieldBinding;
import com.ingot.framework.commons.annotation.field.FieldUse;
import com.ingot.framework.commons.annotation.field.PublicField;

/**
 * <p>部门负责人关联分页的完整参数分类，关键词映射显示名并在SQL与计数前校验。</p>
 * @author jy
 * @since 1.0.0
 * @param keyword 显示名关键词，不包含尚未接入的手机号或邮箱检索
 * @param page 关联页码，服务层验证合法范围
 * @param pageSize 关联页大小，服务层验证合法范围
 */
public record OrganizationMemberNameFilter(
        @FieldBinding(key = MemberFieldKey.VALUE_DISPLAY_NAME, uses = FieldUse.FILTER) String keyword,
        @PublicField Integer page,
        @PublicField Integer pageSize) { }
