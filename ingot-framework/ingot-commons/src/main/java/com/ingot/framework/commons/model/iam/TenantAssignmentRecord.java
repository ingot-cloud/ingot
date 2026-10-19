package com.ingot.framework.commons.model.iam;

import java.time.Instant;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * <p>返回角色身份分配、有效状态及待补范围。</p>
 * @author jy
 * @since 1.0.0
 * @param id 对象ID
 * @param assignment 角色分配定义
 * @param status 当前启停或撤销状态
 * @param source 真实分配来源
 * @param subjectName 接收者名称
 * @param roleName 角色名称
 * @param roleKind 角色归属类型
 * @param delegationSummary 来源委派摘要
 * @param createdAt UTC创建瞬时
 * @param grantedBy 真实授权人
 * @param effectiveStatus 持续约束下的生效状态
 * @param scopeIssues 安全关闭的待补范围
 */
@Schema(description = "返回角色身份分配、有效状态及待补范围")
public record TenantAssignmentRecord(
        String id,
        TenantAssignmentInput assignment,
        GrantStatus status,
        AssignmentSource source,
        String subjectName,
        String roleName,
        RoleKind roleKind,
        String delegationSummary,
        Instant createdAt,
        AssignmentAuthor grantedBy,
        AssignmentEffectiveStatus effectiveStatus,
        List<ScopeIssue> scopeIssues) {

    /** 固定授权集合，避免调用方在验证后修改内容。 */
    public TenantAssignmentRecord {
        if (scopeIssues != null) scopeIssues = List.copyOf(scopeIssues);
    }
}
