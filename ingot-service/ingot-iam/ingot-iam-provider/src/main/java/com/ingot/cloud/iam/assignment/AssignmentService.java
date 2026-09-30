package com.ingot.cloud.iam.assignment;

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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.AssignmentRepository;
import com.ingot.cloud.iam.delegation.DelegationAdmission;
import com.ingot.cloud.iam.persistence.IamRoleRevisionJoin;
import com.ingot.cloud.iam.persistence.RoleRepository;
import com.ingot.cloud.iam.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.persistence.entity.IamDelegationGrantEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleDefinitionEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleRevisionEntity;
import com.ingot.cloud.iam.role.RoleService;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamCapabilities;
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
import com.ingot.framework.commons.model.iam.MemberRoleView;
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
    private static final int MAX_SUBJECT_KEYWORD_LENGTH = 128;
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() {
    };
    private static final String ASSIGNMENT = "assignment";
    private static final String EMPTY_SCOPE_JSON = "{}";
    private final IamAccess access;
    private final IamAuditWriter audits;
    private final AuthorizationChangeNotifier changes;
    private final RoleService roles;
    private final RoleRepository roleStore;
    private final AssignmentRepository assignments;
    private final DelegationAdmission delegations;
    private final TransactionTemplate transaction;
    private final com.ingot.cloud.iam.evaluation.ResourceAccess resourceAccess;
    private final PlatformAuthorizationEditor editor;

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
     * @param editor 平台参数对象验证
     * @param resourceAccess 成员对象边界
     * @param transactionManager 同一数据源事务
     */
    public AssignmentService(IamAccess access, IamAuditWriter audits, AuthorizationChangeNotifier changes,
                                 RoleService roles, RoleRepository roleStore, AssignmentRepository assignments,
                                 DelegationAdmission delegations,
                                 com.ingot.cloud.iam.evaluation.ResourceAccess resourceAccess, PlatformAuthorizationEditor editor,
                                 PlatformTransactionManager transactionManager) {
        this.access = access;
        this.audits = audits;
        this.changes = changes;
        this.roles = roles;
        this.roleStore = roleStore;
        this.assignments = assignments;
        this.delegations = delegations;
        this.resourceAccess = resourceAccess;
        this.editor = editor;
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
        return list(domain, page, pageSize, null, null);
    }

    /**
     * 在平台分配可见边界内按接收主体和名称分页；租户列表保持原契约。
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @param subjectType 平台接收主体类型，可空
     * @param keyword 平台成员显示名或组名称，可空
     * @return 筛选后的分配页
     */
    public PageResponse<ResourceDetail<AssignmentRecord>> list(AuthorizationDomain domain, int page, int pageSize,
            SubjectType subjectType, String keyword) {
        IamAdmission admission = access.admit(domain, action(domain, AccessKind.READ));
        ActiveIdentity actor = admission.actor();
        IamPages.require(page, pageSize);
        String search = keyword == null ? null : keyword.strip();
        if (search != null && search.length() > MAX_SUBJECT_KEYWORD_LENGTH) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        if (search != null && search.isEmpty()) {
            search = null;
        }
        if (domain != AuthorizationDomain.PLATFORM && (subjectType != null || search != null)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        Page<IamRoleAssignmentEntity> rows = domain == AuthorizationDomain.PLATFORM
                ? admission.governed()
                    ? assignments.pagePlatform(page, pageSize, subjectType, search)
                    : assignments.pageOwned(IamIds.require(actor.context().memberId()), page, pageSize,
                            subjectType, search)
                : assignments.page(domain, tenantId(domain, actor), page, pageSize);
        var presentation = domain == AuthorizationDomain.PLATFORM ? assignments.presentation(rows.getRecords().stream()
                .map(IamRoleAssignmentEntity::getId).toList()) : Map.<BigInteger,
                com.ingot.cloud.iam.persistence.projection.AssignmentPresentation>of();
        IamCapabilities permissions = rows.getRecords().isEmpty() ? new IamCapabilities(Map.of())
                : recordCapabilities(domain, actor);
        var sources = recordSources(domain, actor, rows.getRecords(), permissions);
        List<ResourceDetail<AssignmentRecord>> items = rows.getRecords().stream()
                .map(row -> detail(domain, actor, row, presentation.get(row.getId()), permissions, sources)).toList();
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
            assignments.lockAuthorization(domain);
            IamAdmission currentAdmission = access.admit(domain, action(domain, AccessKind.CREATE));
            if (!batchSource(currentAdmission, input).isEmpty()) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
            CreatedResource first = null;
            int index = 0;
            for (AssignmentInput item : input.items()) {
                List<ValidationIssue> errors = validate(domain, currentAdmission, item, null);
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
     * 预览一条既有分配，读、编辑与撤销遵循相同来源边界。
     * @param domain 管理域
     * @param id 分配 ID
     * @param input 草稿与版本
     * @return 无写入的校验效果
     */
    public Preview<AssignmentPreviewResult> previewUpdate(AuthorizationDomain domain, String id, AssignmentUpdateInput input) {
        IamAdmission admission = access.admit(domain, action(domain, AccessKind.UPDATE));
        var current = assignments.find(domain, tenantId(domain, admission.actor()), IamIds.require(id));
        requireRecord(domain, admission, current);
        IamIds.requireVersion(input.expectedVersion(), version(current.getVersion()));
        if (current.getStatus() != GrantStatus.ACTIVE) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        AssignmentInput next = input.assignment();
        if (!sameSubject(current, next.subject()) || !sameDelegation(current.getDelegationGrantId(), next.delegationGrantId())
                || current.getRevisionKind() != next.roleRevisionRef().kind()
                || current.getRevisionId().longValueExact() != IamIds.require(next.roleRevisionRef().id())) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        var errors = validate(domain, admission, next, IamIds.require(id));
        var grants = errors.isEmpty() ? roles.synthesizedGrants(IamIds.require(next.roleRevisionRef().id())) : List.<ActionGrant>of();
        return new Preview<>(version(current.getVersion()), errors.isEmpty(), errors, List.of(),
                new ImpactSummary(null, 1L, null, false), new AssignmentPreviewResult(List.of(
                    new AssignmentPreviewItem(next.subject(), errors.isEmpty(), errors, grants))));
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
            assignments.lockAuthorization(domain);
            IamAdmission currentAdmission = access.admit(domain, action(domain, AccessKind.UPDATE));
            IamRoleAssignmentEntity current = lock(domain, actor, assignmentId);
            requireRecord(domain, currentAdmission, current);
            IamIds.requireVersion(input.expectedVersion(), version(current.getVersion()));
            AssignmentInput next = input.assignment();
            if (current.getStatus() != GrantStatus.ACTIVE
                    || domain == AuthorizationDomain.PLATFORM && (current.getRevisionKind() != next.roleRevisionRef().kind()
                    || current.getRevisionId().longValueExact() != IamIds.require(next.roleRevisionRef().id()))
                    || !sameSubject(current, next.subject()) || !sameDelegation(current.getDelegationGrantId(),
                    next.delegationGrantId())) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            List<ValidationIssue> errors = validate(domain, currentAdmission, next, assignmentId);
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
            assignments.lockAuthorization(domain);
            IamAdmission currentAdmission = access.admit(domain, action(domain, AccessKind.DELETE));
            IamRoleAssignmentEntity current = lock(domain, actor, assignmentId);
            requireRecord(domain, currentAdmission, current);
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

    /**
     * 读取平台成员的简单直接角色，不含用户组继承或带范围、有效期、委派来源的授权。
     *
     * @param domain 接口管理域
     * @param memberId 平台成员 ID
     * @return 按名称排序的直接角色
     */
    public List<MemberRoleView> listDirectRoles(AuthorizationDomain domain, String memberId) {
        access.require(domain, IamAction.PLATFORM_MEMBER_READ);
        requirePlatform(domain);
        long id = IamIds.require(memberId);
        if (!assignments.platformMemberExists(id)) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return collectDirectRoles(id);
    }

    /**
     * 为新建平台成员追加简单直接角色，调用方须已处于同一事务。
     *
     * @param domain 接口管理域
     * @param memberId 平台成员 ID
     * @param roleIds 角色定义 ID
     */
    public void grantDirectRoles(AuthorizationDomain domain, String memberId, List<String> roleIds) {
        requirePlatform(domain);
        IamAdmission admission = access.admit(domain, IamAction.PLATFORM_ASSIGNMENT_CREATE);
        if (!admission.governed()) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        assignments.lockAuthorization(domain);
        admission = access.admit(domain, IamAction.PLATFORM_ASSIGNMENT_CREATE);
        if (!admission.governed()) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        grantDirectRoles(domain, admission, IamIds.require(memberId), uniqueIds(roleIds));
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
        access.require(domain, IamAction.PLATFORM_MEMBER_UPDATE);
        requirePlatform(domain);
        IamAdmission admission = access.admit(domain, IamAction.PLATFORM_ASSIGNMENT_CREATE);
        long id = IamIds.require(memberId);
        List<String> desired = uniqueIds(roleIds);
        return transaction.execute(status -> {
            assignments.lockAuthorization(domain);
            IamAdmission creating = access.admit(domain, IamAction.PLATFORM_ASSIGNMENT_CREATE);
            IamAdmission revoking = access.admit(domain, IamAction.PLATFORM_ASSIGNMENT_DELETE);
            if (!creating.governed() || !revoking.governed()) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
            resourceAccess.requireVisibleMember(creating.actor().context(), IamAction.PLATFORM_MEMBER_UPDATE, id);
            if (!assignments.platformMemberExists(id)) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            Map<String, List<IamRoleAssignmentEntity>> current = simpleAssignmentsByRole(id);
            for (Map.Entry<String, List<IamRoleAssignmentEntity>> entry : current.entrySet()) {
                if (desired.contains(entry.getKey())) {
                    continue;
                }
                for (IamRoleAssignmentEntity row : entry.getValue()) {
                    if (assignments.revoke(row.getId().longValueExact(), row.getVersion()) != 1) {
                        throw new BizException(IamReasonCode.REVISION_CONFLICT);
                    }
                    audits.write(creating.actor().context(), access.nextId(), ASSIGNMENT, text(row.getId()),
                            AuditChangeType.DISABLE, Map.of(AuditField.STATUS, GrantStatus.ACTIVE.name()),
                            Map.of(AuditField.STATUS, GrantStatus.REVOKED.name()),
                            Map.of(ASSIGNMENT, nextVersion(row.getVersion())), null, text(row.getId()));
                }
            }
            List<String> adding = desired.stream().filter(roleId -> !current.containsKey(roleId)).toList();
            grantDirectRoles(domain, creating, id, adding);
            changes.markAll();
            return collectDirectRoles(id);
        });
    }

    /**
     * 返回平台分配配置资格；任何直接资格都必须来自对应精确操作的非委派来源。
     * @return 当前配置资格
     */
    public com.ingot.framework.commons.model.iam.AssignmentContext context() {
        ActiveIdentity actor = access.requireCurrent();
        if (actor.context().domain() != AuthorizationDomain.PLATFORM) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        var actions = List.of(
                IamAction.PLATFORM_ASSIGNMENT_READ, IamAction.PLATFORM_ASSIGNMENT_CREATE,
                IamAction.PLATFORM_ASSIGNMENT_UPDATE, IamAction.PLATFORM_ASSIGNMENT_DELETE);
        IamCapabilities permissions = access.capabilities(actor, actions);
        if (actions.stream().noneMatch(action -> permissions.allows(action, false))) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        return new com.ingot.framework.commons.model.iam.AssignmentContext(
                permissions.allows(IamAction.PLATFORM_ASSIGNMENT_READ, true),
                permissions.allows(IamAction.PLATFORM_ASSIGNMENT_CREATE, true),
                permissions.allows(IamAction.PLATFORM_ASSIGNMENT_UPDATE, true),
                permissions.allows(IamAction.PLATFORM_ASSIGNMENT_DELETE, true),
                assignments.effectiveDelegations(IamIds.require(actor.context().memberId())).size());
    }

    /**
     * 成员创建事务在插入成员前取得共用授权首锁。
     * @param domain 管理域
     */
    public void lockAuthorization(AuthorizationDomain domain) {
        assignments.lockAuthorization(domain);
    }

    /**
     * 读取分配详情，沿用列表的可信来源边界。
     * @param domain 管理域
     * @param id 分配 ID
     * @return 可披露详情
     */
    public ResourceDetail<AssignmentRecord> detail(AuthorizationDomain domain, String id) {
        IamAdmission admission = access.admit(domain, action(domain, AccessKind.READ));
        IamRoleAssignmentEntity row = assignments.find(domain, tenantId(domain, admission.actor()), IamIds.require(id));
        requireRecord(domain, admission, row);
        return detail(domain, admission.actor(), row);
    }

    private void requireRecord(AuthorizationDomain domain, IamAdmission admission, IamRoleAssignmentEntity row) {
        if (row == null || !recordAllowed(domain, admission, row)) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
    }

    private boolean recordAllowed(AuthorizationDomain domain, IamAdmission admission, IamRoleAssignmentEntity row) {
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

    private ResourceDetail<AssignmentRecord> detail(AuthorizationDomain domain, ActiveIdentity actor,
                                                   IamRoleAssignmentEntity row) {
        IamCapabilities permissions = recordCapabilities(domain, actor);
        return detail(domain, actor, row, domain == AuthorizationDomain.PLATFORM
                ? assignments.presentation(List.of(row.getId())).get(row.getId()) : null,
                permissions, recordSources(domain, actor, List.of(row), permissions));
    }

    private IamCapabilities recordCapabilities(AuthorizationDomain domain, ActiveIdentity actor) {
        return domain == AuthorizationDomain.PLATFORM ? access.capabilities(actor, List.of(
                IamAction.PLATFORM_ASSIGNMENT_READ, IamAction.PLATFORM_ASSIGNMENT_UPDATE,
                IamAction.PLATFORM_ASSIGNMENT_DELETE)) : new IamCapabilities(Map.of());
    }

    private Map<BigInteger, IamDelegationGrantEntity> recordSources(AuthorizationDomain domain, ActiveIdentity actor,
            List<IamRoleAssignmentEntity> rows, IamCapabilities permissions) {
        if (domain != AuthorizationDomain.PLATFORM || List.of(IamAction.PLATFORM_ASSIGNMENT_READ,
                IamAction.PLATFORM_ASSIGNMENT_UPDATE, IamAction.PLATFORM_ASSIGNMENT_DELETE).stream()
                .allMatch(action -> permissions.allows(action, true))) {
            return Map.of();
        }
        List<BigInteger> ids = rows.stream().map(IamRoleAssignmentEntity::getDelegationGrantId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        return assignments.findDelegations(domain, tenantId(domain, actor), ids);
    }

    private ResourceDetail<AssignmentRecord> detail(AuthorizationDomain domain, ActiveIdentity actor,
            IamRoleAssignmentEntity row, com.ingot.cloud.iam.persistence.projection.AssignmentPresentation presentation,
            IamCapabilities permissions, Map<BigInteger, IamDelegationGrantEntity> sources) {
        if (domain != AuthorizationDomain.PLATFORM) { return IamDetails.of(record(row), version(row.getVersion())); }
        var source = row.getDelegationGrantId() == null ? null : sources.get(row.getDelegationGrantId());
        boolean owned = source != null && source.getPlatformAdministratorId() != null
                && source.getPlatformAdministratorId().toString().equals(actor.context().memberId());
        Map<String, com.ingot.framework.commons.model.iam.ObjectCapability> capabilities = new LinkedHashMap<>();
        for (AccessKind kind : List.of(AccessKind.READ, AccessKind.UPDATE, AccessKind.DELETE)) {
            IamAction operation = action(domain, kind);
            boolean allowed = (kind == AccessKind.READ || row.getStatus() == GrantStatus.ACTIVE)
                    && (kind != AccessKind.UPDATE || presentation == null || presentation.sourceValid())
                    && permissions.allows(operation, false)
                    && (permissions.allows(operation, true) || owned);
            capabilities.put(operation.getCode(), new com.ingot.framework.commons.model.iam.ObjectCapability(
                    allowed, allowed ? null : IamReasonCode.ACTION_DENIED, allowed ? null : "当前身份不可操作该分配"));
        }
        AssignmentRecord basic = record(row);
        if (domain == AuthorizationDomain.PLATFORM && presentation != null) {
            var state = com.ingot.framework.commons.model.iam.AssignmentEffectiveStatus.ACTIVE;
            Instant now = Instant.now();
            if (row.getStatus() == GrantStatus.REVOKED) {
                state = com.ingot.framework.commons.model.iam.AssignmentEffectiveStatus.REVOKED;
            } else if (row.getValidUntil() != null && !now.isBefore(instant(row.getValidUntil()))) {
                state = com.ingot.framework.commons.model.iam.AssignmentEffectiveStatus.EXPIRED;
            } else if (!presentation.sourceValid()) {
                state = com.ingot.framework.commons.model.iam.AssignmentEffectiveStatus.SOURCE_INVALID;
            } else if (row.getValidFrom() != null && now.isBefore(instant(row.getValidFrom()))) {
                state = com.ingot.framework.commons.model.iam.AssignmentEffectiveStatus.PENDING;
            }
            basic = new AssignmentRecord(basic.id(), basic.assignment(), basic.status(), basic.source(),
                    presentation.subjectName(), presentation.roleName(), text(presentation.revisionNumber()),
                    row.getDelegationGrantId() == null ? "直接分配" : "来源委派 " + row.getDelegationGrantId(),
                    instant(row.getCreatedAt()), new com.ingot.framework.commons.model.iam.AssignmentAuthor(
                        text(presentation.authorId()), presentation.authorName() == null ? "未知" : presentation.authorName()),
                    state);
        }
        return IamDetails.of(basic, capabilities, version(row.getVersion()));
    }

    private ResourceDetail<AssignmentRecord> get(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        IamRoleAssignmentEntity row = assignments.find(domain, tenantId(domain, actor), id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return detail(domain, actor, row);
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
        if (domain == AuthorizationDomain.PLATFORM && revisionUsable) {
            errors.addAll(editor.validateBindings(item));
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
            AssignmentInput item, Instant from, boolean revisionUsable, boolean governingExisting) {
        long delegationId = IamIds.require(item.delegationGrantId());
        long revisionId = IamIds.require(item.roleRevisionRef().id());
        List<ActionGrant> grants = revisionUsable ? roles.synthesizedGrants(revisionId) : List.of();
        return delegations.check(new DelegationAdmission.Request(domain, tenantId(domain, actor), delegationId,
                governingExisting ? null : IamIds.require(actor.context().memberId()), revisionId, grants,
                item.scopeBindings(), item.subject(),
                from, item.validUntil()));
    }

    private static boolean sourced(AssignmentInput item) {
        return item != null && item.delegationGrantId() != null && !item.delegationGrantId().isBlank();
    }

    private boolean subjectExists(AuthorizationDomain domain, ActiveIdentity actor, SubjectRef subject) {
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

    private void grantDirectRoles(AuthorizationDomain domain, IamAdmission admission, long memberId,
                                  List<String> roleIds) {
        ActiveIdentity actor = admission.actor();
        for (String roleId : roleIds) {
            AssignmentInput item = simpleAssignment(memberId, latestPublished(domain, roleId));
            List<ValidationIssue> errors = validate(domain, admission, item, null);
            if (!errors.isEmpty()) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            insert(domain, actor, item);
        }
        if (!roleIds.isEmpty()) {
            changes.markAll();
        }
    }

    private List<MemberRoleView> collectDirectRoles(long memberId) {
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

    private Map<String, List<IamRoleAssignmentEntity>> simpleAssignmentsByRole(long memberId) {
        Map<String, List<IamRoleAssignmentEntity>> grouped = new LinkedHashMap<>();
        for (IamRoleAssignmentEntity row : assignments.listActivePlatformMember(memberId)) {
            if (!isSimpleDirect(row)) {
                continue;
            }
            IamRoleRevisionJoin revision = assignments.findRevision(row.getRevisionId().longValueExact());
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
        IamRoleDefinitionEntity definition = roleStore.findDefinition(domain, false, null, id);
        if (definition == null || !Boolean.TRUE.equals(definition.getEnabled())) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        IamRoleRevisionEntity latest = roleStore.latestRevision(id);
        if (latest == null || latest.getId() == null) {
            throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
        }
        return new RoleRevisionRef(latest.getKind(), IamIds.text(latest.getId().longValueExact()));
    }

    private static AssignmentInput simpleAssignment(long memberId, RoleRevisionRef revision) {
        return new AssignmentInput(new SubjectRef(SubjectType.MEMBER, IamIds.text(memberId)), revision, Map.of(),
                null, null, null);
    }

    private static boolean isSimpleDirect(IamRoleAssignmentEntity row) {
        if (row.getSource() != AssignmentSource.MANUAL || row.getDelegationGrantId() != null
                || row.getValidUntil() != null) {
            return false;
        }
        String bindings = row.getScopeBindings();
        return bindings == null || bindings.isBlank() || EMPTY_SCOPE_JSON.equals(bindings.trim());
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

    private static void requirePlatform(AuthorizationDomain domain) {
        if (domain != AuthorizationDomain.PLATFORM) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
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
