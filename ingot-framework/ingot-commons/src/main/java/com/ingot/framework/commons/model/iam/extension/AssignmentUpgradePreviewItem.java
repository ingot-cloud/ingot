package com.ingot.framework.commons.model.iam.extension;

import java.util.List;
import java.util.Map;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.SubjectRef;
import com.ingot.framework.commons.model.iam.ValidationIssue;

/**
 * <p>
 * 单条分配升级的完整差异及待补参数。
 * </p>
 *
 * @param id 分配ID
 * @param subject 接收主体
 * @param previousRevisionRef 原固定版本
 * @param scopeBindings 可复用或草稿参数
 * @param before 原操作范围
 * @param after 目标操作范围
 * @param allowed 能否提交
 * @param issues 阻止原因
 * @param beforeFieldPermissions 升级前字段快照
 * @param afterFieldPermissions 升级后字段快照
 * @author jy
 * @since 1.0.0
 */
public record AssignmentUpgradePreviewItem(String id, SubjectRef subject, RoleRevisionRef previousRevisionRef,
        Map<String, ScopeBinding> scopeBindings, List<ActionGrant> before, List<ActionGrant> after, boolean allowed,
        List<ValidationIssue> issues,
        Map<String, Map<String, com.ingot.framework.commons.model.iam.FieldAccess>> beforeFieldPermissions,
        Map<String, Map<String, com.ingot.framework.commons.model.iam.FieldAccess>> afterFieldPermissions) {
    /** 兼容旧范围升级预览。 */
    public AssignmentUpgradePreviewItem(String id, SubjectRef subject, RoleRevisionRef previousRevisionRef,
            Map<String, ScopeBinding> scopeBindings, List<ActionGrant> before, List<ActionGrant> after, boolean allowed,
            List<ValidationIssue> issues) {
        this(id, subject, previousRevisionRef, scopeBindings, before, after, allowed, issues, null, null);
    }

}
