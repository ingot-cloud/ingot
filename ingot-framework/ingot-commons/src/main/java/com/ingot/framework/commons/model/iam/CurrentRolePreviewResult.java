package com.ingot.framework.commons.model.iam;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * <p>解释当前权限草稿的变化与安全关闭影响。</p>
 * @author jy
 * @since 1.0.0
 * @param definition 角色完整当前定义
 * @param addedActionIds 新增操作
 * @param removedActionIds 移除操作
 * @param changedScopeActionIds 范围变化操作
 * @param scopeIssueAssignments 可披露待补分配计数
 */
@Schema(description = "解释当前权限草稿的变化与安全关闭影响")
public record CurrentRolePreviewResult(
        CurrentRoleDefinition definition,
        List<String> addedActionIds,
        List<String> removedActionIds,
        List<String> changedScopeActionIds,
        Long scopeIssueAssignments) {

    /** 固定授权集合，避免调用方在验证后修改内容。 */
    public CurrentRolePreviewResult {
        if (addedActionIds != null) addedActionIds = List.copyOf(addedActionIds);
        if (removedActionIds != null) removedActionIds = List.copyOf(removedActionIds);
        if (changedScopeActionIds != null) changedScopeActionIds = List.copyOf(changedScopeActionIds);
    }
}
