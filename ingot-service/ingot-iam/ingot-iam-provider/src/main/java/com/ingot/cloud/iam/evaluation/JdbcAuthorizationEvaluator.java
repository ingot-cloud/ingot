package com.ingot.cloud.iam.evaluation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ingot.cloud.iam.authorization.IamActionAuthorizer;
import com.ingot.cloud.iam.role.RoleRevisionSnapshot;
import com.ingot.cloud.iam.role.RoleSynthesis;
import com.ingot.cloud.iam.role.RoleSynthesisCache;
import com.ingot.cloud.iam.support.IamJson;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.ActionScopeCeiling;
import com.ingot.framework.commons.model.iam.AudienceKind;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.GrantStatus;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.RoleDelta;
import com.ingot.framework.commons.model.iam.RoleDeltaOperation;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.SubjectType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>按身份、开通、人群、组展开和角色合成求值精确 ACTION，失败关闭且不使用过期放行。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Primary
@Service
public class JdbcAuthorizationEvaluator implements IamActionAuthorizer {
    private static final TypeReference<List<ScopeExpression>> SCOPES = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, ScopeBinding>> BINDINGS = new TypeReference<>() {
    };
    private static final String KEY_SEPARATOR = ":";
    private static final String PLATFORM_TENANT = "0";
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectProvider<LayeredCache<String, AuthorizationView>> cache;
    private final ObjectProvider<RoleSynthesisCache> synthesis;

    /**
     * 仅绑定 IAM 目标库，供无缓存的单元测试直接构造。
     *
     * @param dataSource 独立 IAM 数据源
     */
    public JdbcAuthorizationEvaluator(DataSource dataSource) {
        this(dataSource, null, null);
    }

    /**
     * 绑定 IAM 目标库与可选分层缓存。
     *
     * @param dataSource 独立 IAM 数据源
     * @param cache 授权视图缓存；测试可空
     */
    public JdbcAuthorizationEvaluator(DataSource dataSource,
                                      ObjectProvider<LayeredCache<String, AuthorizationView>> cache) {
        this(dataSource, cache, null);
    }

    /**
     * 绑定 IAM 目标库、授权热缓存与角色合成派生缓存。
     *
     * @param dataSource 独立 IAM 数据源
     * @param cache 授权视图缓存；测试可空
     * @param synthesis 角色合成派生缓存；测试可空
     */
    @Autowired
    public JdbcAuthorizationEvaluator(DataSource dataSource,
                                      @Qualifier(AuthorizationCacheConfiguration.CACHE_BEAN_NAME)
                                      ObjectProvider<LayeredCache<String, AuthorizationView>> cache,
                                      ObjectProvider<RoleSynthesisCache> synthesis) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
        this.cache = cache;
        this.synthesis = synthesis;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void require(AuthorizationContext actor, IamAction action) {
        if (actor == null || action == null) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        try {
            if (!evaluate(actor).actionCodes().contains(action.getCode())) {
                throw new BizException(IamReasonCode.ACTION_DENIED);
            }
        } catch (BizException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            var failure = new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
            failure.initCause(exception);
            throw failure;
        }
    }

    /**
     * 读取指定操作的已求值范围；无操作时拒绝，有操作但无对象时返回空范围。
     *
     * @param actor 已验证身份
     * @param action 精确 ACTION
     * @return 范围并集
     */
    public ResolvedActionScope scope(AuthorizationContext actor, IamAction action) {
        require(actor, action);
        AuthorizationView view = evaluate(actor);
        ResolvedActionScope resolved = view.scopes().get(action.getCode());
        return resolved == null ? ResolvedActionScope.empty() : resolved;
    }

