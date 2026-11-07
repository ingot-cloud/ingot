package com.ingot.cloud.iam.application.catalog;

import com.ingot.cloud.iam.domain.catalog.CatalogInUse;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamActionEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamMenuEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamResourceEntity;
import com.ingot.cloud.iam.application.shared.ResourceDetailFactory;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.cloud.iam.infrastructure.serialization.IamJsonCodec;
import com.ingot.cloud.iam.application.shared.IamPages;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.domain.audit.AuditChangeType;
import com.ingot.cloud.iam.domain.audit.AuditField;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.cloud.iam.application.shared.model.CreatedResource;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.application.catalog.model.MenuActionRecord;
import com.ingot.cloud.iam.domain.catalog.MenuDraft;
import com.ingot.cloud.iam.domain.catalog.MenuRoutePaths;
import com.ingot.cloud.iam.application.catalog.model.MenuRecord;
import com.ingot.cloud.iam.application.catalog.model.MenuTreeNode;
import com.ingot.cloud.iam.application.catalog.model.MenuUpdateInput;
import com.ingot.framework.commons.model.iam.PageResponse;
import com.ingot.cloud.iam.application.catalog.model.ResourceDetail;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

/**
 * <p>维护对应目录对象并保留原授权、审计与事务顺序。</p>
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class MenuCatalogService {
    private final CatalogOperations operationsContext;

    /**
     * 一次返回菜单已绑定操作及资源名称，供详情回显。
     * @param applicationId 应用 ID
     * @param menuId 菜单 ID
     * @return 已关联操作
     */
    public List<MenuActionRecord> listMenuActions(String applicationId, String menuId) {
        operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MENU_READ);
        long appId = IamIds.require(applicationId);
        long id = IamIds.require(menuId);
        if (operationsContext.catalog().findMenu(appId, id) == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        List<BigInteger> actionIds = operationsContext.catalog().menuActionIds(appId, id);
        if (actionIds.isEmpty()) {
            return List.of();
        }
        List<IamActionEntity> rows = operationsContext.catalog().listActionsByIds(actionIds.stream().map(BigInteger::longValue).toList());
        Map<BigInteger, IamResourceEntity> resources = new LinkedHashMap<>();
        for (IamResourceEntity resource : operationsContext.catalog()
            .listResourcesByIds(rows.stream().map(row -> row.getResourceId().longValue()).distinct().toList())) {
            resources.put(resource.getId(), resource);
        }
        Map<BigInteger, IamActionEntity> byId = rows.stream()
            .collect(Collectors.toMap(IamActionEntity::getId, row -> row, (left, right) -> left, LinkedHashMap::new));
        List<MenuActionRecord> result = new ArrayList<>();
        for (BigInteger actionId : actionIds) {
            IamActionEntity row = byId.get(actionId);
            if (row == null) {
                continue;
            }
            IamResourceEntity resource = resources.get(row.getResourceId());
            result.add(new MenuActionRecord(CatalogOperations.text(row.getId()), row.getCode(), row.getName(), CatalogOperations.text(row.getResourceId()),
                    resource == null ? "" : resource.getCode(), resource == null ? "" : resource.getName()));
        }
        return result;
    }

    /**
     * 列出应用菜单。
     * @param applicationId 应用 ID
     * @param page 页码
     * @param pageSize 页大小
     * @return 菜单页
     */
    public PageResponse<ResourceDetail<MenuRecord>> listMenus(String applicationId, int page, int pageSize) {
        operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MENU_READ);
        long appId = IamIds.require(applicationId);
        operationsContext.requireApplication(appId);
        IamPages.require(page, pageSize);
        Page<IamMenuEntity> rows = operationsContext.catalog().pageMenus(appId, page, pageSize);
        java.util.Map<BigInteger, List<BigInteger>> actions = operationsContext.catalog().menuActionIds(appId,
                rows.getRecords().stream().map(IamMenuEntity::getId).toList());
        List<ResourceDetail<MenuRecord>> items = rows.getRecords()
            .stream()
            .map(row -> ResourceDetailFactory.of(menu(row, CatalogOperations.texts(actions.getOrDefault(row.getId(), List.of()))),
                    CatalogOperations.version(row.getVersion())))
            .toList();
        return IamPages.details(items, rows.getTotal(), page, pageSize);
    }

    /**
     * 返回应用菜单树，不带分页。
     * @param applicationId 应用 ID
     * @return 根节点
     */
    public List<MenuTreeNode> listMenuTree(String applicationId) {
        operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MENU_READ);
        long appId = IamIds.require(applicationId);
        operationsContext.requireApplication(appId);
        List<IamMenuEntity> rows = operationsContext.catalog().listMenus(appId);
        Map<BigInteger, List<BigInteger>> actions = operationsContext.catalog().menuActionIds(appId,
                rows.stream().map(IamMenuEntity::getId).toList());
        Map<BigInteger, List<IamMenuEntity>> children = new LinkedHashMap<>();
        List<IamMenuEntity> roots = new ArrayList<>();
        Set<BigInteger> ids = rows.stream().map(IamMenuEntity::getId).collect(Collectors.toSet());
        for (IamMenuEntity row : rows) {
            if (row.getParentId() == null || !ids.contains(row.getParentId())) {
                roots.add(row);
                continue;
            }
            children.computeIfAbsent(row.getParentId(), key -> new ArrayList<>()).add(row);
        }
        return roots.stream().map(row -> menuTree(row, children, actions)).toList();
    }

    /**
     * 创建菜单并绑定本应用精确操作。
     * @param applicationId 应用 ID
     * @param input 菜单草稿
     * @return 新菜单 ID
     */
    public CreatedResource createMenu(String applicationId, MenuDraft input) {
        ActiveIdentity actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MENU_CREATE);
        long appId = IamIds.require(applicationId);
        return operationsContext.transaction().execute(status -> {
            CatalogOperations.requireLocked(operationsContext.catalog().lockApplication(appId));
            Long parentId = requireMenuParent(appId, input.parentId());
            long id = operationsContext.access().nextId();
            operationsContext.catalog().insertMenu(CatalogOperations.menuEntity(id, appId, parentId, MenuConfiguration.normalize(input, null), true));
            operationsContext.replaceMenuActions(appId, id, input.actionIds());
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.MENU, IamIds.text(id), AuditChangeType.CREATE, Map.of(),
                    Map.of(AuditField.NAME, input.name()), Map.of(CatalogOperations.MENU, "0"));
            return new CreatedResource(IamIds.text(id), "0");
        });
    }

    /**
     * 整体更新菜单并重验本应用操作引用。
     * @param applicationId 应用 ID
     * @param menuId 菜单 ID
     * @param input 更新命令
     * @return 更新后详情
     */
    public ResourceDetail<MenuRecord> updateMenu(String applicationId, String menuId, MenuUpdateInput input) {
        ActiveIdentity actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MENU_UPDATE);
        long appId = IamIds.require(applicationId);
        long id = IamIds.require(menuId);
        return operationsContext.transaction().execute(status -> {
            IamMenuEntity current = CatalogOperations.requireLocked(operationsContext.catalog().lockMenu(appId, id));
            IamIds.requireVersion(input.expectedVersion(), CatalogOperations.version(current.getVersion()));
            Long parentId = requireMenuParent(appId, input.menu().parentId());
            if (parentId != null && parentId == id) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            if (parentId != null && operationsContext.catalog().isMenuAncestor(appId, parentId, id)) {
                throw new BizException(IamReasonCode.INVALID_ARGUMENT);
            }
            MenuDraft draft = MenuConfiguration.normalize(input.menu(), current);
            operationsContext.catalog().updateMenu(appId, id, parentId, draft.name(), CatalogOperations.nullable(draft.path()), CatalogOperations.nullable(draft.viewPath()),
                    CatalogOperations.nullable(draft.routeName()), CatalogOperations.nullable(draft.icon()), draft.kind(), draft.matchMode(),
                    draft.accessMode(), draft.sortOrder(), current.getVersion(), draft.hidden(), draft.isCache(),
                    draft.props(), IamJsonCodec.array(draft.routeParams()));
            operationsContext.replaceMenuActions(appId, id, draft.actionIds());
            String next = CatalogOperations.nextVersion(current.getVersion());
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.MENU, menuId, AuditChangeType.UPDATE,
                    Map.of(AuditField.NAME, current.getName()), Map.of(AuditField.NAME, draft.name()),
                    Map.of(CatalogOperations.MENU, next));
            return loadMenu(appId, id);
        });
    }

    /**
     * 删除没有子菜单的菜单。
     * @param applicationId 应用 ID
     * @param menuId 菜单 ID
     * @return 删除前版本
     */
    public CreatedResource deleteMenu(String applicationId, String menuId) {
        ActiveIdentity actor = operationsContext.access().require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_MENU_DELETE);
        long appId = IamIds.require(applicationId);
        long id = IamIds.require(menuId);
        return operationsContext.transaction().execute(status -> {
            IamMenuEntity current = CatalogOperations.requireLocked(operationsContext.catalog().lockMenu(appId, id));
            CatalogInUse.requireUnused(operationsContext.catalog().countChildMenus(appId, id), CatalogInUse.MENU_HAS_CHILDREN);
            operationsContext.catalog().deleteMenu(appId, id);
            operationsContext.audits().write(actor.context(), operationsContext.access().nextId(), CatalogOperations.MENU, menuId, AuditChangeType.REMOVE,
                    Map.of(AuditField.NAME, current.getName()), Map.of(), Map.of(CatalogOperations.MENU, CatalogOperations.version(current.getVersion())));
            return new CreatedResource(menuId, CatalogOperations.version(current.getVersion()));
        });
    }

    private ResourceDetail<MenuRecord> loadMenu(long applicationId, long id) {
        IamMenuEntity row = operationsContext.catalog().findMenu(applicationId, id);
        if (row == null) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return ResourceDetailFactory.of(menu(row, CatalogOperations.texts(operationsContext.catalog().menuActionIds(applicationId, id))), CatalogOperations.version(row.getVersion()));
    }

    private Long requireMenuParent(long applicationId, String parentId) {
        if (parentId == null || parentId.isBlank()) {
            return null;
        }
        long id = IamIds.require(parentId);
        if (!operationsContext.catalog().existsMenu(applicationId, id)) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        return id;
    }

    private MenuTreeNode menuTree(IamMenuEntity row, Map<BigInteger, List<IamMenuEntity>> children,
            Map<BigInteger, List<BigInteger>> actions) {
        List<MenuTreeNode> nodes = children.getOrDefault(row.getId(), List.of())
            .stream()
            .map(child -> menuTree(child, children, actions))
            .toList();
        return MenuTreeNode.of(ResourceDetailFactory.of(menu(row, CatalogOperations.texts(actions.getOrDefault(row.getId(), List.of()))),
                CatalogOperations.version(row.getVersion())), nodes);
    }

    private static MenuRecord menu(IamMenuEntity row, List<String> actionIds) {
        return new MenuRecord(CatalogOperations.text(row.getId()), CatalogOperations.text(row.getApplicationId()),
                row.getParentId() == null ? null : CatalogOperations.text(row.getParentId()), row.getName(), row.getKind(), row.getPath(),
                row.getViewPath(), row.getRouteName(), row.getIcon(), row.getAccessMode(), row.getMatchMode(),
                actionIds == null ? List.of() : new ArrayList<>(actionIds),
                row.getSortOrder() == null ? 0 : row.getSortOrder(), CatalogOperations.statusOf(row.getEnabled()),
                Boolean.TRUE.equals(row.getHidden()), Boolean.TRUE.equals(row.getIsCache()),
                Boolean.TRUE.equals(row.getProps()), MenuConfiguration.parameters(row),
                MenuRoutePaths.resolve(row.getPath(), MenuConfiguration.parameters(row)));
    }
}
