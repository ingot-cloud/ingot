package com.ingot.cloud.iam.role;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.TenantCurrentRoleRepository;
import com.ingot.cloud.iam.persistence.mapper.TenantLiveMapper;
import com.ingot.cloud.iam.persistence.projection.TenantLiveRows;
import com.ingot.cloud.iam.support.*;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * <p>管理共享与私有角色的完整当前权限，不改变平台自有角色固定版本流程。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantCurrentRoleService {
    private static final String ROLE_TARGET = "role";
    private static final String DEFINITION_FIELD = "definition";
    private static final TypeReference<List<ScopeKind>> SCOPE_KINDS = new TypeReference<>() { };
    private final IamAccess access;
    private final com.ingot.cloud.iam.evaluation.ResourceAccess resources;
    private final TenantLiveMapper live;
    private final TenantCurrentRoleRepository roles;
    private final RoleGrantValidator validator;
    private final TenantLiveTransaction transactions;
    private final IamAuditWriter audits;
    private final AuthorizationChangeNotifier changes;

    /** 分页列出当前租户共享和私有角色，平台入口仅列共享。 */
    public PageResponse<ResourceDetail<CurrentRoleSummary>> list(boolean shared, int page, int pageSize,
                                                                String name, String status, RoleKind kind) {
        var actor = access.require(domain(shared), read(shared));
        IamPages.require(page, pageSize);
        if (kind != null && kind != RoleKind.SHARED && kind != RoleKind.TENANT_CUSTOM) invalid();
        var tenant = tenant(actor, shared);
        String search = name == null || name.isBlank() ? null : "%" + escapeSearch(name.trim()) + "%";
        Boolean enabled = IamFilters.enabledOf(status);
        var scope=resources.objects(actor.context(),read(shared));
        var visible=scope.coversAll()?null:scope.clauses().stream().flatMap(c->c.requiredIds().stream()).distinct().toList();
        var items = live.roles(tenant, shared, kind, search, enabled, pageSize, IamPages.offset(page, pageSize),visible)
                .stream().map(row -> detail(actor, shared, row)).toList();
        return IamPages.details(items, live.roleCount(tenant, shared, kind, search, enabled,visible), page, pageSize);
    }

    /** 读取可见角色基本信息和当前写资格。 */
    public ResourceDetail<CurrentRoleSummary> get(boolean shared, String id) {
        var actor = access.require(domain(shared), read(shared));
        resources.requireVisibleObject(actor.context(),read(shared),IamIds.require(id));
        return detail(actor, shared, require(actor, shared, id));
    }

    /** 读取完整当前权限及其操作资源目录。 */
    public CurrentRoleGrantList grants(boolean shared, String id) {
        var actor = access.require(domain(shared), read(shared));
        resources.requireVisibleObject(actor.context(),read(shared),IamIds.require(id));
        return grants(require(actor, shared, id));
    }

    /** 为已校验受限分配流程读取当前定义，不授予通用角色读取能力。 */
    public CurrentRoleGrantList grantsForAssignment(ActiveIdentity actor, String id) {
        return grants(require(actor, false, id));
    }

    /** 预览创建草稿，不分配角色或快照标识。 */
    public Preview<CurrentRolePreviewResult> previewCreate(boolean shared, CurrentRoleCreateInput input) {
        var actor = access.requireGoverned(domain(shared), createAction(shared));
        if(!resources.objects(actor.context(),createAction(shared)).coversAll())throw new BizException(IamReasonCode.ACTION_DENIED);
        var preview=previewResult(actor, shared, null, input.definition());
        if(live.roleCodeExists(shared?null:tenant(actor,false),input.code().trim())>0)
            return new Preview<>(preview.version(),false,List.of(issue("角色编码已存在")),preview.warnings(),preview.impactSummary(),preview.effectiveResult());
        return preview;
    }

    /** 原子建立角色身份与首个完整当前权限快照。 */
    public CreatedResource create(boolean shared, CurrentRoleCreateInput input) {
        var actor = access.requireGoverned(domain(shared), createAction(shared));
        if(!resources.objects(actor.context(),createAction(shared)).coversAll())throw new BizException(IamReasonCode.ACTION_DENIED);
        return transactions.execute(tx -> {
            lockActor(actor, shared, createAction(shared));
            if(live.roleCodeExists(shared?null:tenant(actor,false),input.code().trim())>0)invalid();
            requireValid(validate(actor, shared, null, input.definition()));
            long id = access.nextId();
            try {
                roles.insert(id, shared ? null : IamIds.require(actor.context().tenantId()),
                        shared ? RoleKind.SHARED : RoleKind.TENANT_CUSTOM, input);
                var row = live.lockRole(BigInteger.valueOf(id));
                if (!roles.save(row, access.nextId(), input.definition())) conflict();
            } catch (DataIntegrityViolationException failure) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT, "角色编码已存在或定义引用不合法");
            }
            var row = live.lockRole(BigInteger.valueOf(id));
            audits.write(actor.context(), access.nextId(), ROLE_TARGET, Long.toString(id), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, input.name()), Map.of(ROLE_TARGET, row.version().toString()));
            changes.markAll();
            return new CreatedResource(Long.toString(id), row.version().toString());
        });
    }

    /** 预览当前完整权限变化及待补范围，不更改分配引用。 */
    public Preview<CurrentRolePreviewResult> preview(boolean shared, String id, CurrentRolePermissionInput input) {
        var actor = access.requireGoverned(domain(shared), previewAction(shared));
        resources.requireVisibleObject(actor.context(),previewAction(shared),IamIds.require(id));
        var row = writable(actor, shared, id, false);
        IamIds.requireVersion(input.expectedVersion(), row.version().toString());
        return previewResult(actor, shared, row, input.definition());
    }

    /** 保存完整当前权限，所有角色身份分配在后续请求自动跟随。 */
    public CurrentRolePermissionResult save(boolean shared, String id, CurrentRolePermissionInput input) {
        var actor = access.requireGoverned(domain(shared), updateAction(shared));
        resources.requireVisibleObject(actor.context(),updateAction(shared),IamIds.require(id));
        return transactions.execute(tx -> {
            lockActor(actor, shared, updateAction(shared));
            var row = writable(actor, shared, id, true);
            IamIds.requireVersion(input.expectedVersion(), row.version().toString());
            requireValid(validate(actor, shared, row, input.definition()));
            long snapshot = access.nextId();
            if (!roles.save(row, snapshot, input.definition())) conflict();
            String next = row.version().add(BigInteger.ONE).toString();
            audits.write(actor.context(), access.nextId(), ROLE_TARGET, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.ROLE_REVISION, row.currentRevisionId().toString()),
                    Map.of(AuditField.ROLE_REVISION, Long.toString(snapshot)), Map.of(ROLE_TARGET, next));
            changes.markAll();
            return new CurrentRolePermissionResult(id, next, null);
        });
    }

    /** 更新本地资料或平台共享资料，不能通过租户入口停用共享。 */
    public ResourceDetail<CurrentRoleSummary> profile(boolean shared, String id, CurrentRoleProfileInput input) {
        var actor = access.requireGoverned(domain(shared), statusAction(shared));
        resources.requireVisibleObject(actor.context(),statusAction(shared),IamIds.require(id));
        return transactions.execute(tx -> {
            lockActor(actor, shared, statusAction(shared));
            var row = writable(actor, shared, id, true);
            IamIds.requireVersion(input.expectedVersion(), row.version().toString());
            if (input.name() != null && input.name().isBlank()) invalid();
            if (live.profile(row.id(), row.version(), input.name() == null ? row.name() : input.name().trim(),
                    input.description() == null ? row.description() : input.description(),
                    input.groupName() == null ? row.groupName() : input.groupName(), input.status() == ConfigurationStatus.ENABLED) != 1) conflict();
            var next = live.lockRole(row.id());
            audits.write(actor.context(), access.nextId(), ROLE_TARGET, id, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, row.name()), Map.of(AuditField.NAME, next.name()), Map.of(ROLE_TARGET, next.version().toString()));
            changes.markAll();
            return detail(actor, shared, next);
        });
    }

    /** 删除无实际授权引用角色，数据库历史基础引用也必须保留。 */
    public CreatedResource delete(boolean shared, String id) {
        var actor = access.requireGoverned(domain(shared), deleteAction(shared));
        resources.requireVisibleObject(actor.context(),deleteAction(shared),IamIds.require(id));
        return transactions.execute(tx -> {
            lockActor(actor, shared, deleteAction(shared));
            var row = writable(actor, shared, id, true);
            if (live.roleAssignments(row.id()) > 0 || live.roleDelegations(row.id()) > 0)
                throw new BizException(IamReasonCode.OBJECT_IN_USE);
            try { roles.delete(row); }
            catch (DataIntegrityViolationException failure) { throw new BizException(IamReasonCode.OBJECT_IN_USE); }
            audits.write(actor.context(), access.nextId(), ROLE_TARGET, id, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, row.name()), Map.of(), Map.of(ROLE_TARGET, row.version().toString()));
            changes.markAll();
            return new CreatedResource(id, row.version().toString());
        });
    }

    /** 返回本组织可编辑的已开通操作分页目录。 */
    public PageResponse<CurrentRoleCatalogAction> catalog(String applicationId, String keyword, List<String> ids,
                                                         int page, int pageSize) {
        var actor = access.requireCurrent();
        if (actor.context().domain() != AuthorizationDomain.TENANT ||
                !access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_ROLE_CREATE, true)
                && !access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_ROLE_UPDATE, true)
                && !access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_ROLE_PREVIEW, true))
            throw new BizException(IamReasonCode.ACTION_DENIED);
        IamPages.require(page, pageSize);
        List<BigInteger> selected = ids == null ? List.of() : ids.stream().map(IamIds::require).map(BigInteger::valueOf).distinct().toList();
        String search = keyword == null || keyword.isBlank() ? null : "%" + escapeSearch(keyword.trim()) + "%";
        var tenant = new BigInteger(actor.context().tenantId());
        var app = BigInteger.valueOf(IamIds.require(applicationId));
        return new PageResponse<>(live.catalog(tenant, app, search, selected, pageSize, IamPages.offset(page, pageSize))
                .stream().map(row -> action(row, List.of())).toList(), live.catalogCount(tenant, app, search, selected), page, pageSize);
    }

    private CurrentRoleGrantList grants(TenantLiveRows.Role row) {
        var definition = roles.definition(row);
        var ids = definition.grants().stream().map(ActionGrant::actionId).map(BigInteger::new).toList();
        return new CurrentRoleGrantList(ids.isEmpty() ? List.of() : live.actions(ids).stream()
                .map(value -> action(value, definition.parameterDefinitions())).toList(), definition);
    }

    private Preview<CurrentRolePreviewResult> previewResult(ActiveIdentity actor, boolean shared, TenantLiveRows.Role row,
                                                           CurrentRoleDefinition next) {
        var errors = validate(actor, shared, row, next);
        var before = row == null ? new CurrentRoleDefinition(List.of(), List.of()) : roles.definition(row);
        var old = before.grants().stream().collect(Collectors.toMap(ActionGrant::actionId, Function.identity()));
        var current = next.grants().stream().collect(Collectors.toMap(ActionGrant::actionId, Function.identity(), (left, right) -> left));
        var added = current.keySet().stream().filter(id -> !old.containsKey(id)).toList();
        var removed = old.keySet().stream().filter(id -> !current.containsKey(id)).toList();
        var changed = current.keySet().stream().filter(id -> old.containsKey(id) && !old.get(id).scopes().equals(current.get(id).scopes())).toList();
        var warnings = new ArrayList<ValidationIssue>();
        if (row != null && next.parameterDefinitions().stream().anyMatch(p -> before.parameterDefinitions().stream().noneMatch(oldParameter -> oldParameter.equals(p))))
            warnings.add(new ValidationIssue(DEFINITION_FIELD, IamReasonCode.DATA_SCOPE_DENIED, "新增资源参数未补齐的分配仅关闭对应操作，请通过普通分配编辑补齐"));
        // 无治理租户读取资格时不披露跨组织人数或授权清单。
        boolean restricted=shared || row!=null && (!access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_READ,true) || !resources.objects(actor.context(),IamAction.TENANT_ASSIGNMENT_READ).coversAll());
        return new Preview<>(row == null ? java.util.HexFormat.of().formatHex(hash(actionsDependency(next))) : row.version().toString(), errors.isEmpty(), errors, warnings,
                new ImpactSummary(null, restricted ? null : row == null ? 0L : live.roleAssignments(row.id()), null, restricted),
                new CurrentRolePreviewResult(next, added, removed, changed, null));
    }

    private String actionsDependency(CurrentRoleDefinition definition) {
        return definition.grants().isEmpty()?definition.toString():live.actions(definition.grants().stream().map(g->new BigInteger(g.actionId())).toList()).toString()+definition.parameterDefinitions();
    }
    private static byte[] hash(String value) {
        try {return java.security.MessageDigest.getInstance(TenantAuthorizationConstants.FINGERPRINT_ALGORITHM).digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        catch(java.security.NoSuchAlgorithmException failure){throw new IllegalStateException(failure);}
    }

    private List<ValidationIssue> validate(ActiveIdentity actor, boolean shared, TenantLiveRows.Role row, CurrentRoleDefinition definition) {
        if (definition.grants().stream().map(ActionGrant::actionId).distinct().count() != definition.grants().size()
                || definition.parameterDefinitions().stream().map(CurrentRoleParameter::key).distinct().count() != definition.parameterDefinitions().size())
            return List.of(issue("操作或资源参数不能重复"));
        var selected = definition.grants().stream().map(ActionGrant::actionId).map(IamIds::require).map(BigInteger::valueOf).toList();
        Map<String, TenantLiveRows.Action> actions = selected.isEmpty() ? Map.of() : live.actions(selected).stream()
                .collect(Collectors.toMap(value -> value.actionId().toString(), Function.identity()));
        var errors = new ArrayList<ValidationIssue>();
        var previous = row == null ? new CurrentRoleDefinition(List.of(), List.of()) : roles.definition(row);
        Map<String, ActionGrant> old = previous.grants().stream().collect(Collectors.toMap(ActionGrant::actionId, Function.identity()));
        var opened = shared ? Set.<BigInteger>of() : live.applications(tenant(actor, false)).stream().map(TenantLiveRows.Application::id).collect(Collectors.toSet());
        var enabled = new ArrayList<ActionGrant>();
        for (var grant : definition.grants()) {
            var action = actions.get(grant.actionId());
            if (action == null) { errors.add(issue("操作不存在或不属于租户域")); continue; }
            boolean unchanged = grant.equals(old.get(grant.actionId()));
            if (!Boolean.TRUE.equals(action.enabled())) {
                if (!unchanged) errors.add(issue("停用操作不能新选或修改范围"));
            } else enabled.add(grant);
            if (!shared && !opened.contains(action.applicationId()) && !unchanged) errors.add(issue("操作所属应用尚未有效开通"));
            for (var scope : grant.scopes()) if (scope.parameterKey() != null) {
                var parameter = definition.parameterDefinitions().stream().filter(p -> p.key().equals(scope.parameterKey())).findFirst();
                if (parameter.isEmpty() || !parameter.get().resourceId().equals(action.resourceId().toString())) errors.add(issue("参数必须属于操作实际资源"));
            }
        }
        var historic=row==null?List.<TenantLiveRows.Parameter>of():live.historicParameters(row.id());
        for (var parameter : definition.parameterDefinitions()) {
            if(historic.stream().anyMatch(p->p.key().equals(parameter.key()) && (p.kind()!=parameter.kind() || p.resourceId()!=null && !p.resourceId().toString().equals(parameter.resourceId()))))errors.add(issue("历史参数键不得更换资源或类型，请使用新键"));
            var oldParameter = previous.parameterDefinitions().stream().filter(p -> p.key().equals(parameter.key())).findFirst();
            if (oldParameter.isPresent() && !oldParameter.get().equals(parameter)) errors.add(issue("已有参数键不得更换资源或类型，请使用新键"));
            if (definition.grants().stream().noneMatch(g -> g.scopes().stream().anyMatch(s -> parameter.key().equals(s.parameterKey())))) errors.add(issue("不能声明未使用的范围参数"));
        }
        errors.addAll(validator.validate(AuthorizationDomain.TENANT, enabled, definition.parameterDefinitions().stream()
                .map(p -> new RoleParameterDefinition(p.key(), p.kind())).toList()));
        return List.copyOf(errors);
    }

    private ResourceDetail<CurrentRoleSummary> detail(ActiveIdentity actor, boolean shared, TenantLiveRows.Role row) {
        var permissions = new LinkedHashMap<String, ObjectCapability>();
        var operations=List.of(read(shared),updateAction(shared),statusAction(shared),deleteAction(shared),previewAction(shared));
        var granted=access.capabilities(actor,operations);
        for(var operation:operations) {
            boolean allowed=(operation==read(shared)||shared||row.kind()==RoleKind.TENANT_CUSTOM) && granted.allows(operation,operation!=read(shared));
            if(allowed)allowed=com.ingot.cloud.iam.persistence.ObjectScopeSql.matches(resources.objects(actor.context(),operation),row.id().longValueExact());
            permissions.put(operation.getCode(),new ObjectCapability(allowed,allowed?null:IamReasonCode.ACTION_DENIED,allowed?null:"当前角色不可执行该操作"));
        }
        return IamDetails.of(summary(row), permissions, row.version().toString());
    }

    /** 转换当前角色展示模型。 */
    public static CurrentRoleSummary summary(TenantLiveRows.Role row) {
        return new CurrentRoleSummary(row.id().toString(), row.code(), row.name(), row.description(), row.groupName(), row.kind(),
                Boolean.TRUE.equals(row.enabled()) ? ConfigurationStatus.ENABLED : ConfigurationStatus.DISABLED);
    }

    private CurrentRoleCatalogAction action(TenantLiveRows.Action row, List<CurrentRoleParameter> parameters) {
        List<ScopeKind> kinds = IamJson.read(row.scopeCapabilities(), SCOPE_KINDS);
        var options = new ArrayList<>(parameters.stream().filter(p -> p.resourceId().equals(row.resourceId().toString())).toList());
        for (var kind : List.of(ScopeBindingKind.DEPARTMENTS, ScopeBindingKind.OBJECTS)) {
            var scopeKind = kind == ScopeBindingKind.DEPARTMENTS ? ScopeKind.MANAGED_DEPARTMENTS : ScopeKind.OBJECT_SET;
            if (kinds.contains(scopeKind) && options.stream().noneMatch(p -> p.kind() == kind))
                options.add(new CurrentRoleParameter(TenantAuthorizationConstants.resourceParameterKey(row.resourceId().toString(),kind), kind, row.resourceId().toString()));
        }
        return new CurrentRoleCatalogAction(row.actionId().toString(), row.actionCode(), row.actionName(), row.applicationId().toString(),
                row.applicationName(), row.resourceId().toString(), row.resourceName(), Boolean.TRUE.equals(row.enabled()), kinds, options);
    }

    private TenantLiveRows.Role require(ActiveIdentity actor, boolean shared, String id) {
        var row = live.role(tenant(actor, shared), BigInteger.valueOf(IamIds.require(id)));
        if (row == null || shared && row.kind() != RoleKind.SHARED) throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        return row;
    }

    private TenantLiveRows.Role writable(ActiveIdentity actor, boolean shared, String id, boolean lock) {
        var row = require(actor, shared, id);
        if (!shared && row.kind() != RoleKind.TENANT_CUSTOM) throw new BizException(IamReasonCode.ACTION_DENIED);
        return lock ? live.lockRole(row.id()) : row;
    }

    private void lockActor(ActiveIdentity actor, boolean shared, IamAction operation) {
        if (shared) access.lockPlatformAuthorization();
        else if (live.lockTenant(tenant(actor, false)) == null) throw new BizException(IamReasonCode.IDENTITY_INVALID);
        access.requireGoverned(domain(shared), operation);
    }

    private static BigInteger tenant(ActiveIdentity actor, boolean shared) { return shared ? BigInteger.ZERO : new BigInteger(actor.context().tenantId()); }
    private static AuthorizationDomain domain(boolean shared) { return shared ? AuthorizationDomain.PLATFORM : AuthorizationDomain.TENANT; }
    private static IamAction read(boolean shared) { return shared ? IamAction.PLATFORM_SHARED_ROLE_READ : IamAction.TENANT_ROLE_READ; }
    private static IamAction createAction(boolean shared) { return shared ? IamAction.PLATFORM_SHARED_ROLE_CREATE : IamAction.TENANT_ROLE_CREATE; }
    private static IamAction updateAction(boolean shared) { return shared ? IamAction.PLATFORM_SHARED_ROLE_UPDATE : IamAction.TENANT_ROLE_UPDATE; }
    private static IamAction statusAction(boolean shared) { return shared ? IamAction.PLATFORM_SHARED_ROLE_STATUS : IamAction.TENANT_ROLE_STATUS; }
    private static IamAction deleteAction(boolean shared) { return shared ? IamAction.PLATFORM_SHARED_ROLE_DELETE : IamAction.TENANT_ROLE_DELETE; }
    private static IamAction previewAction(boolean shared) { return shared ? IamAction.PLATFORM_SHARED_ROLE_PREVIEW : IamAction.TENANT_ROLE_PREVIEW; }
    private static ValidationIssue issue(String message) { return new ValidationIssue(DEFINITION_FIELD, IamReasonCode.INVALID_ARGUMENT, message); }
    private static void requireValid(List<ValidationIssue> errors) { if (!errors.isEmpty()) throw new BizException(errors.getFirst().code(), errors.getFirst().message()); }
    private static void invalid() { throw new BizException(IamReasonCode.INVALID_ARGUMENT); }
    private static void conflict() { throw new BizException(IamReasonCode.REVISION_CONFLICT); }
    private static String escapeSearch(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

}
