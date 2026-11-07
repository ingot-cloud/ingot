package com.ingot.cloud.iam.application.assignment;

import com.ingot.cloud.iam.application.authorization.candidate.PlatformScopeBindingValidator;
import com.ingot.cloud.iam.application.authorization.scope.TenantScopeCandidates;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.application.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.AssignmentRepository;
import com.ingot.cloud.iam.application.delegation.DelegationAdmission;
import com.ingot.cloud.iam.infrastructure.persistence.IamRoleRevisionJoin;
import com.ingot.cloud.iam.infrastructure.persistence.RoleRepository;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamDelegationGrantEntity;
import com.ingot.cloud.iam.application.role.PlatformRoleFacade;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.authorization.ActionCapabilitySnapshot;
import com.ingot.cloud.iam.domain.shared.AuthorizationAdmission;
import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import com.ingot.cloud.iam.application.shared.ResourceDetailFactory;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.cloud.iam.application.assignment.model.AssignmentBatchInput;
import com.ingot.cloud.iam.application.assignment.model.AssignmentInput;
import com.ingot.cloud.iam.application.assignment.model.AssignmentRecord;
import com.ingot.cloud.iam.domain.assignment.AssignmentSource;
import com.ingot.cloud.iam.application.assignment.model.AssignmentUpdateInput;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.domain.assignment.GrantStatus;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.application.organization.model.MemberRoleView;
import com.ingot.cloud.iam.application.organization.model.MemberRoleScopeChange;
import com.ingot.cloud.iam.domain.organization.MemberRoleAssignmentDraft;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeBindingKind;
import com.ingot.cloud.iam.application.assignment.model.SubjectRef;
import com.ingot.cloud.iam.domain.assignment.SubjectType;
import com.ingot.cloud.iam.application.assignment.model.ValidationIssue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>提供assignment用例共享的持久化装配与内部规则，事务边界由各用例保持。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
public class PlatformAssignmentOperations {

    static final int MAX_SUBJECT_KEYWORD_LENGTH = 128;
    static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() {
    };
    static final String ASSIGNMENT = "assignment";
    static final String EMPTY_SCOPE_JSON = "{}";
    private final IamAccess access;
    private final AuthorizationAuditWriter audits;
    private final AuthorizationChangeNotifier changes;
    private final PlatformRoleFacade roles;
    private final RoleRepository roleStore;
    private final AssignmentRepository assignments;
    private final DelegationAdmission delegations;
    private final TransactionTemplate transaction;
    private final com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService resourceAccess;
    private final PlatformScopeBindingValidator bindingValidator;
    private final com.ingot.cloud.iam.application.authorization.candidate.PlatformAuthorizationCandidateQueryService candidateQueries;
    private final TenantScopeCandidates tenantScopeCandidates;

    /**
     * <p>单个固定版本参数的兼容语义；仅在当前预检批次内复用。</p>
     * @param kind 参数类型
     * @param resources 关联应用和资源
     * @author jy
     * @since 1.0.0
     */
    record ParameterMeaning(com.ingot.framework.commons.model.iam.ScopeBindingKind kind, java.util.Set<String> resources) { }

    enum AccessKind {
        READ, CREATE, UPDATE, DELETE
    }

