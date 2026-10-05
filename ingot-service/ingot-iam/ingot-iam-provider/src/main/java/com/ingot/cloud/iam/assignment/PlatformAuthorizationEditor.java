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
        return assignments(kind, source, revisionId, parameterKey, actionId, applicationId, keyword, ids,
                page, pageSize, false, null);
    }

    /**
     * 在原有分配资格边界内按真实层级资源读取树的当前分支。
     * @param kind 候选种类
     * @param source 所选委派
     * @param revisionId 固定版本
     * @param parameterKey 范围参数
     * @param actionId 范围操作
     * @param applicationId 应用
     * @param keyword 搜索词
     * @param ids 已选回显
     * @param page 页码
     * @param pageSize 每页大小
     * @param tree 请求树分支
     * @param parentId 父节点，根分支为空
     * @return 候选页
     */
    public AuthorizationCandidatePage assignments(AuthorizationCandidateKind kind, String source, String revisionId,
            String parameterKey, String actionId, String applicationId, String keyword, List<String> ids,
            int page, int pageSize, boolean tree, String parentId) {
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
                ids, page, pageSize, null, tree, parentId);
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
        return rolePage(roleId, keyword, ids, page, pageSize, basis);
    }

    /**
     * 查询委派管理专用的角色与固定版本树，不要求角色分配资格。
     * @param roleId 指定角色时只查询其版本
     * @param keyword 角色名称搜索
     * @param ids 当前层少量已选节点回显
     * @param page 从 1 开始的页码
     * @param pageSize 页大小
     * @return 平台委派可选择的角色树当前层
     */
    public AuthorizationRoleCandidatePage delegationRoleCandidates(String roleId, String keyword,
            List<String> ids, int page, int pageSize) {
        requireDelegationActor();
        return rolePage(roleId, keyword, ids, page, pageSize, null);
    }

    private AuthorizationRoleCandidatePage rolePage(String roleId, String keyword, List<String> ids,
            int page, int pageSize, DelegationInput basis) {
        IamPages.require(page, pageSize);
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
        return delegations(kind, actionId, keyword, ids, page, pageSize, false, null);
    }

    /**
     * 在委派管理权限内读取层级范围对象的当前分支。
     * @param kind 候选种类
     * @param actionId 范围操作
     * @param keyword 搜索词
     * @param ids 已选回显
     * @param page 页码
     * @param pageSize 每页大小
     * @param tree 请求树分支
     * @param parentId 父节点
     * @return 候选页
     */
    public AuthorizationCandidatePage delegations(AuthorizationCandidateKind kind, String actionId, String keyword,
            List<String> ids, int page, int pageSize, boolean tree, String parentId) {
        ActiveIdentity actor = requireDelegationActor();
        if (!Set.of(AuthorizationCandidateKind.MEMBER, AuthorizationCandidateKind.ROLE_REVISION,
                AuthorizationCandidateKind.OBJECT).contains(kind)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return query(actor, kind, null, null, null, null, actionId, null, keyword, ids, page, pageSize,
                null, tree, parentId);
    }

    /**
     * 查询委派接收成员候选，排除授权管理员发生在计数与分页之前。
     * @param kind 候选类型
     * @param actionId 对象操作
     * @param keyword 搜索词
     * @param ids 少量回显标识
     * @param page 页码
     * @param pageSize 页大小
     * @param tree 是否查询树分支
     * @param parentId 父节点
     * @param excludeMemberId 需要排除的管理员
     * @return 可选候选页
     */
    public AuthorizationCandidatePage delegations(AuthorizationCandidateKind kind, String actionId, String keyword,
            List<String> ids, int page, int pageSize, boolean tree, String parentId, String excludeMemberId) {
        ActiveIdentity actor = requireDelegationActor();
        if (!Set.of(AuthorizationCandidateKind.MEMBER, AuthorizationCandidateKind.ROLE_REVISION,
                AuthorizationCandidateKind.OBJECT).contains(kind)) throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        return query(actor, kind, null, null, null, null, actionId, null, keyword, ids, page, pageSize,
                null, tree, parentId, excludeMemberId, null);
    }

    /**
     * 按可见委派的真实关系读取已选实体，不将全集 ID 拆分为伪关联查询。
     * @param id 委派 ID
     * @param kind 已选实体类型
     * @param actionId 对象关系所属操作
     * @param page 页码
     * @param pageSize 页大小
     * @return 经候选有效性过滤的关联页
     */
    public AuthorizationCandidatePage selectedDelegationCandidates(String id, AuthorizationCandidateKind kind,
            String actionId, int page, int pageSize) {
        return selectedDelegationCandidates(id, kind, actionId, page, pageSize, null);
    }

    /**
     * 分页读取委派已选实体，编辑接收名单时排除当前管理员。
     * @param id 委派标识
     * @param kind 已选实体种类
     * @param actionId 指定对象所属操作
     * @param page 页码
     * @param pageSize 页大小
     * @param excludeMemberId 草稿管理员，缺省排除已存管理员
     * @return 同候选边界的关联分页
     */
    public AuthorizationCandidatePage selectedDelegationCandidates(String id, AuthorizationCandidateKind kind,
            String actionId, int page, int pageSize, String excludeMemberId) {
        ActiveIdentity actor = access.requireGoverned(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_DELEGATION_READ);
        long delegationId = IamIds.require(id);
        var visibility = visibleIds(evaluator.evaluate(actor.context()).scope(IamAction.PLATFORM_DELEGATION_READ.getCode()), null);
        var delegation = delegations.find(AuthorizationDomain.PLATFORM, null, delegationId);
        if (visibility != null && !visibility.contains(BigInteger.valueOf(delegationId))
                || delegation == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        if (!Set.of(AuthorizationCandidateKind.MEMBER, AuthorizationCandidateKind.ROLE_REVISION,
                AuthorizationCandidateKind.OBJECT).contains(kind)
                || kind == AuthorizationCandidateKind.OBJECT && (actionId == null || actionId.isBlank())) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return query(actor, kind, null, null, null, null, actionId, null, null, List.of(), page, pageSize,
                null, false, null, excludeMemberId == null || excludeMemberId.isBlank() ? delegation.getPlatformAdministratorId().toString() : excludeMemberId, id);
    }

    private ActiveIdentity requireDelegationActor() {
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
        return actor;
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
        var view = evaluator.evaluate(actor.context());
        List<BigInteger> visible = kind == AuthorizationCandidateKind.MEMBER
                ? visibleIds(view.scope(IamAction.PLATFORM_AUTHORIZATION_DIAGNOSE.getCode()), actor.context().memberId())
                : null;
        if (kind == AuthorizationCandidateKind.OBJECT) {
            PlatformScopeObjectResource adapter = objectResource(objectActions(null, null, actionId, null));
            if (adapter == null) { return unsupported(page, pageSize); }
            IamAction read = adapter.getReadAction();
            visible = view.actionCodes().contains(read.getCode())
                    ? visibleIds(view.scope(read.getCode()), adapter == PlatformScopeObjectResource.MEMBER
                            ? actor.context().memberId() : null)
                    : List.of();
        }
        return query(actor, kind, null, null, null, null, actionId, applicationId, keyword, ids, page, pageSize,
                visible);
    }

    private static List<BigInteger> visibleIds(com.ingot.cloud.iam.evaluation.ResolvedActionScope scope,
            String selfMemberId) {
        if (scope.clauses().stream().anyMatch(com.ingot.cloud.iam.evaluation.ScopeClause::all)) { return null; }
        return scope.clauses().stream().flatMap(clause -> {
            var ids = new ArrayList<>(clause.objectIds());
            if (clause.self() && selfMemberId != null) { ids.add(selfMemberId); }
            return ids.stream();
        }).distinct().map(BigInteger::new).toList();
    }

    /**
     * 按已验证可见的分配读取固定版本或已绑定对象，不扩大为通用候选列表。
     * @param actor 已通过分配详情边界的身份
     * @param assignmentId 可信分配标识
     * @param input 持久化分配定义，不接收客户端替换版本或来源
     * @param kind 固定版本或范围对象
     * @param parameterKey 对象所属固定版本参数
     * @param page 页码
     * @param pageSize 每页数量
     * @return 与实际关联相交的候选页
     */
    public AuthorizationCandidatePage selectedAssignmentCandidates(ActiveIdentity actor, String assignmentId,
            AssignmentInput input, AuthorizationCandidateKind kind, String parameterKey, int page, int pageSize) {
        IamPages.require(page, pageSize);
        if (kind != AuthorizationCandidateKind.ROLE_REVISION && kind != AuthorizationCandidateKind.OBJECT) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        DelegationInput basis = null;
        if (kind == AuthorizationCandidateKind.OBJECT) {
            if (parameterKey == null || parameterKey.isBlank()
                    || !input.scopeBindings().containsKey(parameterKey)) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            if (input.delegationGrantId() != null && !input.delegationGrantId().isBlank()) {
                var grant = delegations.find(AuthorizationDomain.PLATFORM, null,
                        IamIds.require(input.delegationGrantId()));
                var children = grant == null ? null : delegations.children(AuthorizationDomain.PLATFORM,
                        List.of(grant.getId())).get(grant.getId());
                try {
                    basis = source(actor, grant, children, new IamCapabilities(Map.of()), true);
                } catch (BizException unavailable) {
                    return new AuthorizationCandidatePage(List.of(), 0, page, pageSize, false,
                            "来源委派已失效，当前对象不能作为可提交候选");
                }
            }
            try {
                return query(actor, kind, input.delegationGrantId(), basis, input.roleRevisionRef().id(),
                        parameterKey, null, null, null, List.of(), page, pageSize, null,
                        false, null, null, null, assignmentId);
            } catch (BizException unavailable) {
                return new AuthorizationCandidatePage(List.of(), 0, page, pageSize, false,
                        "固定版本或范围参数已不可用，请检查角色与授权依据");
            }
        }
        return query(actor, kind, null, null, null, null, null, null, null, List.of(),
                page, pageSize, null, false, null, null, null, assignmentId);
    }

    private AuthorizationCandidatePage query(ActiveIdentity actor, AuthorizationCandidateKind kind, String source,
            DelegationInput basis, String revisionId, String key, String actionId, String applicationId,
            String keyword, List<String> ids, int page, int size, List<BigInteger> visibility) {
        return query(actor, kind, source, basis, revisionId, key, actionId, applicationId, keyword, ids,
                page, size, visibility, false, null);
    }

    private AuthorizationCandidatePage query(ActiveIdentity actor, AuthorizationCandidateKind kind, String source,
            DelegationInput basis, String revisionId, String key, String actionId, String applicationId,
            String keyword, List<String> ids, int page, int size, List<BigInteger> visibility,
            boolean tree, String parentId) {
        return query(actor, kind, source, basis, revisionId, key, actionId, applicationId, keyword, ids,
                page, size, visibility, tree, parentId, null, null);
    }

    private AuthorizationCandidatePage query(ActiveIdentity actor, AuthorizationCandidateKind kind, String source,
            DelegationInput basis, String revisionId, String key, String actionId, String applicationId,
            String keyword, List<String> ids, int page, int size, List<BigInteger> visibility,
            boolean tree, String parentId, String excludeMemberId, String selectedDelegationId) {
        return query(actor, kind, source, basis, revisionId, key, actionId, applicationId, keyword, ids,
                page, size, visibility, tree, parentId, excludeMemberId, selectedDelegationId, null);
    }

    private AuthorizationCandidatePage query(ActiveIdentity actor, AuthorizationCandidateKind kind, String source,
            DelegationInput basis, String revisionId, String key, String actionId, String applicationId,
            String keyword, List<String> ids, int page, int size, List<BigInteger> visibility,
            boolean tree, String parentId, String excludeMemberId, String selectedDelegationId,
            String selectedAssignmentId) {
        IamPages.require(page, size);
        List<BigInteger> selected = ids == null ? List.of() : ids.stream().map(value ->
                BigInteger.valueOf(IamIds.require(value))).distinct().toList();
        if (selected.size() > IamPages.MAX_SIZE) { throw new BizException(IamReasonCode.INVALID_ARGUMENT); }
        List<BigInteger> allowed = visibility;
        if (basis != null && kind == AuthorizationCandidateKind.ROLE_REVISION) {
            allowed = basis.allowedRoleRevisionRefs().stream().map(ref -> new BigInteger(ref.id())).toList();
        } else if (basis != null && kind == AuthorizationCandidateKind.MEMBER) {
            String administratorId = basis.administratorMemberId();
            allowed = basis.recipientSelection().members().stream().filter(id -> !id.equals(administratorId))
                    .map(BigInteger::new).toList();
        }
        String resource = null;
        if (kind == AuthorizationCandidateKind.OBJECT) {
            var actions = objectActions(revisionId, key, actionId, basis);
            PlatformScopeObjectResource adapter = objectResource(actions);
            if (adapter == null) { return unsupported(page, size); }
            resource = adapter.getValue();
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
        boolean hierarchical = kind == AuthorizationCandidateKind.OBJECT
                && PlatformScopeObjectResource.find(resource) == PlatformScopeObjectResource.MENU;
        List<BigInteger> navigationIds = allowed;
        if (hierarchical && tree && selected.isEmpty() && "%".equals(search)
                && allowed != null && !allowed.isEmpty()) {
            navigationIds = candidates.menuAncestorIds(allowed);
        }
        var query = new AuthorizationCandidateSql.Query(kind, new BigInteger(actor.context().memberId()),
                source == null || source.isBlank() ? null : BigInteger.valueOf(IamIds.require(source)),
                applicationId == null || applicationId.isBlank() ? null : BigInteger.valueOf(IamIds.require(applicationId)),
                resource, search, selected, navigationIds, Math.multiplyExact(page - 1, size), size,
                hierarchical && tree && selectedDelegationId == null, parentId == null || parentId.isBlank() ? null
                    : BigInteger.valueOf(IamIds.require(parentId)),
                excludeMemberId == null || excludeMemberId.isBlank() ? null : BigInteger.valueOf(IamIds.require(excludeMemberId)),
                selectedDelegationId == null ? null : BigInteger.valueOf(IamIds.require(selectedDelegationId)),
                actionId == null || actionId.isBlank() ? null : BigInteger.valueOf(IamIds.require(actionId)),
                selectedAssignmentId == null ? null : BigInteger.valueOf(IamIds.require(selectedAssignmentId)), key);
        var rows = candidates.page(query);
        Map<BigInteger, String> paths = hierarchical && !rows.isEmpty()
                ? candidates.menuPaths(rows.stream().map(
                        com.ingot.cloud.iam.persistence.projection.AuthorizationCandidateRow::id).toList())
                    .stream().collect(java.util.stream.Collectors.toMap(
                            AuthorizationCandidateMapper.TreePath::leafId,
                            AuthorizationCandidateMapper.TreePath::ancestorPath))
                : Map.of();
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
        List<BigInteger> selectableIds = allowed;
        List<AuthorizationOption> options = rows.stream().map(row -> {
            String id = row.id().toString();
            if (kind == AuthorizationCandidateKind.DELEGATION) {
                DelegationInput input = source(actor, sources.get(row.id()), children.get(row.id()), permissions);
                return new AuthorizationOption(id, "委派 " + id, "允许 " + input.allowedRoleRevisionRefs().size()
                        + " 个固定版本 / " + input.recipientSelection().members().size() + " 名接收成员 / 最长 "
                        + (input.assignmentDurationMode() == AssignmentDurationMode.UNLIMITED ? "不限期限" : input.maxAssignmentDuration()), null, null, null, null, input);
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
            return new AuthorizationOption(id, row.name(),
                    kind == AuthorizationCandidateKind.ACTION ? row.resourceName() : null,
                    null, null, null, null, null,
                    row.parentId() == null ? null : row.parentId().toString(), row.hasChildren(),
                    paths.get(row.id()), !hierarchical || selectableIds == null
                            || selectableIds.contains(row.id()));
        }).toList();
        return new AuthorizationCandidatePage(options, candidates.count(query), page, size, true, null,
                null, hierarchical);
    }

    private PlatformScopeObjectResource objectResource(List<AuthorizationCandidateMapper.ActionRow> actions) {
        // 只信任关联目录元数据，整组操作必须指向同一已接入资源。
        PlatformScopeObjectResource resource = null;
        for (var action : actions) {
            if (!CORE_APPLICATION.equals(action.applicationCode())) { return null; }
            PlatformScopeObjectResource current = PlatformScopeObjectResource.find(action.resourceCode());
            if (current == null || resource != null && resource != current) { return null; }
            resource = current;
        }
        return resource;
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
        if (rows.size() != actions.size() || rows.stream().map(AuthorizationCandidateMapper.ActionRow::applicationId)
                .distinct().count() != 1 || rows.stream().map(AuthorizationCandidateMapper.ActionRow::resourceId)
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
        if (bindings.size() != definitions.size()
                || definitions.stream().anyMatch(parameter -> !bindings.containsKey(parameter.getParameterKey()))) {
            return List.of(new ValidationIssue("scopeBindings", IamReasonCode.INVALID_ARGUMENT,
                    "固定角色版本的范围参数必须全部配置"));
        }
        for (var entry : bindings.entrySet()) {
            var parameter = definitions.stream().filter(value -> value.getParameterKey().equals(entry.getKey())).findFirst().orElse(null);
            if (parameter == null || entry.getValue().kind() != parameter.getBindingKind()) {
                return List.of(new ValidationIssue("scopeBindings", IamReasonCode.INVALID_ARGUMENT, "参数未声明或类型不匹配"));
            }
            var resource = objectResource(objectActions(input.roleRevisionRef().id(), entry.getKey(), null, null));
            if (entry.getValue() == null || entry.getValue().ids() == null
                    || entry.getValue().ids().isEmpty() || !objectsExist(resource, entry.getValue())) {
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
        var resource = objectResource(objectActions(null, null, ceiling.actionId(), null));
        return ceiling.scopeBindings().values().stream().allMatch(value -> objectsExist(resource, value));
    }

    private boolean objectsExist(PlatformScopeObjectResource resource, ScopeBinding binding) {
        if (resource == null || binding.kind() != ScopeBindingKind.OBJECTS) { return false; }
        if (binding.ids().isEmpty()) { return true; }
        List<BigInteger> ids = binding.ids().stream().map(value -> BigInteger.valueOf(IamIds.require(value))).distinct().toList();
        var query = new AuthorizationCandidateSql.Query(AuthorizationCandidateKind.OBJECT, null, null,
                null, resource.getValue(), "%", ids, null, 0, IamPages.DEFAULT_SIZE);
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
        return source(actor, source, children, permissions, false);
    }

    private DelegationInput source(ActiveIdentity actor, IamDelegationGrantEntity source,
            DelegationRepository.Children children, IamCapabilities permissions, boolean visibleRecord) {
        Instant now = Instant.now();
        if (source == null || source.getPlatformAdministratorId() == null
                || !source.getPlatformAdministratorId().toString().equals(actor.context().memberId())
                    && !permissions.allows(IamAction.PLATFORM_ASSIGNMENT_UPDATE, true) && !visibleRecord
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
                source.getAssignmentDurationMode() == AssignmentDurationMode.UNLIMITED ? null
                    : Duration.ofSeconds(source.getMaxAssignmentDurationSeconds(), source.getMaxAssignmentDurationNanos()),
                source.getAssignmentDurationMode());
    }

    private static AuthorizationCandidatePage unsupported(int page, int size) {
        return new AuthorizationCandidatePage(List.of(), 0, page, size, false, "该资源暂未接入范围对象查询，不能配置指定对象");
    }
}
