package com.ingot.cloud.pms.authorization.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ingot.cloud.pms.api.model.domain.PlatformApp;
import com.ingot.cloud.pms.api.model.domain.PlatformPermission;
import com.ingot.cloud.pms.api.model.enums.MenuTypeEnum;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.service.domain.PlatformAppService;
import com.ingot.cloud.pms.service.domain.PlatformMenuPermissionService;
import com.ingot.cloud.pms.service.domain.PlatformMenuService;
import com.ingot.cloud.pms.service.domain.PlatformPermissionService;
import com.ingot.framework.commons.constants.IDConstants;
import com.ingot.framework.commons.model.enums.CommonStatusEnum;
import com.ingot.framework.commons.utils.tree.TreeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>应用菜单树生成器，按应用排序聚合可见菜单并递归排序子节点。</p>
 *
 * <p>过滤按钮、禁用与不可访问应用；受保护页按菜单关联权限的 ANY/ALL 判定；目录靠祖先补齐。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ApplicationMenuTreeBuilder {

    private final PlatformMenuService platformMenuService;
    private final PlatformAppService platformAppService;
    private final PlatformMenuPermissionService platformMenuPermissionService;
    private final PlatformPermissionService platformPermissionService;

    /**
     * 按有效授权构建当前用户可见菜单树。
     *
     * @param authorization 有效授权
     * @return 按应用排序的菜单根节点
     */
    public List<MenuTreeNodeVO> build(EffectiveAuthorization authorization) {
        List<MenuTreeNodeVO> allNodes = platformMenuService.nodeList();
        Set<Long> accessibleAppIds = authorization.safeAccessibleAppIds();
        Map<Long, PlatformApp> appById = platformAppService.list().stream()
                .collect(Collectors.toMap(PlatformApp::getId, item -> item,
                        (a, b) -> a, LinkedHashMap::new));
        Map<Long, List<Long>> permissionIdsByMenu = platformMenuPermissionService.mapPermissionIds();
        Map<Long, String> permissionCodeById = platformPermissionService.list().stream()
                .collect(Collectors.toMap(PlatformPermission::getId, PlatformPermission::getCode, (a, b) -> a));

        List<MenuTreeNodeVO> visibleNodes = allNodes.stream()
                .filter(node -> node.getMenuType() != MenuTypeEnum.Button)
                .filter(node -> node.getStatus() == CommonStatusEnum.ENABLE)
                .filter(node -> isAppAccessible(node, accessibleAppIds))
                .peek(node -> node.setPermissionIds(
                        permissionIdsByMenu.getOrDefault(node.getId(), List.of())))
                .filter(node -> isMenuVisible(node, permissionIdsByMenu, permissionCodeById, authorization))
                .sorted(Comparator.comparing(MenuTreeNodeVO::getOrgType)
                        .thenComparing(MenuTreeNodeVO::getSort))
                .collect(Collectors.toCollection(ArrayList::new));

        visibleNodes = appendAncestors(allNodes, visibleNodes);

        Map<Long, List<MenuTreeNodeVO>> nodesByApp = new HashMap<>();
        for (MenuTreeNodeVO node : visibleNodes) {
            Long appId = node.getAppId();
            nodesByApp.computeIfAbsent(appId == null ? 0L : appId, key -> new ArrayList<>()).add(node);
        }

        List<PlatformApp> orderedApps = appById.values().stream()
                .filter(app -> accessibleAppIds.contains(app.getId()))
                .sorted(Comparator.<PlatformApp>comparingInt(app -> app.getSort() == null ? 999 : app.getSort())
                        .thenComparing(PlatformApp::getId))
                .toList();

        List<MenuTreeNodeVO> roots = new ArrayList<>();
        for (PlatformApp app : orderedApps) {
            List<MenuTreeNodeVO> appNodes = nodesByApp.getOrDefault(app.getId(), List.of());
            List<MenuTreeNodeVO> appRoots = TreeUtil.build(appNodes).stream()
                    .sorted(Comparator.comparingInt(MenuTreeNodeVO::getSort)
                            .thenComparing(MenuTreeNodeVO::getId))
                    .peek(node -> {
                        node.setAppId(app.getId());
                        node.setAppCode(app.getCode());
                    })
                    .toList();
            sortChildrenRecursively(appRoots);
            roots.addAll(appRoots);
        }

        List<MenuTreeNodeVO> legacyRoots = nodesByApp.getOrDefault(0L, List.of());
        if (!legacyRoots.isEmpty()) {
            roots.addAll(TreeUtil.build(legacyRoots));
        }
        return roots;
    }

    private boolean isAppAccessible(MenuTreeNodeVO node, Set<Long> accessibleAppIds) {
        if (node.getAppId() == null) {
            return true;
        }
        return accessibleAppIds.contains(node.getAppId());
    }

    private boolean isMenuVisible(MenuTreeNodeVO node,
                                  Map<Long, List<Long>> permissionIdsByMenu,
                                  Map<Long, String> permissionCodeById,
                                  EffectiveAuthorization authorization) {
        List<String> requiredCodes = permissionIdsByMenu.getOrDefault(node.getId(), List.of()).stream()
                .map(permissionCodeById::get)
                .toList();
        return MenuVisibility.visible(node.getMenuType(), node.getAccessMode(),
                node.getPermissionMatchMode(), requiredCodes, authorization);
    }

    private List<MenuTreeNodeVO> appendAncestors(List<MenuTreeNodeVO> allNodes, List<MenuTreeNodeVO> visibleNodes) {
        Map<Long, MenuTreeNodeVO> byId = allNodes.stream()
                .collect(Collectors.toMap(MenuTreeNodeVO::getId, item -> item, (a, b) -> a));
        Set<Long> included = visibleNodes.stream().map(MenuTreeNodeVO::getId).collect(Collectors.toCollection(HashSet::new));
        List<MenuTreeNodeVO> result = new ArrayList<>(visibleNodes);
        for (MenuTreeNodeVO node : visibleNodes) {
            Long pid = node.getPid();
            while (pid != null && pid > IDConstants.ROOT_TREE_ID) {
                if (included.contains(pid)) {
                    break;
                }
                MenuTreeNodeVO parent = byId.get(pid);
                if (parent == null || parent.getStatus() != CommonStatusEnum.ENABLE) {
                    break;
                }
                result.add(parent);
                included.add(parent.getId());
                pid = parent.getPid();
            }
        }
        return result;
    }

    private void sortChildrenRecursively(List<MenuTreeNodeVO> nodes) {
        for (MenuTreeNodeVO node : nodes) {
            if (node.getChildren() == null) {
                continue;
            }
            node.getChildren().sort(Comparator.comparingInt(MenuTreeNodeVO::getSort)
                    .thenComparing(MenuTreeNodeVO::getId));
            sortChildrenRecursively(node.getChildren());
        }
    }
}
