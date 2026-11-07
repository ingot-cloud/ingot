package com.ingot.cloud.iam.application.diagnose;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import com.ingot.cloud.iam.domain.assignment.TenantScopeObjectResource;
import com.ingot.cloud.iam.application.authorization.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.domain.authorization.ResolvedActionScope;
import com.ingot.cloud.iam.domain.authorization.ScopeClause;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.infrastructure.persistence.MemberQueryRepository;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantAdministrationMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantScopeCandidateMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantScopeCandidateSql;
import com.ingot.cloud.iam.infrastructure.persistence.projection.TenantAdministrationRows;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.application.shared.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.domain.shared.AuthorizationCandidateKind;
import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.AuthorizationOption;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.domain.tenant.TenantAuthorizationConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>组织权限诊断的成员、应用、操作和目标对象候选，口径与诊断校验一致，能选到的对象不会被判为不存在。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantDiagnoseCandidates {
    private static final String UNSUPPORTED_OBJECT = "该资源暂未接入目标对象查询，留空只诊断操作权限";
    private static final Set<AuthorizationCandidateKind> KINDS = Set.of(AuthorizationCandidateKind.MEMBER,
            AuthorizationCandidateKind.APPLICATION, AuthorizationCandidateKind.ACTION,
            AuthorizationCandidateKind.OBJECT);
    private final IamAccess access;
    private final AuthorizationEvaluator evaluator;
    private final ResourceScopeAccessService scopes;
    private final MemberQueryRepository members;
    private final DepartmentQueryRepository departments;
    private final TenantAdministrationMapper live;
    private final TenantScopeCandidateMapper candidates;

    /**
     * 查询一页诊断候选。
     *
     * @param kind 候选类型，仅成员、应用、操作、目标对象
     * @param applicationId 操作所属应用，查询操作时必填
     * @param actionId 目标对象所属操作，查询对象时必填
     * @param keyword 名称包含匹配；操作同时匹配资源名
     * @param ids 已选回显
     * @param page 页码
     * @param pageSize 页大小
     * @return 候选页
     */
    public AuthorizationCandidatePage list(AuthorizationCandidateKind kind, String applicationId, String actionId,
                                           String keyword, List<String> ids, int page, int pageSize) {
        ActiveIdentity actor = access.require(AuthorizationDomain.TENANT, IamAction.TENANT_AUTHORIZATION_DIAGNOSE);
        if (kind == null || !KINDS.contains(kind)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        IamPages.require(page, pageSize);
        long tenantId = IamIds.require(actor.context().tenantId());
        List<BigInteger> selected = ids(ids);
        return switch (kind) {
            case MEMBER -> access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_READ, false)
                    ? members(actor, tenantId, keyword, selected, page, pageSize)
                    : DiagnoseSubjects.self(actor.context(), keyword, ids, page, pageSize);
            case APPLICATION -> applications(tenantId, keyword, selected, page, pageSize);
            case ACTION -> actions(tenantId, applicationId, keyword, selected, page, pageSize);
            default -> objects(actor, tenantId, applicationId, actionId, keyword, selected, page, pageSize);
        };
    }

    private AuthorizationCandidatePage members(ActiveIdentity actor, long tenantId, String keyword,
                                               List<BigInteger> selected, int page, int pageSize) {
        var view = evaluator.evaluate(actor.context());
        if (!view.actionCodes().contains(IamAction.TENANT_MEMBER_READ.getCode())) {
            return empty(page, pageSize);
        }
        var scope = scopes.memberRead(actor.context(), IamAction.TENANT_MEMBER_READ);
        if (scope.coversNone()) {
            return empty(page, pageSize);
        }
        var rows = members.pageTenantCandidates(tenantId, scope, name(keyword), selected, page, pageSize);
        return supported(rows.getRecords().stream()
                .map(row -> option(row.getId().toString(), row.getDisplayName(), null)).toList(),
                rows.getTotal(), page, pageSize);
    }

    private AuthorizationCandidatePage applications(long tenantId, String keyword, List<BigInteger> selected,
                                                    int page, int pageSize) {
        var rows = live.applications(BigInteger.valueOf(tenantId)).stream()
                .filter(row -> selected.isEmpty() || selected.contains(row.id()))
                .filter(row -> matches(row.name(), keyword))
                .map(row -> option(row.id().toString(), row.name(), null)).toList();
        return slice(rows, page, pageSize);
    }

    private AuthorizationCandidatePage actions(long tenantId, String applicationId, String keyword,
                                               List<BigInteger> selected, int page, int pageSize) {
        if (applicationId == null || applicationId.isBlank()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        var rows = live.catalog(BigInteger.valueOf(tenantId), BigInteger.valueOf(IamIds.require(applicationId)),
                        null, selected).stream()
                .filter(row -> matches(row.actionName(), keyword) || matches(row.resourceName(), keyword))
                .map(row -> option(row.actionId().toString(), row.actionName(), row.resourceName())).toList();
        return slice(rows, page, pageSize);
    }

    private AuthorizationCandidatePage objects(ActiveIdentity actor, long tenantId, String applicationId, String actionId, String keyword,
                                               List<BigInteger> selected, int page, int pageSize) {
        if (actionId == null || actionId.isBlank()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        List<TenantAdministrationRows.Action> actions = live.actions(List.of(BigInteger.valueOf(IamIds.require(actionId))));
        if (actions.size() != 1) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        TenantAdministrationRows.Action action = actions.getFirst();
        if ((applicationId != null && !BigInteger.valueOf(IamIds.require(applicationId)).equals(action.applicationId()))
                || live.catalog(BigInteger.valueOf(tenantId), action.applicationId(), null,
                        List.of(action.actionId())).isEmpty()) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        var resource = live.resource(action.resourceId());
        TenantScopeObjectResource target = resource == null
                || !TenantAuthorizationConstants.CORE_APPLICATION.equals(resource.applicationCode())
                ? null : TenantScopeObjectResource.find(resource.resourceCode());
        if (target == null) {
            return new AuthorizationCandidatePage(List.of(), 0, page, pageSize, false, UNSUPPORTED_OBJECT);
        }
        if (target == TenantScopeObjectResource.MEMBER) {
            return members(actor, tenantId, keyword, selected, page, pageSize);
        }
        if (target == TenantScopeObjectResource.DEPARTMENT) {
            return departments(actor, tenantId, keyword, selected, page, pageSize);
        }
        var view = evaluator.evaluate(actor.context());
        String read = switch (target) {
            case APPLICATION -> IamAction.VALUE_TENANT_APPLICATION_READ;
            case ASSIGNMENT -> IamAction.VALUE_TENANT_ASSIGNMENT_READ;
            case DELEGATION -> IamAction.VALUE_TENANT_DELEGATION_READ;
            case GROUP -> IamAction.VALUE_TENANT_GROUP_READ;
            case ROLE -> IamAction.VALUE_TENANT_ROLE_READ;
            case MEMBER, DEPARTMENT -> throw new IllegalStateException("对象范围未匹配");
        };
        if (!view.actionCodes().contains(read)) {
            return empty(page, pageSize);
        }
        List<BigInteger> allowed = allowedObjects(view.scope(read));
        List<BigInteger> chosen = allowed == null ? selected
                : selected.isEmpty() ? allowed : selected.stream().filter(allowed::contains).toList();
        if (allowed != null && chosen.isEmpty()) {
            return empty(page, pageSize);
        }
        var query = new TenantScopeCandidateSql.Query(target, tenantId, search(keyword), chosen,
                IamPages.offset(page, pageSize), pageSize);
        return supported(candidates.page(query).stream()
                .map(row -> option(row.id().toString(), row.name(), null)).toList(),
                candidates.count(query), page, pageSize);
    }

    private AuthorizationCandidatePage departments(ActiveIdentity actor, long tenantId, String keyword,
                                                   List<BigInteger> selected, int page, int pageSize) {
        var view = evaluator.evaluate(actor.context());
        if (!view.actionCodes().contains(IamAction.TENANT_DEPARTMENT_READ.getCode())) {
            return empty(page, pageSize);
        }
        var scope = scopes.departmentRead(actor.context(), IamAction.TENANT_DEPARTMENT_READ);
        if (scope.coversNone()) {
            return empty(page, pageSize);
        }
        var rows = departments.pageCandidates(tenantId, scope, name(keyword), selected, page, pageSize);
        return supported(rows.getRecords().stream()
                .map(row -> option(row.getId().toString(), row.getName(), null)).toList(),
                rows.getTotal(), page, pageSize);
    }

    /** 全部范围返回 null 表示不限 ID；否则为显式对象 ID 并集。 */
    private static List<BigInteger> allowedObjects(ResolvedActionScope scope) {
        if (scope.clauses().stream().anyMatch(ScopeClause::all)) {
            return null;
        }
        Set<BigInteger> ids = new LinkedHashSet<>();
        for (ScopeClause clause : scope.clauses()) {
            clause.objectIds().stream().map(BigInteger::new).forEach(ids::add);
        }
        return List.copyOf(ids);
    }

    private static AuthorizationCandidatePage slice(List<AuthorizationOption> rows, int page, int pageSize) {
        int from = Math.min(IamPages.offset(page, pageSize), rows.size());
        int to = Math.min(from + pageSize, rows.size());
        return supported(new ArrayList<>(rows.subList(from, to)), rows.size(), page, pageSize);
    }

    private static AuthorizationCandidatePage supported(List<AuthorizationOption> items, long total, int page,
                                                        int pageSize) {
        return new AuthorizationCandidatePage(items, total, page, pageSize, true, null);
    }

    private static AuthorizationCandidatePage empty(int page, int pageSize) {
        return supported(List.of(), 0, page, pageSize);
    }

    private static AuthorizationOption option(String id, String name, String summary) {
        return new AuthorizationOption(id, name, summary, null, null, null, null, null);
    }

    private static List<BigInteger> ids(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).filter(id -> !id.isBlank())
                .map(id -> BigInteger.valueOf(IamIds.require(id))).distinct().toList();
    }

    private static String name(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private static boolean matches(String value, String keyword) {
        String search = name(keyword);
        return search == null || value != null
                && value.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }

    private static String search(String keyword) {
        String value = keyword == null ? "" : keyword.trim();
        return "%" + value.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
