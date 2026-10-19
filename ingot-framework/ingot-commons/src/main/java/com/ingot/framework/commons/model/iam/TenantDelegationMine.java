package com.ingot.framework.commons.model.iam;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * <p>仅返回本人有效委派的受限分配依据。</p>
 * @author jy
 * @since 1.0.0
 * @param id 对象ID
 * @param allowedRoles 允许分配角色
 * @param recipientSummary 不泄露范围外信息的接收摘要
 * @param validFrom 可空UTC生效瞬时
 * @param validUntil 可空UTC截止瞬时
 * @param maxAssignmentDuration 正的最长分配持续时间
 * @param scopeIssues 安全关闭的待补范围
 * @param directAssignmentActions 当前身份可独立治理的分配操作，用于来源选择；不是委派所授权限
 */
@Schema(description = "仅返回本人有效委派的受限分配依据")
public record TenantDelegationMine(
        String id,
        List<CurrentRoleSummary> allowedRoles,
        String recipientSummary,
        Instant validFrom,
        Instant validUntil,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Duration maxAssignmentDuration,
        List<ScopeIssue> scopeIssues,
        List<String> directAssignmentActions) {

    /** 固定授权集合，避免调用方在验证后修改内容。 */
    public TenantDelegationMine {
        if (allowedRoles != null) allowedRoles = List.copyOf(allowedRoles);
        if (scopeIssues != null) scopeIssues = List.copyOf(scopeIssues);
        directAssignmentActions = directAssignmentActions == null ? List.of() : List.copyOf(directAssignmentActions);
    }
}
