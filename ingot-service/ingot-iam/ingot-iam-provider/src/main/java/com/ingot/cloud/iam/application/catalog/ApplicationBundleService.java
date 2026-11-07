package com.ingot.cloud.iam.application.catalog;

import com.ingot.framework.commons.model.iam.ImageUploadPurpose;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamActionEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamApplicationEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamResourceEntity;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.domain.catalog.ActionCodes;
import com.ingot.cloud.iam.application.catalog.model.ApplicationBundleAction;
import com.ingot.cloud.iam.application.catalog.model.ApplicationBundleDraft;
import com.ingot.cloud.iam.application.catalog.model.ApplicationBundleMenu;
import com.ingot.cloud.iam.application.catalog.model.ApplicationBundleResource;
import com.ingot.cloud.iam.application.catalog.model.ApplicationDraft;
import com.ingot.cloud.iam.domain.audit.AuditChangeType;
import com.ingot.cloud.iam.domain.audit.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.domain.catalog.MenuAccessMode;
import com.ingot.cloud.iam.domain.catalog.MenuDraft;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * <p>维护对应目录对象并保留原授权、审计与事务顺序。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ApplicationBundleService {
    private final CatalogOperations operationsContext;

    /**
     * 一次创建应用及其资源、操作与菜单；任一步失败整单回滚。
     * @param input 应用、资源与菜单草稿
     * @return 新应用 ID 与版本
     */
    public CreatedResource createApplicationBundle(ApplicationBundleDraft input) {
        ActiveIdentity actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_APPLICATION_CREATE);
        operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_RESOURCE_CREATE);
        operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ACTION_CREATE);
        operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MENU_CREATE);
        ApplicationDraft application = input.application();
        if (application.domain() == AuthorizationDomain.PLATFORM && application.baseline()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return operationsContext.transaction().execute(status -> {
            long appId = operationsContext.access().nextId();
            IamApplicationEntity entity = new IamApplicationEntity();
            entity.setId(BigInteger.valueOf(appId));
            entity.setCode(application.code());
            entity.setDomain(application.domain());
            entity.setName(application.name());
            entity.setDescription(CatalogOperations.nullable(application.description()));
            operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_APPLICATION_CREATE);
            entity.setIcon(operationsContext.images().bind(actor.context(), ImageUploadPurpose.APPLICATION_CREATE, null, CatalogOperations.nullable(application.icon()), null));
            entity.setSortOrder(application.sortOrder());
            entity.setBaseline(application.baseline());
            entity.setEnabled(true);
            try {
                operationsContext.catalog().insertApplication(entity);
            }
            catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.APPLICATION, IamIds.text(appId), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, application.name()), Map.of(CatalogOperations.APPLICATION, "0"));
            Map<String, Long> actionIds = persistBundleCatalog(actor, appId, application.code(), application.domain(),
                    input.resources());
            persistBundleMenus(actor, appId, input.menus(), actionIds);
            return new CreatedResource(IamIds.text(appId), "0");
        });
    }

    private Map<String, Long> persistBundleCatalog(ActiveIdentity actor, long appId, String applicationCode,
            AuthorizationDomain domain, List<ApplicationBundleResource> resources) {
        Map<String, Long> actionIds = new LinkedHashMap<>();
        Set<String> resourceCodes = new HashSet<>();
        Set<String> tempIds = new HashSet<>();
        for (ApplicationBundleResource resource : resources == null ? List.<ApplicationBundleResource>of()
                : resources) {
            CatalogOperations.requireResourceScopes(domain, resource.scopeCapabilities());
            var fieldApplication = new IamApplicationEntity();
            fieldApplication.setDomain(domain); fieldApplication.setCode(applicationCode);
            operationsContext.requireFieldCapabilities(fieldApplication, resource.code(), resource.fieldCapabilities());
            String resourceTempId = requireTempId(resource.tempId());
            if (!tempIds.add(resourceTempId) || !resourceCodes.add(resource.code())) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            long resourceId = operationsContext.access().nextId();
            IamResourceEntity entity = new IamResourceEntity();
            entity.setId(BigInteger.valueOf(resourceId));
            entity.setApplicationId(BigInteger.valueOf(appId));
            entity.setCode(resource.code());
            entity.setName(resource.name());
            entity.setScopeCapabilities(IamJsonCodec.array(resource.scopeCapabilities()));
            entity.setFieldCapabilities(IamJsonCodec.array(resource.fieldCapabilities()));
            entity.setFieldPolicy(false);
            entity.setEnabled(true);
            try {
                operationsContext.catalog().insertResource(entity);
            }
            catch (DuplicateKeyException exception) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.RESOURCE, IamIds.text(resourceId), AuditChangeType.CREATE,
                    Map.of(), Map.of(AuditField.NAME, resource.name()), Map.of(CatalogOperations.RESOURCE, "0"));
            Set<String> actionCodes = new HashSet<>();
            for (ApplicationBundleAction action : resource.actions() == null ? List.<ApplicationBundleAction>of()
                    : resource.actions()) {
                String actionTempId = requireTempId(action.tempId());
                if (!tempIds.add(actionTempId) || !actionCodes.add(action.code())) {
                    throw new BizException(IamReasonCode.INVALID_ARGUMENT);
                }
                long actionId = operationsContext.access().nextId();
                IamActionEntity row = new IamActionEntity();
                row.setId(BigInteger.valueOf(actionId));
                row.setApplicationId(BigInteger.valueOf(appId));
                row.setResourceId(BigInteger.valueOf(resourceId));
                row.setCode(ActionCodes.compose(applicationCode, resource.code(), action.code()));
                row.setName(action.name());
                row.setEnabled(true);
                try {
                    operationsContext.catalog().insertAction(row);
                }
                catch (DuplicateKeyException exception) {
                    throw new BizException(IamReasonCode.INVALID_ARGUMENT);
                }
                operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.ACTION, IamIds.text(actionId), AuditChangeType.CREATE,
                        Map.of(), Map.of(AuditField.NAME, action.name()), Map.of(CatalogOperations.ACTION, "0"));
                actionIds.put(actionTempId, actionId);
            }
        }
        return actionIds;
    }

    private void persistBundleMenus(ActiveIdentity actor, long appId, List<ApplicationBundleMenu> menus,
            Map<String, Long> actionIds) {
        Map<String, Long> menuIds = new LinkedHashMap<>();
        for (ApplicationBundleMenu menu : orderBundleMenus(menus)) {
            Long parentId = null;
            String parentTempId = blankToNull(menu.parentTempId());
            if (parentTempId != null) {
                parentId = menuIds.get(parentTempId);
                if (parentId == null) {
                    throw new BizException(IamReasonCode.INVALID_ARGUMENT);
                }
            }
            List<String> bound = menu.accessMode() == MenuAccessMode.OPEN ? List.of()
                    : resolveBundleActionIds(menu.actionTempIds(), actionIds);
            long id = operationsContext.access().nextId();
            MenuDraft draft = new MenuDraft(parentId == null ? null : IamIds.text(parentId), menu.name(), menu.kind(),
                    menu.path(), menu.viewPath(), menu.routeName(), menu.icon(), menu.accessMode(), menu.matchMode(),
                    bound, menu.sortOrder(), menu.hidden(), menu.isCache(), menu.props(), menu.routeParams());
            draft = MenuConfiguration.normalize(draft, null);
            operationsContext.catalog().insertMenu(CatalogOperations.menuEntity(id, appId, parentId, draft, true));
            operationsContext.replaceMenuActions(appId, id, bound);
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.MENU, IamIds.text(id), AuditChangeType.CREATE, Map.of(),
                    Map.of(AuditField.NAME, menu.name()), Map.of(CatalogOperations.MENU, "0"));
            menuIds.put(requireTempId(menu.tempId()), id);
        }
    }

    private List<ApplicationBundleMenu> orderBundleMenus(List<ApplicationBundleMenu> menus) {
        if (menus == null || menus.isEmpty()) {
            return List.of();
        }
        Map<String, ApplicationBundleMenu> byId = new LinkedHashMap<>();
        for (ApplicationBundleMenu menu : menus) {
            if (byId.put(requireTempId(menu.tempId()), menu) != null) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
        }
        Map<String, List<ApplicationBundleMenu>> children = new LinkedHashMap<>();
        List<ApplicationBundleMenu> roots = new ArrayList<>();
        for (ApplicationBundleMenu menu : byId.values()) {
            String parent = blankToNull(menu.parentTempId());
            if (parent == null) {
                roots.add(menu);
                continue;
            }
            if (!byId.containsKey(parent)) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            children.computeIfAbsent(parent, key -> new ArrayList<>()).add(menu);
        }
        List<ApplicationBundleMenu> ordered = new ArrayList<>();
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (ApplicationBundleMenu root : roots) {
            visitBundleMenu(root, children, visiting, visited, ordered);
        }
        if (ordered.size() != byId.size()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return ordered;
    }

    private static void visitBundleMenu(ApplicationBundleMenu menu, Map<String, List<ApplicationBundleMenu>> children,
            Set<String> visiting, Set<String> visited, List<ApplicationBundleMenu> ordered) {
        String tempId = menu.tempId();
        if (visited.contains(tempId)) {
            return;
        }
        if (!visiting.add(tempId)) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        ordered.add(menu);
        for (ApplicationBundleMenu child : children.getOrDefault(tempId, List.of())) {
            visitBundleMenu(child, children, visiting, visited, ordered);
        }
        visiting.remove(tempId);
        visited.add(tempId);
    }

    private static List<String> resolveBundleActionIds(List<String> actionTempIds, Map<String, Long> actionIds) {
        if (actionTempIds == null || actionTempIds.isEmpty()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (String tempId : actionTempIds) {
            Long id = actionIds.get(requireTempId(tempId));
            if (id == null) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            ids.add(IamIds.text(id));
        }
        return ids;
    }

    private static String requireTempId(String value) {
        if (value == null || value.isBlank()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
