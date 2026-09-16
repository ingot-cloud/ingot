package com.ingot.cloud.iam.assignment;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.AssignmentRepository;
import com.ingot.cloud.iam.delegation.DelegationAdmission;
import com.ingot.cloud.iam.persistence.IamRoleRevisionJoin;
import com.ingot.cloud.iam.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.role.RoleService;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAdmission;
import com.ingot.cloud.iam.support.IamAuditWriter;
import com.ingot.cloud.iam.support.IamDetails;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.cloud.iam.support.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.AssignmentBatchInput;
import com.ingot.framework.commons.model.iam.AssignmentInput;
import com.ingot.framework.commons.model.iam.AssignmentPreviewItem;
import com.ingot.framework.commons.model.iam.AssignmentPreviewResult;
import com.ingot.framework.commons.model.iam.AssignmentRecord;
import com.ingot.framework.commons.model.iam.AssignmentSource;
import com.ingot.framework.commons.model.iam.AssignmentUpdateInput;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.CreatedResource;
import com.ingot.framework.commons.model.iam.GrantStatus;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.ImpactSummary;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.framework.commons.model.iam.Preview;
import com.ingot.framework.commons.model.iam.ResourceDetail;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeBindingKind;
import com.ingot.framework.commons.model.iam.SubjectRef;
import com.ingot.framework.commons.model.iam.SubjectType;
import com.ingot.framework.commons.model.iam.ValidationIssue;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>维护原子角色分配，提交时重验主体、固定版本、范围和委派来源。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class AssignmentService {
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() {
    };
    private static final String ASSIGNMENT = "assignment";
    private final IamAccess access;
    private final IamAuditWriter audits;
    private final AuthorizationChangeNotifier changes;
    private final RoleService roles;
    private final AssignmentRepository assignments;
    private final DelegationAdmission delegations;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、审计、角色合成与分配表。
     * <p>TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成，保留显式构造器。</p>
     *
     * @param access 当前身份
     * @param audits 同事务审计
     * @param changes 授权热缓存失效
     * @param roles 角色版本合成
     * @param assignments 分配持久化
     * @param delegations 委派派生授权准入
     * @param transactionManager 同一数据源事务
     */
    public AssignmentService(IamAccess access, IamAuditWriter audits, AuthorizationChangeNotifier changes,
                                 RoleService roles, AssignmentRepository assignments,
                                 DelegationAdmission delegations,
                                 PlatformTransactionManager transactionManager) {
        this.access = access;
        this.audits = audits;
        this.changes = changes;
        this.roles = roles;
        this.assignments = assignments;
        this.delegations = delegations;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 分页列出当前域授权。
     *
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @return 授权页
     */
    public PageResponse<ResourceDetail<AssignmentRecord>> list(AuthorizationDomain domain, int page, int pageSize) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.READ));
        IamPages.require(page, pageSize);
        Page<IamRoleAssignmentEntity> rows = assignments.page(domain, tenantId(domain, actor), page, pageSize);
        List<ResourceDetail<AssignmentRecord>> items = rows.getRecords().stream()
                .map(row -> IamDetails.of(record(row), version(row.getVersion()))).toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 原子批量创建授权，任一条失败整批回滚。
     *
     * @param domain 接口管理域
     * @param input 批次
     * @return 首条授权 ID
     */
    public CreatedResource create(AuthorizationDomain domain, AssignmentBatchInput input) {
        IamAdmission admission = access.admit(domain, action(domain, AccessKind.CREATE));
        ActiveIdentity actor = admission.actor();
        if (!batchSource(admission, input).isEmpty()) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        return transaction.execute(status -> {
            CreatedResource first = null;
            int index = 0;
            for (AssignmentInput item : input.items()) {
                List<ValidationIssue> errors = validate(domain, admission, item, null);
                if (!errors.isEmpty()) {
                    throw new BizException(IamReasonCode.INVALID_ARGUMENT);
                }
                CreatedResource created = insert(domain, actor, item);
                if (index == 0) {
                    first = created;
                }
                index++;
            }
            changes.markAll();
            return first;
        });
    }

    /**
     * 预览批量分配，无写入。
     *
     * @param domain 接口管理域
     * @param input 批次
     * @return 逐条效果
     */
    public Preview<AssignmentPreviewResult> preview(AuthorizationDomain domain, AssignmentBatchInput input) {
        IamAdmission admission = access.admit(domain, action(domain, AccessKind.CREATE));
        List<AssignmentPreviewItem> items = new ArrayList<>();
        List<ValidationIssue> errors = new ArrayList<>(batchSource(admission, input));
        for (AssignmentInput item : input.items()) {
            List<ValidationIssue> itemErrors = validate(domain, admission, item, null);
            List<ActionGrant> grants = List.of();
            if (itemErrors.isEmpty()) {
                grants = roles.synthesizedGrants(IamIds.require(item.roleRevisionRef().id()));
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
     * 调整既有授权，禁止改写主体或伪造委派来源。
     *
     * @param domain 接口管理域
     * @param id 授权 ID
     * @param input 待保存定义
     * @return 更新后详情
     */
    public ResourceDetail<AssignmentRecord> replace(AuthorizationDomain domain, String id, AssignmentUpdateInput input) {
        IamAdmission admission = access.admit(domain, action(domain, AccessKind.UPDATE));
        ActiveIdentity actor = admission.actor();
        long assignmentId = IamIds.require(id);
        return transaction.execute(status -> {
            IamRoleAssignmentEntity current = lock(domain, actor, assignmentId);
            IamIds.requireVersion(input.expectedVersion(), version(current.getVersion()));
            AssignmentInput next = input.assignment();
            if (!sameSubject(current, next.subject()) || !sameDelegation(current.getDelegationGrantId(),
                    next.delegationGrantId())) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            List<ValidationIssue> errors = validate(domain, admission, next, assignmentId);
            if (!errors.isEmpty()) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            Instant from = next.validFrom() == null ? Instant.now() : next.validFrom();
            assignments.update(assignmentId, IamIds.require(next.roleRevisionRef().id()), next.roleRevisionRef().kind(),
                    IamJson.object(next.scopeBindings()), utc(from), utc(next.validUntil()), current.getVersion());
            audits.write(actor.context(), access.nextId(), ASSIGNMENT, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.ROLE_REVISION, IamIds.text(current.getRevisionId().longValue())),
                    Map.of(AuditField.ROLE_REVISION, next.roleRevisionRef().id()),
                    Map.of(ASSIGNMENT, nextVersion(current.getVersion())),
                    text(current.getDelegationGrantId()), id);
            changes.markAll();
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
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.DELETE));
        long assignmentId = IamIds.require(id);
        return transaction.execute(status -> {
            IamRoleAssignmentEntity current = lock(domain, actor, assignmentId);
            if (assignments.revoke(assignmentId, current.getVersion()) != 1) {
                throw new BizException(IamReasonCode.REVISION_CONFLICT);
            }
            audits.write(actor.context(), access.nextId(), ASSIGNMENT, id, AuditChangeType.DISABLE,
                    Map.of(AuditField.STATUS, current.getStatus().name()),
                    Map.of(AuditField.STATUS, GrantStatus.REVOKED.name()),
                    Map.of(ASSIGNMENT, nextVersion(current.getVersion())),
                    text(current.getDelegationGrantId()), id);
            changes.markAll();
            return new CreatedResource(id, version(current.getVersion()));
        });
    }

    private ResourceDetail<AssignmentRecord> get(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        IamRoleAssignmentEntity row = assignments.find(domain, tenantId(domain, actor), id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return IamDetails.of(record(row), version(row.getVersion()));
    }

    private CreatedResource insert(AuthorizationDomain domain, ActiveIdentity actor, AssignmentInput item) {
        long id = access.nextId();
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
        entity.setScopeBindings(IamJson.object(item.scopeBindings()));
        entity.setDelegationGrantId(item.delegationGrantId() == null || item.delegationGrantId().isBlank()
                ? null : BigInteger.valueOf(IamIds.require(item.delegationGrantId())));
        entity.setValidFrom(utc(from));
        entity.setValidUntil(utc(item.validUntil()));
        entity.setStatus(GrantStatus.ACTIVE);
        entity.setSource(AssignmentSource.MANUAL);
        try {
            assignments.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        audits.write(actor.context(), access.nextId(), ASSIGNMENT, IamIds.text(id), AuditChangeType.CREATE,
                Map.of(), Map.of(AuditField.ROLE_REVISION, item.roleRevisionRef().id()), Map.of(ASSIGNMENT, "0"),
                item.delegationGrantId(), IamIds.text(id));
        return new CreatedResource(IamIds.text(id), "0");
    }

    /**
     * 校验单条分配；受限方还必须绑定属于自己的委派。
     */
    private List<ValidationIssue> validate(AuthorizationDomain domain, IamAdmission admission, AssignmentInput item,
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
        Instant from = item.validFrom() == null ? Instant.now() : item.validFrom();
        if (item.validUntil() != null && !from.isBefore(item.validUntil())) {
            errors.add(new ValidationIssue("validUntil", IamReasonCode.INVALID_ARGUMENT, "起止时间必须形成左闭右开区间"));
        }
        if (sourced(item)) {
            errors.addAll(validateDelegation(domain, actor, item, from, revisionUsable));
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
    private List<ValidationIssue> batchSource(IamAdmission admission, AssignmentBatchInput input) {
        if (admission.governed() || input == null || input.items() == null) {
            return List.of();
        }
        long sources = input.items().stream()
                .filter(AssignmentService::sourced)
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
    private List<ValidationIssue> validateDelegation(AuthorizationDomain domain, ActiveIdentity actor,
                                                     AssignmentInput item, Instant from, boolean revisionUsable) {
        long delegationId = IamIds.require(item.delegationGrantId());
        long revisionId = IamIds.require(item.roleRevisionRef().id());
        List<ActionGrant> grants = revisionUsable ? roles.synthesizedGrants(revisionId) : List.of();
        return delegations.check(new DelegationAdmission.Request(domain, tenantId(domain, actor), delegationId,
                IamIds.require(actor.context().memberId()), revisionId, grants, item.scopeBindings(), item.subject(),
                from, item.validUntil()));
    }

    private static boolean sourced(AssignmentInput item) {
        return item != null && item.delegationGrantId() != null && !item.delegationGrantId().isBlank();
    }

    private boolean subjectExists(AuthorizationDomain domain, ActiveIdentity actor, SubjectRef subject) {
        long id = IamIds.require(subject.id());
        if (domain == AuthorizationDomain.PLATFORM && subject.type() == SubjectType.MEMBER) {
            return assignments.platformMemberExists(id);
        }
        if (domain == AuthorizationDomain.PLATFORM) {
            return assignments.platformGroupExists(id);
        }
        long tenant = IamIds.require(actor.context().tenantId());
        if (subject.type() == SubjectType.MEMBER) {
            return assignments.tenantMemberExists(tenant, id);
        }
        return assignments.tenantGroupExists(tenant, id);
    }

    private boolean revisionAssignable(AuthorizationDomain domain, ActiveIdentity actor, IamRoleRevisionJoin revision) {
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

    private static boolean hasDepartmentBinding(Map<String, ScopeBinding> bindings) {
        if (bindings == null) {
            return false;
        }
        return bindings.values().stream().anyMatch(binding -> binding.kind() == ScopeBindingKind.DEPARTMENTS);
    }

    private IamRoleAssignmentEntity lock(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        IamRoleAssignmentEntity row = assignments.lock(domain, tenantId(domain, actor), id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return row;
    }

    private AssignmentRecord record(IamRoleAssignmentEntity row) {
        SubjectType type = row.getSubjectType();
        String subjectId = type == SubjectType.MEMBER
                ? firstNonNull(row.getPlatformMemberId(), row.getTenantMemberId())
                : firstNonNull(row.getPlatformGroupId(), row.getTenantGroupId());
        Instant from = instant(row.getValidFrom());
        Instant until = instant(row.getValidUntil());
        String delegation = row.getDelegationGrantId() == null ? null : row.getDelegationGrantId().toString();
        AssignmentInput assignment = new AssignmentInput(new SubjectRef(type, subjectId),
                new RoleRevisionRef(row.getRevisionKind(), text(row.getRevisionId())),
                IamJson.read(row.getScopeBindings(), BINDINGS), from, until, delegation);
        return new AssignmentRecord(text(row.getId()), assignment, row.getStatus(), row.getSource());
    }

    private static boolean sameSubject(IamRoleAssignmentEntity current, SubjectRef subject) {
        long id = IamIds.require(subject.id());
        if (current.getSubjectType() != subject.type()) {
            return false;
        }
        BigInteger actual = subject.type() == SubjectType.MEMBER
                ? first(current.getPlatformMemberId(), current.getTenantMemberId())
                : first(current.getPlatformGroupId(), current.getTenantGroupId());
        return actual != null && actual.longValue() == id;
    }

    private static boolean sameDelegation(BigInteger current, String next) {
        if (current == null) {
            return next == null || next.isBlank();
        }
        return next != null && IamIds.require(next) == current.longValue();
    }

    private static Long tenantId(AuthorizationDomain domain, ActiveIdentity actor) {
        return domain == AuthorizationDomain.TENANT ? IamIds.require(actor.context().tenantId()) : null;
    }

    private static LocalDateTime utc(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    private static Instant instant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static String firstNonNull(BigInteger left, BigInteger right) {
        BigInteger value = left != null ? left : right;
        return value == null ? null : value.toString();
    }

    private static BigInteger first(BigInteger left, BigInteger right) {
        return left != null ? left : right;
    }

    private static String version(BigInteger value) {
        return value == null ? "0" : value.toString();
    }

    private static String nextVersion(BigInteger current) {
        return (current == null ? BigInteger.ZERO : current).add(BigInteger.ONE).toString();
    }

    private static String text(BigInteger id) {
        return id == null ? null : IamIds.text(id.longValue());
    }

    private static IamAction action(AuthorizationDomain domain, AccessKind kind) {
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

    private enum AccessKind {
        READ, CREATE, UPDATE, DELETE
    }
}
