package com.ingot.cloud.pms.api.model.vo.authorization;

import com.ingot.cloud.pms.api.model.enums.AuthorizationAuditCategoryEnum;
import lombok.Builder;
import lombok.Getter;

/**
 * <p>授权数据审计单条问题，含分类、定位实体与修复建议。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Builder
public class AuthorizationAuditIssueVO {
    private final AuthorizationAuditCategoryEnum category;
    private final String entityType;
    private final Long entityId;
    private final String message;
    private final String suggestion;
}
