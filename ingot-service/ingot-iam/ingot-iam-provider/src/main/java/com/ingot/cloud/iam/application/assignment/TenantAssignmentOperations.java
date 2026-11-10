package com.ingot.cloud.iam.application.assignment;

import com.ingot.cloud.iam.domain.shared.IamIds;

import com.ingot.cloud.iam.application.authorization.scope.TenantScopeBindingService;
import com.ingot.cloud.iam.application.role.TenantRoleLockService;
import com.ingot.cloud.iam.application.role.TenantRoleDefinitionResolver;
import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.application.shared.ResourceDetailFactory;
import com.ingot.cloud.iam.application.shared.TenantAuthorizationTransaction;
import com.ingot.cloud.iam.infrastructure.persistence.ObjectScopeSql;
import com.ingot.cloud.iam.infrastructure.persistence.TenantAuthorizationOptionSql;
import com.ingot.cloud.iam.infrastructure.persistence.TenantRoleDefinitionRepository;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantAdministrationMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantRoleWorkspaceMapper;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.cloud.iam.application.assignment.model.SubjectRef;
import com.ingot.cloud.iam.application.assignment.model.TenantAssignmentBatchInput;
import com.ingot.cloud.iam.application.assignment.model.TenantAssignmentInput;
import com.ingot.cloud.iam.application.assignment.model.TenantAssignmentRecord;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import com.ingot.cloud.iam.application.role.model.ScopeIssue;
import com.ingot.cloud.iam.application.shared.model.ObjectCapability;
import com.ingot.cloud.iam.domain.assignment.AssignmentEffectiveStatus;
import com.ingot.cloud.iam.domain.authorization.ObjectScope;
import com.ingot.cloud.iam.domain.assignment.GrantStatus;
import com.ingot.cloud.iam.domain.assignment.SubjectType;
import com.ingot.cloud.iam.domain.assignment.TenantAssignmentOptionKind;
import com.ingot.cloud.iam.domain.role.CurrentRoleDefinition;
import com.ingot.cloud.iam.domain.role.CurrentRoleParameter;
import com.ingot.cloud.iam.domain.role.ScopeIssueReason;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.application.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.projection.TenantAdministrationRows;
import com.ingot.cloud.iam.application.role.TenantRoleDefinitionService;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>提供assignment用例共享的持久化装配与内部规则，事务边界由各用例保持。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantAssignmentOperations {

    private final TenantRoleDefinitionResolver roleDefinitions;
    private final TenantRoleLockService roleLocks;
    private final TenantScopeBindingService scopeBindings;

    static final String TARGET = "assignment";
    static final String SCOPE_FIELD = "scopeBindings";
    static final TypeReference<Map<String,ScopeBinding>> BINDINGS = new TypeReference<>() { };
    static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() { };
    static final TypeReference<List<String>> SOURCE_TYPES = new TypeReference<>() { };
    private final IamAccess access;
    private final TenantAdministrationMapper live;
    private final TenantRoleWorkspaceMapper workspace;
    private final TenantRoleDefinitionRepository roles;
    private final TenantRoleDefinitionService roleService;
    private final ResourceScopeAccessService resources;
    private final TenantAuthorizationTransaction transactions;
    private final AuthorizationAuditWriter audits;
    private final AuthorizationChangeNotifier changes;

    private final com.ingot.cloud.iam.application.organization.OrganizationMemberDepartments memberDepartments;
    TenantRoleDefinitionResolver roleDefinitions() { return roleDefinitions; }

    TenantRoleLockService roleLocks() { return roleLocks; }

    TenantScopeBindingService scopeBindings() { return scopeBindings; }

    IamAccess access() { return access; }

    TenantAdministrationMapper live() { return live; }

    TenantRoleWorkspaceMapper workspace() { return workspace; }

    TenantRoleDefinitionRepository roles() { return roles; }

    TenantRoleDefinitionService roleService() { return roleService; }

    ResourceScopeAccessService resources() { return resources; }

    TenantAuthorizationTransaction transactions() { return transactions; }

    AuthorizationAuditWriter audits() { return audits; }

    AuthorizationChangeNotifier changes() { return changes; }

    com.ingot.cloud.iam.application.organization.OrganizationMemberDepartments memberDepartments() { return memberDepartments; }

    /** 校验当前完整绑定及持续委派约束，绝不自动扩大参数。 */
    void validate(ActiveIdentity actor,TenantAssignmentInput input,IamAction action) {
        var role=role(actor,input.roleId()); if(!Boolean.TRUE.equals(role.enabled()))throw new BizException(IamReasonCode.ROLE_UNAVAILABLE); var definition=activeDefinition(actor,role);
        var grant=source(actor,input.delegationGrantId(),action);
        if(grant==null&&action==IamAction.TENANT_ASSIGNMENT_CREATE&&!resources.objects(actor.context(),action).coversAll())denied();
        if (grant != null && !live.delegatedRoles(grant.id()).contains(role.id())) {
            throw exceeded("所选角色不在当前委派允许的范围内，请重新选择角色");
        }
        boolean memberSubject = input.subject().type() == SubjectType.MEMBER;
        String subjectType = memberSubject ? "成员" : "用户组";
        BigInteger subjectId;
        try {
            subjectId = BigInteger.valueOf(IamIds.require(input.subject().id()));
        } catch (BizException exception) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), "接收" + subjectType + "标识无效，请重新选择接收对象");
        }
        boolean subjectExists = memberSubject ? live.members(tenant(actor), List.of(subjectId)) == 1
                : live.groupExists(tenant(actor), subjectId) == 1;
        if (!subjectExists) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND.getCode(), "接收" + subjectType + "不存在或不可访问，请重新选择接收对象");
        }
        validateBindings(actor,definition.parameterDefinitions(),input.scopeBindings());
        if(grant!=null){
            if (memberSubject
                    ? subjectId.equals(member(actor)) || live.recipient(tenant(actor), grant.id(), subjectId) == 0
                    : !live.groupWithinSource(new TenantAuthorizationOptionSql.Query(tenant(actor), grant.administratorMemberId(),
                            grant.id(), TenantAssignmentOptionKind.GROUP, "%", List.of(), 1, 0), subjectId)) {
                throw exceeded("接收" + subjectType + "不在当前委派允许的范围内，请重新选择接收对象");
            }
            Instant from=input.validFrom()==null?Instant.now():input.validFrom(),until=input.validUntil();
            if(until==null||!until.isAfter(from)||Duration.between(from,until).compareTo(Duration.ofSeconds(grant.durationSeconds().longValueExact(),grant.durationNanos()))>0
                ||grant.validFrom()!=null&&from.isBefore(instant(grant.validFrom()))||grant.validUntil()!=null&&until.isAfter(instant(grant.validUntil()))) {
                throw exceeded("分配的生效时间或截止时间超出当前委派允许的期限");
            }
            var limits=live.ceilings(grant.id());var actions=definition.grants().isEmpty()?List.<TenantAdministrationRows.Action>of():live.actions(definition.grants().stream().map(g->new BigInteger(g.actionId())).toList());
            for(var g:definition.grants()){var a=actions.stream().filter(v->v.actionId().toString().equals(g.actionId())).findFirst().orElseThrow(()->new BizException(IamReasonCode.INVALID_ARGUMENT));
                var limit=limits.stream().filter(c->c.resourceId().equals(a.resourceId())).findFirst().orElse(null);if(limit==null)exceeded();
                // 固定角色ALL/SELF声明由资源上限持续收缩；仅拒绝操作者显式配置的越界绑定。
                for(var expression:g.scopes()) {
                    if(expression.parameterKey()==null)continue;
                    var parameter=definition.parameterDefinitions().stream().filter(p->p.key().equals(expression.parameterKey())).findFirst().orElseThrow();
                    var allowed=scopeBindings.allowedBindings(actor,parameter,grant);
                    if(allowed!=null && input.scopeBindings().get(parameter.key()).ids().stream().map(BigInteger::new).anyMatch(id->!allowed.contains(id)))exceeded();
                }}
        }
    }

    /** 按资源真实归属校验全部命名绑定。 */
    void validateBindings(ActiveIdentity actor,List<CurrentRoleParameter> parameters,Map<String,ScopeBinding> bindings) {
        scopeBindings.validateBindings(actor, parameters, bindings);
    }

    /** 本人有效来源；独立资格可不选来源。 */
    TenantAdministrationRows.Delegation source(ActiveIdentity actor,String id,IamAction action) {
        boolean governed=action==null?(access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_CREATE,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_UPDATE,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ASSIGNMENT_READ,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_CREATE,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_DELEGATION_UPDATE,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ROLE_CREATE,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ROLE_UPDATE,true)||access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_ROLE_PREVIEW,true))
            :access.allows(AuthorizationDomain.TENANT,action,true);
        if(id==null||id.isBlank()){if(!governed)denied();return null;}
        var source=live.mine(tenant(actor),member(actor)).stream().filter(d->d.id().toString().equals(id)).findFirst().orElseThrow(()->new BizException(IamReasonCode.DELEGATION_EXCEEDED));
        return source;
    }

    /** 计算当前定义待补参数或资源上限，旧有效资源持续可用。 */
    List<ScopeIssue> issues(ActiveIdentity actor,TenantAdministrationRows.Role role,Map<String,ScopeBinding> bindings,BigInteger source) {
        var definition = activeDefinition(actor, role);
        if (source == null || definition.grants().isEmpty()) {
            return issues(definition, bindings, null, Map.of());
        }
        var ceilings = live.ceilings(source).stream().map(TenantAdministrationRows.Ceiling::resourceId)
                .collect(java.util.stream.Collectors.toSet());
        var actionIds = definition.grants().stream().map(grant -> new BigInteger(grant.actionId())).toList();
        Map<String, BigInteger> actionResources = new LinkedHashMap<>();
        for (var action : live.actions(actionIds)) {
            actionResources.put(action.actionId().toString(), action.resourceId());
        }
        return issues(definition, bindings, ceilings, actionResources);
    }

    private static List<ScopeIssue> issues(CurrentRoleDefinition definition, Map<String, ScopeBinding> bindings,
            Set<BigInteger> ceilings, Map<String, BigInteger> actionResources) {
        List<ScopeIssue> issues = new ArrayList<>();
        for (var parameter : definition.parameterDefinitions()) {
            var binding = bindings.get(parameter.key());
            if (binding == null || binding.kind() != parameter.kind()) {
                var actions = definition.grants().stream().filter(grant -> grant.scopes().stream()
                        .anyMatch(scope -> parameter.key().equals(scope.parameterKey()))).map(ActionGrant::actionId).toList();
                issues.add(new ScopeIssue(parameter.resourceId(), actions, parameter.key(),
                        binding == null ? ScopeIssueReason.MISSING_BINDING : ScopeIssueReason.INVALID_BINDING));
            }
        }
        if (ceilings != null) {
            definition.grants().stream().map(grant -> new BigInteger(grant.actionId())).distinct().sorted()
                    .map(BigInteger::toString).forEach(id -> {
                        var resource = actionResources.get(id);
                        if (resource != null && !ceilings.contains(resource)) {
                            issues.add(new ScopeIssue(resource.toString(), List.of(id), null,
                                    ScopeIssueReason.MISSING_RESOURCE_CEILING));
                        }
                    });
        }
        return List.copyOf(issues);
    }

    /** 分配仅配置本组织当前可执行资源；未开通或停用资源不制造不可见必填参数。 */
    CurrentRoleDefinition activeDefinition(ActiveIdentity actor,TenantAdministrationRows.Role role) {
        return roleDefinitions.activeDefinition(actor, role);
    }

    private static final List<IamAction> WRITE_ACTIONS = List.of(
            IamAction.TENANT_ASSIGNMENT_UPDATE, IamAction.TENANT_ASSIGNMENT_DELETE);

    ResourceDetail<TenantAssignmentRecord> detail(ActiveIdentity actor, TenantAdministrationRows.Assignment row) {
        var role = role(actor, row.roleId().toString());
        var state = state(row, LocalDateTime.now(ZoneOffset.UTC));
        if (needsSourceCheck(state) && (!Boolean.TRUE.equals(role.enabled())
                || row.delegationGrantId() != null && sourceInvalid(actor, row))) {
            state = AssignmentEffectiveStatus.SOURCE_INVALID;
        }
        var input = input(row);
        return project(row, input, state, issues(actor, role, input.scopeBindings(), row.delegationGrantId()),
                readAccess(actor, null));
    }

    /** 在已通过范围过滤的分页结果内批量加载展示所需的当前事实。 */
    List<ResourceDetail<TenantAssignmentRecord>> details(ActiveIdentity actor,
            List<TenantAdministrationRows.Assignment> rows, List<BigInteger> ownSources) {
        if (rows.isEmpty()) return List.of();
        var tenant = tenant(actor);
        var roleIds = rows.stream().map(TenantAdministrationRows.Assignment::roleId).distinct().toList();
        var heads = live.roleHeads(tenant, roleIds);
        if (heads.size() != roleIds.size()) missing();
        var enabled = heads.stream().filter(role -> Boolean.TRUE.equals(role.enabled()))
                .map(TenantAdministrationRows.Role::id).collect(java.util.stream.Collectors.toSet());
        var active = roleDefinitions.activeDefinitions(actor, heads);
        var now = LocalDateTime.now(ZoneOffset.UTC);
        var delegated = rows.stream().filter(row -> row.delegationGrantId() != null
                        && enabled.contains(row.roleId()) && needsSourceCheck(state(row, now)))
                .map(TenantAdministrationRows.Assignment::id).toList();
        Map<BigInteger, BigInteger> eligible = delegated.isEmpty() ? Map.of()
                : live.eligibleDelegatedAssignments(tenant, delegated, now).stream().collect(
                        java.util.stream.Collectors.toMap(TenantAdministrationMapper.DelegatedAssignmentVersion::id,
                                TenantAdministrationMapper.DelegatedAssignmentVersion::version));
        var sourceIds = rows.stream().filter(row -> row.delegationGrantId() != null
                        && !active.definitions().get(row.roleId()).grants().isEmpty())
                .map(TenantAdministrationRows.Assignment::delegationGrantId).distinct().toList();
        Map<BigInteger, Set<BigInteger>> ceilings = new HashMap<>();
        if (!sourceIds.isEmpty()) {
            for (var ceiling : live.assignmentCeilingResources(tenant, sourceIds)) {
                ceilings.computeIfAbsent(ceiling.delegationId(), ignored -> new HashSet<>()).add(ceiling.resourceId());
            }
        }
        var access = readAccess(actor, ownSources);
        return rows.stream().map(row -> {
            var state = state(row, now);
            if (needsSourceCheck(state) && (!enabled.contains(row.roleId())
                    || row.delegationGrantId() != null && !row.version().equals(eligible.get(row.id())))) {
                state = AssignmentEffectiveStatus.SOURCE_INVALID;
            }
            var input = input(row);
            var limits = row.delegationGrantId() == null ? null
                    : ceilings.getOrDefault(row.delegationGrantId(), Set.of());
            return project(row, input, state, issues(active.definitions().get(row.roleId()), input.scopeBindings(),
                    limits, active.actionResources()), access);
        }).toList();
    }

    private static TenantAssignmentInput input(TenantAdministrationRows.Assignment row) {
        var subjectId = row.subjectType() == SubjectType.MEMBER ? row.memberId() : row.groupId();
        var source = row.delegationGrantId();
        return new TenantAssignmentInput(new SubjectRef(row.subjectType(), subjectId.toString()),
                row.roleId().toString(), IamJsonCodec.read(row.scopeBindings(), BINDINGS),
                instant(row.validFrom()), instant(row.validUntil()), source == null ? null : source.toString());
    }

    private static AssignmentEffectiveStatus state(TenantAdministrationRows.Assignment row, LocalDateTime now) {
        if (row.status() == GrantStatus.REVOKED) return AssignmentEffectiveStatus.REVOKED;
        if (row.validFrom().isAfter(now)) return AssignmentEffectiveStatus.PENDING;
        if (row.validUntil() != null && !row.validUntil().isAfter(now)) return AssignmentEffectiveStatus.EXPIRED;
        return AssignmentEffectiveStatus.ACTIVE;
    }

    private static boolean needsSourceCheck(AssignmentEffectiveStatus state) {
        return state == AssignmentEffectiveStatus.ACTIVE || state == AssignmentEffectiveStatus.PENDING;
    }

    private DetailAccess readAccess(ActiveIdentity actor, List<BigInteger> knownSources) {
        var rights = access.capabilities(actor, WRITE_ACTIONS);
        Map<IamAction, ObjectScope> scopes = new EnumMap<>(IamAction.class);
        Set<IamAction> delegated = EnumSet.noneOf(IamAction.class);
        for (var operation : WRITE_ACTIONS) {
            if (!rights.allows(operation, false)) continue;
            if (rights.allows(operation, true)) scopes.put(operation, resources.objects(actor.context(), operation));
            else delegated.add(operation);
        }
        Set<BigInteger> ownSources = Set.of();
        if (!delegated.isEmpty()) {
            ownSources = knownSources != null ? Set.copyOf(knownSources) : live.mine(tenant(actor), member(actor))
                    .stream().map(TenantAdministrationRows.Delegation::id).collect(java.util.stream.Collectors.toSet());
        }
        return new DetailAccess(scopes, delegated, ownSources);
    }

    private record DetailAccess(Map<IamAction, ObjectScope> scopes, Set<IamAction> delegated,
                                Set<BigInteger> ownSources) { }

    private static ResourceDetail<TenantAssignmentRecord> project(TenantAdministrationRows.Assignment row,
            TenantAssignmentInput input, AssignmentEffectiveStatus state, List<ScopeIssue> issues, DetailAccess access) {
        var source = row.delegationGrantId();
        var record = new TenantAssignmentRecord(row.id().toString(), input, row.status(), row.source(),
                row.subjectName(), row.roleName(), row.roleKind(), source == null ? null : source.toString(),
                instant(row.createdAt()), null, state, issues);
        Map<String, ObjectCapability> capabilities = new LinkedHashMap<>();
        for (var operation : WRITE_ACTIONS) {
            var scope = access.scopes().get(operation);
            boolean allowed = scope != null ? ObjectScopeSql.matches(scope, row.id().longValueExact())
                    : access.delegated().contains(operation) && source != null && access.ownSources().contains(source);
            if (operation == IamAction.TENANT_ASSIGNMENT_UPDATE && row.status() != GrantStatus.ACTIVE) allowed = false;
            capabilities.put(operation.getCode(), new ObjectCapability(allowed, allowed ? null : IamReasonCode.ACTION_DENIED,
                    allowed ? null : "当前来源或对象范围不允许"));
        }
        return ResourceDetailFactory.of(record, capabilities, row.version().toString());
    }

    boolean sourceInvalid(ActiveIdentity actor,TenantAdministrationRows.Assignment row) {
        var source=live.delegation(tenant(actor),row.delegationGrantId());var now=LocalDateTime.now(ZoneOffset.UTC);
        if(source==null||source.status()!=GrantStatus.ACTIVE||!live.delegatedRoles(source.id()).contains(row.roleId())
            ||source.validFrom()!=null&&source.validFrom().isAfter(now)||source.validUntil()!=null&&!source.validUntil().isAfter(now)
            ||live.members(tenant(actor),List.of(source.administratorMemberId()))!=1||row.validUntil()==null
            ||source.validFrom()!=null&&row.validFrom().isBefore(source.validFrom())||source.validUntil()!=null&&row.validUntil().isAfter(source.validUntil())
            ||Duration.between(row.validFrom(),row.validUntil()).compareTo(Duration.ofSeconds(source.durationSeconds().longValueExact(),source.durationNanos()))>0)return true;
        return row.subjectType()==SubjectType.GROUP
            ? !live.groupWithinSource(new TenantAuthorizationOptionSql.Query(tenant(actor),source.administratorMemberId(),source.id(),TenantAssignmentOptionKind.GROUP,"%",List.of(),1,0),row.groupId())
            : live.recipient(tenant(actor),source.id(),row.memberId())==0;
    }
    void validateBatchSource(ActiveIdentity actor,TenantAssignmentBatchInput batch,IamAction operation){if(batch.items()==null||batch.items().isEmpty())invalid();if(!access.allows(AuthorizationDomain.TENANT,operation,true)&&batch.items().stream().map(TenantAssignmentInput::delegationGrantId).distinct().count()!=1)exceeded();}
    TenantAdministrationRows.Role role(ActiveIdentity actor,String id){
        var role=org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()
            ? live.lockRole(new BigInteger(id)) : live.role(tenant(actor),new BigInteger(id));
        if(role==null || role.kind()!=RoleKind.SHARED && (role.kind()!=RoleKind.TENANT_CUSTOM || !tenant(actor).equals(role.tenantId())))missing();return role;
    }
    static BigInteger tenant(ActiveIdentity a){return new BigInteger(a.context().tenantId());}
    static BigInteger member(ActiveIdentity a){return new BigInteger(a.context().memberId());}
    static Instant instant(LocalDateTime i){return i==null?null:i.toInstant(ZoneOffset.UTC);}
    static void invalid(){throw new BizException(IamReasonCode.INVALID_ARGUMENT);}
    static void missing(){throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);}
    static void denied(){throw new BizException(IamReasonCode.ACTION_DENIED);}
    static void exceeded(){throw new BizException(IamReasonCode.DELEGATION_EXCEEDED);}

    private static BizException exceeded(String message) {
        return new BizException(IamReasonCode.DELEGATION_EXCEEDED.getCode(), message);
    }
}
