package com.ingot.cloud.iam.application.assignment;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.IamRoleRevisionJoin;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleDefinitionEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleRevisionEntity;
import com.ingot.cloud.iam.domain.shared.AuthorizationAdmission;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.application.assignment.model.AssignmentBatchInput;
import com.ingot.cloud.iam.application.assignment.model.AssignmentInput;
import com.ingot.cloud.iam.application.assignment.model.AssignmentRecord;
import com.ingot.cloud.iam.domain.assignment.AssignmentSource;
import com.ingot.cloud.iam.application.assignment.model.AssignmentUpdateInput;
import com.ingot.cloud.iam.domain.audit.AuditChangeType;
import com.ingot.cloud.iam.domain.audit.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.cloud.iam.domain.assignment.GrantStatus;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.application.organization.model.MemberRoleView;
import com.ingot.cloud.iam.application.organization.model.MemberRoleChanges;
import com.ingot.cloud.iam.domain.organization.MemberRoleAssignmentDraft;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;
import com.ingot.cloud.iam.application.assignment.model.SubjectRef;
import com.ingot.cloud.iam.domain.assignment.SubjectType;
import com.ingot.cloud.iam.application.assignment.model.ValidationIssue;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * <p>执行对应平台分配用例，逐项保留当前身份、来源和范围校验。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PlatformAssignmentCommandService {
    private final PlatformAssignmentOperations operationsContext;
    private final PlatformAssignmentPreviewService platformAssignmentPreviewService;

    /**
     * 原子批量创建授权，任一条失败整批回滚。
     *
     * @param domain 接口管理域
     * @param input 批次
     * @return 首条授权 ID
     */
    public CreatedResource create(AuthorizationDomain domain, AssignmentBatchInput input) {
        AuthorizationAdmission admission = operationsContext.access().admit(domain, PlatformAssignmentOperations.action(domain, PlatformAssignmentOperations.AccessKind.CREATE));
        ActiveIdentity actor = admission.actor();
        if (!operationsContext.batchSource(admission, input).isEmpty()) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        return operationsContext.transaction().execute(status -> {
            operationsContext.assignments().lockAuthorization(domain);
            AuthorizationAdmission currentAdmission = operationsContext.access().admit(domain, PlatformAssignmentOperations.action(domain, PlatformAssignmentOperations.AccessKind.CREATE));
            if (!operationsContext.batchSource(currentAdmission, input).isEmpty()) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
            CreatedResource first = null;
            int index = 0;
            for (AssignmentInput item : input.items()) {
                List<ValidationIssue> errors = operationsContext.validate(domain, currentAdmission, item, null);
                if (!errors.isEmpty()) {
                    throw new BizException(IamReasonCode.INVALID_ARGUMENT);
                }
                CreatedResource created = insert(domain, actor, item);
                if (index == 0) {
                    first = created;
                }
                index++;
            }
            operationsContext.changes().markAll();
            return first;
        });
    }

    /**
     * 调整既有授权，禁止改写主体或伪造委派来源。
     *
     * @param domain 接口管理域
     * @param id 授权 ID
     * @param input 待保存定义
     * @return 更新后详情
     */
    public ResourceDetail<AssignmentRecord> replace(AuthorizationDomain domain, String id, AssignmentUpdateInput input) {
        AuthorizationAdmission admission = operationsContext.access().admit(domain, PlatformAssignmentOperations.action(domain, PlatformAssignmentOperations.AccessKind.UPDATE));
        ActiveIdentity actor = admission.actor();
        long assignmentId = IamIds.require(id);
        return operationsContext.transaction().execute(status -> {
            operationsContext.assignments().lockAuthorization(domain);
            AuthorizationAdmission currentAdmission = operationsContext.access().admit(domain, PlatformAssignmentOperations.action(domain, PlatformAssignmentOperations.AccessKind.UPDATE));
            IamRoleAssignmentEntity current = lock(domain, actor, assignmentId);
            operationsContext.requireRecord(domain, currentAdmission, current);
            IamIds.requireVersion(input.expectedVersion(), PlatformAssignmentOperations.version(current.getVersion()));
            AssignmentInput next = input.assignment();
            if (current.getStatus() != GrantStatus.ACTIVE
                    || domain == AuthorizationDomain.PLATFORM && (current.getRevisionKind() != next.roleRevisionRef().kind()
                    || current.getRevisionId().longValueExact() != IamIds.require(next.roleRevisionRef().id()))
                    || !PlatformAssignmentOperations.sameSubject(current, next.subject()) || !PlatformAssignmentOperations.sameDelegation(current.getDelegationGrantId(),
                    next.delegationGrantId())) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            List<ValidationIssue> errors = operationsContext.validate(domain, currentAdmission, next, assignmentId);
            if (!errors.isEmpty()) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            Instant from = next.validFrom() == null ? Instant.now() : next.validFrom();
            boolean administrator = requireAdministratorMutation(actor, current);
            operationsContext.assignments().update(assignmentId, IamIds.require(next.roleRevisionRef().id()), next.roleRevisionRef().kind(),
                    IamJsonCodec.object(next.scopeBindings()), PlatformAssignmentOperations.utc(from), PlatformAssignmentOperations.utc(next.validUntil()), current.getVersion());
            if (administrator) operationsContext.assignments().requireAvailableAdministrator();
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), PlatformAssignmentOperations.ASSIGNMENT, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.ROLE_REVISION, IamIds.text(current.getRevisionId().longValue())),
                    Map.of(AuditField.ROLE_REVISION, next.roleRevisionRef().id()),
                    Map.of(PlatformAssignmentOperations.ASSIGNMENT, PlatformAssignmentOperations.nextVersion(current.getVersion())),
                    PlatformAssignmentOperations.text(current.getDelegationGrantId()), id);
            operationsContext.changes().markAll();
            return get(domain, actor, assignmentId);
        });
    }

    /**
     * 撤销授权并保留审计行。
     *
     * @param domain 接口管理域
     * @param id 授权 ID
     * @return 撤销前版本
     */
    public CreatedResource delete(AuthorizationDomain domain, String id) {
        ActiveIdentity actor = operationsContext.access().require(domain, PlatformAssignmentOperations.action(domain, PlatformAssignmentOperations.AccessKind.DELETE));
        long assignmentId = IamIds.require(id);
        return operationsContext.transaction().execute(status -> {
            operationsContext.assignments().lockAuthorization(domain);
            AuthorizationAdmission currentAdmission = operationsContext.access().admit(domain, PlatformAssignmentOperations.action(domain, PlatformAssignmentOperations.AccessKind.DELETE));
            IamRoleAssignmentEntity current = lock(domain, actor, assignmentId);
            operationsContext.requireRecord(domain, currentAdmission, current);
            boolean administrator = requireAdministratorMutation(actor, current);
            if (operationsContext.assignments().revoke(assignmentId, current.getVersion()) != 1) {
                throw new BizException(IamReasonCode.REVISION_CONFLICT);
            }
            if (administrator) operationsContext.assignments().requireAvailableAdministrator();
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), PlatformAssignmentOperations.ASSIGNMENT, id, AuditChangeType.DISABLE,
                    Map.of(AuditField.STATUS, current.getStatus().name()),
                    Map.of(AuditField.STATUS, GrantStatus.REVOKED.name()),
                    Map.of(PlatformAssignmentOperations.ASSIGNMENT, PlatformAssignmentOperations.nextVersion(current.getVersion())),
                    PlatformAssignmentOperations.text(current.getDelegationGrantId()), id);
            operationsContext.changes().markAll();
            return new CreatedResource(id, PlatformAssignmentOperations.version(current.getVersion()));
        });
    }

    /**
     * 为新建平台成员追加简单直接角色，调用方须已处于同一事务。
     *
     * @param domain 接口管理域
     * @param memberId 平台成员 ID
     * @param roleIds 角色定义 ID
     */
    public void grantDirectRoles(AuthorizationDomain domain, String memberId, List<String> roleIds) {
        PlatformAssignmentOperations.requirePlatform(domain);
        AuthorizationAdmission admission = operationsContext.access().admit(domain, IamAction.PLATFORM_ASSIGNMENT_CREATE);
        if (!admission.governed()) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        operationsContext.assignments().lockAuthorization(domain);
        admission = operationsContext.access().admit(domain, IamAction.PLATFORM_ASSIGNMENT_CREATE);
        if (!admission.governed()) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        grantDirectRoles(domain, admission, IamIds.require(memberId), uniqueIds(roleIds));
    }

    /**
     * 在成员事务内写入明确选定的可分配固定版本及其全部范围参数。
     *
     * @param memberId 刚创建的平台成员
     * @param drafts 已配置的直接分配
     */
    public void grantMemberRoleAssignments(String memberId, List<MemberRoleAssignmentDraft> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            return;
        }
        create(AuthorizationDomain.PLATFORM, new AssignmentBatchInput(operationsContext.memberDrafts(memberId, drafts)));
    }

    /**
     * 在成员外层事务及授权首锁内差量保存，新增先于撤销以检查最终超管存续。
     * @param memberId 成员
     * @param changes 差量
     */
    public void applyMemberRoleChanges(String memberId, MemberRoleChanges changes) {
        if (changes == null) return;
        if (!platformAssignmentPreviewService.previewMemberRoleChanges(memberId, changes).isEmpty()) throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        grantMemberRoleAssignments(memberId, changes.additions());
        for (var change : changes.updates().stream().sorted(java.util.Comparator.comparing(
                item -> new BigInteger(item.assignmentId()))).toList()) {
            var row = operationsContext.memberEditableAssignment(memberId, change.assignmentId(), change.expectedVersion(), IamAction.PLATFORM_ASSIGNMENT_UPDATE);
            replace(AuthorizationDomain.PLATFORM, change.assignmentId(), operationsContext.memberScopeInput(row, change));
        }
        for (var removal : changes.removals().stream().sorted(java.util.Comparator.comparing(
                item -> new BigInteger(item.assignmentId()))).toList()) {
            operationsContext.memberEditableAssignment(memberId, removal.assignmentId(), removal.expectedVersion(), IamAction.PLATFORM_ASSIGNMENT_DELETE);
            delete(AuthorizationDomain.PLATFORM, removal.assignmentId());
        }
    }

    /**
     * 替换平台成员的简单直接角色；带范围、有效期或委派来源的授权保持不变。
     *
     * @param domain 接口管理域
     * @param memberId 平台成员 ID
     * @param roleIds 目标角色定义 ID
     * @return 替换后的直接角色
     */
    public List<MemberRoleView> replaceDirectRoles(AuthorizationDomain domain, String memberId, List<String> roleIds) {
        operationsContext.access().require(domain, IamAction.PLATFORM_MEMBER_UPDATE);
        PlatformAssignmentOperations.requirePlatform(domain);
        AuthorizationAdmission admission = operationsContext.access().admit(domain, IamAction.PLATFORM_ASSIGNMENT_CREATE);
        long id = IamIds.require(memberId);
        List<String> desired = uniqueIds(roleIds);
        return operationsContext.transaction().execute(status -> {
            operationsContext.assignments().lockAuthorization(domain);
            AuthorizationAdmission creating = operationsContext.access().admit(domain, IamAction.PLATFORM_ASSIGNMENT_CREATE);
            AuthorizationAdmission revoking = operationsContext.access().admit(domain, IamAction.PLATFORM_ASSIGNMENT_DELETE);
            if (!creating.governed() || !revoking.governed()) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
            operationsContext.resourceAccess().requireVisibleMember(creating.actor().context(), IamAction.PLATFORM_MEMBER_UPDATE, id);
            if (!operationsContext.assignments().platformMemberExists(id)) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            Map<String, List<IamRoleAssignmentEntity>> current = simpleAssignmentsByRole(id);
            for (Map.Entry<String, List<IamRoleAssignmentEntity>> entry : current.entrySet()) {
                if (desired.contains(entry.getKey())) {
                    continue;
                }
                for (IamRoleAssignmentEntity row : entry.getValue()) {
                    requireAdministratorMutation(creating.actor(), row);
                    if (operationsContext.assignments().revoke(row.getId().longValueExact(), row.getVersion()) != 1) {
                        throw new BizException(IamReasonCode.REVISION_CONFLICT);
                    }
                    operationsContext.audits().write(creating.actor().context(), operationsContext.access().nextId(), PlatformAssignmentOperations.ASSIGNMENT, PlatformAssignmentOperations.text(row.getId()),
                            AuditChangeType.DISABLE, Map.of(AuditField.STATUS, GrantStatus.ACTIVE.name()),
                            Map.of(AuditField.STATUS, GrantStatus.REVOKED.name()),
                            Map.of(PlatformAssignmentOperations.ASSIGNMENT, PlatformAssignmentOperations.nextVersion(row.getVersion())), null, PlatformAssignmentOperations.text(row.getId()));
                }
            }
            List<String> adding = desired.stream().filter(roleId -> !current.containsKey(roleId)).toList();
            grantDirectRoles(domain, creating, id, adding);
            if (current.values().stream().flatMap(List::stream).anyMatch(row ->
                    com.ingot.cloud.iam.application.authorization.PlatformAdministratorGuard.isAdministrator(
                            operationsContext.assignments().findRevision(row.getRevisionId().longValueExact())))) {
                operationsContext.assignments().requireAvailableAdministrator();
            }
            operationsContext.changes().markAll();
            return operationsContext.collectDirectRoles(id);
        });
    }

    /**
     * 成员创建事务在插入成员前取得共用授权首锁。
     * @param domain 管理域
     */
    public void lockAuthorization(AuthorizationDomain domain) {
        operationsContext.assignments().lockAuthorization(domain);
    }

    private ResourceDetail<AssignmentRecord> get(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        IamRoleAssignmentEntity row = operationsContext.assignments().find(domain, PlatformAssignmentOperations.tenantId(domain, actor), id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return operationsContext.detail(domain, actor, row);
    }

    private CreatedResource insert(AuthorizationDomain domain, ActiveIdentity actor, AssignmentInput item) {
        long id = operationsContext.access().nextId();
        Instant from = item.validFrom() == null ? Instant.now() : item.validFrom();
        IamRoleAssignmentEntity entity = new IamRoleAssignmentEntity();
        entity.setId(BigInteger.valueOf(id));
        entity.setDomain(domain);
        entity.setTenantId(domain == AuthorizationDomain.TENANT
                ? BigInteger.valueOf(IamIds.require(actor.context().tenantId())) : null);
        entity.setSubjectType(item.subject().type());
        long subjectId = IamIds.require(item.subject().id());
        if (domain == AuthorizationDomain.PLATFORM && item.subject().type() == SubjectType.MEMBER) {
            entity.setPlatformMemberId(BigInteger.valueOf(subjectId));
        } else if (domain == AuthorizationDomain.PLATFORM) {
            entity.setPlatformGroupId(BigInteger.valueOf(subjectId));
        } else if (item.subject().type() == SubjectType.MEMBER) {
            entity.setTenantMemberId(BigInteger.valueOf(subjectId));
        } else {
            entity.setTenantGroupId(BigInteger.valueOf(subjectId));
        }
        entity.setRevisionId(BigInteger.valueOf(IamIds.require(item.roleRevisionRef().id())));
        entity.setRevisionKind(item.roleRevisionRef().kind());
        entity.setScopeBindings(IamJsonCodec.object(item.scopeBindings()));
        entity.setDelegationGrantId(item.delegationGrantId() == null || item.delegationGrantId().isBlank()
                ? null : BigInteger.valueOf(IamIds.require(item.delegationGrantId())));
        entity.setValidFrom(PlatformAssignmentOperations.utc(from));
        entity.setValidUntil(PlatformAssignmentOperations.utc(item.validUntil()));
        entity.setStatus(GrantStatus.ACTIVE);
        entity.setSource(AssignmentSource.MANUAL);
        try {
            operationsContext.assignments().insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), PlatformAssignmentOperations.ASSIGNMENT, IamIds.text(id), AuditChangeType.CREATE,
                Map.of(), Map.of(AuditField.ROLE_REVISION, item.roleRevisionRef().id()), Map.of(PlatformAssignmentOperations.ASSIGNMENT, "0"),
                item.delegationGrantId(), IamIds.text(id));
        return new CreatedResource(IamIds.text(id), "0");
    }

    private IamRoleAssignmentEntity lock(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        IamRoleAssignmentEntity row = operationsContext.assignments().lock(domain, PlatformAssignmentOperations.tenantId(domain, actor), id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return row;
    }

    private void grantDirectRoles(AuthorizationDomain domain, AuthorizationAdmission admission, long memberId,
                                  List<String> roleIds) {
        ActiveIdentity actor = admission.actor();
        for (String roleId : roleIds) {
            RoleRevisionRef revision = latestPublished(domain, roleId);
            if (!operationsContext.roleStore().listParameters(IamIds.require(revision.id())).isEmpty()) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(),
                        "参数化角色不能通过简单角色接口分配");
            }
            AssignmentInput item = simpleAssignment(memberId, revision);
            List<ValidationIssue> errors = operationsContext.validate(domain, admission, item, null);
            if (!errors.isEmpty()) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            insert(domain, actor, item);
        }
        if (!roleIds.isEmpty()) {
            operationsContext.changes().markAll();
        }
    }

    private Map<String, List<IamRoleAssignmentEntity>> simpleAssignmentsByRole(long memberId) {
        Map<String, List<IamRoleAssignmentEntity>> grouped = new LinkedHashMap<>();
        for (IamRoleAssignmentEntity row : operationsContext.assignments().listActivePlatformMember(memberId)) {
            if (!PlatformAssignmentOperations.isSimpleDirect(row)) {
                continue;
            }
            IamRoleRevisionJoin revision = operationsContext.assignments().findRevision(row.getRevisionId().longValueExact());
            if (revision == null || revision.getRoleId() == null) {
                continue;
            }
            grouped.computeIfAbsent(IamIds.text(revision.getRoleId().longValueExact()), key -> new ArrayList<>())
                    .add(row);
        }
        return grouped;
    }

    private RoleRevisionRef latestPublished(AuthorizationDomain domain, String roleId) {
        long id = IamIds.require(roleId);
        IamRoleDefinitionEntity definition = operationsContext.roleStore().findDefinition(domain, false, null, id);
        if (definition == null || !Boolean.TRUE.equals(definition.getEnabled())) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        IamRoleRevisionEntity latest = operationsContext.roleStore().latestRevision(id);
        if (latest == null || latest.getId() == null) {
            throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
        }
        return new RoleRevisionRef(latest.getKind(), IamIds.text(latest.getId().longValueExact()));
    }

    private static AssignmentInput simpleAssignment(long memberId, RoleRevisionRef revision) {
        return new AssignmentInput(new SubjectRef(SubjectType.MEMBER, IamIds.text(memberId)), revision, Map.of(),
                null, null, null);
    }

    private static List<String> uniqueIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                unique.add(id.trim());
            }
        }
        return List.copyOf(unique);
    }

    private boolean requireAdministratorMutation(ActiveIdentity actor, IamRoleAssignmentEntity row) {
        boolean administrator = com.ingot.cloud.iam.application.authorization.PlatformAdministratorGuard.isAdministrator(
                operationsContext.assignments().findRevision(row.getRevisionId().longValueExact()));
        if (administrator && !operationsContext.access().platformAdministrator(actor)) throw new BizException(IamReasonCode.ACTION_DENIED);
        return administrator;
    }
}
