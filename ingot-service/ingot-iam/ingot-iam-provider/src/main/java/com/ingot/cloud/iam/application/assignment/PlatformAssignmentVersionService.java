package com.ingot.cloud.iam.application.assignment;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ingot.cloud.iam.infrastructure.persistence.IamRoleRevisionJoin;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleRevisionEntity;
import com.ingot.cloud.iam.domain.shared.AuthorizationAdmission;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.cloud.iam.application.assignment.model.AssignmentBatchInput;
import com.ingot.cloud.iam.application.assignment.model.AssignmentInput;
import com.ingot.cloud.iam.domain.audit.AuditChangeType;
import com.ingot.cloud.iam.domain.audit.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.domain.assignment.GrantStatus;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.application.shared.model.ImpactSummary;
import com.ingot.cloud.iam.application.assignment.model.AssignmentUpgradeInput;
import com.ingot.cloud.iam.application.assignment.model.AssignmentUpgradeItem;
import com.ingot.cloud.iam.application.assignment.model.AssignmentUpgradePreviewItem;
import com.ingot.cloud.iam.application.assignment.model.AssignmentUpgradeResult;
import com.ingot.cloud.iam.application.shared.model.Preview;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;
import com.ingot.framework.commons.model.iam.ScopeBinding;
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
public class PlatformAssignmentVersionService {
    private final PlatformAssignmentOperations operationsContext;

