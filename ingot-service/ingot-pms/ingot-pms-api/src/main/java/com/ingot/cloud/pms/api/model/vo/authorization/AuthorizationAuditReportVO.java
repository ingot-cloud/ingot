package com.ingot.cloud.pms.api.model.vo.authorization;

import java.util.List;
import java.util.Map;

import com.ingot.cloud.pms.api.model.enums.AuthorizationAuditCategoryEnum;
import lombok.Builder;
import lombok.Getter;

/**
 * <p>授权数据审计报告，汇总问题总数、分类计数与明细列表。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@Builder
public class AuthorizationAuditReportVO {
    private final int totalIssues;
    private final Map<AuthorizationAuditCategoryEnum, Integer> countsByCategory;
    private final List<AuthorizationAuditIssueVO> issues;
}
