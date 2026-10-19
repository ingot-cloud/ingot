package com.ingot.framework.commons.model.iam;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * <p>返回组织资源委派和当前待补范围。</p>
 * @author jy
 * @since 1.0.0
 * @param id 对象ID
 * @param delegation 完整委派限制
 * @param status 当前启停或撤销状态
 * @param administratorName 管理员可披露名称
 * @param scopeIssues 安全关闭的待补范围
 */
@Schema(description = "返回组织资源委派和当前待补范围")
public record TenantDelegationRecord(
        String id,
        TenantDelegationInput delegation,
        GrantStatus status,
        String administratorName,
        List<ScopeIssue> scopeIssues) {

    /** 固定授权集合，避免调用方在验证后修改内容。 */
    public TenantDelegationRecord {
        if (scopeIssues != null) scopeIssues = List.copyOf(scopeIssues);
    }
}
