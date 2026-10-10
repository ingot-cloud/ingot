package com.ingot.framework.commons.model.iam;

import com.ingot.framework.commons.annotation.field.*;

/**
 * <p>成员管理与通讯录已有筛选条件，逻辑键独立于业务 SQL。</p>
 * @param phone 组织联系手机号
 * @param email 组织联系邮箱
 * @param purpose 通讯录场景
 * @param page 页码
 * @param pageSize 页大小
 * @author jy
 * @since 1.0.0
 */
public record TenantMemberFilter(@FieldBinding(key = MemberFieldKey.VALUE_PHONE, uses = FieldUse.FILTER) String phone,
        @FieldBinding(key = MemberFieldKey.VALUE_EMAIL, uses = FieldUse.FILTER) String email,
        @PublicField PolicyScenario purpose, @PublicField Integer page, @PublicField Integer pageSize) {
}