    /**
     * 绑定身份、审计、角色合成与分配表。
     * <p>TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成，保留显式构造器。</p>
     *
     * @param access 当前身份
     * @param audits 同事务审计
     * @param changes 授权热缓存失效
     * @param roles 角色版本合成
     * @param roleStore 角色定义与最新版本
     * @param assignments 分配持久化
     * @param delegations 委派派生授权准入
     * @param bindingValidator 平台参数对象验证
     * @param candidateQueries 平台候选与操作标签查询
     * @param tenantScopeCandidates 租户参数对象验证
     * @param resourceAccess 成员对象边界
     * @param transactionManager 同一数据源事务
     */
    public PlatformAssignmentOperations(IamAccess access, AuthorizationAuditWriter audits, AuthorizationChangeNotifier changes,
                                 PlatformRoleFacade roles, RoleRepository roleStore, AssignmentRepository assignments,
                                 DelegationAdmission delegations,
                                 com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService resourceAccess, PlatformScopeBindingValidator bindingValidator,
                                 com.ingot.cloud.iam.application.authorization.candidate.PlatformAuthorizationCandidateQueryService candidateQueries,
                                 TenantScopeCandidates tenantScopeCandidates,
                                 PlatformTransactionManager transactionManager) {
        this.access = access;
        this.audits = audits;
        this.changes = changes;
        this.roles = roles;
        this.roleStore = roleStore;
        this.assignments = assignments;
        this.delegations = delegations;
        this.resourceAccess = resourceAccess;
        this.bindingValidator = bindingValidator;
        this.candidateQueries = candidateQueries;
        this.tenantScopeCandidates = tenantScopeCandidates;
        this.transaction = new TransactionTemplate(transactionManager);
    }
    IamAccess access() { return access; }

    AuthorizationAuditWriter audits() { return audits; }

    AuthorizationChangeNotifier changes() { return changes; }

    PlatformRoleFacade roles() { return roles; }

    RoleRepository roleStore() { return roleStore; }

    AssignmentRepository assignments() { return assignments; }

    DelegationAdmission delegations() { return delegations; }

    TransactionTemplate transaction() { return transaction; }

    com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService resourceAccess() { return resourceAccess; }

    com.ingot.cloud.iam.application.authorization.candidate.PlatformAuthorizationCandidateQueryService editor() { return candidateQueries; }

    PlatformScopeBindingValidator bindingValidator() { return bindingValidator; }

    TenantScopeCandidates tenantScopeCandidates() { return tenantScopeCandidates; }

    List<AssignmentInput> memberDrafts(String memberId, List<MemberRoleAssignmentDraft> drafts) {
        requireGoverned(IamAction.PLATFORM_ASSIGNMENT_CREATE);
        Set<String> roleIds = new LinkedHashSet<>();
        List<AssignmentInput> items = new ArrayList<>(drafts.size());
        for (MemberRoleAssignmentDraft draft : drafts) {
            var revision = draft == null || draft.roleRevisionRef() == null ? null
                    : assignments.findRevision(IamIds.require(draft.roleRevisionRef().id()));
            if (draft == null || !roleIds.add(draft.roleId()) || revision == null
                    || !revision.getRoleId().equals(BigInteger.valueOf(IamIds.require(draft.roleId())))
                    || revision.getKind() != draft.roleRevisionRef().kind()) {
                throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
            }
            items.add(new AssignmentInput(new SubjectRef(SubjectType.MEMBER, memberId), draft.roleRevisionRef(),
                    draft.scopeBindings(), draft.validFrom(), draft.validUntil(), null));
        }
        return items;
    }

    AuthorizationAdmission requireGoverned(IamAction action) {
        var admission = access.admit(AuthorizationDomain.PLATFORM, action);
        if (!admission.governed()) throw new BizException(IamReasonCode.ACTION_DENIED);
        return admission;
    }

    IamRoleAssignmentEntity memberEditableAssignment(String memberId, String assignmentId,
            String expectedVersion, IamAction action) {
        var admission = requireGoverned(action);
        var row = assignments.find(AuthorizationDomain.PLATFORM, null, IamIds.require(assignmentId));
        requireRecord(AuthorizationDomain.PLATFORM, admission, row);
        if (row.getSubjectType() != SubjectType.MEMBER || row.getPlatformMemberId() == null
                || !row.getPlatformMemberId().equals(BigInteger.valueOf(IamIds.require(memberId)))
                || row.getDelegationGrantId() != null || row.getStatus() != GrantStatus.ACTIVE
                || row.getValidFrom() != null && row.getValidFrom().isAfter(utc(Instant.now()))
                || row.getValidUntil() != null && !row.getValidUntil().isAfter(utc(Instant.now())))
            throw new BizException(IamReasonCode.ACTION_DENIED);
        var revision = assignments.findRevision(row.getRevisionId().longValueExact());
        if (revision == null || !Boolean.TRUE.equals(revision.getEnabled()))
            throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
        IamIds.requireVersion(expectedVersion, version(row.getVersion()));
        return row;
    }

