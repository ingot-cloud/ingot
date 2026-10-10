package com.ingot.framework.commons.model.iam;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ingot.framework.commons.annotation.field.*;

/**
 * <p>平台成员已有查询条件的绑定声明，不自动生成 SQL。</p>
 * @param displayName 接口 name 条件
 * @param status 成员资格
 * @param ids 已选 ID
 * @param page 页码
 * @param pageSize 页大小
 * @author jy
 * @since 1.0.0
 */
public record PlatformMemberFilter(
        @FieldBinding(key = MemberFieldKey.VALUE_DISPLAY_NAME, uses = FieldUse.FILTER) @JsonProperty("name") String displayName,
        @PublicField String status, @PublicField String ids, @PublicField Integer page, @PublicField Integer pageSize) {
}
