package com.ingot.cloud.iam.application.delegation;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.application.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.DelegationRepository;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamDelegationActionCeilingEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamDelegationGrantEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamDelegationRecipientDepartmentEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamDelegationRecipientMemberEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamDelegationRoleRevisionEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.authorization.ActionCapabilitySnapshot;
import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import com.ingot.cloud.iam.application.shared.ResourceDetailFactory;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.cloud.iam.application.shared.IamPages;
import com.ingot.cloud.iam.domain.shared.IamSelections;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionScopeCeiling;
import com.ingot.cloud.iam.domain.audit.AuditChangeType;
import com.ingot.cloud.iam.domain.audit.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.framework.commons.model.iam.DelegationInput;
import com.ingot.cloud.iam.application.delegation.model.DelegationRecord;
import com.ingot.cloud.iam.application.delegation.model.DelegationUpdateInput;
import com.ingot.framework.commons.model.iam.DepartmentSelection;
import com.ingot.cloud.iam.domain.assignment.GrantStatus;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.application.shared.model.ImpactSummary;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.cloud.iam.application.shared.model.Preview;
import com.ingot.cloud.iam.application.shared.model.ReferenceImpactPreview;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.Selection;
import com.ingot.cloud.iam.application.assignment.model.ValidationIssue;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>维护不可拼接的委派限制，来源撤销会使派生授权立即无效。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class DelegationService {
    private static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() {
    };
    private static final String DELEGATION = "delegation";
    private static final int MAX_ADMINISTRATOR_NAME_LENGTH = 128;
    private final IamAccess access;
    private final AuthorizationAuditWriter audits;
    private final AuthorizationChangeNotifier changes;
    private final DelegationRepository delegations;
    private final TransactionTemplate transaction;
    private final com.ingot.cloud.iam.application.role.PlatformRoleFacade roles;
    private final com.ingot.cloud.iam.infrastructure.persistence.AssignmentRepository assignments;
    private final com.ingot.cloud.iam.application.role.RoleGrantValidator grantValidator;
    private final com.ingot.cloud.iam.application.authorization.candidate.PlatformScopeBindingValidator bindingValidator;

    /**
     * 绑定身份、审计、失效与委派表。
     * <p>TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成，保留显式构造器。</p>
     *
     * @param access 当前身份
     * @param audits 同事务审计
     * @param changes 授权热缓存失效
     * @param delegations 委派持久化
     * @param roles 角色固定版本合成
     * @param assignments 版本、主体与组边界
     * @param bindingValidator 平台范围对象验证
     * @param grantValidator 操作资源能力验证
     * @param transactionManager 同一数据源事务
     */
    public DelegationService(IamAccess access, AuthorizationAuditWriter audits, AuthorizationChangeNotifier changes,
                                 DelegationRepository delegations, com.ingot.cloud.iam.application.role.PlatformRoleFacade roles,
                             com.ingot.cloud.iam.infrastructure.persistence.AssignmentRepository assignments,
                             com.ingot.cloud.iam.application.role.RoleGrantValidator grantValidator,
                             com.ingot.cloud.iam.application.authorization.candidate.PlatformScopeBindingValidator bindingValidator,
                             PlatformTransactionManager transactionManager) {
        this.access = access;
        this.audits = audits;
        this.changes = changes;
        this.delegations = delegations;
        this.roles = roles;
        this.assignments = assignments;
        this.grantValidator = grantValidator;
        this.bindingValidator = bindingValidator;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 分页列出当前域委派。
     *
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @return 委派页
     */
    public PageResponse<ResourceDetail<DelegationRecord>> list(AuthorizationDomain domain, int page, int pageSize) {
        return list(domain, page, pageSize, null);
    }

    /**
     * 在当前治理边界内按平台管理员显示名称筛选并分页列出委派。
     *
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @param administratorName 平台管理员显示名称包含匹配，空白表示不限制
     * @return 委派页
     */
    public PageResponse<ResourceDetail<DelegationRecord>> list(AuthorizationDomain domain, int page, int pageSize,
                                                                 String administratorName) {
        ActiveIdentity actor = requireManagement(domain, AccessKind.READ);
        IamPages.require(page, pageSize);
        String search = administratorName == null ? null : administratorName.strip();
        if (search != null && search.length() > MAX_ADMINISTRATOR_NAME_LENGTH) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        if (search != null && search.isEmpty()) {
            search = null;
        }
        if (domain != AuthorizationDomain.PLATFORM && search != null) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        Long tenantId = tenantId(domain, actor);
        Page<IamDelegationGrantEntity> rows = delegations.page(domain, tenantId, page, pageSize, search);
        var children = delegations.children(domain, rows.getRecords().stream().map(IamDelegationGrantEntity::getId).toList());
        Map<BigInteger, String> administratorNames = domain == AuthorizationDomain.PLATFORM
                ? delegations.platformAdministratorNames(rows.getRecords().stream()
                    .map(IamDelegationGrantEntity::getPlatformAdministratorId).toList()) : Map.of();
        ActionCapabilitySnapshot permissions = rows.getRecords().isEmpty() ? new ActionCapabilitySnapshot(Map.of())
                : recordCapabilities(domain, actor);
        List<ResourceDetail<DelegationRecord>> items = new ArrayList<>();
        for (IamDelegationGrantEntity row : rows.getRecords()) {
            BigInteger administratorId = domain == AuthorizationDomain.PLATFORM
                    ? row.getPlatformAdministratorId() : row.getTenantAdministratorId();
            items.add(detail(domain, row, children.get(row.getId()), permissions,
                    administratorNames.get(administratorId)));
        }
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 读取委派详情。
     *
     * @param domain 接口管理域
     * @param id 委派 ID
     * @return 委派详情
     */
    public ResourceDetail<DelegationRecord> get(AuthorizationDomain domain, String id) {
        ActiveIdentity actor = requireManagement(domain, AccessKind.READ);
        return load(domain, actor, IamIds.require(id));
    }

    /**
     * 创建委派。
     *
     * @param domain 接口管理域
     * @param input 委派限制
     * @return 新委派 ID
     */
    public CreatedResource create(AuthorizationDomain domain, DelegationInput input) {
        ActiveIdentity actor = requireManagement(domain, AccessKind.CREATE);
        validate(domain, actor, input);
        return transaction.execute(status -> {
            assignments.lockAuthorization(domain);
            requireManagement(domain, AccessKind.CREATE);
            validate(domain, actor, input);
            long id = access.nextId();
            insertGrant(domain, actor, id, input);
            replaceChildren(domain, actor, id, input);
            audits.write(actor.context(), access.nextId(), DELEGATION, IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.RECIPIENT_SELECTION, input.recipientSelection()), Map.of(DELEGATION, "0"),
                    IamIds.text(id), null);
            changes.markAll();
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 预览新委派定义，不产生分配或委派。
     * @param domain 管理域
     * @param input 委派草稿
     * @return 校验结果
     */
    public Preview<ReferenceImpactPreview> previewCreate(AuthorizationDomain domain, DelegationInput input) {
        ActiveIdentity actor = requireManagement(domain, AccessKind.CREATE);
        List<ValidationIssue> errors = new ArrayList<>();
        try { validate(domain, actor, input); }
        catch (BizException exception) { errors.add(new ValidationIssue("delegation", IamReasonCode.find(exception.getCode()), exception.getMessage())); }
        var impact = new ImpactSummary(null, 0L, 0L, false);
        return new Preview<>("0", errors.isEmpty(), errors, List.of(), impact, new ReferenceImpactPreview(List.of(), impact));
    }

    /**
     * 调整委派；派生授权超出新上限时整批拒绝。
     *
     * @param domain 接口管理域
     * @param id 委派 ID
     * @param input 完整限制
     * @return 更新后详情
     */
    public ResourceDetail<DelegationRecord> replace(AuthorizationDomain domain, String id, DelegationUpdateInput input) {
        ActiveIdentity actor = requireManagement(domain, AccessKind.UPDATE);
        validate(domain, actor, input.delegation());
        long delegationId = IamIds.require(id);
        return transaction.execute(status -> {
            assignments.lockAuthorization(domain);
            requireManagement(domain, AccessKind.UPDATE);
            ResourceDetail<DelegationRecord> current = lock(domain, actor, delegationId);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            if (current.record().status() != GrantStatus.ACTIVE) { throw new BizException(IamReasonCode.INVALID_ARGUMENT); }
            validate(domain, actor, input.delegation());
            if (!derivedStillValid(delegationId, input.delegation())) {
                throw new BizException(IamReasonCode.POLICY_CONFLICT);
            }
            updateGrant(domain, actor, delegationId, input.delegation(), current.version());
            replaceChildren(domain, actor, delegationId, input.delegation());
            audits.write(actor.context(), access.nextId(), DELEGATION, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.RECIPIENT_SELECTION, current.record().delegation().recipientSelection()),
                    Map.of(AuditField.RECIPIENT_SELECTION, input.delegation().recipientSelection()),
                    Map.of(DELEGATION, Long.toString(Long.parseLong(current.version()) + 1)),
                    id, null);
            changes.markAll();
            return load(domain, actor, delegationId);
        });
    }

    /**
     * 撤销委派并使派生授权无效。
     *
     * @param domain 接口管理域
     * @param id 委派 ID
     * @return 撤销前版本
     */
    public CreatedResource delete(AuthorizationDomain domain, String id) {
        ActiveIdentity actor = requireManagement(domain, AccessKind.DELETE);
        long delegationId = IamIds.require(id);
        return transaction.execute(status -> {
            assignments.lockAuthorization(domain);
            requireManagement(domain, AccessKind.DELETE);
            ResourceDetail<DelegationRecord> current = lock(domain, actor, delegationId);
            delegations.revoke(delegationId, new BigInteger(current.version()));
            delegations.revokeDerivedAssignments(delegationId);
            audits.write(actor.context(), access.nextId(), DELEGATION, id, AuditChangeType.DISABLE,
                    Map.of(AuditField.STATUS, GrantStatus.ACTIVE.name()),
                    Map.of(AuditField.STATUS, GrantStatus.REVOKED.name()), Map.of(DELEGATION, current.version()),
                    id, null);
            changes.markAll();
            return new CreatedResource(id, current.version());
        });
    }

    /**
     * 预览委派收缩影响，无写入。
     *
     * @param domain 接口管理域
     * @param id 委派 ID
     * @param input 待保存限制
     * @return 引用影响
     */
    public Preview<ReferenceImpactPreview> preview(AuthorizationDomain domain, String id, DelegationUpdateInput input) {
        ActiveIdentity actor = requireManagement(domain, AccessKind.PREVIEW);
        ResourceDetail<DelegationRecord> current = load(domain, actor, IamIds.require(id));
        List<ValidationIssue> errors = new ArrayList<>();
        try {
            IamIds.requireVersion(input.expectedVersion(), current.version());
            validate(domain, actor, input.delegation());
        } catch (BizException exception) {
            errors.add(new ValidationIssue("delegation", IamReasonCode.INVALID_ARGUMENT, exception.getMessage()));
        }
        List<String> allAffected = errors.isEmpty() ? conflictingIds(IamIds.require(id), input.delegation()) : List.of();
        boolean canDisclose = access.allows(domain, domain == AuthorizationDomain.PLATFORM
                ? IamAction.PLATFORM_ASSIGNMENT_READ : IamAction.TENANT_ASSIGNMENT_READ, true);
        List<String> affected = canDisclose ? allAffected : List.of();
        if (errors.isEmpty() && !derivedStillValid(IamIds.require(id), input.delegation())) {
            errors.add(new ValidationIssue("delegation", IamReasonCode.POLICY_CONFLICT, "存在超出新上限的派生授权"));
        }
        ReferenceImpactPreview result = new ReferenceImpactPreview(affected,
                new ImpactSummary(null, canDisclose ? (long) allAffected.size() : null, 1L, !canDisclose && !allAffected.isEmpty()));
        return new Preview<>(current.version(), errors.isEmpty(), errors, List.of(), result.impactSummary(), result);
    }

    private void validate(AuthorizationDomain domain, ActiveIdentity actor, DelegationInput input) {
        IamSelections.requireCompatible(domain, input.recipientSelection());
        long adminId = IamIds.require(input.administratorMemberId());
        boolean admin = domain == AuthorizationDomain.PLATFORM
                ? assignments.platformMemberActive(adminId)
                : delegations.tenantMemberExists(IamIds.require(actor.context().tenantId()), adminId);
        if (!admin) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        if (!input.isValidPeriod() || !input.isPositiveDuration()
                || domain == AuthorizationDomain.TENANT && input.assignmentDurationMode()
                    == com.ingot.framework.commons.model.iam.AssignmentDurationMode.UNLIMITED) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        if (domain == AuthorizationDomain.PLATFORM) {
            for (String member : input.recipientSelection().members()) {
                if (IamIds.require(member) == adminId || !assignments.platformMemberActive(IamIds.require(member))) {
                    throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
                }
            }
        }
        java.util.Set<String> actions = new java.util.LinkedHashSet<>();
        for (RoleRevisionRef ref : input.allowedRoleRevisionRefs()) {
            var revision = assignments.findRevision(IamIds.require(ref.id()));
            if (com.ingot.cloud.iam.application.authorization.PlatformAdministratorGuard.isAdministrator(revision)
                    || revision == null || revision.getKind() != ref.kind() || !Boolean.TRUE.equals(revision.getEnabled())
                    || domain == AuthorizationDomain.PLATFORM && (revision.getDomain() != domain
                        || revision.getTenantId() != null || ref.kind() != com.ingot.framework.commons.model.iam.RoleKind.PLATFORM_CUSTOM
                        && ref.kind() != com.ingot.framework.commons.model.iam.RoleKind.SYSTEM)) {
                throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
            }
            roles.synthesizedGrants(IamIds.require(ref.id())).forEach(grant -> actions.add(grant.actionId()));
        }
        if (domain == AuthorizationDomain.PLATFORM) {
            java.util.Set<String> ceilings = input.actionScopeCeilings().stream().map(ActionScopeCeiling::actionId)
                    .collect(java.util.stream.Collectors.toSet());
            if (!ceilings.equals(actions) || ceilings.size() != input.actionScopeCeilings().size()) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            for (ActionScopeCeiling ceiling : input.actionScopeCeilings()) {
                List<com.ingot.framework.commons.model.iam.RoleParameterDefinition> parameters = ceiling.scopeBindings()
                        .entrySet().stream().map(entry -> new com.ingot.framework.commons.model.iam.RoleParameterDefinition(
                                entry.getKey(), entry.getValue().kind())).toList();
                if (!bindingValidator.validCeilingObjects(ceiling) || !grantValidator.validate(domain, List.of(new com.ingot.framework.commons.model.iam.ActionGrant(
                        ceiling.actionId(), ceiling.scopes())), parameters).isEmpty()) {
                    throw new BizException(IamReasonCode.INVALID_ARGUMENT);
                }
            }
        }
    }

    private ResourceDetail<DelegationRecord> load(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        IamDelegationGrantEntity grant = delegations.find(domain, tenantId(domain, actor), id);
        if (grant == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        var children = delegations.children(domain, List.of(grant.getId())).get(grant.getId());
        String administratorName = domain == AuthorizationDomain.PLATFORM
                ? delegations.platformAdministratorNames(List.of(grant.getPlatformAdministratorId()))
                    .get(grant.getPlatformAdministratorId()) : null;
        return detail(domain, grant, children, recordCapabilities(domain, actor), administratorName);
    }

    private ActionCapabilitySnapshot recordCapabilities(AuthorizationDomain domain, ActiveIdentity actor) {
        return domain == AuthorizationDomain.PLATFORM ? access.capabilities(actor, List.of(
                IamAction.PLATFORM_DELEGATION_READ, IamAction.PLATFORM_DELEGATION_UPDATE,
                IamAction.PLATFORM_DELEGATION_DELETE, IamAction.PLATFORM_DELEGATION_PREVIEW))
                : new ActionCapabilitySnapshot(Map.of());
    }

    private ResourceDetail<DelegationRecord> detail(AuthorizationDomain domain, IamDelegationGrantEntity grant,
            DelegationRepository.Children children, ActionCapabilitySnapshot permissions, String administratorName) {
        long id = grant.getId().longValueExact();
        long administratorId = first(grant.getPlatformAdministratorId(), grant.getTenantAdministratorId());
        DelegationInput input = new DelegationInput(IamIds.text(administratorId), revisions(children),
                recipients(domain, children), ceilings(children), instant(grant.getValidFrom()), instant(grant.getValidUntil()),
                duration(grant), grant.getAssignmentDurationMode());
        Map<String, com.ingot.cloud.iam.application.shared.model.ObjectCapability> capabilities = new java.util.LinkedHashMap<>();
        if (domain != AuthorizationDomain.PLATFORM) {
            return ResourceDetailFactory.of(new DelegationRecord(IamIds.text(id), input, grant.getStatus(), administratorName,
                    instant(grant.getCreatedAt())), version(grant.getVersion()));
        }
        for (AccessKind kind : List.of(AccessKind.READ, AccessKind.UPDATE, AccessKind.DELETE, AccessKind.PREVIEW)) {
            IamAction operation = action(domain, kind);
            boolean allowed = (kind == AccessKind.READ || grant.getStatus() == GrantStatus.ACTIVE)
                    && permissions.allows(operation, true);
            capabilities.put(operation.getCode(), new com.ingot.cloud.iam.application.shared.model.ObjectCapability(
                    allowed, allowed ? null : IamReasonCode.ACTION_DENIED, allowed ? null : "当前身份不可操作该委派"));
        }
        return ResourceDetailFactory.of(new DelegationRecord(IamIds.text(id), input, grant.getStatus(), administratorName,
                instant(grant.getCreatedAt())), capabilities, version(grant.getVersion()));
    }

    private ResourceDetail<DelegationRecord> lock(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        if (delegations.lock(domain, tenantId(domain, actor), id) == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return load(domain, actor, id);
    }

    private void insertGrant(AuthorizationDomain domain, ActiveIdentity actor, long id, DelegationInput input) {
        IamDelegationGrantEntity entity = new IamDelegationGrantEntity();
        entity.setId(BigInteger.valueOf(id));
        entity.setDomain(domain);
        entity.setTenantId(domain == AuthorizationDomain.TENANT
                ? BigInteger.valueOf(IamIds.require(actor.context().tenantId())) : null);
        entity.setPlatformAdministratorId(domain == AuthorizationDomain.PLATFORM
                ? BigInteger.valueOf(IamIds.require(input.administratorMemberId())) : null);
        entity.setTenantAdministratorId(domain == AuthorizationDomain.TENANT
                ? BigInteger.valueOf(IamIds.require(input.administratorMemberId())) : null);
        entity.setValidFrom(utc(input.validFrom()));
        entity.setValidUntil(utc(input.validUntil()));
        entity.setAssignmentDurationMode(input.assignmentDurationMode());
        entity.setMaxAssignmentDurationSeconds(input.maxAssignmentDuration() == null ? null : input.maxAssignmentDuration().getSeconds());
        entity.setMaxAssignmentDurationNanos(input.maxAssignmentDuration() == null ? 0 : input.maxAssignmentDuration().getNano());
        entity.setStatus(GrantStatus.ACTIVE);
        try {
            delegations.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
    }

    private void updateGrant(AuthorizationDomain domain, ActiveIdentity actor, long id, DelegationInput input,
                             String currentVersion) {
        delegations.update(id,
                domain == AuthorizationDomain.PLATFORM
                        ? BigInteger.valueOf(IamIds.require(input.administratorMemberId())) : null,
                domain == AuthorizationDomain.TENANT
                        ? BigInteger.valueOf(IamIds.require(input.administratorMemberId())) : null,
                utc(input.validFrom()), utc(input.validUntil()),
                input.maxAssignmentDuration() == null ? null : input.maxAssignmentDuration().getSeconds(),
                input.maxAssignmentDuration() == null ? 0 : input.maxAssignmentDuration().getNano(),
                input.assignmentDurationMode(), new BigInteger(currentVersion));
    }

    private void replaceChildren(AuthorizationDomain domain, ActiveIdentity actor, long id, DelegationInput input) {
        delegations.deleteChildren(id);
        for (RoleRevisionRef ref : input.allowedRoleRevisionRefs()) {
            IamDelegationRoleRevisionEntity row = new IamDelegationRoleRevisionEntity();
            row.setDelegationId(BigInteger.valueOf(id));
            row.setRevisionId(BigInteger.valueOf(IamIds.require(ref.id())));
            row.setRevisionKind(ref.kind());
            try {
                delegations.insertRoleRevision(row);
            } catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
        }
        for (String memberId : input.recipientSelection().members()) {
            IamDelegationRecipientMemberEntity row = new IamDelegationRecipientMemberEntity();
            row.setId(BigInteger.valueOf(access.nextId()));
            row.setDelegationId(BigInteger.valueOf(id));
            row.setDomain(domain);
            row.setTenantId(domain == AuthorizationDomain.TENANT
                    ? BigInteger.valueOf(IamIds.require(actor.context().tenantId())) : null);
            row.setPlatformMemberId(domain == AuthorizationDomain.PLATFORM
                    ? BigInteger.valueOf(IamIds.require(memberId)) : null);
            row.setTenantMemberId(domain == AuthorizationDomain.TENANT
                    ? BigInteger.valueOf(IamIds.require(memberId)) : null);
            try {
                delegations.insertRecipientMember(row);
            } catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
        }
        for (DepartmentSelection department : input.recipientSelection().departments()) {
            IamDelegationRecipientDepartmentEntity row = new IamDelegationRecipientDepartmentEntity();
            row.setDelegationId(BigInteger.valueOf(id));
            row.setTenantId(BigInteger.valueOf(IamIds.require(actor.context().tenantId())));
            row.setDepartmentId(BigInteger.valueOf(IamIds.require(department.id())));
            row.setIncludeDescendants(department.includeDescendants());
            try {
                delegations.insertRecipientDepartment(row);
            } catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
        }
        for (ActionScopeCeiling ceiling : input.actionScopeCeilings()) {
            IamDelegationActionCeilingEntity row = new IamDelegationActionCeilingEntity();
            row.setDelegationId(BigInteger.valueOf(id));
            row.setActionId(BigInteger.valueOf(IamIds.require(ceiling.actionId())));
            row.setScopes(IamJsonCodec.array(ceiling.scopes()));
            row.setScopeBindings(IamJsonCodec.object(ceiling.scopeBindings()));
            try {
                delegations.insertCeiling(row);
            } catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
        }
    }

    private List<RoleRevisionRef> revisions(DelegationRepository.Children children) {
        return children.revisions().stream()
                .map(row -> new RoleRevisionRef(row.getRevisionKind(), text(row.getRevisionId())))
                .toList();
    }

    private Selection recipients(AuthorizationDomain domain, DelegationRepository.Children children) {
        List<IamDelegationRecipientMemberEntity> members = children.members();
        List<String> memberIds;
        if (domain == AuthorizationDomain.PLATFORM) {
            memberIds = members.stream()
                    .sorted(Comparator.comparing(IamDelegationRecipientMemberEntity::getPlatformMemberId,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(row -> text(row.getPlatformMemberId()))
                    .toList();
        } else {
            memberIds = members.stream()
                    .sorted(Comparator.comparing(IamDelegationRecipientMemberEntity::getTenantMemberId,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(row -> text(row.getTenantMemberId()))
                    .toList();
        }
        List<DepartmentSelection> departments = domain == AuthorizationDomain.PLATFORM ? List.of()
                : children.departments().stream()
                .map(row -> new DepartmentSelection(text(row.getDepartmentId()),
                        Boolean.TRUE.equals(row.getIncludeDescendants())))
                .toList();
        return new Selection(memberIds, departments);
    }

    private List<ActionScopeCeiling> ceilings(DelegationRepository.Children children) {
        return children.ceilings().stream()
                .map(row -> new ActionScopeCeiling(text(row.getActionId()),
                        IamJsonCodec.read(row.getScopes(), SCOPES),
                        IamJsonCodec.read(row.getScopeBindings(), BINDINGS)))
                .toList();
    }

    private boolean derivedStillValid(long delegationId, DelegationInput next) {
        return conflictingIds(delegationId, next).isEmpty();
    }

    private List<String> conflictingIds(long delegationId, DelegationInput next) {
        return delegations.activeDerivedAssignments(delegationId).stream()
                .filter(row -> !derivedValid(row, next)).map(row -> text(row.getId())).toList();
    }

    private boolean derivedValid(IamRoleAssignmentEntity row, DelegationInput next) {
            long revisionId = row.getRevisionId().longValue();
            boolean allowed = next.allowedRoleRevisionRefs().stream()
                    .anyMatch(ref -> IamIds.require(ref.id()) == revisionId);
            if (!allowed) {
                return false;
            }
            if (row.getDomain() != AuthorizationDomain.PLATFORM) { return true; }
            Instant from = instant(row.getValidFrom());
            Instant until = instant(row.getValidUntil());
            if (from == null || next.assignmentDurationMode() == com.ingot.framework.commons.model.iam.AssignmentDurationMode.LIMITED
                    && (until == null || Duration.between(from, until).compareTo(next.maxAssignmentDuration()) > 0)
                    || next.validFrom() != null && from.isBefore(next.validFrom())
                    || next.validUntil() != null && (!from.isBefore(next.validUntil()) || until != null && until.isAfter(next.validUntil()))) {
                return false;
            }
            if (row.getDomain() == AuthorizationDomain.PLATFORM) {
                List<String> receivers = row.getSubjectType() == com.ingot.cloud.iam.domain.assignment.SubjectType.MEMBER
                        ? List.of(text(row.getPlatformMemberId()))
                        : assignments.platformGroupMemberIds(row.getPlatformGroupId().longValueExact()).stream()
                            .map(BigInteger::toString).toList();
                if (receivers.isEmpty() || receivers.contains(next.administratorMemberId())
                        || !next.recipientSelection().members().containsAll(receivers)) {
                    return false;
                }
                var bindings = IamJsonCodec.read(row.getScopeBindings(), BINDINGS);
                for (var grant : roles.synthesizedGrants(revisionId)) {
                    var ceiling = next.actionScopeCeilings().stream().filter(value -> value.actionId().equals(grant.actionId()))
                            .findFirst().orElse(null);
                    if (ceiling == null || !com.ingot.cloud.iam.domain.authorization.ScopeBinder.covers(
                            com.ingot.cloud.iam.domain.authorization.ScopeBinder.bind(ceiling),
                            com.ingot.cloud.iam.domain.authorization.ScopeBinder.bind(grant, bindings))) {
                        return false;
                    }
                }
            }
        return true;
    }

    private static Long tenantId(AuthorizationDomain domain, ActiveIdentity actor) {
        return domain == AuthorizationDomain.TENANT ? IamIds.require(actor.context().tenantId()) : null;
    }

    private static Duration duration(IamDelegationGrantEntity grant) {
        if (grant.getAssignmentDurationMode() == com.ingot.framework.commons.model.iam.AssignmentDurationMode.UNLIMITED) return null;
        return Duration.ofSeconds(
                grant.getMaxAssignmentDurationSeconds() == null ? 0 : grant.getMaxAssignmentDurationSeconds(),
                grant.getMaxAssignmentDurationNanos() == null ? 0 : grant.getMaxAssignmentDurationNanos());
    }

    private static LocalDateTime utc(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    private static Instant instant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static long first(BigInteger left, BigInteger right) {
        BigInteger value = left != null ? left : right;
        return value.longValue();
    }

    private static String version(BigInteger value) {
        return value == null ? "0" : value.toString();
    }

    private static String text(BigInteger id) {
        return id == null ? null : IamIds.text(id.longValue());
    }

    private ActiveIdentity requireManagement(AuthorizationDomain domain, AccessKind kind) {
        return domain == AuthorizationDomain.PLATFORM ? access.requireGoverned(domain, action(domain, kind))
                : access.require(domain, action(domain, kind));
    }

    private static IamAction action(AuthorizationDomain domain, AccessKind kind) {
        return switch (kind) {
            case READ -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_DELEGATION_READ
                    : IamAction.TENANT_DELEGATION_READ;
            case CREATE -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_DELEGATION_CREATE
                    : IamAction.TENANT_DELEGATION_CREATE;
            case UPDATE -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_DELEGATION_UPDATE
                    : IamAction.TENANT_DELEGATION_UPDATE;
            case DELETE -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_DELEGATION_DELETE
                    : IamAction.TENANT_DELEGATION_DELETE;
            case PREVIEW -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_DELEGATION_PREVIEW
                    : IamAction.TENANT_DELEGATION_PREVIEW;
        };
    }

    private enum AccessKind {
        READ, CREATE, UPDATE, DELETE, PREVIEW
    }
}
