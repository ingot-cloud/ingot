package com.ingot.cloud.iam.evaluation;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.authorization.IamActionAuthorizer;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.cloud.iam.role.RoleRevisionSnapshot;
import com.ingot.cloud.iam.role.RoleSynthesis;
import com.ingot.cloud.iam.role.RoleSynthesisCache;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.ActionScopeCeiling;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.RoleDelta;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * <p>按身份、开通、人群、组展开和角色合成求值精确 ACTION，失败关闭且不使用过期放行。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Primary
@Service
public class AuthorizationEvaluator implements IamActionAuthorizer {
    private static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() {
    };
    private static final String KEY_SEPARATOR = ":";
    private static final String PLATFORM_TENANT = "0";
    /**
     * 授权视图最长热窗口，与 {@link IamAuthorizationCacheProperties} 的 TTL 上限一致。
     */
    private static final Duration MAX_HOT_WINDOW = Duration.ofSeconds(30);
    private final AuthorizationEvaluationRepository evaluations;
    private final ObjectProvider<LayeredCache<String, AuthorizationView>> cache;
    private final ObjectProvider<RoleSynthesisCache> synthesis;

    /**
     * 绑定求值查询、授权热缓存与角色合成派生缓存。
     * <p>保留显式构造器并标注 {@code @Autowired}，因为 Lombok 不会复制参数上的 {@link Qualifier}。</p>
     *
     * @param evaluations 授权求值查询
     * @param cache 授权视图缓存；测试可空
     * @param synthesis 角色合成派生缓存；测试可空
     */
    @Autowired
    public AuthorizationEvaluator(AuthorizationEvaluationRepository evaluations,
                                  @Qualifier(AuthorizationCacheConfiguration.CACHE_BEAN_NAME)
                                  ObjectProvider<LayeredCache<String, AuthorizationView>> cache,
                                  ObjectProvider<RoleSynthesisCache> synthesis) {
        this.evaluations = evaluations;
        this.cache = cache;
        this.synthesis = synthesis;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Admission admit(AuthorizationContext actor, IamAction action) {
        return new Admission(authorized(actor, action).governedCodes().contains(action.getCode()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<IamAction, Admission> capabilities(AuthorizationContext actor, Collection<IamAction> actions) {
        if (actor == null) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        if (actions.isEmpty()) {
            return Map.of();
        }
        // 展示资格只读取一次最新事实，绝不缓存或复用于后续写命令。
        AuthorizationView view = evaluateRaw(actor);
        Set<String> allowed = new LinkedHashSet<>(view.actionCodes());
        Set<String> governed = new LinkedHashSet<>(view.governedCodes());
        Map<IamAction, Admission> result = new LinkedHashMap<>();
        for (IamAction action : actions) {
            if (allowed.contains(action.getCode())) {
                result.put(action, new Admission(governed.contains(action.getCode())));
            }
        }
        return Map.copyOf(result);
    }

    /**
     * 读取指定操作的已求值范围；无操作时拒绝，有操作但无对象时返回空范围。
     *
     * @param actor 已验证身份
     * @param action 精确 ACTION
     * @return 范围并集
     */
    public ResolvedActionScope scope(AuthorizationContext actor, IamAction action) {
        return authorized(actor, action).scope(action.getCode());
    }

    /**
     * 校验授权并返回本次判定所用的同一份快照，避免准入与范围来自不同版本。
     */
    private AuthorizationView authorized(AuthorizationContext actor, IamAction action) {
        if (actor == null || action == null) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        try {
            // 改写状态的操作按最新事实求值，不接受任何热缓存快照放行。
            AuthorizationView view = evaluateForExecution(actor, action.getOperation().isMutating());
            if (!view.actionCodes().contains(action.getCode())) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
            return view;
        } catch (BizException exception) {
            throw exception;
        } catch (DataAccessException | PersistenceException exception) {
            var failure = new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
            failure.initCause(exception);
            throw failure;
        }
    }

    /**
     * 计算当前身份的有效精确操作集合，优先命中分层缓存。
     *
     * @param actor 已验证身份
     * @return 操作码、范围与版本
     */
    public AuthorizationView evaluateForExecution(AuthorizationContext actor, boolean mutating) {
        return mutating ? evaluateRaw(actor) : evaluate(actor);
    }

    /** 读取未过期授权视图。 @param actor 可信身份 @return 授权视图 */
    public AuthorizationView evaluate(AuthorizationContext actor) {
        if (actor == null) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        LayeredCache<String, AuthorizationView> layered = cache == null ? null : cache.getIfAvailable();
        String key = cacheKey(actor);
        if (layered == null) {
            return evaluateRaw(key);
        }
        AuthorizationView view = layered.get(key);
        if (view != null && !view.expired(Instant.now())) {
            return view;
        }
        // 命中已过期快照时清掉各层再重载，L2 回填 L1 不得为过期视图续命。
        layered.evict(key);
        return layered.get(key);
    }

    /**
     * 按缓存键从数据库求值，不读热缓存。
     *
     * @param cacheKey {@link #cacheKey(AuthorizationContext)} 生成的键
     * @return 操作码与版本
     */
    public AuthorizationView evaluateRaw(String cacheKey) {
        return evaluateRaw(contextFromKey(cacheKey));
    }

    /**
     * 生成授权视图缓存键。
     *
     * @param actor 已验证身份
     * @return 域、租户与成员组成的键
     */
    public static String cacheKey(AuthorizationContext actor) {
        String tenant = actor.tenantId() == null || actor.tenantId().isBlank() ? PLATFORM_TENANT : actor.tenantId();
        return actor.domain().name() + KEY_SEPARATOR + tenant + KEY_SEPARATOR + actor.memberId();
    }

    /** {@inheritDoc} */
    @Override
    public boolean platformAdministrator(AuthorizationContext actor) {
        if (actor == null || actor.domain() != AuthorizationDomain.PLATFORM) return false;
        try {
            return !evaluations.platformAdministrators(actor.memberId()).isEmpty();
        } catch (DataAccessException | PersistenceException failure) {
            throw new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
        }
    }

    private AuthorizationView administratorView(AuthorizationContext actor,
            List<AuthorizationEvalRows.Assignment> assignments) {
        var actions = evaluations.enabledPlatformActions();
        var codes = actions.stream().map(AuthorizationEvalRows.IndexedAction::code).distinct().toList();
        Map<String, ResolvedActionScope> scopes = new LinkedHashMap<>();
        codes.forEach(code -> scopes.put(code, new ResolvedActionScope(List.of(ScopeClause.universe()))));
        Deadline deadline = new Deadline();
        assignments.forEach(row -> deadline.merge(row.validUntil()));
        String version = "administrator" + KEY_SEPARATOR + assignments.stream()
                .map(row -> row.assignmentId() + KEY_SEPARATOR + row.revisionId()).collect(java.util.stream.Collectors.joining(KEY_SEPARATOR))
                + KEY_SEPARATOR + Integer.toUnsignedString(actions.hashCode());
        return new AuthorizationView(codes, codes, scopes, version, deadline.expiresAt(Instant.now()), List.of(), true);
    }

    private AuthorizationView evaluateRaw(AuthorizationContext actor) {
        try {
            if (actor.domain() == AuthorizationDomain.PLATFORM) {
                var administrators = evaluations.platformAdministrators(actor.memberId());
                if (!administrators.isEmpty()) return administratorView(actor, administrators);
            }
            List<AssignmentEval> assignments = new ArrayList<>();
            assignments.addAll(directAssignments(actor));
            assignments.addAll(groupAssignments(actor));
            Set<String> codes = new LinkedHashSet<>();
            Set<String> governed = new LinkedHashSet<>();
            Map<String, List<ScopeClause>> scopes = new LinkedHashMap<>();
            Set<String> sources = new LinkedHashSet<>();
            List<RoleFieldSource> fieldSources = new ArrayList<>();
            EvaluationScope scope = new EvaluationScope();
            Deadline deadline = scope.deadline;
            Map<Long, List<ActionGrant>> revisionGrants = new HashMap<>();
            Set<BigInteger> actionIds = new LinkedHashSet<>();
            for (AssignmentEval assignment : assignments) {
                List<ActionGrant> grants = revisionGrants.computeIfAbsent(assignment.revisionId(), this::synthesized);
                grants.forEach(grant -> actionIds.add(new BigInteger(grant.actionId())));
            }
            for (AuthorizationEvalRows.IndexedAction action : evaluations.listActionsByIds(actionIds)) {
                scope.actions.put(action.id(), action);
            }
            Map<Long, Map<String, ActionScopeCeiling>> delegationCeilings = new HashMap<>();
            for (AssignmentEval assignment : assignments) {
                List<ActionGrant> grants = revisionGrants.get(assignment.revisionId());
                sources.add(Long.toString(assignment.revisionId()));
                Map<String, ActionScopeCeiling> ceilings = assignment.delegationId() == null
                        ? Map.of() : delegationCeilings.computeIfAbsent(assignment.delegationId(), this::ceilings);
                boolean delegated = assignment.delegationId() != null;
                for (ActionGrant grant : grants) {
                    String code = actionCode(actor, new BigInteger(grant.actionId()), scope);
                    if (code == null) {
                        continue;
                    }
                    // 只有真正贡献了操作的分配才收紧热窗口。
                    deadline.merge(assignment.validUntil());
                    deadline.merge(assignment.delegationValidUntil());
                    ActionScopeCeiling ceiling = delegated ? ceilings.get(grant.actionId()) : null;
                    if (delegated && ceiling == null) {
                        continue;
                    }
                    List<ScopeClause> clauses = ScopeBinder.constrain(
                            ScopeBinder.bind(grant, assignment.bindings()), ceiling);
                    var contributed = clauses.stream().filter(clause -> !clause.empty()).toList();
                    fieldSources.add(new RoleFieldSource(assignment.assignmentId(), assignment.groupId(),
                            assignment.delegationId(), assignment.revisionId(), code, contributed));
                    codes.add(code);
                    if (!delegated) {
                        // 非委派来源即完整治理资格，受限方只能凭自己的委派派生授权。
                        governed.add(code);
                    }
                    scopes.computeIfAbsent(code, key -> new ArrayList<>()).addAll(clauses.stream()
                            .filter(clause -> !clause.empty()).toList());
                }
            }
            // 委派仅补充分配入口，绝不向业务范围或完整治理资格集合写入。
            var effectiveDelegations = evaluations.effectiveDelegations(actor);
            if (!effectiveDelegations.isEmpty()) {
                codes.addAll(evaluations.enabledPlatformEntries(List.of(IamAction.PLATFORM_ASSIGNMENT_READ.getCode(),
                        IamAction.PLATFORM_ASSIGNMENT_CREATE.getCode(), IamAction.PLATFORM_ASSIGNMENT_UPDATE.getCode(),
                        IamAction.PLATFORM_ASSIGNMENT_DELETE.getCode(), IamAction.PLATFORM_ASSIGNMENT_UPGRADE.getCode())));
            }
            for (var delegation : effectiveDelegations) {
                sources.add("delegation" + KEY_SEPARATOR + delegation.getId() + KEY_SEPARATOR + delegation.getVersion());
                deadline.merge(delegation.getValidUntil());
            }
            Map<String, ResolvedActionScope> resolved = new LinkedHashMap<>();
            for (String code : codes) {
                resolved.put(code, new ResolvedActionScope(scopes.getOrDefault(code, List.of())));
            }
            String version = sources.isEmpty() ? "0" : String.join(KEY_SEPARATOR, sources);
            return new AuthorizationView(List.copyOf(codes), List.copyOf(governed), Map.copyOf(resolved), version,
                    deadline.expiresAt(Instant.now()), fieldSources);
        } catch (DataAccessException | PersistenceException exception) {
            var failure = new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
            failure.initCause(exception);
            throw failure;
        }
    }

    /**
     * 展开真实有效分配来源及该操作范围，不伪造来源记录。
     * @param actor 被诊断的可信域成员
     * @param actionId 已验证操作
     * @return 操作对应的真实来源，披露边界由诊断入口再次过滤
     */
    public List<com.ingot.framework.commons.model.iam.DecisionSource> decisionSources(AuthorizationContext actor,
                                                                                    String actionId) {
        if (actor.domain() == AuthorizationDomain.PLATFORM) {
            var administrators = evaluations.platformAdministrators(actor.memberId());
            if (!administrators.isEmpty() && evaluations.enabledPlatformActions().stream()
                    .anyMatch(action -> action.id().toString().equals(actionId))) {
                return administrators.stream().map(row -> new com.ingot.framework.commons.model.iam.DecisionSource(
                        row.assignmentId().toString(), null,
                        new com.ingot.framework.commons.model.iam.RoleRevisionRef(com.ingot.framework.commons.model.iam.RoleKind.SYSTEM,
                                row.revisionId().toString()),
                        "系统超级管理员 / 分配 " + row.assignmentId() + " / 全部对象 / 注册业务字段完整可见", null)).toList();
            }
        }
        List<AuthorizationEvalRows.Assignment> rows = new ArrayList<>(evaluations.listDirectAssignments(actor));
        rows.addAll(evaluations.listGroupAssignments(actor));
        rows.removeIf(row -> invalidPlatformSystemSource(actor, row));
        var fieldVersions = actor.domain() == AuthorizationDomain.PLATFORM
                ? evaluations.fieldVersions(rows.stream().map(AuthorizationEvalRows.Assignment::revisionId).distinct().toList())
                        .stream().collect(java.util.stream.Collectors.toMap(
                                com.ingot.cloud.iam.persistence.entity.IamRoleRevisionEntity::getId, v -> v))
                : Map.<BigInteger, com.ingot.cloud.iam.persistence.entity.IamRoleRevisionEntity>of();
        List<com.ingot.framework.commons.model.iam.DecisionSource> result = new ArrayList<>();
        for (var row : rows) {
            for (ActionGrant grant : synthesized(row.revisionId().longValueExact())) {
                if (!grant.actionId().equals(actionId) || row.assignmentId() == null) { continue; }
                var ceiling = row.delegationGrantId() == null ? null
                        : ceilings(row.delegationGrantId().longValueExact()).get(actionId);
                if (row.delegationGrantId() != null && ceiling == null) { continue; }
                var bound = ScopeBinder.constrain(ScopeBinder.bind(grant, bindings(row.scopeBindings())), ceiling);
                String ranges = bound.isEmpty() ? "无对象范围" : bound.stream().map(clause -> clause.all() ? "全部"
                        : clause.self() ? "本人" : !clause.objectIds().isEmpty() ? "指定对象 " + clause.objectIds().size() + " 个"
                        : "部门范围").distinct().collect(java.util.stream.Collectors.joining("、"));
                String summary = "分配 " + row.assignmentId() + (row.groupId() == null ? "（直接成员）" : "（用户组 " + row.groupId() + "）")
                        + " / 固定版本 " + row.revisionId() + (row.delegationGrantId() == null ? " / 直接授权" : " / 来源委派 " + row.delegationGrantId())
                        + " / " + ranges;
                result.add(new com.ingot.framework.commons.model.iam.DecisionSource(row.assignmentId().toString(),
                        row.delegationGrantId() == null ? null : row.delegationGrantId().toString(),
                        row.revisionKind() == null ? null : new com.ingot.framework.commons.model.iam.RoleRevisionRef(
                                row.revisionKind(), row.revisionId().toString()), summary,
                        fieldVersions.containsKey(row.revisionId()) ? com.ingot.cloud.iam.extension.RoleFieldPermissionService
                                .snapshot(fieldVersions.get(row.revisionId()).getResourceFieldPermissions()) : null));
            }
        }
        return List.copyOf(result);
    }

    private static AuthorizationContext contextFromKey(String cacheKey) {
        String[] parts = cacheKey.split(KEY_SEPARATOR, 3);
        if (parts.length != 3) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        AuthorizationDomain domain = AuthorizationDomain.valueOf(parts[0]);
        String tenantId = PLATFORM_TENANT.equals(parts[1]) ? null : parts[1];
        return new AuthorizationContext(domain, tenantId, "0", parts[2]);
    }

    private List<AssignmentEval> directAssignments(AuthorizationContext actor) {
        return mapAssignments(actor, evaluations.listDirectAssignments(actor));
    }

    private List<AssignmentEval> groupAssignments(AuthorizationContext actor) {
        return mapAssignments(actor, evaluations.listGroupAssignments(actor));
    }

    private List<AssignmentEval> mapAssignments(AuthorizationContext actor, List<AuthorizationEvalRows.Assignment> rows) {
        List<AssignmentEval> result = new ArrayList<>(rows.size());
        for (AuthorizationEvalRows.Assignment row : rows) {
            // 平台SYSTEM仅由前面的可信直接超管分支贡献权限，不作为普通组或委派角色降级生效。
            if (invalidPlatformSystemSource(actor, row)) continue;
            result.add(new AssignmentEval(row.assignmentId(), row.groupId(), row.revisionId().longValueExact(), bindings(row.scopeBindings()),
                    row.delegationGrantId() == null ? null : row.delegationGrantId().longValueExact(),
                    row.validUntil(), row.delegationValidUntil()));
        }
        return result;
    }

    private static boolean invalidPlatformSystemSource(AuthorizationContext actor, AuthorizationEvalRows.Assignment row) {
        return actor.domain() == AuthorizationDomain.PLATFORM
                && row.revisionKind() == com.ingot.framework.commons.model.iam.RoleKind.SYSTEM;
    }

    private Map<String, ScopeBinding> bindings(String json) {
        Map<String, ScopeBinding> bindings = IamJson.read(json, BINDINGS);
        return bindings == null ? Map.of() : bindings;
    }

    private Map<String, ActionScopeCeiling> ceilings(long delegationId) {
        Map<String, ActionScopeCeiling> ceilings = new LinkedHashMap<>();
        for (AuthorizationEvalRows.Ceiling row : evaluations.listCeilings(BigInteger.valueOf(delegationId))) {
            String actionId = row.actionId().toString();
            List<ScopeExpression> scopes = IamJson.read(row.scopes(), SCOPES);
            ceilings.put(actionId, new ActionScopeCeiling(actionId, scopes == null ? List.of() : scopes,
                    bindings(row.scopeBindings())));
        }
        return ceilings;
    }

    private List<ActionGrant> synthesized(long revisionId) {
        RoleRevisionSnapshot snapshot = snapshot(revisionId);
        if (snapshot == null) {
            return List.of();
        }
        RoleSynthesisCache derived = synthesis == null ? null : synthesis.getIfAvailable();
        if (derived != null) {
            return derived.grants(snapshot);
        }
        return RoleSynthesis.synthesize(snapshot.baseGrants(), snapshot.deltas()).grants();
    }

    private RoleRevisionSnapshot snapshot(long revisionId) {
        List<AuthorizationEvalRows.Revision> rows = evaluations.listEnabledRevisions(BigInteger.valueOf(revisionId));
        if (rows.size() != 1) {
            return null;
        }
        AuthorizationEvalRows.Revision revision = rows.getFirst();
        if (revision.baseRevisionId() == null) {
            return new RoleRevisionSnapshot(revision.id().longValueExact(), null,
                    grants(revision.id()), List.of());
        }
        return new RoleRevisionSnapshot(revision.id().longValueExact(), revision.baseRevisionId().longValueExact(),
                grants(revision.baseRevisionId()), deltas(revision.id()));
    }

    private List<ActionGrant> grants(BigInteger revisionId) {
        List<ActionGrant> result = new ArrayList<>();
        for (AuthorizationEvalRows.Grant row : evaluations.listGrants(revisionId)) {
            List<ScopeExpression> scopes = IamJson.read(row.scopes(), SCOPES);
            result.add(new ActionGrant(row.actionId().toString(), scopes == null ? List.of() : scopes));
        }
        return result;
    }

    private List<RoleDelta> deltas(BigInteger revisionId) {
        List<RoleDelta> result = new ArrayList<>();
        for (AuthorizationEvalRows.Delta row : evaluations.listDeltas(revisionId)) {
            result.add(new RoleDelta(row.actionId().toString(), row.operation(),
                    IamJson.read(row.scopes(), SCOPES)));
        }
        return result;
    }

    private String actionCode(AuthorizationContext actor, BigInteger actionId, EvaluationScope scope) {
        AuthorizationEvalRows.IndexedAction action = scope.actions.get(actionId);
        if (action == null) {
            return null;
        }
        if (!Boolean.TRUE.equals(action.enabled()) || !Boolean.TRUE.equals(action.appEnabled())) {
            return null;
        }
        if (actor.domain() == AuthorizationDomain.TENANT && action.domain() == AuthorizationDomain.TENANT) {
            AuthorizationEvalRows.Entitlement entitlement = scope.entitlements.computeIfAbsent(
                    action.applicationId(), applicationId -> evaluations.entitlement(
                            new BigInteger(actor.tenantId()), applicationId, new BigInteger(actor.memberId())));
            if (entitlement.hits() <= 0) {
                return null;
            }
            scope.deadline.merge(entitlement.earliestExpiry());
        }
        return action.code();
    }

    /**
     * <p>保存一次求值得到的精确操作、范围并集与截止时间。</p>
     *
     * @param actionCodes 有效操作码
     * @param governedCodes 至少来自一条非委派授权的操作码，即操作者的完整治理资格
     * @param scopes 各操作的范围并集；缺省键表示有操作无对象
     * @param platformAdministrator 服务器有效系统直接分配产生的特权
     * @param version 参与求值的版本指纹
     * @param expiresAt 热缓存截止；取热窗口与最近授权/委派/开通边界的较早者
     * @author jy
     * @since 1.0.0
     */
    public record AuthorizationView(List<String> actionCodes, List<String> governedCodes,
                                    Map<String, ResolvedActionScope> scopes, String version, Instant expiresAt, List<RoleFieldSource> fieldSources, boolean platformAdministrator) {
        /** 兼容旧授权快照；新模型不据此扩权。 */
        public AuthorizationView(List<String> actionCodes, List<String> governedCodes,
                Map<String, ResolvedActionScope> scopes, String version, Instant expiresAt) {
            this(actionCodes, governedCodes, scopes, version, expiresAt, List.of(), false);
        }
        /** 普通角色快照不产生系统特权。 */
        public AuthorizationView(List<String> actionCodes, List<String> governedCodes,
                Map<String, ResolvedActionScope> scopes, String version, Instant expiresAt, List<RoleFieldSource> fieldSources) {
            this(actionCodes, governedCodes, scopes, version, expiresAt, fieldSources, false);
        }
        /**
         * 复制操作与范围集合。
         */
        public AuthorizationView {
            fieldSources = fieldSources == null ? List.of() : List.copyOf(fieldSources);
            actionCodes = actionCodes == null ? List.of() : List.copyOf(actionCodes);
            governedCodes = governedCodes == null ? List.of() : List.copyOf(governedCodes);
            scopes = scopes == null ? Map.of() : Map.copyOf(scopes);
        }

        /**
         * 判断快照在给定时刻是否已过期。
         *
         * @param at 判定时刻
         * @return 已到或已过截止时为 {@code true}；无截止的视图视为过期
         */
        public boolean expired(Instant at) {
            return expiresAt == null || !at.isBefore(expiresAt);
        }

        /**
         * 读取指定操作范围；操作不存在时为空。
         *
         * @param actionCode 精确操作码
         * @return 范围；无此操作时为空范围
         */
        public ResolvedActionScope scope(String actionCode) {
            if (actionCode == null || !actionCodes.contains(actionCode)) {
                return ResolvedActionScope.empty();
            }
            ResolvedActionScope resolved = scopes.get(actionCode);
            return resolved == null ? ResolvedActionScope.empty() : resolved;
        }
    }

    /**
     * <p>一条有效分配对精确操作的字段贡献，范围已经施加来源委派上限。</p>
     * @param assignmentId 真实分配
     * @param groupId 组来源
     * @param delegationId 委派来源
     * @param revisionId 固定角色版本
     * @param actionCode 精确操作码
     * @param clauses 本贡献独立范围
     * @author jy
     * @since 1.0.0
     */
    public record RoleFieldSource(BigInteger assignmentId, BigInteger groupId, Long delegationId,
            long revisionId, String actionCode, List<ScopeClause> clauses) {
        /** 复制贡献范围。 */
        public RoleFieldSource { clauses = List.copyOf(clauses); }
    }

    private record AssignmentEval(BigInteger assignmentId, BigInteger groupId, long revisionId, Map<String, ScopeBinding> bindings, Long delegationId,
                                  LocalDateTime validUntil, LocalDateTime delegationValidUntil) {
    }

    /**
     * 单次求值内共享的期限与查询结果。同一操作或应用在多条分配上重复出现时只查一次，
     * 组与人群的部门展开代价不随授权条数放大。
     */
    private static final class EvaluationScope {
        private final Deadline deadline = new Deadline();
        private final Map<BigInteger, AuthorizationEvalRows.IndexedAction> actions = new HashMap<>();
        private final Map<BigInteger, AuthorizationEvalRows.Entitlement> entitlements = new HashMap<>();
    }

    /**
     * 收敛一次求值涉及的最近有效期边界。数据库会话为 UTC，边界按 UTC 解释。
     */
    private static final class Deadline {
        private Instant earliest;

        private void merge(LocalDateTime boundary) {
            if (boundary == null) {
                return;
            }
            Instant candidate = boundary.toInstant(ZoneOffset.UTC);
            if (earliest == null || candidate.isBefore(earliest)) {
                earliest = candidate;
            }
        }

        private Instant expiresAt(Instant now) {
            Instant window = now.plus(MAX_HOT_WINDOW);
            return earliest == null || window.isBefore(earliest) ? window : earliest;
        }
    }
}
