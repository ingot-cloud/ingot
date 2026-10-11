package com.ingot.framework.commons.model.iam;

import java.util.Map;

/**
 * <p>一条保留主体、来源和期限的分配应用预览。</p>
 *
 * @param id 明细ID
 * @param assignmentId 分配ID
 * @param status 处理状态
 * @param original 原分配完整快照
 * @param configuredBindings 配置后参数
 * @param preview 操作、范围和字段权限差异
 * @param failureReason 冲突原因
 * @author jy
 * @since 1.0.0
 */
public record RoleApplicationItem(
        String id,
        String assignmentId,
        RoleApplicationItemStatus status,
        AssignmentRecord original,
        Map<String, ScopeBinding> configuredBindings,
        com.ingot.framework.commons.model.iam.extension.AssignmentUpgradePreviewItem preview,
        String failureReason) {
}
