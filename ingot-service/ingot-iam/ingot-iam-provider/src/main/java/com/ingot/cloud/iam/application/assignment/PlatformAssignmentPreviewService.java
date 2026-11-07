package com.ingot.cloud.iam.application.assignment;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.ingot.cloud.iam.domain.shared.AuthorizationAdmission;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.cloud.iam.application.assignment.model.AssignmentBatchInput;
import com.ingot.cloud.iam.application.assignment.model.AssignmentInput;
import com.ingot.cloud.iam.application.assignment.model.AssignmentPreviewItem;
import com.ingot.cloud.iam.application.assignment.model.AssignmentPreviewResult;
import com.ingot.cloud.iam.application.assignment.model.AssignmentUpdateInput;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.domain.assignment.GrantStatus;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.application.shared.model.ImpactSummary;
import com.ingot.cloud.iam.application.organization.model.MemberRoleChanges;
import com.ingot.cloud.iam.application.shared.model.Preview;
import com.ingot.cloud.iam.application.assignment.model.ValidationIssue;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * <p>执行对应平台分配用例，逐项保留当前身份、来源和范围校验。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PlatformAssignmentPreviewService {
    private final PlatformAssignmentOperations operationsContext;

    /**
     * 预览批量分配，无写入。
     *
     * @param domain 接口管理域
     * @param input 批次
     * @return 逐条效果
     */
    public Preview<AssignmentPreviewResult> preview(AuthorizationDomain domain, AssignmentBatchInput input) {
        AuthorizationAdmission admission = operationsContext.access().admit(domain, PlatformAssignmentOperations.action(domain, PlatformAssignmentOperations.AccessKind.CREATE));
        List<AssignmentPreviewItem> items = new ArrayList<>();
        List<ValidationIssue> errors = new ArrayList<>(operationsContext.batchSource(admission, input));
        for (AssignmentInput item : input.items()) {
            List<ValidationIssue> itemErrors = operationsContext.validate(domain, admission, item, null);
            List<ActionGrant> grants = List.of();
            if (itemErrors.isEmpty()) {
                grants = operationsContext.roles().synthesizedGrants(IamIds.require(item.roleRevisionRef().id()));
            } else {
                errors.addAll(itemErrors);
            }
            items.add(new AssignmentPreviewItem(item.subject(), itemErrors.isEmpty(), itemErrors, grants));
        }
        AssignmentPreviewResult result = new AssignmentPreviewResult(items);
        return new Preview<>("0", errors.isEmpty(), errors, List.of(),
                new ImpactSummary(null, (long) input.items().size(), null, false), result);
    }

    /**
     * 预览一条既有分配，读、编辑与撤销遵循相同来源边界。
     * @param domain 管理域
     * @param id 分配 ID
     * @param input 草稿与版本
     * @return 无写入的校验效果
     */
    public Preview<AssignmentPreviewResult> previewUpdate(AuthorizationDomain domain, String id, AssignmentUpdateInput input) {
        AuthorizationAdmission admission = operationsContext.access().admit(domain, PlatformAssignmentOperations.action(domain, PlatformAssignmentOperations.AccessKind.UPDATE));
        var current = operationsContext.assignments().find(domain, PlatformAssignmentOperations.tenantId(domain, admission.actor()), IamIds.require(id));
        operationsContext.requireRecord(domain, admission, current);
        IamIds.requireVersion(input.expectedVersion(), PlatformAssignmentOperations.version(current.getVersion()));
        if (current.getStatus() != GrantStatus.ACTIVE) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        AssignmentInput next = input.assignment();
        if (!PlatformAssignmentOperations.sameSubject(current, next.subject()) || !PlatformAssignmentOperations.sameDelegation(current.getDelegationGrantId(), next.delegationGrantId())
                || current.getRevisionKind() != next.roleRevisionRef().kind()
                || current.getRevisionId().longValueExact() != IamIds.require(next.roleRevisionRef().id())) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        var errors = operationsContext.validate(domain, admission, next, IamIds.require(id));
        var grants = errors.isEmpty() ? operationsContext.roles().synthesizedGrants(IamIds.require(next.roleRevisionRef().id())) : List.<ActionGrant>of();
        return new Preview<>(PlatformAssignmentOperations.version(current.getVersion()), errors.isEmpty(), errors, List.of(),
                new ImpactSummary(null, 1L, null, false), new AssignmentPreviewResult(List.of(
                    new AssignmentPreviewItem(next.subject(), errors.isEmpty(), errors, grants))));
    }

    /**
     * 无写入地重验成员差量；任何一条越界、版本冲突或缺少完整治理资格均拒绝。
     * @param memberId 成员
     * @param changes 差量
     * @return 共用分配校验错误
     */
    public List<ValidationIssue> previewMemberRoleChanges(String memberId,
            MemberRoleChanges changes) {
        if (changes == null) return List.of();
        List<ValidationIssue> errors = new ArrayList<>();
        Set<String> touched = new LinkedHashSet<>();
        if (!changes.additions().isEmpty()) errors.addAll(preview(AuthorizationDomain.PLATFORM,
                new AssignmentBatchInput(operationsContext.memberDrafts(memberId, changes.additions()))).errors());
        for (var change : changes.updates()) {
            if (!touched.add(change.assignmentId())) throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            var row = operationsContext.memberEditableAssignment(memberId, change.assignmentId(), change.expectedVersion(), IamAction.PLATFORM_ASSIGNMENT_UPDATE);
            errors.addAll(previewUpdate(AuthorizationDomain.PLATFORM, change.assignmentId(), operationsContext.memberScopeInput(row, change)).errors());
        }
        for (var removal : changes.removals()) {
            if (!touched.add(removal.assignmentId())) throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            operationsContext.memberEditableAssignment(memberId, removal.assignmentId(), removal.expectedVersion(), IamAction.PLATFORM_ASSIGNMENT_DELETE);
        }
        return errors;
    }
}
