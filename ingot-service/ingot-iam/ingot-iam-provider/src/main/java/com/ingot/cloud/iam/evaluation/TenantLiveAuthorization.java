package com.ingot.cloud.iam.evaluation;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.persistence.TenantCurrentRoleRepository;
import com.ingot.cloud.iam.persistence.mapper.TenantLiveMapper;
import com.ingot.cloud.iam.persistence.projection.TenantLiveRows;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>每次租户请求读取当前拥有者、角色头、来源委派及有效开通，失败关闭。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantLiveAuthorization {
    private static final Duration DISPLAY_WINDOW = Duration.ofSeconds(30);
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() { };
    private static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() { };
    private static final List<IamAction> ASSIGNMENT_ENTRIES = List.of(IamAction.TENANT_ASSIGNMENT_READ,
            IamAction.TENANT_ASSIGNMENT_CREATE, IamAction.TENANT_ASSIGNMENT_UPDATE, IamAction.TENANT_ASSIGNMENT_DELETE);
    private static final Set<String> INDEPENDENT_MANAGEMENT = java.util.stream.Stream.concat(ASSIGNMENT_ENTRIES.stream(),java.util.stream.Stream.of(
            IamAction.TENANT_ROLE_CREATE,IamAction.TENANT_ROLE_UPDATE,IamAction.TENANT_ROLE_STATUS,IamAction.TENANT_ROLE_DELETE,IamAction.TENANT_ROLE_PREVIEW,
            IamAction.TENANT_DELEGATION_READ,IamAction.TENANT_DELEGATION_CREATE,IamAction.TENANT_DELEGATION_UPDATE,IamAction.TENANT_DELEGATION_DELETE,IamAction.TENANT_DELEGATION_PREVIEW))
            .map(IamAction::getCode).collect(java.util.stream.Collectors.toUnmodifiableSet());
    private final TenantLiveMapper live;
    private final TenantCurrentRoleRepository roles;
    private final AuthorizationEvaluationRepository evaluations;
    private final DepartmentClosure departments;

    /** 按当前事实生成授权视图；调用方先校验凭证及改密限制。 */
    public AuthorizationEvaluator.AuthorizationView evaluate(AuthorizationContext actor) {
        BigInteger tenant = new BigInteger(actor.tenantId()), member = new BigInteger(actor.memberId());
        var identity = live.owner(tenant, member);
        if (identity == null) throw new BizException(IamReasonCode.IDENTITY_INVALID);
        Instant deadline = Instant.now().plus(DISPLAY_WINDOW);
        Set<String> codes = new LinkedHashSet<>(), governed = new LinkedHashSet<>();
        Map<String, List<ScopeClause>> scopes = new LinkedHashMap<>();
        List<AuthorizationEvaluator.RoleFieldSource> fieldSources = new ArrayList<>();
        List<Object> fingerprint = new ArrayList<>();
        fingerprint.add(identity);
        fingerprint.add(live.dependencies(tenant));
        var applications=live.applications(tenant);
        fingerprint.add(applications);
        boolean owner = member.equals(identity.ownerMemberId());
        if (owner) {
            var actions = live.ownerActions(tenant);
            fingerprint.add(actions);
            actions.forEach(a -> { codes.add(a.actionCode()); governed.add(a.actionCode());
                scopes.put(a.actionCode(), List.of(ScopeClause.universe())); });
            for (var app : applications) deadline = earlier(deadline, app.expiresAt());
        } else {
            var assignments = live.effectiveAssignments(tenant, member);
            var heads=assignments.isEmpty()?List.<TenantLiveRows.Role>of():live.roleHeads(tenant,assignments.stream().map(TenantLiveRows.Assignment::roleId).distinct().toList()).stream().filter(role->Boolean.TRUE.equals(role.enabled())).toList();
            var definitions=roles.definitions(heads);
            var headsById=heads.stream().collect(java.util.stream.Collectors.toMap(TenantLiveRows.Role::id,java.util.function.Function.identity()));
            Map<BigInteger, List<TenantLiveRows.Ceiling>> ceilings = new HashMap<>();
            Map<BigInteger,TenantLiveRows.Delegation> sourcesById=new HashMap<>();
            Map<BigInteger,List<BigInteger>> allowedRolesBySource=new HashMap<>();
            Map<BigInteger, AuthorizationEvalRows.Entitlement> entitlements = new HashMap<>();
            Map<ScopeClause,ScopeClause> expanded = new HashMap<>();
            Set<BigInteger> ids = new LinkedHashSet<>();
            fingerprint.add(heads);fingerprint.add(assignments);
            definitions.values().forEach(definition->definition.grants().forEach(grant->ids.add(new BigInteger(grant.actionId()))));
            Map<String, TenantLiveRows.Action> actions = new HashMap<>();
            if (!ids.isEmpty()) live.actions(List.copyOf(ids)).forEach(a -> actions.put(a.actionId().toString(), a));
            fingerprint.add(actions);
            for (var assignment : assignments) {
                var definition = definitions.get(assignment.roleId());
                if (definition == null) continue;
                TenantLiveRows.Delegation source=null;
                if(assignment.delegationGrantId()!=null) {
                    source=sourcesById.computeIfAbsent(assignment.delegationGrantId(),id->live.delegation(tenant,id));
                    if(source==null || source.status()!=GrantStatus.ACTIVE || !allowedRolesBySource.computeIfAbsent(source.id(),live::delegatedRoles).contains(assignment.roleId()))continue;
                    if(assignment.groupId()!=null && !live.groupWithinSource(new com.ingot.cloud.iam.persistence.TenantLiveOptionSql.Query(tenant,source.administratorMemberId(),source.id(),TenantAssignmentOptionKind.GROUP,"%",List.of(),1,0),assignment.groupId()))continue;
                    deadline=earlier(deadline,source.validUntil());fingerprint.add(source);
                }
                var bindings = IamJson.read(assignment.scopeBindings(), BINDINGS);
                var limits = assignment.delegationGrantId() == null ? List.<TenantLiveRows.Ceiling>of()
                    : ceilings.computeIfAbsent(assignment.delegationGrantId(), live::ceilings);
                fingerprint.add(limits);
                for (var grant : definition.grants()) {
                    var action = actions.get(grant.actionId());
                    if (action == null || !Boolean.TRUE.equals(action.enabled()) || assignment.delegationGrantId()!=null && INDEPENDENT_MANAGEMENT.contains(action.actionCode())) continue;
                    var entitlement = entitlements.computeIfAbsent(action.applicationId(), id -> evaluations.entitlement(tenant, id, member));
                    if (entitlement.hits() <= 0) continue;
                    deadline = earlier(deadline, entitlement.earliestExpiry());
                    boolean missing = grant.scopes().stream().filter(s -> s.parameterKey() != null).anyMatch(s -> {
                        var expected = definition.parameterDefinitions().stream().filter(p -> p.key().equals(s.parameterKey())).findFirst().orElse(null);
                        var binding = bindings.get(s.parameterKey());
                        return expected == null || binding == null || binding.kind() != expected.kind();
                    });
                    if (missing) continue;
                    var clauses = ScopeBinder.bind(grant, bindings);
                    if (assignment.delegationGrantId() != null) {
                        var limit = limits.stream().filter(c -> c.resourceId().equals(action.resourceId())).findFirst().orElse(null);
                        if (limit == null) continue;
                        var ceiling = new ActionScopeCeiling(grant.actionId(), IamJson.read(limit.scopes(),SCOPES), IamJson.read(limit.scopeBindings(),BINDINGS));
                        clauses = ScopeBinder.intersect(expand(tenant,clauses,expanded),expand(tenant,ScopeBinder.bind(ceiling),expanded));

                    } else governed.add(action.actionCode());
                    deadline = earlier(deadline, assignment.validUntil());
                    codes.add(action.actionCode()); scopes.computeIfAbsent(action.actionCode(), key -> new ArrayList<>()).addAll(clauses);
                    fieldSources.add(new AuthorizationEvaluator.RoleFieldSource(assignment.id(), assignment.groupId(),
                        assignment.delegationGrantId() == null ? null : assignment.delegationGrantId().longValueExact(),
                        headsById.get(assignment.roleId()).currentRevisionId().longValueExact(), action.actionCode(), clauses));
                }
            }
            var sources = live.mine(tenant, member);
            var enabled = (sources.isEmpty() ? List.<TenantLiveRows.Action>of() : live.ownerActions(tenant)).stream().map(TenantLiveRows.Action::actionCode).collect(java.util.stream.Collectors.toSet());
            for (var source : sources) {
                // 有效来源只给分配入口；完整治理集合和业务范围不增加。
                if (live.delegatedRoles(source.id()).stream().noneMatch(id -> {
                    var role = live.role(tenant, id); return role != null && Boolean.TRUE.equals(role.enabled());
                })) continue;
                ASSIGNMENT_ENTRIES.stream().map(IamAction::getCode).filter(enabled::contains).forEach(codes::add);
                deadline = earlier(deadline, source.validUntil()); fingerprint.add(source);
            }
        }
        Map<String, ResolvedActionScope> resolved = new LinkedHashMap<>();
        codes.forEach(code -> resolved.put(code, new ResolvedActionScope(scopes.getOrDefault(code, List.of()))));
        fingerprint.add(resolved);fingerprint.add(governed);
        return new AuthorizationEvaluator.AuthorizationView(List.copyOf(codes), List.copyOf(governed), resolved,
            fingerprint(fingerprint), deadline, fieldSources, false, owner);
    }

    /** 诊断租户来源，不把内部快照标识作为可分配版本输出。 */
    public List<DecisionSource> sources(AuthorizationContext actor, String actionId) {
        var view = evaluate(actor);
        var action = live.actions(List.of(new BigInteger(actionId))).stream().findFirst().orElse(null);
        if (action == null || !view.actionCodes().contains(action.actionCode())) return List.of();
        if (view.tenantOwner()) return List.of(new DecisionSource(null, null, null, "当前组织拥有者 / 本组织已开通应用全部权限", null, null));
        Map<BigInteger, TenantLiveRows.Assignment> rows = new HashMap<>();
        live.effectiveAssignments(new BigInteger(actor.tenantId()), new BigInteger(actor.memberId())).forEach(a -> rows.put(a.id(), a));
        return view.fieldSources().stream().filter(s -> s.actionCode().equals(action.actionCode()) && rows.containsKey(s.assignmentId())).map(s -> {
            var row = rows.get(s.assignmentId());
            return new DecisionSource(s.assignmentId().toString(), s.delegationId() == null ? null : s.delegationId().toString(),
                null, row.roleName(), null, row.roleId().toString());
        }).toList();
    }

    private List<ScopeClause> expand(BigInteger tenant,List<ScopeClause> clauses,Map<ScopeClause,ScopeClause> compiled) {
        return clauses.stream().map(clause->compiled.computeIfAbsent(clause,key->{
            if(key.departmentIds().isEmpty())return key;
            var ids=departments.expand(tenant,key.departmentIds().stream().map(BigInteger::new).toList(),key.departmentDescendants());
            if(ids.isEmpty())return new ScopeClause(false,false,false,false,List.of(),false,List.of());
            return new ScopeClause(key.all(),key.self(),key.memberDepartments(),key.memberDepartmentDescendants(),ids.stream().map(BigInteger::toString).toList(),false,key.objectIds());
        })).toList();
    }

    private static String fingerprint(List<Object> parts) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance(TenantAuthorizationConstants.FINGERPRINT_ALGORITHM)
                .digest(parts.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch(java.security.NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
    }

    private static Instant earlier(Instant deadline, LocalDateTime boundary) {
        if (boundary == null) return deadline;
        Instant end = boundary.toInstant(ZoneOffset.UTC); return end.isBefore(deadline) ? end : deadline;
    }
}
