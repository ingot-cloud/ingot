package com.ingot.cloud.iam.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ingot.cloud.iam.evaluation.JdbcAuthorizationEvaluator;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.ActionMatchMode;
import com.ingot.framework.commons.model.iam.ApplicationSummary;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.Bootstrap;
import com.ingot.framework.commons.model.iam.CurrentCapabilities;
import com.ingot.framework.commons.model.iam.CurrentProfile;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MenuAccessMode;
import com.ingot.framework.commons.model.iam.MenuKind;
import com.ingot.framework.commons.model.iam.MenuNode;
import com.ingot.cloud.iam.persistence.SessionRepository;
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
    private final JdbcAuthorizationEvaluator evaluator;
    private final SessionRepository sessions;

    /**
     * 返回当前身份一致授权视图。
     *
     * @return bootstrap
     */
    public Bootstrap bootstrap() {
        ActiveIdentity actor = access.requireCurrent();
        JdbcAuthorizationEvaluator.AuthorizationView view = evaluator.evaluate(actor.context());
        return new Bootstrap(actor.context(), profile(actor), applications(actor, view),
                menus(actor, view), view.actionCodes(), view.version(), view.expiresAt());
    }

    /**
     * 返回当前身份精确操作集合。
     *
     * @return 能力快照
     */
    public CurrentCapabilities capabilities() {
        ActiveIdentity actor = access.requireCurrent();
        JdbcAuthorizationEvaluator.AuthorizationView view = evaluator.evaluate(actor.context());
        return new CurrentCapabilities(view.actionCodes(), view.version(), view.expiresAt());
    }

    private CurrentProfile profile(ActiveIdentity actor) {
        CurrentProfile profile = sessions.profile(actor.context());
        if (profile == null) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        return profile;
    }

    private List<ApplicationSummary> applications(ActiveIdentity actor,
                                                 JdbcAuthorizationEvaluator.AuthorizationView view) {
        Set<String> codes = Set.copyOf(view.actionCodes());
        if (codes.isEmpty()) {
            return List.of();
        }
        List<ApplicationSummary> apps = sessions.applications(actor.context().domain());
        List<ApplicationSummary> visible = new ArrayList<>();
        for (ApplicationSummary app : apps) {
            List<String> actionCodes = sessions.enabledActionCodes(IamIds.require(app.id()));
            if (actionCodes.stream().anyMatch(codes::contains)) {
                visible.add(app);
            }
        }
        return visible;
    }

    private List<MenuNode> menus(ActiveIdentity actor, JdbcAuthorizationEvaluator.AuthorizationView view) {
        Set<String> codes = Set.copyOf(view.actionCodes());
        List<MenuRow> rows = sessions.menus().stream().map(row -> new MenuRow(
                row.getId().longValueExact(), row.getApplicationId().longValueExact(),
                row.getParentId() == null ? null : row.getParentId().longValueExact(), row.getName(),
                row.getKind(), row.getPath(), row.getViewPath(), row.getRouteName(), row.getIcon(),
                row.getSortOrder(), row.getMatchMode(), row.getAccessMode())).toList();
        Map<Long, MenuNode> nodes = new LinkedHashMap<>();
        Map<Long, Long> parents = new HashMap<>();
        Map<Long, List<MenuNode>> children = new LinkedHashMap<>();
        List<MenuNode> roots = new ArrayList<>();
        for (MenuRow row : rows) {
            if (!visible(row, codes)) {
                continue;
            }
            MenuNode node = new MenuNode(IamIds.text(row.id()), IamIds.text(row.applicationId()), row.name(),
                    row.kind(), row.path(), row.viewPath(), row.routeName(), row.icon(), row.sortOrder(),
                    List.of());
            nodes.put(row.id(), node);
            parents.put(row.id(), row.parentId());
        }
        for (Map.Entry<Long, MenuNode> entry : nodes.entrySet()) {
            Long parentId = parents.get(entry.getKey());
            if (parentId == null || !nodes.containsKey(parentId)) {
                roots.add(entry.getValue());
            } else {
                children.computeIfAbsent(parentId, key -> new ArrayList<>()).add(entry.getValue());
            }
        }
        return roots.stream().map(root -> withChildren(root, children)).toList();
    }

    private MenuNode withChildren(MenuNode node, Map<Long, List<MenuNode>> children) {
        long id = IamIds.require(node.id());
        List<MenuNode> nested = children.getOrDefault(id, List.of()).stream()
                .map(child -> withChildren(child, children)).toList();
        return new MenuNode(node.id(), node.applicationId(), node.name(), node.kind(), node.path(), node.viewPath(),
                node.routeName(), node.icon(), node.sortOrder(), nested);
    }

    private boolean visible(MenuRow row, Set<String> codes) {
        if (row.accessMode() == MenuAccessMode.OPEN) {
            return true;
        }
        List<String> required = sessions.requiredActionCodes(row.id());
        if (required.isEmpty()) {
            return false;
        }
        return row.matchMode() == ActionMatchMode.ALL
                ? required.stream().allMatch(codes::contains)
                : required.stream().anyMatch(codes::contains);
    }

    /**
     * <p>保存构建菜单树所需的目录快照。</p>
     * @author jy
     * @since 1.0.0
     */
    private record MenuRow(long id, long applicationId, Long parentId, String name, MenuKind kind, String path,
                           String viewPath, String routeName, String icon, int sortOrder, ActionMatchMode matchMode,
                           MenuAccessMode accessMode) {
    }
}
