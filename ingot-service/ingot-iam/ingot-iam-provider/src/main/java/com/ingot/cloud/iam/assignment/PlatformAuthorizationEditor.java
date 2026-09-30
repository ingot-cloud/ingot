package com.ingot.cloud.iam.assignment;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.AssignmentRepository;
import com.ingot.cloud.iam.persistence.DelegationRepository;
import com.ingot.cloud.iam.persistence.RoleRepository;
import com.ingot.cloud.iam.persistence.mapper.AuthorizationCandidateMapper;
import com.ingot.cloud.iam.persistence.mapper.AuthorizationCandidateSql;
import com.ingot.cloud.iam.persistence.entity.IamDelegationGrantEntity;
import com.ingot.cloud.iam.role.RoleService;
import com.ingot.cloud.iam.support.*;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>平台授权编辑专用候选，只以可信当前身份与单条授权依据计算可配置集合。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class PlatformAuthorizationEditor {
    private static final String CORE_APPLICATION = "iam-platform";
    private static final int ACTION_METADATA_BATCH_SIZE = 500;
    private static final String VERSION_LABEL_PREFIX = "v";
    private static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() { };
    private static final TypeReference<List<ScopeKind>> KINDS = new TypeReference<>() { };
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() { };
    private final IamAccess access;
    private final com.ingot.cloud.iam.evaluation.AuthorizationEvaluator evaluator;
    private final AssignmentRepository assignments;
    private final DelegationRepository delegations;
    private final RoleRepository roleStore;
    private final RoleService roles;
    private final AuthorizationCandidateMapper candidates;

    /**
     * 查询分配候选，包括已选 ID 回显；未携带依据时只允许完整直接分配资格。
     * @param kind 候选种类
     * @param source 所选授权依据
     * @param revisionId 所选固定版本
     * @param parameterKey 角色范围参数
     * @param actionId 对象所属操作
     * @param applicationId 诊断应用
     * @param keyword 搜索文字
     * @param ids 已选标识
     * @param page 页码
     * @param pageSize 页大小
     * @return 经过筛选的候选
     */
    public AuthorizationCandidatePage assignments(AuthorizationCandidateKind kind, String source, String revisionId,
            String parameterKey, String actionId, String applicationId, String keyword, List<String> ids,
            int page, int pageSize) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_READ);
        if (kind == AuthorizationCandidateKind.APPLICATION || kind == AuthorizationCandidateKind.ACTION) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        DelegationInput basis = null;
        if (kind != AuthorizationCandidateKind.DELEGATION) {
            IamCapabilities permissions = access.capabilities(actor, List.of(
                    IamAction.PLATFORM_ASSIGNMENT_CREATE, IamAction.PLATFORM_ASSIGNMENT_UPDATE));
            if (source != null && !source.isBlank()) {
                basis = source(actor, IamIds.require(source), permissions);
            } else if (!permissions.allows(IamAction.PLATFORM_ASSIGNMENT_CREATE, true)
                    && !permissions.allows(IamAction.PLATFORM_ASSIGNMENT_UPDATE, true)) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
        }
        return query(actor, kind, source, basis, revisionId, parameterKey, actionId, applicationId, keyword,
                ids, page, pageSize, null);
    }

    /**
     * 查询可分配角色树的一层，不合成版本授权；确认版本后另取完整候选。
     * @param source 当前单条授权依据；直接分配时为空
     * @param roleId 指定角色查询其版本；为空查询根角色
     * @param keyword 角色名称搜索
     * @param ids 当前层少量节点回显，仍受依据限制
     * @param page 从 1 开始的页码
     * @param pageSize 页大小
     * @return 当前层的可见最小节点
     */
    public AuthorizationRoleCandidatePage roleCandidates(String source, String roleId, String keyword,
            List<String> ids, int page, int pageSize) {
        IamPages.require(page, pageSize);
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_READ);
        IamCapabilities permissions = access.capabilities(actor, List.of(
                IamAction.PLATFORM_ASSIGNMENT_CREATE, IamAction.PLATFORM_ASSIGNMENT_UPDATE));
        DelegationInput basis = null;
        if (source != null && !source.isBlank()) {
            basis = source(actor, IamIds.require(source), permissions);
        } else if (!permissions.allows(IamAction.PLATFORM_ASSIGNMENT_CREATE, true)
                && !permissions.allows(IamAction.PLATFORM_ASSIGNMENT_UPDATE, true)) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        List<BigInteger> selected = ids == null ? List.of() : ids.stream()
                .map(value -> BigInteger.valueOf(IamIds.require(value))).distinct().toList();
        if (selected.size() > IamPages.MAX_SIZE) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        BigInteger parent = roleId == null || roleId.isBlank() ? null
                : BigInteger.valueOf(IamIds.require(roleId));
        List<BigInteger> allowed = basis == null ? null : basis.allowedRoleRevisionRefs().stream()
                .map(ref -> BigInteger.valueOf(IamIds.require(ref.id()))).distinct().toList();
        String search = keyword == null ? "" : keyword.trim();
        search = "%" + search.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        var query = new AuthorizationCandidateSql.RoleQuery(parent, search, selected, allowed,
                Math.multiplyExact(page - 1, pageSize), pageSize);
        List<AuthorizationRoleNode> nodes = candidates.rolePage(query).stream().map(row -> {
            String id = row.id().toString();
            if (parent == null) {
                return new AuthorizationRoleNode(id, row.roleId().toString(), row.name(), row.name(),
                        AuthorizationRoleNodeType.ROLE, null, null);
            }
            long number = row.revision().longValueExact();
            return new AuthorizationRoleNode(id, row.roleId().toString(), row.name(), VERSION_LABEL_PREFIX + number,
                    AuthorizationRoleNodeType.REVISION, number, new RoleRevisionRef(row.kind(), id));
        }).toList();
        return new AuthorizationRoleCandidatePage(nodes, candidates.roleCount(query), page, pageSize);
    }

    /**
     * 委派编辑专用候选，不要求取得通用人员、角色列表权限。
     * @param kind 候选类型
     * @param actionId 范围操作
     * @param keyword 搜索文字
     * @param ids 已选标识
     * @param page 页码
     * @param pageSize 页大小
     * @return 最小候选
     */
    public AuthorizationCandidatePage delegations(AuthorizationCandidateKind kind, String actionId, String keyword,
            List<String> ids, int page, int pageSize) {
        ActiveIdentity actor = access.requireCurrent();
        if (actor.context().domain() != AuthorizationDomain.PLATFORM) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        IamCapabilities permissions = access.capabilities(actor, List.of(IamAction.PLATFORM_DELEGATION_CREATE,
                IamAction.PLATFORM_DELEGATION_UPDATE, IamAction.PLATFORM_DELEGATION_READ));
        if (!permissions.allows(IamAction.PLATFORM_DELEGATION_CREATE, true)
                && !permissions.allows(IamAction.PLATFORM_DELEGATION_UPDATE, true)
                && !permissions.allows(IamAction.PLATFORM_DELEGATION_READ, true)) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        if (!Set.of(AuthorizationCandidateKind.MEMBER, AuthorizationCandidateKind.ROLE_REVISION,
                AuthorizationCandidateKind.OBJECT).contains(kind)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return query(actor, kind, null, null, null, null, actionId, null, keyword, ids, page, pageSize, null);
    }

    /**
     * 只读诊断专用候选。
     * @param kind 成员、应用、操作或目标
     * @param actionId 诊断操作
     * @param applicationId 所选应用
     * @param keyword 搜索文字
     * @param ids 已选标识
     * @param page 页码
     * @param pageSize 页大小
     * @return 最小候选
     */
    public AuthorizationCandidatePage diagnose(AuthorizationCandidateKind kind, String actionId, String applicationId,
            String keyword, List<String> ids, int page, int pageSize) {
        ActiveIdentity actor = access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_AUTHORIZATION_DIAGNOSE);
        if (!Set.of(AuthorizationCandidateKind.MEMBER, AuthorizationCandidateKind.APPLICATION,
                AuthorizationCandidateKind.ACTION, AuthorizationCandidateKind.OBJECT).contains(kind)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        var clauses = evaluator.evaluate(actor.context()).scope(IamAction.PLATFORM_AUTHORIZATION_DIAGNOSE.getCode()).clauses();
        List<BigInteger> visible = clauses.stream().anyMatch(com.ingot.cloud.iam.evaluation.ScopeClause::all) ? null
                : clauses.stream().flatMap(clause -> clause.objectIds().stream()).distinct().map(BigInteger::new).toList();
        return query(actor, kind, null, null, null, null, actionId, applicationId, keyword, ids, page, pageSize,
                kind == AuthorizationCandidateKind.MEMBER || kind == AuthorizationCandidateKind.OBJECT ? visible : null);
    }

    private AuthorizationCandidatePage query(ActiveIdentity actor, AuthorizationCandidateKind kind, String source,
            DelegationInput basis, String revisionId, String key, String actionId, String applicationId,
            String keyword, List<String> ids, int page, int size, List<BigInteger> visibility) {
        IamPages.require(page, size);
        List<BigInteger> selected = ids == null ? List.of() : ids.stream().map(value ->
                BigInteger.valueOf(IamIds.require(value))).distinct().toList();
        if (selected.size() > IamPages.MAX_SIZE) { throw new BizException(IamReasonCode.INVALID_ARGUMENT); }
        List<BigInteger> allowed = visibility;
        if (basis != null && kind == AuthorizationCandidateKind.ROLE_REVISION) {
            allowed = basis.allowedRoleRevisionRefs().stream().map(ref -> new BigInteger(ref.id())).toList();
        } else if (basis != null && kind == AuthorizationCandidateKind.MEMBER) {
            allowed = basis.recipientSelection().members().stream().map(BigInteger::new).toList();
        }
        String resource = null;
        if (kind == AuthorizationCandidateKind.OBJECT) {
            var actions = objectActions(revisionId, key, actionId, basis);
            var action = actions.isEmpty() ? null : actions.getFirst();
            if (action == null || !action.code().startsWith(CORE_APPLICATION + ":")) {
                return unsupported(page, size);
            }
            String code = action.code();
            resource = code.substring(code.indexOf(':') + 1, code.lastIndexOf(':'));
            if (PlatformScopeObjectResource.find(resource) == null) { return unsupported(page, size); }
            if (basis != null) {
                for (var boundedAction : actions) {
                ActionScopeCeiling ceiling = basis.actionScopeCeilings().stream()
                        .filter(value -> value.actionId().equals(boundedAction.id().toString())).findFirst()
                        .orElseThrow(() -> new BizException(IamReasonCode.DELEGATION_EXCEEDED));
                var clauses = com.ingot.cloud.iam.evaluation.ScopeBinder.bind(ceiling);
                if (clauses.stream().noneMatch(com.ingot.cloud.iam.evaluation.ScopeClause::all)) {
                    List<BigInteger> bounded = clauses.stream().filter(clause -> !clause.self()
                            && !clause.memberDepartments() && clause.departmentIds().isEmpty())
                            .flatMap(value -> value.objectIds().stream()).distinct().map(BigInteger::new).toList();
                    allowed = allowed == null ? bounded : allowed.stream().filter(bounded::contains).toList();
                }
                }
            }
        }
        String search = keyword == null ? "" : keyword.trim();
        search = "%" + search.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        var query = new AuthorizationCandidateSql.Query(kind, new BigInteger(actor.context().memberId()),
                source == null || source.isBlank() ? null : BigInteger.valueOf(IamIds.require(source)),
                applicationId == null || applicationId.isBlank() ? null : BigInteger.valueOf(IamIds.require(applicationId)),
                resource, search, selected, allowed, Math.multiplyExact(page - 1, size), size);
        var rows = candidates.page(query);
        List<BigInteger> delegationIds = kind == AuthorizationCandidateKind.DELEGATION
                ? rows.stream().map(com.ingot.cloud.iam.persistence.projection.AuthorizationCandidateRow::id).toList()
                : List.of();
        var sources = assignments.findDelegations(AuthorizationDomain.PLATFORM, null, delegationIds);
        var children = delegations.children(AuthorizationDomain.PLATFORM, delegationIds);
        IamCapabilities permissions = delegationIds.isEmpty() ? new IamCapabilities(Map.of())
                : access.capabilities(actor, List.of(IamAction.PLATFORM_ASSIGNMENT_UPDATE));
        Map<BigInteger, List<ActionGrant>> revisionGrants = new LinkedHashMap<>();
        if (kind == AuthorizationCandidateKind.ROLE_REVISION) {
            rows.forEach(row -> revisionGrants.put(row.id(), roles.synthesizedGrants(row.id().longValueExact())));
        }
        List<AuthorizationActionOption> operationOptions = actionOptions(revisionGrants.values().stream()
                .flatMap(Collection::stream).map(ActionGrant::actionId).distinct().toList());
        List<AuthorizationOption> options = rows.stream().map(row -> {
            String id = row.id().toString();
            if (kind == AuthorizationCandidateKind.DELEGATION) {
                DelegationInput input = source(actor, sources.get(row.id()), children.get(row.id()), permissions);
                return new AuthorizationOption(id, "委派 " + id, "允许 " + input.allowedRoleRevisionRefs().size()
                        + " 个固定版本 / " + input.recipientSelection().members().size() + " 名接收成员 / 最长 "
                        + input.maxAssignmentDuration(), null, null, null, null, input);
            }
            if (kind == AuthorizationCandidateKind.ROLE_REVISION) {
                long revision = row.id().longValueExact();
                List<ActionGrant> grants = revisionGrants.get(row.id());
                Set<String> operationIds = grants.stream().map(ActionGrant::actionId)
                        .collect(java.util.stream.Collectors.toSet());
                var parameters = roleStore.listParameters(revision).stream()
                        .map(value -> new RoleParameterDefinition(value.getParameterKey(), value.getBindingKind())).toList();
                return new AuthorizationOption(id, row.name() + " · v" + row.revision(), null,
                        new RoleRevisionRef(row.kind(), id), parameters, grants,
                        operationOptions.stream().filter(option -> operationIds.contains(option.id())).toList(), null);
            }
            return new AuthorizationOption(id, row.name(), null, null, null, null, null, null);
        }).toList();
        return new AuthorizationCandidatePage(options, candidates.count(query), page, size, true, null);
    }

    private AuthorizationCandidateMapper.ActionRow objectAction(String revisionId, String key, String actionId,
                                                                 DelegationInput basis) {
        var actions = objectActions(revisionId, key, actionId, basis);
        return actions.isEmpty() ? null : actions.getFirst();
    }

    private List<AuthorizationCandidateMapper.ActionRow> objectActions(String revisionId, String key, String actionId,
                                                                      DelegationInput basis) {
        List<String> actions;
        if (revisionId != null && !revisionId.isBlank()) {
            if (basis != null && basis.allowedRoleRevisionRefs().stream().noneMatch(ref -> ref.id().equals(revisionId))) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
            var revision = assignments.findRevision(IamIds.require(revisionId));
            if (revision == null || revision.getDomain() != AuthorizationDomain.PLATFORM
                    || revision.getTenantId() != null || !Boolean.TRUE.equals(revision.getEnabled())) {
                throw new BizException(IamReasonCode.ROLE_REVISION_UNAVAILABLE);
            }
            actions = roles.synthesizedGrants(IamIds.require(revisionId)).stream().filter(grant -> grant.scopes().stream()
                    .anyMatch(scope -> scope.kind() == ScopeKind.OBJECT_SET && Objects.equals(key, scope.parameterKey())))
                    .map(ActionGrant::actionId).distinct().toList();
        } else {
            actions = actionId == null || actionId.isBlank() ? List.of() : List.of(actionId);
        }
        if (actions.isEmpty()) { return List.of(); }
        var rows = candidates.actions(actions.stream().map(value -> BigInteger.valueOf(IamIds.require(value))).toList());
        if (rows.size() != actions.size() || rows.stream().map(AuthorizationCandidateMapper.ActionRow::resourceId)
                .distinct().count() != 1) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return rows;
    }

    /**
     * 批量补齐操作标签和资源允许范围。
     * @param ids 操作集合
     * @return 可用操作元数据
     */
    public List<AuthorizationActionOption> actionOptions(List<String> ids) {
        if (ids.isEmpty()) { return List.of(); }
        List<BigInteger> actionIds = ids.stream().map(BigInteger::new).distinct().toList();
        List<AuthorizationCandidateMapper.ActionRow> rows = new ArrayList<>(actionIds.size());
        for (int offset = 0; offset < actionIds.size(); offset += ACTION_METADATA_BATCH_SIZE) {
            rows.addAll(candidates.actions(actionIds.subList(offset,
                    Math.min(offset + ACTION_METADATA_BATCH_SIZE, actionIds.size()))));
        }
        return rows.stream().sorted(Comparator.comparing(AuthorizationCandidateMapper.ActionRow::applicationId)
                .thenComparing(AuthorizationCandidateMapper.ActionRow::resourceId)
                .thenComparing(AuthorizationCandidateMapper.ActionRow::id)).map(row ->
                new AuthorizationActionOption(row.id().toString(), row.name(), row.applicationId().toString(),
                        row.applicationName(), row.resourceId().toString(), row.resourceName(), row.code(),
                        IamJson.read(row.scopeCapabilities(), KINDS))).toList();
    }

    /**
     * 提交路径验证固定角色参数类型、资源归属及对象真实存在，不能信任前端候选筛选。
     * @param input 分配定义
     * @return 问题集合
     */
    public List<ValidationIssue> validateBindings(AssignmentInput input) {
        long revisionId = IamIds.require(input.roleRevisionRef().id());
        var definitions = roleStore.listParameters(revisionId);
        var bindings = input.scopeBindings();
        if (bindings == null) { return List.of(new ValidationIssue("scopeBindings", IamReasonCode.INVALID_ARGUMENT, "缺少范围参数")); }
        for (var entry : bindings.entrySet()) {
            var parameter = definitions.stream().filter(value -> value.getParameterKey().equals(entry.getKey())).findFirst().orElse(null);
            if (parameter == null || entry.getValue().kind() != parameter.getBindingKind()) {
                return List.of(new ValidationIssue("scopeBindings", IamReasonCode.INVALID_ARGUMENT, "参数未声明或类型不匹配"));
            }
            var action = objectAction(input.roleRevisionRef().id(), entry.getKey(), null, null);
            if (!objectsExist(action, entry.getValue())) {
                return List.of(new ValidationIssue("scopeBindings", IamReasonCode.INVALID_ARGUMENT, "资源不支持对象查询或包含无效对象"));
            }
        }
        return List.of();
    }

    /**
     * 委派范围提交时按实际受保护对象标识校验。
     * @param ceiling 逐操作上限
     * @return 对象合法时 true
     */
    public boolean validCeilingObjects(ActionScopeCeiling ceiling) {
        var action = objectAction(null, null, ceiling.actionId(), null);
        return ceiling.scopeBindings().values().stream().allMatch(value -> objectsExist(action, value));
    }

    private boolean objectsExist(AuthorizationCandidateMapper.ActionRow action, ScopeBinding binding) {
        if (binding.kind() != ScopeBindingKind.OBJECTS) { return false; }
        if (binding.ids().isEmpty()) { return true; }
        if (action == null || !action.code().startsWith(CORE_APPLICATION + ":")) { return false; }
        String resource = action.code().substring(action.code().indexOf(':') + 1, action.code().lastIndexOf(':'));
        if (PlatformScopeObjectResource.find(resource) == null) { return false; }
        List<BigInteger> ids = binding.ids().stream().map(value -> BigInteger.valueOf(IamIds.require(value))).distinct().toList();
        var query = new AuthorizationCandidateSql.Query(AuthorizationCandidateKind.OBJECT, null, null,
                null, resource, "%", ids, null, 0, IamPages.DEFAULT_SIZE);
        return candidates.count(query) == ids.size();
    }

    private DelegationInput source(ActiveIdentity actor, long id, IamCapabilities permissions) {
        IamDelegationGrantEntity source = assignments.findDelegation(AuthorizationDomain.PLATFORM, null, id);
        var children = source == null ? null : delegations.children(AuthorizationDomain.PLATFORM,
                List.of(source.getId())).get(source.getId());
        return source(actor, source, children, permissions);
    }

    private DelegationInput source(ActiveIdentity actor, IamDelegationGrantEntity source,
            DelegationRepository.Children children, IamCapabilities permissions) {
        Instant now = Instant.now();
        if (source == null || source.getPlatformAdministratorId() == null
                || !source.getPlatformAdministratorId().toString().equals(actor.context().memberId())
                    && !permissions.allows(IamAction.PLATFORM_ASSIGNMENT_UPDATE, true)
                || source.getStatus() != GrantStatus.ACTIVE
                || source.getValidFrom() != null && now.isBefore(source.getValidFrom().toInstant(ZoneOffset.UTC))
                || source.getValidUntil() != null && !now.isBefore(source.getValidUntil().toInstant(ZoneOffset.UTC))) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        var refs = children.revisions().stream().map(row ->
                new RoleRevisionRef(row.getRevisionKind(), row.getRevisionId().toString())).toList();
        var members = children.members().stream().map(row -> row.getPlatformMemberId().toString()).toList();
        var ceilings = children.ceilings().stream().map(row -> new ActionScopeCeiling(row.getActionId().toString(),
                IamJson.read(row.getScopes(), SCOPES), IamJson.read(row.getScopeBindings(), BINDINGS))).toList();
        return new DelegationInput(source.getPlatformAdministratorId().toString(), refs, new Selection(members, List.of()), ceilings,
                source.getValidFrom() == null ? null : source.getValidFrom().toInstant(ZoneOffset.UTC),
                source.getValidUntil() == null ? null : source.getValidUntil().toInstant(ZoneOffset.UTC),
                Duration.ofSeconds(source.getMaxAssignmentDurationSeconds(), source.getMaxAssignmentDurationNanos()));
    }

    private static AuthorizationCandidatePage unsupported(int page, int size) {
        return new AuthorizationCandidatePage(List.of(), 0, page, size, false, "该资源暂未接入范围对象查询，不能配置指定对象");
    }
}
