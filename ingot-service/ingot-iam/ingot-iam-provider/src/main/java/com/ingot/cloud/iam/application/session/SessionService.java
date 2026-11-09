package com.ingot.cloud.iam.application.session;

import com.ingot.cloud.iam.domain.authorization.AuthorizationView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ingot.cloud.iam.application.authorization.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.application.catalog.MenuConfiguration;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.SessionRepository;
import com.ingot.cloud.iam.infrastructure.persistence.projection.SessionMenuRow;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.domain.catalog.ApplicationSummary;
import com.ingot.cloud.iam.application.session.model.Bootstrap;
import com.ingot.cloud.iam.application.session.model.CurrentCapabilities;
import com.ingot.cloud.iam.domain.account.CurrentProfile;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.application.catalog.model.MenuNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>组装当前身份一致授权视图，不跨域聚合，授权服务不可用时失败关闭。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SessionService {
    private final IamAccess access;
    private final AuthorizationEvaluator evaluator;
    private final SessionRepository sessions;
    private final com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantAdministrationMapper live;

    /**
     * 返回当前身份一致授权视图。
     *
     * @return bootstrap
     */
    public Bootstrap bootstrap() {
        ActiveIdentity actor = access.requireCurrent();
        AuthorizationView view = evaluator.evaluate(actor.context());
        List<ApplicationSummary> applications = view.tenantOwner()
            ? live.applications(new java.math.BigInteger(actor.context().tenantId())).stream().map(a -> new ApplicationSummary(a.id().toString(), a.code(), a.name(), a.icon(), a.sortOrder())).toList()
            : sessions.accessibleApplications(actor.context());
        return new Bootstrap(actor.context(), profile(actor), applications,
                menus(actor, view, applications), view.actionCodes(), view.version(), view.expiresAt(), view.tenantOwner());
    }

    /**
     * 返回当前身份精确操作集合。
     *
     * @return 能力快照
     */
    public CurrentCapabilities capabilities() {
        ActiveIdentity actor = access.requireCurrent();
        AuthorizationView view = evaluator.evaluate(actor.context());
        return new CurrentCapabilities(view.actionCodes(), view.version(), view.expiresAt());
    }

    private CurrentProfile profile(ActiveIdentity actor) {
        CurrentProfile profile = sessions.profile(actor.context());
        if (profile == null) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        return profile;
    }

    private List<MenuNode> menus(ActiveIdentity actor, AuthorizationView view,
                                 List<ApplicationSummary> applications) {
        Set<Long> accessibleAppIds = new HashSet<>();
        for (ApplicationSummary app : applications) {
            accessibleAppIds.add(Long.parseLong(app.id()));
        }
        Map<Long, SessionMenuAssembler.MenuRow> rows = new LinkedHashMap<>();
        Map<Long, List<String>> required = new LinkedHashMap<>();
        for (SessionMenuRow row : sessions.menus(actor.context().domain(), accessibleAppIds)) {
            long menuId = row.getId().longValueExact();
            rows.computeIfAbsent(menuId, id -> new SessionMenuAssembler.MenuRow(
                    id, row.getApplicationId().longValueExact(),
                    row.getParentId() == null ? null : row.getParentId().longValueExact(), row.getName(),
                    row.getKind(), row.getPath(), row.getViewPath(), row.getRouteName(), row.getIcon(),
                    row.getSortOrder() == null ? 0 : row.getSortOrder(), row.getMatchMode(), row.getAccessMode(),
                    Boolean.TRUE.equals(row.getHidden()), Boolean.TRUE.equals(row.getIsCache()),
                    Boolean.TRUE.equals(row.getProps()), MenuConfiguration.parameters(row)));
            if (row.getRequiredActionCode() != null) {
                required.computeIfAbsent(menuId, id -> new ArrayList<>()).add(row.getRequiredActionCode());
            }
        }
        return SessionMenuAssembler.assemble(List.copyOf(rows.values()), accessibleAppIds, Set.copyOf(view.actionCodes()), required);
    }
}