    AssignmentUpdateInput memberScopeInput(IamRoleAssignmentEntity row,
            MemberRoleScopeChange change) {
        return new AssignmentUpdateInput(change.expectedVersion(), new AssignmentInput(
                new SubjectRef(SubjectType.MEMBER, row.getPlatformMemberId().toString()),
                new RoleRevisionRef(row.getRevisionKind(), row.getRevisionId().toString()), change.scopeBindings(),
                instant(row.getValidFrom()), instant(row.getValidUntil()), null));
    }

    void requireRecord(AuthorizationDomain domain, AuthorizationAdmission admission, IamRoleAssignmentEntity row) {
        if (row == null || !recordAllowed(domain, admission, row)) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
    }

    boolean recordAllowed(AuthorizationDomain domain, AuthorizationAdmission admission, IamRoleAssignmentEntity row) {
        if (domain != AuthorizationDomain.PLATFORM || admission.governed()) {
            return true;
        }
        if (row.getDelegationGrantId() == null) {
            return false;
        }
        var source = assignments.findDelegation(domain, null, row.getDelegationGrantId().longValueExact());
        return source != null && source.getPlatformAdministratorId() != null
                && source.getPlatformAdministratorId().toString().equals(admission.actor().context().memberId());
    }

    ResourceDetail<AssignmentRecord> detail(AuthorizationDomain domain, ActiveIdentity actor,
                                                   IamRoleAssignmentEntity row) {
        ActionCapabilitySnapshot permissions = recordCapabilities(domain, actor);
        return detail(domain, actor, row, domain == AuthorizationDomain.PLATFORM
                ? assignments.presentation(List.of(row.getId())).get(row.getId()) : null,
                permissions, recordSources(domain, actor, List.of(row), permissions));
    }

    ActionCapabilitySnapshot recordCapabilities(AuthorizationDomain domain, ActiveIdentity actor) {
        return domain == AuthorizationDomain.PLATFORM ? access.capabilities(actor, List.of(
                IamAction.PLATFORM_ASSIGNMENT_READ, IamAction.PLATFORM_ASSIGNMENT_UPDATE,
                IamAction.PLATFORM_ASSIGNMENT_DELETE, IamAction.PLATFORM_ASSIGNMENT_UPGRADE)) : new ActionCapabilitySnapshot(Map.of());
    }

