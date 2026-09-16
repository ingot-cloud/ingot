package com.ingot.cloud.iam.session;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.SessionRepository;
import com.ingot.cloud.iam.persistence.entity.IamMenuEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ApplicationSummary;
import com.ingot.framework.commons.model.iam.Bootstrap;
import com.ingot.framework.commons.model.iam.CurrentCapabilities;
import com.ingot.framework.commons.model.iam.CurrentProfile;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MenuNode;
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

    /**
     * 返回当前身份一致授权视图。
     *
     * @return bootstrap
     */
    public Bootstrap bootstrap() {
        ActiveIdentity actor = access.requireCurrent();
        AuthorizationEvaluator.AuthorizationView view = evaluator.evaluate(actor.context());
        List<ApplicationSummary> applications = sessions.accessibleApplications(actor.context());
        return new Bootstrap(actor.context(), profile(actor), applications,
                menus(actor, view, applications), view.actionCodes(), view.version(), view.expiresAt());
    }

    /**
     * 返回当前身份精确操作集合。
     *
     * @return 能力快照
     */
    public CurrentCapabilities capabilities() {
        ActiveIdentity actor = access.requireCurrent();
        AuthorizationEvaluator.AuthorizationView view = evaluator.evaluate(actor.context());
        return new CurrentCapabilities(view.actionCodes(), view.version(), view.expiresAt());
    }

    private CurrentProfile profile(ActiveIdentity actor) {
        CurrentProfile profile = sessions.profile(actor.context());
        if (profile == null) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        return profile;
    }

    private List<MenuNode> menus(ActiveIdentity actor, AuthorizationEvaluator.AuthorizationView view,
                                 List<ApplicationSummary> applications) {
        Set<Long> accessibleAppIds = new HashSet<>();
        for (ApplicationSummary app : applications) {
            accessibleAppIds.add(Long.parseLong(app.id()));
        }
        List<SessionMenuAssembler.MenuRow> rows = new ArrayList<>();
        List<Long> menuIds = new ArrayList<>();
        for (IamMenuEntity row : sessions.menus(actor.context().domain())) {
            SessionMenuAssembler.MenuRow snapshot = new SessionMenuAssembler.MenuRow(
                    row.getId().longValueExact(), row.getApplicationId().longValueExact(),
                    row.getParentId() == null ? null : row.getParentId().longValueExact(), row.getName(),
                    row.getKind(), row.getPath(), row.getViewPath(), row.getRouteName(), row.getIcon(),
                    row.getSortOrder() == null ? 0 : row.getSortOrder(), row.getMatchMode(), row.getAccessMode());
            rows.add(snapshot);
            menuIds.add(snapshot.id());
        }
        Map<Long, List<String>> required = sessions.requiredActionCodes(menuIds);
        return SessionMenuAssembler.assemble(rows, accessibleAppIds, Set.copyOf(view.actionCodes()), required);
    }
}