    /**
     * 计算当前身份的有效精确操作集合，优先命中分层缓存。
     *
     * @param actor 已验证身份
     * @return 操作码、范围与版本
     */
    public AuthorizationView evaluate(AuthorizationContext actor) {
        if (actor == null) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        LayeredCache<String, AuthorizationView> layered = cache == null ? null : cache.getIfAvailable();
        if (layered == null) {
            return evaluateRaw(cacheKey(actor));
        }
        return layered.get(cacheKey(actor));
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

    private AuthorizationView evaluateRaw(AuthorizationContext actor) {
        try {
            List<AssignmentEval> assignments = new ArrayList<>();
            assignments.addAll(directAssignments(actor));
            assignments.addAll(groupAssignments(actor));
            Set<String> codes = new LinkedHashSet<>();
            Map<String, List<ScopeClause>> scopes = new LinkedHashMap<>();
            Set<String> sources = new LinkedHashSet<>();
            for (AssignmentEval assignment : assignments) {
                List<ActionGrant> grants = synthesized(assignment.revisionId());
                sources.add(Long.toString(assignment.revisionId()));
                Map<String, ActionScopeCeiling> ceilings = assignment.delegationId() == null
                        ? Map.of() : ceilings(assignment.delegationId());
                boolean delegated = assignment.delegationId() != null;
                for (ActionGrant grant : grants) {
                    String code = actionCode(actor, Long.parseLong(grant.actionId()));
                    if (code == null) {
                        continue;
                    }
                    ActionScopeCeiling ceiling = delegated ? ceilings.get(grant.actionId()) : null;
                    if (delegated && ceiling == null) {
                        continue;
                    }
                    List<ScopeClause> clauses = ScopeBinder.constrain(
                            ScopeBinder.bind(grant, assignment.bindings()), ceiling);
                    codes.add(code);
                    scopes.computeIfAbsent(code, key -> new ArrayList<>()).addAll(clauses.stream()
                            .filter(clause -> !clause.empty()).toList());
                }
            }
            Map<String, ResolvedActionScope> resolved = new LinkedHashMap<>();
            for (String code : codes) {
                resolved.put(code, new ResolvedActionScope(scopes.getOrDefault(code, List.of())));
            }
            String version = sources.isEmpty() ? "0" : String.join(KEY_SEPARATOR, sources);
            return new AuthorizationView(List.copyOf(codes), Map.copyOf(resolved), version,
                    Instant.now().plusSeconds(30));
        } catch (DataAccessException exception) {
            var failure = new BizException(IamReasonCode.AUTHORIZATION_UNAVAILABLE);
            failure.initCause(exception);
            throw failure;
        }
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
        Map<String, Object> parameters = parameters(actor);
        String sql = actor.domain() == AuthorizationDomain.PLATFORM
                ? """
                SELECT ra.revision_id,ra.scope_bindings,ra.delegation_grant_id FROM iam_role_assignment ra
                 WHERE ra.domain=:domain AND ra.status=:active AND ra.subject_type=:member
                   AND ra.platform_member_id=:memberId AND ra.tenant_id IS NULL
                   AND ra.valid_from<=CURRENT_TIMESTAMP
                   AND (ra.valid_until IS NULL OR ra.valid_until>CURRENT_TIMESTAMP)
                   AND (ra.delegation_grant_id IS NULL OR EXISTS (
                        SELECT 1 FROM iam_delegation_grant d
                         WHERE d.id=ra.delegation_grant_id AND d.status=:active
                           AND (d.valid_from IS NULL OR d.valid_from<=CURRENT_TIMESTAMP)
                           AND (d.valid_until IS NULL OR d.valid_until>CURRENT_TIMESTAMP)))
                """
                : """
                SELECT ra.revision_id,ra.scope_bindings,ra.delegation_grant_id FROM iam_role_assignment ra
                 WHERE ra.domain=:domain AND ra.status=:active AND ra.subject_type=:member
                   AND ra.tenant_member_id=:memberId AND ra.tenant_id=:tenantId
                   AND ra.valid_from<=CURRENT_TIMESTAMP
                   AND (ra.valid_until IS NULL OR ra.valid_until>CURRENT_TIMESTAMP)
                   AND (ra.delegation_grant_id IS NULL OR EXISTS (
                        SELECT 1 FROM iam_delegation_grant d
                         WHERE d.id=ra.delegation_grant_id AND d.status=:active
                           AND (d.valid_from IS NULL OR d.valid_from<=CURRENT_TIMESTAMP)
                           AND (d.valid_until IS NULL OR d.valid_until>CURRENT_TIMESTAMP)))
                """;
        return assignments(sql, parameters);
    }

    private List<AssignmentEval> groupAssignments(AuthorizationContext actor) {
        Map<String, Object> parameters = parameters(actor);
        String sql = actor.domain() == AuthorizationDomain.PLATFORM
                ? """
                SELECT ra.revision_id,ra.scope_bindings,ra.delegation_grant_id FROM iam_role_assignment ra
                  JOIN iam_platform_group_member gm ON gm.group_id=ra.platform_group_id
                 WHERE ra.domain=:domain AND ra.status=:active AND ra.subject_type=:groupType
                   AND gm.member_id=:memberId AND ra.tenant_id IS NULL
                   AND ra.valid_from<=CURRENT_TIMESTAMP
                   AND (ra.valid_until IS NULL OR ra.valid_until>CURRENT_TIMESTAMP)
                   AND (ra.delegation_grant_id IS NULL OR EXISTS (
                        SELECT 1 FROM iam_delegation_grant d
                         WHERE d.id=ra.delegation_grant_id AND d.status=:active
                           AND (d.valid_from IS NULL OR d.valid_from<=CURRENT_TIMESTAMP)
                           AND (d.valid_until IS NULL OR d.valid_until>CURRENT_TIMESTAMP)))
                """
                : """
                SELECT ra.revision_id,ra.scope_bindings,ra.delegation_grant_id FROM iam_role_assignment ra
                  JOIN iam_tenant_group_member gm ON gm.group_id=ra.tenant_group_id AND gm.tenant_id=ra.tenant_id
                 WHERE ra.domain=:domain AND ra.status=:active AND ra.subject_type=:groupType
                   AND gm.member_id=:memberId AND ra.tenant_id=:tenantId
                   AND ra.valid_from<=CURRENT_TIMESTAMP
                   AND (ra.valid_until IS NULL OR ra.valid_until>CURRENT_TIMESTAMP)
                   AND (ra.delegation_grant_id IS NULL OR EXISTS (
                        SELECT 1 FROM iam_delegation_grant d
                         WHERE d.id=ra.delegation_grant_id AND d.status=:active
                           AND (d.valid_from IS NULL OR d.valid_from<=CURRENT_TIMESTAMP)
                           AND (d.valid_until IS NULL OR d.valid_until>CURRENT_TIMESTAMP)))
                """;
        parameters.put("groupType", SubjectType.GROUP.name());
        return assignments(sql, parameters);
    }

    private List<AssignmentEval> assignments(String sql, Map<String, Object> parameters) {
        return jdbc.query(sql, parameters, (row, index) -> new AssignmentEval(row.getLong("revision_id"),
                bindings(row.getString("scope_bindings")),
                row.getObject("delegation_grant_id") == null ? null : row.getLong("delegation_grant_id")));
    }

    private Map<String, ScopeBinding> bindings(String json) {
        Map<String, ScopeBinding> bindings = IamJson.read(json, BINDINGS);
        return bindings == null ? Map.of() : bindings;
    }

    private Map<String, ActionScopeCeiling> ceilings(long delegationId) {
        Map<String, ActionScopeCeiling> ceilings = new LinkedHashMap<>();
        jdbc.query("""
                SELECT action_id,scopes,scope_bindings FROM iam_delegation_action_ceiling
                 WHERE delegation_id=:id
                """, Map.of("id", delegationId), (row, index) -> {
            String actionId = row.getString("action_id");
            List<ScopeExpression> scopes = IamJson.read(row.getString("scopes"), SCOPES);
            Map<String, ScopeBinding> bindings = bindings(row.getString("scope_bindings"));
            ceilings.put(actionId, new ActionScopeCeiling(actionId, scopes == null ? List.of() : scopes, bindings));
            return null;
        });
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
        List<RevisionRow> rows = jdbc.query("""
                SELECT r.id,r.base_revision_id FROM iam_role_revision r
                  JOIN iam_role_definition d ON d.id=r.role_id
                 WHERE r.id=:id AND d.enabled=TRUE
                """, Map.of("id", revisionId),
                (row, index) -> new RevisionRow(row.getLong("id"), row.getObject("base_revision_id") == null
                        ? null : row.getLong("base_revision_id")));
        if (rows.size() != 1) {
            return null;
        }
        RevisionRow revision = rows.getFirst();
        if (revision.baseRevisionId() == null) {
            return new RoleRevisionSnapshot(revision.id(), null, grants(revision.id()), List.of());
        }
        return new RoleRevisionSnapshot(revision.id(), revision.baseRevisionId(),
                grants(revision.baseRevisionId()), deltas(revision.id()));
    }

    private List<ActionGrant> grants(long revisionId) {
        return jdbc.query("SELECT action_id,scopes FROM iam_role_grant WHERE revision_id=:id",
                Map.of("id", revisionId), (row, index) -> {
                    List<ScopeExpression> scopes = IamJson.read(row.getString("scopes"), SCOPES);
                    return new ActionGrant(row.getString("action_id"), scopes == null ? List.of() : scopes);
                });
    }

    private List<RoleDelta> deltas(long revisionId) {
        return jdbc.query("SELECT action_id,operation,scopes FROM iam_role_delta WHERE revision_id=:id",
                Map.of("id", revisionId), (row, index) -> new RoleDelta(row.getString("action_id"),
                        RoleDeltaOperation.valueOf(row.getString("operation")),
                        IamJson.read(row.getString("scopes"), SCOPES)));
    }

    private String actionCode(AuthorizationContext actor, long actionId) {
        List<ActionRow> rows = jdbc.query("""
                SELECT a.code,a.enabled,app.domain,app.enabled AS app_enabled,app.id AS application_id
                  FROM iam_action a JOIN iam_application app ON app.id=a.application_id
                 WHERE a.id=:id
                """, Map.of("id", actionId),
                (row, index) -> new ActionRow(row.getString("code"), row.getBoolean("enabled"),
                        AuthorizationDomain.valueOf(row.getString("domain")), row.getBoolean("app_enabled"),
                        row.getLong("application_id")));
        if (rows.size() != 1) {
            return null;
        }
        ActionRow action = rows.getFirst();
        if (!action.enabled() || !action.applicationEnabled()) {
            return null;
        }
        if (actor.domain() == AuthorizationDomain.TENANT && action.domain() == AuthorizationDomain.TENANT
                && !entitled(Long.parseLong(actor.tenantId()), action.applicationId(), actor)) {
            return null;
        }
        return action.code();
    }

    private boolean entitled(long tenantId, long applicationId, AuthorizationContext actor) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM iam_tenant_app_entitlement e
                  JOIN iam_app_audience aud ON aud.tenant_id=e.tenant_id AND aud.application_id=e.application_id
                 WHERE e.tenant_id=:tenantId AND e.application_id=:applicationId AND e.enabled=TRUE
                   AND aud.enabled=TRUE
                   AND (e.valid_from IS NULL OR e.valid_from<=CURRENT_TIMESTAMP)
                   AND (e.valid_until IS NULL OR e.valid_until>CURRENT_TIMESTAMP)
                   AND (aud.audience_kind=:allKind
                        OR EXISTS (
                            SELECT 1 FROM iam_audience_member m
                             WHERE m.tenant_id=e.tenant_id AND m.application_id=e.application_id
                               AND m.member_id=:memberId)
                        OR EXISTS (
                            SELECT 1 FROM iam_audience_group g
                              JOIN iam_tenant_group_member gm ON gm.tenant_id=g.tenant_id AND gm.group_id=g.group_id
                             WHERE g.tenant_id=e.tenant_id AND g.application_id=e.application_id
                               AND gm.member_id=:memberId)
                        OR EXISTS (
                            SELECT 1 FROM iam_audience_department d
                              JOIN iam_member_department md ON md.tenant_id=d.tenant_id AND md.department_id=d.department_id
                             WHERE d.tenant_id=e.tenant_id AND d.application_id=e.application_id
                               AND md.member_id=:memberId))
                """, Map.of("tenantId", tenantId, "applicationId", applicationId,
                "memberId", Long.parseLong(actor.memberId()), "allKind", AudienceKind.ALL.name()), Long.class);
        return count != null && count > 0;
    }

    private Map<String, Object> parameters(AuthorizationContext actor) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("domain", actor.domain().name());
        parameters.put("active", GrantStatus.ACTIVE.name());
        parameters.put("member", SubjectType.MEMBER.name());
        parameters.put("memberId", Long.parseLong(actor.memberId()));
        if (actor.domain() == AuthorizationDomain.TENANT) {
            parameters.put("tenantId", Long.parseLong(actor.tenantId()));
        }
        return parameters;
    }

    /**
     * <p>保存一次求值得到的精确操作、范围并集与截止时间。</p>
     *
     * @param actionCodes 有效操作码
     * @param scopes 各操作的范围并集；缺省键表示有操作无对象
     * @param version 参与求值的版本指纹
     * @param expiresAt 热缓存截止
     * @author jy
     * @since 1.0.0
     */
    public record AuthorizationView(List<String> actionCodes, Map<String, ResolvedActionScope> scopes, String version,
                                    Instant expiresAt) {
        /**
         * 复制操作与范围集合。
         */
        public AuthorizationView {
            actionCodes = actionCodes == null ? List.of() : List.copyOf(actionCodes);
            scopes = scopes == null ? Map.of() : Map.copyOf(scopes);
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

    private record AssignmentEval(long revisionId, Map<String, ScopeBinding> bindings, Long delegationId) {
    }

    private record RevisionRow(long id, Long baseRevisionId) {
    }

    private record ActionRow(String code, boolean enabled, AuthorizationDomain domain, boolean applicationEnabled,
                             long applicationId) {
    }
}