    /**
     * 预览同角色分配的版本升级，主体、期限与委派来源全部保留。
     * @param input 目标固定版本与记录
     * @return 逐条差异及参数问题
     */
    public Preview<AssignmentUpgradeResult> previewUpgrade(AssignmentUpgradeInput input) {
        var admission = operationsContext.access().admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_UPGRADE);
        var result = upgradePlan(admission, input, false, true);
        var errors = result.items().stream().flatMap(item -> item.issues().stream()).toList();
        return new Preview<>(input.targetRevisionRef().id(), errors.isEmpty(), errors, List.of(),
                new com.ingot.cloud.iam.application.shared.model.ImpactSummary(null, (long) result.items().size(), null,
                        false),
                result);
    }

    /**
     * 原子升级同角色分配；事务内重验权限、版本和全部范围，不撤销重建记录。
     * @param input 目标与逐条草稿
     * @return 保存结果
     */
    public AssignmentUpgradeResult upgrade(AssignmentUpgradeInput input) {
        operationsContext.access().admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_UPGRADE);
        return operationsContext.transaction().execute(status -> {
            operationsContext.assignments().lockAuthorization(AuthorizationDomain.PLATFORM);
            var target = operationsContext.assignments().findRevision(IamIds.require(input.targetRevisionRef().id()));
            if (target == null)
                throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
            operationsContext.roleStore().lockDefinition(AuthorizationDomain.PLATFORM, false, null, target.getRoleId().longValueExact());
            var admission = operationsContext.access().admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_UPGRADE);
            var result = upgradePlan(admission, input, true, true);
            if (result.items().stream().anyMatch(item -> !item.allowed()))
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            writeApplication(admission, input, result);
            return result;
        });
    }

    /**
     * 以已恢复的可信身份预检双向应用；必须具备独立治理资格。
     * @param admission 实时身份和治理准入
     * @param input 目标固定版本和最多一百条记录
     * @return 完整差异及范围问题
     */
    public AssignmentUpgradeResult previewApplication(AuthorizationAdmission admission, AssignmentUpgradeInput input) {
        if (!admission.governed()) throw new BizException(IamReasonCode.ACTION_DENIED);
        return upgradePlan(admission, input, false, false);
    }

    /**
     * 在调用方事务内应用一个批次；调用方先锁授权、角色并恢复身份。
     * @param admission 当批次的实时身份
     * @param input 固定版本及预期分配版本
     * @return 当批次的完整差异
     */
    public AssignmentUpgradeResult applyApplication(AuthorizationAdmission admission, AssignmentUpgradeInput input) {
        if (!admission.governed()) throw new BizException(IamReasonCode.ACTION_DENIED);
        var result = upgradePlan(admission, input, true, false);
        if (result.items().stream().anyMatch(item -> !item.allowed()))
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        writeApplication(admission, input, result);
        return result;
    }

    private void writeApplication(AuthorizationAdmission admission, AssignmentUpgradeInput input, AssignmentUpgradeResult result) {
            for (var item : result.items()) {
                var current = operationsContext.assignments().find(AuthorizationDomain.PLATFORM, null, IamIds.require(item.id()));
                operationsContext.assignments().update(IamIds.require(item.id()), IamIds.require(input.targetRevisionRef().id()),
                        input.targetRevisionRef().kind(), IamJsonCodec.object(item.scopeBindings()), current.getValidFrom(),
                        current.getValidUntil(), current.getVersion());
                operationsContext.audits().write(admission.actor().context(), operationsContext.access().nextId(), PlatformAssignmentOperations.ASSIGNMENT, item.id(),
                        AuditChangeType.UPDATE,
                        Map.of(AuditField.ROLE_REVISION, item.previousRevisionRef().id(), AuditField.SCOPE,
                                current.getScopeBindings()),
                        Map.of(AuditField.ROLE_REVISION, input.targetRevisionRef().id(), AuditField.SCOPE,
                                IamJsonCodec.object(item.scopeBindings())),
                        Map.of(PlatformAssignmentOperations.ASSIGNMENT, PlatformAssignmentOperations.nextVersion(current.getVersion())), PlatformAssignmentOperations.text(current.getDelegationGrantId()),
                        item.id());
            }
            operationsContext.changes().markAll();
    }

    private AssignmentUpgradeResult upgradePlan(AuthorizationAdmission admission, AssignmentUpgradeInput input, boolean locked, boolean higherOnly) {
        if (input.items().isEmpty() || input.items().size() > AssignmentUpgradeInput.MAX_ITEMS
                || input.items().stream().map(AssignmentUpgradeItem::id).distinct().count() != input.items().size())
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        var target = operationsContext.assignments().findRevision(IamIds.require(input.targetRevisionRef().id()));
        var published = operationsContext.roleStore().findRevision(IamIds.require(input.targetRevisionRef().id()));
        if (target == null || published == null || target.getKind() != input.targetRevisionRef().kind()
                || !operationsContext.revisionAssignable(AuthorizationDomain.PLATFORM, admission.actor(), target)
                || !Boolean.TRUE.equals(target.getEnabled()))
            throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
        var after = operationsContext.roles().synthesizedGrants(IamIds.require(input.targetRevisionRef().id()));
        List<AssignmentUpgradePreviewItem> items = new ArrayList<>();
        List<AssignmentInput> drafts = new ArrayList<>();
        var objectScope = admission.governed() ? operationsContext.resourceAccess().objectsForExecution(admission.actor().context(),
                IamAction.PLATFORM_ASSIGNMENT_UPGRADE) : com.ingot.cloud.iam.domain.authorization.ObjectScope.none();
        Map<String, Map<String, PlatformAssignmentOperations.ParameterMeaning>> meaningCache = new LinkedHashMap<>();
        var targetMeaning = parameterMeanings(input.targetRevisionRef(), after);
        Map<BigInteger, com.ingot.cloud.iam.infrastructure.persistence.IamRoleRevisionJoin> previousJoins = new LinkedHashMap<>();
        Map<BigInteger, com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleRevisionEntity> previousVersions = new LinkedHashMap<>();
        Map<BigInteger, List<ActionGrant>> previousGrants = new LinkedHashMap<>();
        // Numeric order preserves the existing global authorization -> role -> assignment
        // lock order.
        for (var requested : input.items()
            .stream()
            .sorted(java.util.Comparator.comparing(i -> new BigInteger(i.id())))
            .toList()) {
            var row = locked ? operationsContext.assignments().lock(AuthorizationDomain.PLATFORM, null, IamIds.require(requested.id()))
                    : operationsContext.assignments().find(AuthorizationDomain.PLATFORM, null, IamIds.require(requested.id()));
            operationsContext.requireRecord(AuthorizationDomain.PLATFORM, admission, row);
            if (admission.governed() && !com.ingot.cloud.iam.infrastructure.persistence.ObjectScopeSql.matches(objectScope, row.getId().longValueExact()))
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            IamIds.requireVersion(requested.expectedVersion(), PlatformAssignmentOperations.version(row.getVersion()));
            var old = previousJoins.computeIfAbsent(row.getRevisionId(), key -> operationsContext.assignments().findRevision(key.longValueExact()));
            var oldPublished = previousVersions.computeIfAbsent(row.getRevisionId(), key -> operationsContext.roleStore().findRevision(key.longValueExact()));
            if (old == null || oldPublished == null || !old.getRoleId().equals(target.getRoleId()))
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            var original = operationsContext.record(row).assignment();
            var before = previousGrants.computeIfAbsent(row.getRevisionId(), key -> operationsContext.roles().synthesizedGrants(key.longValueExact()));
            var previousMeaning = meaningCache.computeIfAbsent(original.roleRevisionRef().id(),
                    key -> parameterMeanings(original.roleRevisionRef(), before));
            var bindings = requested.scopeBindings() == null ? reusableBindings(original, previousMeaning, targetMeaning)
                    : requested.scopeBindings();
            var next = new AssignmentInput(original.subject(), input.targetRevisionRef(), bindings,
                    original.validFrom(), original.validUntil(), original.delegationGrantId());
            List<ValidationIssue> issues = new ArrayList<>();
            if (row.getStatus() != GrantStatus.ACTIVE
                    || original.validUntil() != null && !Instant.now().isBefore(original.validUntil()))
                issues.add(new ValidationIssue("items", IamReasonCode.INVALID_ARGUMENT, "已撤销或到期分配不能升级"));
            if (higherOnly && published.getRevision().compareTo(oldPublished.getRevision()) <= 0)
                issues
                    .add(new ValidationIssue("targetRevisionRef", IamReasonCode.INVALID_ARGUMENT, "目标必须是同角色更高的已发布版本"));
            issues.addAll(operationsContext.validate(AuthorizationDomain.PLATFORM, admission, next, row.getId().longValueExact()));
            drafts.add(next);
            items.add(new AssignmentUpgradePreviewItem(row.getId().toString(), original.subject(),
                    original.roleRevisionRef(), bindings, before,
                    after, issues.isEmpty(), List.copyOf(issues),
                    com.ingot.cloud.iam.application.authorization.extension.RoleFieldPermissionService.snapshot(oldPublished.getResourceFieldPermissions()),
                    com.ingot.cloud.iam.application.authorization.extension.RoleFieldPermissionService.snapshot(published.getResourceFieldPermissions())));
        }
        var sourceIssues = operationsContext.batchSource(admission, new AssignmentBatchInput(drafts));
        if (!sourceIssues.isEmpty())
            return new AssignmentUpgradeResult(input.targetRevisionRef(),
                    items.stream()
                        .map(i -> new AssignmentUpgradePreviewItem(i.id(), i.subject(), i.previousRevisionRef(),
                                i.scopeBindings(), i.before(), i.after(), false, sourceIssues, i.beforeFieldPermissions(), i.afterFieldPermissions()))
                        .toList());
        return new AssignmentUpgradeResult(input.targetRevisionRef(), List.copyOf(items));
    }

    private Map<String, ScopeBinding> reusableBindings(AssignmentInput old, Map<String, PlatformAssignmentOperations.ParameterMeaning> previous,
            Map<String, PlatformAssignmentOperations.ParameterMeaning> target) {
        Map<String, ScopeBinding> result = new LinkedHashMap<>();
        target.forEach((key, meaning) -> {
            if (!meaning.resources().isEmpty() && meaning.equals(previous.get(key)) && old.scopeBindings().containsKey(key))
                result.put(key, old.scopeBindings().get(key));
        });
        return Map.copyOf(result);
    }

    private Map<String, PlatformAssignmentOperations.ParameterMeaning> parameterMeanings(RoleRevisionRef revision, List<ActionGrant> grants) {
        var ids = grants.stream().map(ActionGrant::actionId).distinct().toList();
        var actionOptions = operationsContext.editor().actionOptions(ids);
        Map<String, PlatformAssignmentOperations.ParameterMeaning> result = new LinkedHashMap<>();
        for (var parameter : operationsContext.roleStore().listParameters(IamIds.require(revision.id()))) {
            var associated = grants.stream().filter(g -> g.scopes().stream()
                    .anyMatch(scope -> parameter.getParameterKey().equals(scope.parameterKey()))).map(ActionGrant::actionId).toList();
            var actions = actionOptions.stream().filter(operation -> associated.contains(operation.id())).toList();
            var resources = actions.size() != associated.size() ? java.util.Set.<String>of() : actions.stream()
                    .map(action -> action.applicationId() + ":" + action.resourceId()).collect(java.util.stream.Collectors.toSet());
            result.put(parameter.getParameterKey(), new PlatformAssignmentOperations.ParameterMeaning(parameter.getBindingKind(), resources));
        }
        return Map.copyOf(result);
    }
}
