package com.ingot.cloud.iam.application.group;

import java.util.EnumMap;
import java.util.TreeMap;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.application.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.infrastructure.persistence.DepartmentClosure;
import com.ingot.cloud.iam.domain.authorization.ObjectScope;
import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.DelegationRecipientRepository;
import com.ingot.cloud.iam.infrastructure.persistence.GroupRepository;
import com.ingot.cloud.iam.infrastructure.persistence.ObjectScopeSql;
import com.ingot.cloud.iam.infrastructure.persistence.OrganizationMemberRepository;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamPlatformGroupEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleAssignmentEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamTenantGroupEntity;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import com.ingot.cloud.iam.application.shared.ResourceDetailFactory;
import com.ingot.cloud.iam.domain.shared.IamFilters;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.application.shared.IamPages;
import com.ingot.cloud.iam.domain.shared.IamSelections;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.domain.audit.AuditChangeType;
import com.ingot.cloud.iam.domain.audit.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.framework.commons.model.iam.DepartmentSelection;
import com.ingot.cloud.iam.application.group.model.GroupDraft;
import com.ingot.cloud.iam.application.group.model.GroupRecord;
import com.ingot.cloud.iam.application.group.model.GroupUpdateInput;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.application.shared.model.ImpactSummary;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.cloud.iam.application.shared.model.ObjectCapability;
import com.ingot.cloud.iam.application.shared.model.Preview;
import com.ingot.cloud.iam.application.shared.model.ReferenceImpactPreview;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.framework.commons.model.iam.Selection;
import com.ingot.cloud.iam.application.assignment.model.ValidationIssue;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>维护当前域静态用户组，修改后重验委派派生授权且平台组不得引用部门。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class GroupService {
    private static final String GROUP = "group";
    private final IamAccess access;
    private final AuthorizationAuditWriter audits;
    private final AuthorizationChangeNotifier changes;
    private final GroupRepository groups;
    private final DelegationRecipientRepository recipients;
    private final DepartmentClosure closure;
    private final ResourceScopeAccessService scopes;
    private final OrganizationMemberRepository memberQueries;
    private final TransactionTemplate transaction;

    /**
     * 绑定身份、审计、失效与组表。
     * <p>TransactionTemplate 无法由 Lombok 从 PlatformTransactionManager 直接生成，保留显式构造器。</p>
     *
     * @param access 当前身份
     * @param audits 同事务审计
     * @param changes 授权热缓存失效
     * @param groups 组持久化
     * @param recipients 委派接收人判定
     * @param closure 部门树展开
     * @param scopes 用户组对象与成员可见性
     * @param memberQueries 组的有效成员范围查询
     * @param transactionManager 同一数据源事务
     */
    public GroupService(IamAccess access, AuthorizationAuditWriter audits, AuthorizationChangeNotifier changes,
                            GroupRepository groups, DelegationRecipientRepository recipients,
                            DepartmentClosure closure, ResourceScopeAccessService scopes,
                            OrganizationMemberRepository memberQueries, PlatformTransactionManager transactionManager) {
        this.access = access;
        this.audits = audits;
        this.changes = changes;
        this.groups = groups;
        this.recipients = recipients;
        this.closure = closure;
        this.scopes = scopes;
        this.memberQueries = memberQueries;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * 分页列出当前域用户组。
     *
     * @param domain 接口管理域
     * @param page 页码
     * @param pageSize 页大小
     * @param name 组名包含匹配，可空
     * @return 组页
     */
    public PageResponse<ResourceDetail<GroupRecord>> list(AuthorizationDomain domain, int page, int pageSize,
                                                         String name) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.READ));
        IamPages.require(page, pageSize);
        String keyword = IamFilters.containsName(name);
        var scope = scopes.objects(actor.context(), action(domain, AccessKind.READ));
        Visibility visibility = visibility(domain, actor);
        if (domain == AuthorizationDomain.PLATFORM) {
            Page<IamPlatformGroupEntity> rows = groups.pagePlatform(page, pageSize, keyword, scope);
            List<ResourceDetail<GroupRecord>> items = rows.getRecords().stream()
                    .map(row -> detail(domain, actor, row.getId().longValue(), row.getName(), row.getDescription(),
                            version(row.getVersion()), visibility))
                    .toList();
            return IamPages.details(items, rows.getTotal(), page, pageSize);
        }
        Page<IamTenantGroupEntity> rows = groups.pageTenant(tenantId(actor), page, pageSize, keyword, scope);
        List<ResourceDetail<GroupRecord>> items = rows.getRecords().stream()
                .map(row -> detail(domain, actor, row.getId().longValue(), row.getName(), row.getDescription(),
                        version(row.getVersion()), visibility))
                .toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 把平台成员加入指定用户组；已在组内的项跳过。
     *
     * @param memberId 平台成员 ID
     * @param groupIds 用户组 ID
     */
    public void addPlatformMemberToGroups(long memberId, List<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return;
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String groupId : groupIds) {
            if (groupId != null && !groupId.isBlank()) {
                unique.add(groupId.trim());
            }
        }
        transaction.execute(status -> {
            groups.lockAuthorization(AuthorizationDomain.PLATFORM);
            ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_GROUP_UPDATE);
            var scope = scopes.objectsForExecution(actor.context(), IamAction.PLATFORM_GROUP_UPDATE);
            if (!groups.existsActivePlatformMember(memberId)) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            for (String groupId : unique) {
                long id = IamIds.require(groupId);
                if (!ObjectScopeSql.matches(scope, id)) {
                    throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
                }
                if (groups.findPlatform(id) == null) {
                    throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
                }
                boolean already = groups.platformMemberIds(id).stream()
                        .anyMatch(current -> current.longValue() == memberId);
                if (already) {
                    continue;
                }
                try {
                    groups.insertPlatformMember(id, memberId);
                } catch (DuplicateKeyException exception) {
                    // 并发加入同一组时视为已成功
                }
            }
            changes.markAll();
            return null;
        });
    }

    /**
     * 读取用户组详情。
     *
     * @param domain 接口管理域
     * @param id 组 ID
     * @return 组详情
     */
    public ResourceDetail<GroupRecord> get(AuthorizationDomain domain, String id) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.READ));
        long groupId = IamIds.require(id);
        scopes.requireVisibleObject(actor.context(), action(domain, AccessKind.READ), groupId);
        return load(domain, actor, groupId);
    }

    /**
     * 创建静态用户组。
     *
     * @param domain 接口管理域
     * @param input 组草稿
     * @return 新组 ID
     */
    public CreatedResource create(AuthorizationDomain domain, GroupDraft input) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.CREATE));
        IamSelections.requireCompatible(domain, input.selection());
        return transaction.execute(status -> {
            groups.lockAuthorization(domain);
            access.require(domain, action(domain, AccessKind.CREATE));
            if (!scopes.objectsForExecution(actor.context(), action(domain, AccessKind.CREATE)).coversAll()) {
                throw new BizException(IamReasonCode.DATA_SCOPE_DENIED);
            }
            requireVisibleSelection(domain, actor, input.selection());
            long id = access.nextId();
            insertGroup(domain, actor, id, input);
            replaceSelection(domain, actor, id, input.selection());
            audits.write(actor.context(), access.nextId(), GROUP, IamIds.text(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, input.name()), Map.of(GROUP, "0"));
            changes.markAll();
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 整体替换组内容并重验委派派生授权。
     *
     * @param domain 接口管理域
     * @param id 组 ID
     * @param input 完整内容
     * @return 替换后详情
     */
    public ResourceDetail<GroupRecord> replace(AuthorizationDomain domain, String id, GroupUpdateInput input) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.UPDATE));
        Selection next = input.group().selection();
        if (next != null) {
            IamSelections.requireCompatible(domain, next);
        }
        long groupId = IamIds.require(id);
        return transaction.execute(status -> {
            groups.lockAuthorization(domain);
            access.require(domain, action(domain, AccessKind.UPDATE));
            requireWriteScope(domain, actor, AccessKind.UPDATE, groupId);
            ResourceDetail<GroupRecord> current = lock(domain, actor, groupId);
            IamIds.requireVersion(input.expectedVersion(), current.version());
            if (next != null) {
                requireVisibleSelection(domain, actor, selectionOf(domain, actor, groupId));
                requireVisibleSelection(domain, actor, next);
                if (!invalidDelegatedAssignments(domain, actor, groupId, next).isEmpty()) {
                    throw new BizException(IamReasonCode.POLICY_CONFLICT);
                }
            }
            updateGroup(domain, actor, groupId, input.group(), current.version());
            if (next != null) {
                replaceSelection(domain, actor, groupId, next);
            }
            String version = Long.toString(Long.parseLong(current.version()) + 1);
            audits.write(actor.context(), access.nextId(), GROUP, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.record().name()),
                    Map.of(AuditField.NAME, input.group().name()), Map.of(GROUP, version));
            changes.markAll();
            return load(domain, actor, groupId);
        });
    }

    /**
     * 删除未被授权引用的用户组。
     *
     * @param domain 接口管理域
     * @param id 组 ID
     * @return 删除前版本
     */
    public CreatedResource delete(AuthorizationDomain domain, String id) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.DELETE));
        long groupId = IamIds.require(id);
        return transaction.execute(status -> {
            groups.lockAuthorization(domain);
            access.require(domain, action(domain, AccessKind.DELETE));
            requireWriteScope(domain, actor, AccessKind.DELETE, groupId);
            ResourceDetail<GroupRecord> current = lock(domain, actor, groupId);
            if (referenced(domain, actor, groupId)) {
                throw new BizException(IamReasonCode.OBJECT_IN_USE);
            }
            clearSelection(domain, actor, groupId);
            if (domain == AuthorizationDomain.PLATFORM) {
                groups.deletePlatform(groupId);
            } else {
                groups.deleteTenant(tenantId(actor), groupId);
            }
            audits.write(actor.context(), access.nextId(), GROUP, id, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.record().name()), Map.of(), Map.of(GROUP, current.version()));
            changes.markAll();
            return new CreatedResource(id, current.version());
        });
    }

    /**
     * 预览组替换对委派派生授权的影响，无写入。
     *
     * @param domain 接口管理域
     * @param id 组 ID
     * @param input 待保存内容
     * @return 引用影响
     */
    public Preview<ReferenceImpactPreview> preview(AuthorizationDomain domain, String id, GroupUpdateInput input) {
        ActiveIdentity actor = access.require(domain, action(domain, AccessKind.PREVIEW));
        long groupId = IamIds.require(id);
        scopes.requireVisibleObject(actor.context(), action(domain, AccessKind.PREVIEW), groupId);
        ResourceDetail<GroupRecord> current = load(domain, actor, groupId);
        Selection next = input.group().selection();
        List<ValidationIssue> errors = new ArrayList<>();
        try {
            IamIds.requireVersion(input.expectedVersion(), current.version());
        } catch (BizException exception) {
            errors.add(new ValidationIssue("expectedVersion", IamReasonCode.REVISION_CONFLICT, exception.getMessage()));
        }
        if (next != null) {
            IamSelections.requireCompatible(domain, next);
            requireVisibleSelection(domain, actor, selectionOf(domain, actor, groupId));
            requireVisibleSelection(domain, actor, next);
            if (!invalidDelegatedAssignments(domain, actor, groupId, next).isEmpty()) {
                errors.add(new ValidationIssue("selection", IamReasonCode.POLICY_CONFLICT, "组变更后委派派生授权不再成立"));
            }
        }
        List<String> affected = assignmentIds(domain, actor, groupId);
        ReferenceImpactPreview result = new ReferenceImpactPreview(affected,
                new ImpactSummary(null, (long) affected.size(), null, false));
        return new Preview<>(current.version(), errors.isEmpty(), errors, List.of(), result.impactSummary(), result);
    }

    private ResourceDetail<GroupRecord> load(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        if (domain == AuthorizationDomain.PLATFORM) {
            IamPlatformGroupEntity row = groups.findPlatform(id);
            if (row == null) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            return detail(domain, actor, row.getId().longValue(), row.getName(), row.getDescription(),
                    version(row.getVersion()));
        }
        IamTenantGroupEntity row = groups.findTenant(tenantId(actor), id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return detail(domain, actor, row.getId().longValue(), row.getName(), row.getDescription(),
                version(row.getVersion()));
    }

    private ResourceDetail<GroupRecord> lock(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        if (domain == AuthorizationDomain.PLATFORM) {
            IamPlatformGroupEntity row = groups.lockPlatform(id);
            if (row == null) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            return detail(domain, actor, row.getId().longValue(), row.getName(), row.getDescription(),
                    version(row.getVersion()));
        }
        IamTenantGroupEntity row = groups.lockTenant(tenantId(actor), id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return detail(domain, actor, row.getId().longValue(), row.getName(), row.getDescription(),
                version(row.getVersion()));
    }

    private ResourceDetail<GroupRecord> detail(AuthorizationDomain domain, ActiveIdentity actor, long id, String name,
                                               String description, String version) {
        return detail(domain, actor, id, name, description, version, visibility(domain, actor));
    }

    private Visibility visibility(AuthorizationDomain domain, ActiveIdentity actor) {
        Map<IamAction, ObjectScope> writes = new EnumMap<>(IamAction.class);
        for (AccessKind kind : List.of(AccessKind.UPDATE, AccessKind.DELETE, AccessKind.PREVIEW)) {
            IamAction operation = action(domain, kind);
            if (access.allows(domain, operation, false)) {
                writes.put(operation, scopes.objects(actor.context(), operation));
            }
        }
        IamAction memberRead = memberRead(domain);
        ObjectScope memberScope = access.allows(domain, memberRead, false)
                ? scopes.memberRead(actor.context(), memberRead) : null;
        ObjectScope departmentScope = domain == AuthorizationDomain.TENANT
                && access.allows(domain, IamAction.TENANT_DEPARTMENT_READ, false)
                ? scopes.departmentRead(actor.context(), IamAction.TENANT_DEPARTMENT_READ) : null;
        return new Visibility(writes, memberScope, departmentScope);
    }

    private ResourceDetail<GroupRecord> detail(AuthorizationDomain domain, ActiveIdentity actor, long id, String name,
                                               String description, String version, Visibility visibility) {
        Selection selection = new Selection(List.of(), List.of());
        Map<String, ObjectCapability> capabilities = new LinkedHashMap<>();
        for (AccessKind kind : List.of(AccessKind.UPDATE, AccessKind.DELETE, AccessKind.PREVIEW)) {
            IamAction operation = action(domain, kind);
            ObjectScope scope = visibility.writes().get(operation);
            boolean allowed = scope != null && ObjectScopeSql.matches(scope, id);
            var reason = allowed ? null : scope == null ? IamReasonCode.ACTION_DENIED : IamReasonCode.OBJECT_NOT_FOUND;
            capabilities.put(operation.getCode(), new ObjectCapability(allowed, reason, null));
        }
        boolean selectionEditable = false;
        Long count = null;
        if (visibility.members() != null) {
            Selection linked = selectionOf(domain, actor, id);
            var linkedIds = linked.members().stream().map(BigInteger::new).toList();
            var visibleIds = scopes.visibleMembers(actor.context(), visibility.members(), linkedIds);
            List<DepartmentSelection> departments = visibility.departments() == null ? List.of()
                    : linked.departments().stream()
                        .filter(department -> scopes.departmentVisible(actor.context(), visibility.departments(),
                                IamIds.require(department.id()))).toList();
            selection = new Selection(texts(visibleIds.stream().sorted().toList()), departments);
            selectionEditable = capabilities.get(action(domain, AccessKind.UPDATE).getCode()).allowed()
                    && visibleIds.containsAll(linkedIds) && departments.size() == linked.departments().size();
            count = domain == AuthorizationDomain.PLATFORM ? (long) visibleIds.size()
                    : memberQueries.countEffective(tenantId(actor), visibility.members(), id);
        }
        return ResourceDetailFactory.of(new GroupRecord(IamIds.text(id), name, description, selection, count, selectionEditable),
                capabilities, version);
    }

    private void requireWriteScope(AuthorizationDomain domain, ActiveIdentity actor, AccessKind kind, long id) {
        if (!ObjectScopeSql.matches(scopes.objectsForExecution(actor.context(), action(domain, kind)), id)) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
    }

    private void requireVisibleSelection(AuthorizationDomain domain, ActiveIdentity actor, Selection selection) {
        if (!selection.members().isEmpty()) {
            IamAction read = memberRead(domain);
            var ids = selection.members().stream().map(BigInteger::new).toList();
            if (!scopes.visibleMembers(actor.context(), scopes.memberReadForExecution(actor.context(), read), ids).containsAll(ids)) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
        }
        if (!selection.departments().isEmpty()) {
            var scope = scopes.departmentReadForExecution(actor.context(), IamAction.TENANT_DEPARTMENT_READ);
            for (DepartmentSelection department : selection.departments()) {
                if (!scopes.departmentVisible(actor.context(), scope, IamIds.require(department.id()))) {
                    throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
                }
            }
        }
    }

    private static IamAction memberRead(AuthorizationDomain domain) {
        return domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_MEMBER_READ : IamAction.TENANT_MEMBER_READ;
    }

    private Selection selectionOf(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        if (domain == AuthorizationDomain.PLATFORM) {
            return new Selection(texts(groups.platformMemberIds(id)), List.of());
        }
        long tenantId = tenantId(actor);
        List<DepartmentSelection> departments = groups.tenantDepartments(tenantId, id).stream()
                .map(row -> new DepartmentSelection(text(row.getDepartmentId()),
                        Boolean.TRUE.equals(row.getIncludeDescendants())))
                .toList();
        return new Selection(texts(groups.tenantMemberIds(tenantId, id)), departments);
    }

    private void insertGroup(AuthorizationDomain domain, ActiveIdentity actor, long id, GroupDraft input) {
        if (domain == AuthorizationDomain.PLATFORM) {
            IamPlatformGroupEntity entity = new IamPlatformGroupEntity();
            entity.setId(BigInteger.valueOf(id));
            entity.setName(input.name());
            entity.setDescription(input.description());
            groups.insertPlatform(entity);
            return;
        }
        IamTenantGroupEntity entity = new IamTenantGroupEntity();
        entity.setId(BigInteger.valueOf(id));
        entity.setTenantId(BigInteger.valueOf(tenantId(actor)));
        entity.setName(input.name());
        entity.setDescription(input.description());
        groups.insertTenant(entity);
    }

    private void updateGroup(AuthorizationDomain domain, ActiveIdentity actor, long id, GroupDraft input,
                             String currentVersion) {
        BigInteger version = new BigInteger(currentVersion);
        if (domain == AuthorizationDomain.PLATFORM) {
            groups.updatePlatform(id, input.name(), input.description(), version);
            return;
        }
        groups.updateTenant(tenantId(actor), id, input.name(), input.description(), version);
    }

    private void replaceSelection(AuthorizationDomain domain, ActiveIdentity actor, long id, Selection selection) {
        requireMembers(domain, actor, selection.members());
        requireDepartments(domain, actor, selection.departments());
        List<Long> memberIds = selection.members().stream().map(IamIds::require).toList();
        if (domain == AuthorizationDomain.PLATFORM) {
            groups.replacePlatformMembers(id, memberIds);
            return;
        }
        Map<Long, Boolean> departments = new TreeMap<>();
        for (DepartmentSelection department : selection.departments()) {
            if (departments.putIfAbsent(IamIds.require(department.id()), department.includeDescendants()) != null) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
        }
        groups.replaceTenantSelection(tenantId(actor), id, memberIds, departments);
    }

    private void clearSelection(AuthorizationDomain domain, ActiveIdentity actor, long id) {
        if (domain == AuthorizationDomain.PLATFORM) {
            groups.clearPlatformMembers(id);
            return;
        }
        groups.clearTenantSelection(tenantId(actor), id);
    }

    private void requireMembers(AuthorizationDomain domain, ActiveIdentity actor, List<String> members) {
        for (String memberId : members) {
            long id = IamIds.require(memberId);
            boolean exists = domain == AuthorizationDomain.PLATFORM
                    ? groups.existsActivePlatformMember(id)
                    : groups.existsActiveTenantMember(tenantId(actor), id);
            if (!exists) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
        }
    }

    private void requireDepartments(AuthorizationDomain domain, ActiveIdentity actor,
                                    List<DepartmentSelection> departments) {
        if (domain == AuthorizationDomain.PLATFORM && !departments.isEmpty()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        for (DepartmentSelection department : departments) {
            if (!groups.existsDepartment(tenantId(actor), IamIds.require(department.id()))) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
        }
    }

    private boolean referenced(AuthorizationDomain domain, ActiveIdentity actor, long groupId) {
        return domain == AuthorizationDomain.PLATFORM
                ? groups.countPlatformAssignments(groupId) > 0
                : groups.countTenantAssignments(tenantId(actor), groupId) > 0;
    }

    private List<String> assignmentIds(AuthorizationDomain domain, ActiveIdentity actor, long groupId) {
        return texts(domain == AuthorizationDomain.PLATFORM
                ? groups.activePlatformAssignmentIds(groupId)
                : groups.activeTenantAssignmentIds(tenantId(actor), groupId));
    }

    /**
     * 逐条重验组上的委派派生授权：待保存内容展开后的每个成员都必须仍在来源委派的接收范围内，
     * 组被扩大不能让名单外成员受益。
     */
    private List<String> invalidDelegatedAssignments(AuthorizationDomain domain, ActiveIdentity actor, long groupId,
                                                     Selection next) {
        List<IamRoleAssignmentEntity> delegated = domain == AuthorizationDomain.PLATFORM
                ? groups.delegatedPlatformAssignments(groupId)
                : groups.delegatedTenantAssignments(tenantId(actor), groupId);
        if (delegated.isEmpty()) {
            return List.of();
        }
        List<BigInteger> members = prospectiveMembers(domain, actor, next);
        Long tenantId = domain == AuthorizationDomain.PLATFORM ? null : tenantId(actor);
        List<String> invalid = new ArrayList<>();
        for (IamRoleAssignmentEntity row : delegated) {
            long delegationId = row.getDelegationGrantId().longValue();
            boolean covered = !members.isEmpty() && members.stream().allMatch(memberId ->
                    recipients.reaches(domain, tenantId, delegationId, memberId.longValue()));
            if (!covered) {
                invalid.add(text(row.getId()));
            }
        }
        return List.copyOf(invalid);
    }

    /**
     * 按待保存内容展开组的有效成员，与求值口径一致：显式成员并入部门任职成员，连带下级沿部门树展开。
     */
    private List<BigInteger> prospectiveMembers(AuthorizationDomain domain, ActiveIdentity actor, Selection next) {
        List<BigInteger> explicit = next.members().stream().map(id -> BigInteger.valueOf(IamIds.require(id))).toList();
        if (domain == AuthorizationDomain.PLATFORM || next.departments().isEmpty()) {
            return explicit.stream().distinct().toList();
        }
        BigInteger tenantId = BigInteger.valueOf(tenantId(actor));
        Set<BigInteger> reached = new LinkedHashSet<>();
        for (DepartmentSelection department : next.departments()) {
            reached.addAll(closure.expand(tenantId, List.of(BigInteger.valueOf(IamIds.require(department.id()))),
                    department.includeDescendants()));
        }
        Set<BigInteger> members = new LinkedHashSet<>(explicit);
        members.addAll(groups.membersInDepartments(tenantId(actor), reached));
        return List.copyOf(members);
    }

    private static long tenantId(ActiveIdentity actor) {
        return IamIds.require(actor.context().tenantId());
    }

    private static IamAction action(AuthorizationDomain domain, AccessKind kind) {
        return switch (kind) {
            case READ -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_GROUP_READ
                    : IamAction.TENANT_GROUP_READ;
            case CREATE -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_GROUP_CREATE
                    : IamAction.TENANT_GROUP_CREATE;
            case UPDATE -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_GROUP_UPDATE
                    : IamAction.TENANT_GROUP_UPDATE;
            case DELETE -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_GROUP_DELETE
                    : IamAction.TENANT_GROUP_DELETE;
            case PREVIEW -> domain == AuthorizationDomain.PLATFORM ? IamAction.PLATFORM_GROUP_PREVIEW
                    : IamAction.TENANT_GROUP_PREVIEW;
        };
    }

    private static String version(BigInteger value) {
        return value == null ? "0" : value.toString();
    }

    private static String text(BigInteger id) {
        return id == null ? null : IamIds.text(id.longValue());
    }

    private static List<String> texts(List<BigInteger> ids) {
        return ids.stream().map(GroupService::text).toList();
    }

    /** 同一请求复用的组操作和关联数据可见范围。 */
    private record Visibility(Map<IamAction, ObjectScope> writes, ObjectScope members, ObjectScope departments) { }

    private enum AccessKind {
        READ, CREATE, UPDATE, DELETE, PREVIEW
    }
}
