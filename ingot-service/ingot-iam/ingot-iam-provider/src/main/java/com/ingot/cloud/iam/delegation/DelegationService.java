package com.ingot.cloud.iam.delegation;

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
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.DelegationRepository;
import com.ingot.cloud.iam.persistence.entity.IamDelegationActionCeilingEntity;
import com.ingot.cloud.iam.persistence.entity.IamDelegationGrantEntity;
import com.ingot.cloud.iam.persistence.entity.IamDelegationRecipientDepartmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamDelegationRecipientMemberEntity;
import com.ingot.cloud.iam.persistence.entity.IamDelegationRoleRevisionEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.cloud.iam.support.IamSelections;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionScopeCeiling;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.DelegationInput;
import com.ingot.framework.commons.model.iam.DelegationRecord;
import com.ingot.framework.commons.model.iam.DelegationUpdateInput;
import com.ingot.framework.commons.model.iam.DepartmentSelection;
import com.ingot.framework.commons.model.iam.GrantStatus;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ImpactSummary;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.Preview;
import com.ingot.framework.commons.model.iam.ReferenceImpactPreview;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.Selection;
import com.ingot.framework.commons.model.iam.ValidationIssue;
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
    private final IamAccess access;
    private final IamAuditWriter audits;
    private final AuthorizationChangeNotifier changes;
    private final DelegationRepository delegations;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、审计、失效与委派表。
     * <p>TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成，保留显式构造器。</p>
     *
     * @param access 当前身份
     * @param audits 同事务审计
     * @param changes 授权热缓存失效
     * @param delegations 委派持久化
     * @param transactionManager 同一数据源事务
     */
    public DelegationService(IamAccess access, IamAuditWriter audits, AuthorizationChangeNotifier changes,
                                 DelegationRepository delegations, PlatformTransactionManager transactionManager) {
        this.access = access;
        this.audits = audits;
        this.changes = changes;
        this.delegations = delegations;
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
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.READ));
        IamPages.require(page, pageSize);
        Page<IamDelegationGrantEntity> rows = delegations.page(domain, tenantId(domain, actor), page, pageSize);
        List<ResourceDetail<DelegationRecord>> items = new ArrayList<>();
        for (IamDelegationGrantEntity row : rows.getRecords()) {
            items.add(load(domain, actor, row.getId().longValue()));
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
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.READ));
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
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.CREATE));
        validate(domain, actor, input);
        return transaction.execute(status -> {
            long id = access.nextId();
            insertGrant(domain, actor, id, input);
            replaceChildren(domain, actor, id, input);
            audits.write(actor.context(), access.nextId(), DELEGATION, IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.RECIPIENT_SELECTION, "created"), Map.of(DELEGATION, "0"),
                    IamIds.text(id), null);
            changes.markAll();
            return new CreatedResource(IamIds.text(id), "0");
        });
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
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.UPDATE));
        validate(domain, actor, input.delegation());
        long delegationId = IamIds.require(id);
        return transaction.execute(status -> {
            ResourceDetail<DelegationRecord> current = lock(domain, actor, delegationId);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            if (!derivedStillValid(delegationId, input.delegation())) {
                throw new BizException(IamReasonCode.POLICY_CONFLICT);
            }
            updateGrant(domain, actor, delegationId, input.delegation(), current.version());
            replaceChildren(domain, actor, delegationId, input.delegation());
            audits.write(actor.context(), access.nextId(), DELEGATION, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.RECIPIENT_SELECTION, "before"),
                    Map.of(AuditField.RECIPIENT_SELECTION, "after"),
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
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.DELETE));
        long delegationId = IamIds.require(id);
        return transaction.execute(status -> {
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
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.PREVIEW));
        ResourceDetail<DelegationRecord> current = load(domain, actor, IamIds.require(id));
        List<ValidationIssue> errors = new ArrayList<>();
        try {
            validate(domain, actor, input.delegation());
            IamIds.requireVersion(input.expectedVersion(), current.version());
        } catch (BizException exception) {
            errors.add(new ValidationIssue("delegation", IamReasonCode.INVALID_ARGUMENT, exception.getMessage()));
        }
        List<String> affected = derivedIds(IamIds.require(id));
        if (errors.isEmpty() && !derivedStillValid(IamIds.require(id), input.delegation())) {
            errors.add(new ValidationIssue("delegation", IamReasonCode.POLICY_CONFLICT, "存在超出新上限的派生授权"));
        }
        ReferenceImpactPreview result = new ReferenceImpactPreview(affected,
                new ImpactSummary(null, (long) affected.size(), 1L, false));
        return new Preview<>(current.version(), errors.isEmpty(), errors, List.of(), result.impactSummary(), result);
    }

    private void validate(AuthorizationDomain domain, ActiveIdentity actor, DelegationInput input) {
        IamSelections.requireCompatible(domain, input.recipientSelection());
        long adminId = IamIds.require(input.administratorMemberId());
        boolean admin = domain == AuthorizationDomain.PLATFORM
                ? delegations.platformMemberExists(adminId)
                : delegations.tenantMemberExists(IamIds.require(actor.context().tenantId()), adminId);
        if (!admin) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        for (RoleRevisionRef ref : input.allowedRoleRevisionRefs()) {
            if (!delegations.roleRevisionExists(IamIds.require(ref.id()), ref.kind())) {
                throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
            }
        }
    }

    private ResourceDetail<DelegationRecord> load(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        IamDelegationGrantEntity grant = delegations.find(domain, tenantId(domain, actor), id);
        if (grant == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        long administratorId = first(grant.getPlatformAdministratorId(), grant.getTenantAdministratorId());
        DelegationInput input = new DelegationInput(IamIds.text(administratorId), revisions(id),
                recipients(domain, id), ceilings(id), instant(grant.getValidFrom()), instant(grant.getValidUntil()),
                duration(grant));
        return IamDetails.of(new DelegationRecord(IamIds.text(id), input, grant.getStatus()), version(grant.getVersion()));
    }

    private ResourceDetail<DelegationRecord> lock(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        load(domain, actor, id);
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
        entity.setMaxAssignmentDurationSeconds(input.maxAssignmentDuration().getSeconds());
        entity.setMaxAssignmentDurationNanos(input.maxAssignmentDuration().getNano());
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
                utc(input.validFrom()), utc(input.validUntil()), input.maxAssignmentDuration().getSeconds(),
                input.maxAssignmentDuration().getNano(), new BigInteger(currentVersion));
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
            row.setScopes(IamJson.array(ceiling.scopes()));
            row.setScopeBindings(IamJson.object(ceiling.scopeBindings()));
            try {
                delegations.insertCeiling(row);
            } catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
        }
    }

    private List<RoleRevisionRef> revisions(long id) {
        return delegations.loadRoleRevisions(id).stream()
                .map(row -> new RoleRevisionRef(row.getRevisionKind(), text(row.getRevisionId())))
                .toList();
    }

    private Selection recipients(AuthorizationDomain domain, long id) {
        List<IamDelegationRecipientMemberEntity> members = delegations.loadRecipientMembers(id);
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
                : delegations.loadRecipientDepartments(id).stream()
                .map(row -> new DepartmentSelection(text(row.getDepartmentId()),
                        Boolean.TRUE.equals(row.getIncludeDescendants())))
                .toList();
        return new Selection(memberIds, departments);
    }

    private List<ActionScopeCeiling> ceilings(long id) {
        return delegations.loadCeilings(id).stream()
                .map(row -> new ActionScopeCeiling(text(row.getActionId()),
                        IamJson.read(row.getScopes(), SCOPES),
                        IamJson.read(row.getScopeBindings(), BINDINGS)))
                .toList();
    }

    private boolean derivedStillValid(long delegationId, DelegationInput next) {
        for (IamRoleAssignmentEntity row : delegations.activeDerivedAssignments(delegationId)) {
            long revisionId = row.getRevisionId().longValue();
            boolean allowed = next.allowedRoleRevisionRefs().stream()
                    .anyMatch(ref -> IamIds.require(ref.id()) == revisionId);
            if (!allowed) {
                return false;
            }
            Instant from = instant(row.getValidFrom());
            Instant until = instant(row.getValidUntil());
            if (until == null || Duration.between(from, until).compareTo(next.maxAssignmentDuration()) > 0) {
                return false;
            }
        }
        return true;
    }

    private List<String> derivedIds(long delegationId) {
        return delegations.activeDerivedAssignments(delegationId).stream()
                .map(row -> text(row.getId()))
                .toList();
    }

    private static Long tenantId(AuthorizationDomain domain, ActiveIdentity actor) {
        return domain == AuthorizationDomain.TENANT ? IamIds.require(actor.context().tenantId()) : null;
    }

    private static Duration duration(IamDelegationGrantEntity grant) {
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
