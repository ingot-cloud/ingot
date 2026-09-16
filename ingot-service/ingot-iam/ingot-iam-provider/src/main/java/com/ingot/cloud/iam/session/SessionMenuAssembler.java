package com.ingot.cloud.iam.session;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ingot.cloud.iam.support.IamIds;
import com.ingot.framework.commons.model.iam.ActionMatchMode;
import com.ingot.framework.commons.model.iam.MenuAccessMode;
import com.ingot.framework.commons.model.iam.MenuKind;
import com.ingot.framework.commons.model.iam.MenuNode;

/**
 * <p>按应用边界与菜单 ACTION 组装导航树，开放页仍受应用有效性约束，空目录不展示。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class SessionMenuAssembler {
    private SessionMenuAssembler() {
    }

    /**
     * 组装当前身份可见菜单。先限定可访问应用，再按 OPEN/ACTION 判定页面，目录只作为有子页的祖先出现。
     *
     * @param rows 已启用菜单快照
     * @param accessibleAppIds 当前域启用且开通/人群命中的应用
     * @param actionCodes 当前身份精确操作
     * @param requiredByMenu 菜单关联的操作编码
     * @return 根节点列表
     */
    public static List<MenuNode> assemble(List<MenuRow> rows, Set<Long> accessibleAppIds, Set<String> actionCodes,
                                          Map<Long, List<String>> requiredByMenu) {
        Map<Long, MenuRow> byId = new LinkedHashMap<>();
        for (MenuRow row : rows) {
            if (!accessibleAppIds.contains(row.applicationId())) {
                continue;
            }
            byId.put(row.id(), row);
        }
        Set<Long> pages = new HashSet<>();
        for (MenuRow row : byId.values()) {
            if (row.kind() == MenuKind.PAGE && pageVisible(row, actionCodes,
                    requiredByMenu.getOrDefault(row.id(), List.of()))) {
                pages.add(row.id());
            }
        }
        Set<Long> included = new HashSet<>(pages);
        for (Long pageId : pages) {
            Long parentId = byId.get(pageId).parentId();
            while (parentId != null && byId.containsKey(parentId) && included.add(parentId)) {
                parentId = byId.get(parentId).parentId();
            }
        }
        Map<Long, List<MenuRow>> children = new HashMap<>();
        List<MenuRow> roots = new ArrayList<>();
        for (MenuRow row : byId.values()) {
            if (!included.contains(row.id())) {
                continue;
            }
            Long parentId = row.parentId();
            if (parentId == null || !included.contains(parentId)) {
                roots.add(row);
            } else {
                children.computeIfAbsent(parentId, key -> new ArrayList<>()).add(row);
            }
        }
        roots.sort(SessionMenuAssembler::compare);
        children.values().forEach(items -> items.sort(SessionMenuAssembler::compare));
        return roots.stream().map(root -> node(root, children)).toList();
    }

    private static MenuNode node(MenuRow row, Map<Long, List<MenuRow>> children) {
        List<MenuNode> nested = children.getOrDefault(row.id(), List.of()).stream()
                .map(child -> node(child, children)).toList();
        return new MenuNode(IamIds.text(row.id()), IamIds.text(row.applicationId()), row.name(), row.kind(),
                row.path(), row.viewPath(), row.routeName(), row.icon(), row.sortOrder(), nested);
    }

    private static boolean pageVisible(MenuRow row, Set<String> actionCodes, List<String> required) {
        if (row.accessMode() == MenuAccessMode.OPEN) {
            return true;
        }
        if (required == null || required.isEmpty()) {
            return false;
        }
        return row.matchMode() == ActionMatchMode.ALL
                ? required.stream().allMatch(actionCodes::contains)
                : required.stream().anyMatch(actionCodes::contains);
    }

    private static int compare(MenuRow left, MenuRow right) {
        int order = Integer.compare(left.sortOrder(), right.sortOrder());
        return order != 0 ? order : Long.compare(left.id(), right.id());
    }

    /**
     * <p>保存组装菜单树所需的目录快照。</p>
     *
     * @param id 菜单 ID
     * @param applicationId 所属应用
     * @param parentId 父菜单，根节点为空
     * @param name 导航名称
     * @param kind 目录或页面
     * @param path 路由路径
     * @param viewPath 视图键
     * @param routeName 路由名称
     * @param icon 图标
     * @param sortOrder 排序值
     * @param matchMode 关联操作匹配方式
     * @param accessMode 开放或按操作保护
     * @author jy
     * @since 1.0.0
     */
    public record MenuRow(long id, long applicationId, Long parentId, String name, MenuKind kind, String path,
                          String viewPath, String routeName, String icon, int sortOrder, ActionMatchMode matchMode,
                          MenuAccessMode accessMode) {
    }
}