    Map<BigInteger, IamDelegationGrantEntity> recordSources(AuthorizationDomain domain, ActiveIdentity actor,
            List<IamRoleAssignmentEntity> rows, ActionCapabilitySnapshot permissions) {
        if (domain != AuthorizationDomain.PLATFORM || List.of(IamAction.PLATFORM_ASSIGNMENT_READ,
                IamAction.PLATFORM_ASSIGNMENT_UPDATE, IamAction.PLATFORM_ASSIGNMENT_DELETE, IamAction.PLATFORM_ASSIGNMENT_UPGRADE).stream()
                .allMatch(action -> permissions.allows(action, true))) {
            return Map.of();
        }
        List<BigInteger> ids = rows.stream().map(IamRoleAssignmentEntity::getDelegationGrantId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        return assignments.findDelegations(domain, tenantId(domain, actor), ids);
    }

    ResourceDetail<AssignmentRecord> detail(AuthorizationDomain domain, ActiveIdentity actor,
            IamRoleAssignmentEntity row, com.ingot.cloud.iam.infrastructure.persistence.projection.AssignmentPresentation presentation,
            ActionCapabilitySnapshot permissions, Map<BigInteger, IamDelegationGrantEntity> sources) {
        if (domain != AuthorizationDomain.PLATFORM) { return ResourceDetailFactory.of(record(row), version(row.getVersion())); }
        var source = row.getDelegationGrantId() == null ? null : sources.get(row.getDelegationGrantId());
        boolean owned = source != null && source.getPlatformAdministratorId() != null
                && source.getPlatformAdministratorId().toString().equals(actor.context().memberId());
        Map<String, com.ingot.cloud.iam.application.shared.model.ObjectCapability> capabilities = new LinkedHashMap<>();
        for (AccessKind kind : List.of(AccessKind.READ, AccessKind.UPDATE, AccessKind.DELETE)) {
            IamAction operation = action(domain, kind);
            boolean allowed = (kind == AccessKind.READ || row.getStatus() == GrantStatus.ACTIVE)
                    && (kind != AccessKind.UPDATE || presentation == null || presentation.sourceValid())
                    && permissions.allows(operation, false)
                    && (permissions.allows(operation, true) || owned);
            capabilities.put(operation.getCode(), new com.ingot.cloud.iam.application.shared.model.ObjectCapability(
                    allowed, allowed ? null : IamReasonCode.ACTION_DENIED, allowed ? null : "当前身份不可操作该分配"));
        }
        var upgrade = IamAction.PLATFORM_ASSIGNMENT_UPGRADE;
        boolean canUpgrade = row.getStatus() == GrantStatus.ACTIVE
                && (row.getValidUntil() == null || Instant.now().isBefore(instant(row.getValidUntil())))
                && (presentation == null || presentation.sourceValid())
                && permissions.allows(upgrade, false) && (permissions.allows(upgrade, true) || owned);
        capabilities.put(upgrade.getCode(), new com.ingot.cloud.iam.application.shared.model.ObjectCapability(
                canUpgrade, canUpgrade ? null : IamReasonCode.ACTION_DENIED, canUpgrade ? null : "当前分配不可升级"));
        AssignmentRecord basic = record(row);
        if (domain == AuthorizationDomain.PLATFORM && presentation != null) {
            var state = com.ingot.cloud.iam.domain.assignment.AssignmentEffectiveStatus.ACTIVE;
            Instant now = Instant.now();
            if (row.getStatus() == GrantStatus.REVOKED) {
                state = com.ingot.cloud.iam.domain.assignment.AssignmentEffectiveStatus.REVOKED;
            } else if (row.getValidUntil() != null && !now.isBefore(instant(row.getValidUntil()))) {
                state = com.ingot.cloud.iam.domain.assignment.AssignmentEffectiveStatus.EXPIRED;
            } else if (!presentation.sourceValid()) {
                state = com.ingot.cloud.iam.domain.assignment.AssignmentEffectiveStatus.SOURCE_INVALID;
            } else if (row.getValidFrom() != null && now.isBefore(instant(row.getValidFrom()))) {
                state = com.ingot.cloud.iam.domain.assignment.AssignmentEffectiveStatus.PENDING;
            }
            basic = new AssignmentRecord(basic.id(), basic.assignment(), basic.status(), basic.source(),
                    presentation.subjectName(), presentation.roleName(), text(presentation.revisionNumber()),
                    row.getDelegationGrantId() == null ? "直接分配" : "来源委派 " + row.getDelegationGrantId(),
                    instant(row.getCreatedAt()), new com.ingot.cloud.iam.application.assignment.model.AssignmentAuthor(
                        text(presentation.authorId()), presentation.authorName() == null ? "未知" : presentation.authorName()),
                    state, presentation.versionName());
        }
        return ResourceDetailFactory.of(basic, capabilities, version(row.getVersion()));
    }

    /**
     * 校验单条分配；受限方还必须绑定属于自己的委派。
     */
    List<ValidationIssue> validate(AuthorizationDomain domain, AuthorizationAdmission admission, AssignmentInput item,
                                           Long currentId) {
        ActiveIdentity actor = admission.actor();
        List<ValidationIssue> errors = new ArrayList<>();
        if (item == null || item.subject() == null || item.roleRevisionRef() == null) {
            errors.add(new ValidationIssue("assignment", IamReasonCode.INVALID_ARGUMENT, "分配定义不完整"));
            return errors;
        }
        if (domain == AuthorizationDomain.PLATFORM && hasDepartmentBinding(item.scopeBindings())) {
            errors.add(new ValidationIssue("scopeBindings", IamReasonCode.INVALID_ARGUMENT, "平台不得使用部门参数"));
        }
        if (!subjectExists(domain, actor, item.subject())) {
            errors.add(new ValidationIssue("subject", IamReasonCode.OBJECT_NOT_FOUND, "接收主体不属于当前域"));
        }
        boolean revisionUsable = false;
        IamRoleRevisionJoin revision = assignments.findRevision(IamIds.require(item.roleRevisionRef().id()));
        if (revision == null || revision.getKind() != item.roleRevisionRef().kind()) {
            errors.add(new ValidationIssue("roleRevisionRef", IamReasonCode.ROLE_REVISION_UNAVAILABLE, "角色版本不可用"));
        } else if (!Boolean.TRUE.equals(revision.getEnabled())) {
            errors.add(new ValidationIssue("roleRevisionRef", IamReasonCode.ACTION_DENIED, "角色已停用"));
        } else if (!revisionAssignable(domain, actor, revision)) {
            errors.add(new ValidationIssue("roleRevisionRef", IamReasonCode.INVALID_ARGUMENT, "角色版本不属于当前域"));
        } else {
            revisionUsable = true;
        }
        boolean administrator = domain == AuthorizationDomain.PLATFORM
                && com.ingot.cloud.iam.application.authorization.PlatformAdministratorGuard.isAdministrator(revision);
        if (administrator && (!access.platformAdministrator(actor) || item.subject().type() != SubjectType.MEMBER
                || sourced(item))) {
            errors.add(new ValidationIssue("roleRevisionRef", IamReasonCode.ACTION_DENIED,
                    "只有有效超级管理员可以直接向平台成员分配系统超管，不允许用户组或委派"));
        }
        if (domain == AuthorizationDomain.PLATFORM && revisionUsable && !administrator) {
            errors.addAll(bindingValidator.validateBindings(item));
        } else if (domain == AuthorizationDomain.TENANT && revisionUsable) {
            errors.addAll(tenantScopeCandidates.validate(actor, item));
        }
        Instant from = item.validFrom() == null ? Instant.now() : item.validFrom();
        if (item.validUntil() != null && !from.isBefore(item.validUntil())) {
            errors.add(new ValidationIssue("validUntil", IamReasonCode.INVALID_ARGUMENT, "起止时间必须形成左闭右开区间"));
        }
        if (sourced(item)) {
            errors.addAll(validateDelegation(domain, actor, item, from, revisionUsable,
                    domain == AuthorizationDomain.PLATFORM && currentId != null && admission.governed()));
        } else if (!admission.governed()) {
            // 无完整治理资格时不能凭空落授权，必须声明自己那条委派作为来源。
            errors.add(new ValidationIssue("delegationGrantId", IamReasonCode.ACTION_DENIED,
                    "无完整治理资格时必须绑定属于自己的委派"));
        }
        return errors;
    }

    /**
     * 受限方一次提交只能来自同一条委派，禁止拼接多条委派的不同维度。
     */
    List<ValidationIssue> batchSource(AuthorizationAdmission admission, AssignmentBatchInput input) {
        if (admission.governed() || input == null || input.items() == null) {
            return List.of();
        }
        long sources = input.items().stream()
                .filter(PlatformAssignmentOperations::sourced)
                .map(AssignmentInput::delegationGrantId)
                .distinct()
                .count();
        if (sources > 1) {
            return List.of(new ValidationIssue("delegationGrantId", IamReasonCode.ACTION_DENIED,
                    "一次提交只能使用同一条委派作为来源"));
        }
        return List.of();
    }

    /**
     * 委派派生分配的校验交给共用准入组件，写入路径额外要求委派由当前操作者持有。
     */
    List<ValidationIssue> validateDelegation(AuthorizationDomain domain, ActiveIdentity actor,
            AssignmentInput item, Instant from, boolean revisionUsable, boolean governingExisting) {
        long delegationId = IamIds.require(item.delegationGrantId());
        long revisionId = IamIds.require(item.roleRevisionRef().id());
        List<ActionGrant> grants = revisionUsable ? roles.synthesizedGrants(revisionId) : List.of();
        return delegations.check(new DelegationAdmission.Request(domain, tenantId(domain, actor), delegationId,
                governingExisting ? null : IamIds.require(actor.context().memberId()), revisionId, grants,
                item.scopeBindings(), item.subject(),
                from, item.validUntil()));
    }

    static boolean sourced(AssignmentInput item) {
        return item != null && item.delegationGrantId() != null && !item.delegationGrantId().isBlank();
    }

    boolean subjectExists(AuthorizationDomain domain, ActiveIdentity actor, SubjectRef subject) {
        long id = IamIds.require(subject.id());
        if (domain == AuthorizationDomain.PLATFORM && subject.type() == SubjectType.MEMBER) {
            return assignments.platformMemberActive(id);
        }
        if (domain == AuthorizationDomain.PLATFORM) {
            List<BigInteger> members = assignments.platformGroupMemberIds(id);
            return assignments.platformGroupExists(id) && !members.isEmpty()
                    && members.stream().allMatch(member -> assignments.platformMemberActive(member.longValueExact()));
        }
        long tenant = IamIds.require(actor.context().tenantId());
        if (subject.type() == SubjectType.MEMBER) {
            return assignments.tenantMemberExists(tenant, id);
        }
        return assignments.tenantGroupExists(tenant, id);
    }

    boolean revisionAssignable(AuthorizationDomain domain, ActiveIdentity actor, IamRoleRevisionJoin revision) {
        if (revision.getKind() == RoleKind.SHARED) {
            return true;
        }
        if (domain == AuthorizationDomain.PLATFORM) {
            return revision.getDomain() == AuthorizationDomain.PLATFORM && revision.getTenantId() == null;
        }
        long tenantId = IamIds.require(actor.context().tenantId());
        return (revision.getKind() == RoleKind.SYSTEM && revision.getDomain() == AuthorizationDomain.TENANT)
                || (revision.getKind() == RoleKind.TENANT_CUSTOM && revision.getTenantId() != null
                && tenantId == revision.getTenantId().longValue());
    }

    static boolean hasDepartmentBinding(Map<String, ScopeBinding> bindings) {
        if (bindings == null) {
            return false;
        }
        return bindings.values().stream().anyMatch(binding -> binding.kind() == ScopeBindingKind.DEPARTMENTS);
    }

    AssignmentRecord record(IamRoleAssignmentEntity row) {
        SubjectType type = row.getSubjectType();
        String subjectId = type == SubjectType.MEMBER
                ? firstNonNull(row.getPlatformMemberId(), row.getTenantMemberId())
                : firstNonNull(row.getPlatformGroupId(), row.getTenantGroupId());
        Instant from = instant(row.getValidFrom());
        Instant until = instant(row.getValidUntil());
        String delegation = row.getDelegationGrantId() == null ? null : row.getDelegationGrantId().toString();
        AssignmentInput assignment = new AssignmentInput(new SubjectRef(type, subjectId),
                new RoleRevisionRef(row.getRevisionKind(), text(row.getRevisionId())),
                IamJsonCodec.read(row.getScopeBindings(), BINDINGS), from, until, delegation);
        return new AssignmentRecord(text(row.getId()), assignment, row.getStatus(), row.getSource());
    }

    static boolean sameSubject(IamRoleAssignmentEntity current, SubjectRef subject) {
        long id = IamIds.require(subject.id());
        if (current.getSubjectType() != subject.type()) {
            return false;
        }
        BigInteger actual = subject.type() == SubjectType.MEMBER
                ? first(current.getPlatformMemberId(), current.getTenantMemberId())
                : first(current.getPlatformGroupId(), current.getTenantGroupId());
        return actual != null && actual.longValue() == id;
    }

    static boolean sameDelegation(BigInteger current, String next) {
        if (current == null) {
            return next == null || next.isBlank();
        }
        return next != null && IamIds.require(next) == current.longValue();
    }

    static Long tenantId(AuthorizationDomain domain, ActiveIdentity actor) {
        return domain == AuthorizationDomain.TENANT ? IamIds.require(actor.context().tenantId()) : null;
    }

    static LocalDateTime utc(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    static Instant instant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    static String firstNonNull(BigInteger left, BigInteger right) {
        BigInteger value = left != null ? left : right;
        return value == null ? null : value.toString();
    }

    static BigInteger first(BigInteger left, BigInteger right) {
        return left != null ? left : right;
    }

    static String version(BigInteger value) {
        return value == null ? "0" : value.toString();
    }

    static String nextVersion(BigInteger current) {
        return (current == null ? BigInteger.ZERO : current).add(BigInteger.ONE).toString();
    }

    static String text(BigInteger id) {
        return id == null ? null : IamIds.text(id.longValue());
    }

    List<MemberRoleView> collectDirectRoles(long memberId) {
        Map<String, MemberRoleView> rolesById = new LinkedHashMap<>();
        for (IamRoleAssignmentEntity row : assignments.listActivePlatformMember(memberId)) {
            if (!isSimpleDirect(row)) {
                continue;
            }
            IamRoleRevisionJoin revision = assignments.findRevision(row.getRevisionId().longValueExact());
            if (revision == null || revision.getRoleId() == null) {
                continue;
            }
            String roleId = IamIds.text(revision.getRoleId().longValueExact());
            String name = revision.getName() == null || revision.getName().isBlank() ? roleId : revision.getName();
            rolesById.putIfAbsent(roleId, new MemberRoleView(roleId, name));
        }
        return rolesById.values().stream().sorted(Comparator.comparing(MemberRoleView::name)).toList();
    }

    static boolean isSimpleDirect(IamRoleAssignmentEntity row) {
        if (row.getSource() != AssignmentSource.MANUAL || row.getDelegationGrantId() != null
                || row.getValidUntil() != null) {
            return false;
        }
        String bindings = row.getScopeBindings();
        return bindings == null || bindings.isBlank() || EMPTY_SCOPE_JSON.equals(bindings.trim());
    }

    static void requirePlatform(AuthorizationDomain domain) {
        if (domain != AuthorizationDomain.PLATFORM) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }

    static IamAction action(AuthorizationDomain domain, AccessKind kind) {
        return switch (kind) {
            case READ -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_ASSIGNMENT_READ
                    : IamAction.TENANT_ASSIGNMENT_READ;
            case CREATE -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_ASSIGNMENT_CREATE
                    : IamAction.TENANT_ASSIGNMENT_CREATE;
            case UPDATE -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_ASSIGNMENT_UPDATE
                    : IamAction.TENANT_ASSIGNMENT_UPDATE;
            case DELETE -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_ASSIGNMENT_DELETE
                    : IamAction.TENANT_ASSIGNMENT_DELETE;
        };
    }
}
