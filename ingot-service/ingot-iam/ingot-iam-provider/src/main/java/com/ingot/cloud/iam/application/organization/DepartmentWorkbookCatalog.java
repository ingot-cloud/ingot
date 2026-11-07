package com.ingot.cloud.iam.application.organization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ingot.cloud.iam.infrastructure.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.framework.commons.error.BizException;
import com.ingot.cloud.iam.domain.workbook.DepartmentWorkbookColumns;
import com.ingot.cloud.iam.domain.workbook.DepartmentWorkbookReasons;
import com.ingot.framework.commons.model.iam.IamReasonCode;

/**
 * <p>按当前可见部门树建立路径索引，解析上级路径、校验同级重名，并按树前序展开导出范围。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class DepartmentWorkbookCatalog {
    private static final String ROOT_KEY = "";
    private final Map<String, IamDepartmentEntity> byId = new LinkedHashMap<>();
    private final Map<String, List<String>> idsByPath = new LinkedHashMap<>();
    private final Map<String, String> pathById = new LinkedHashMap<>();
    private final Map<String, List<String>> childrenByParent = new LinkedHashMap<>();

    /**
     * 用可见部门行建立路径索引；同名路径全部保留以便判歧义。
     *
     * @param rows 当前可见部门
     */
    public DepartmentWorkbookCatalog(List<IamDepartmentEntity> rows) {
        if (rows != null) {
            for (IamDepartmentEntity row : rows) {
                index(row);
            }
        }
    }

    /**
     * 把本文件已成功创建的部门并入索引，供后续行引用。
     *
     * @param row 新建部门
     */
    public void register(IamDepartmentEntity row) {
        index(row);
    }

    /**
     * 格式化部门从根到自身的名称路径。
     *
     * @param departmentId 部门 ID
     * @return 路径；未知时为空串
     */
    public String format(String departmentId) {
        return pathById.getOrDefault(departmentId, "");
    }

    /**
     * 解析上级路径为唯一部门 ID。
     *
     * @param path 从根开始的名称路径
     * @return 部门 ID
     */
    public String requireParent(String path) {
        List<String> ids = idsByPath.get(path);
        if (ids == null || ids.isEmpty()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), DepartmentWorkbookReasons.PARENT_NOT_FOUND);
        }
        if (ids.size() > 1) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), DepartmentWorkbookReasons.PARENT_AMBIGUOUS);
        }
        return ids.getFirst();
    }

    /**
     * 同一上级下是否已有同名部门。
     *
     * @param parentId 父部门 ID，根为空
     * @param name 部门名称
     * @param excludeId 更新时排除自身
     * @return 已占用时为 true
     */
    public boolean siblingTaken(String parentId, String name, String excludeId) {
        List<String> children = childrenByParent.getOrDefault(parentKey(parentId), List.of());
        for (String childId : children) {
            if (excludeId != null && excludeId.equals(childId)) {
                continue;
            }
            IamDepartmentEntity child = byId.get(childId);
            if (child != null && name.equals(child.getName() == null ? "" : child.getName().trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 读取已索引部门。
     *
     * @param departmentId 部门 ID
     * @return 部门行；未知时为空
     */
    public IamDepartmentEntity get(String departmentId) {
        return byId.get(departmentId);
    }

    /**
     * 判断把部门挂到指定上级是否形成环。
     *
     * @param departmentId 被移动的部门
     * @param parentId 新上级
     * @return 会形成环时为 true
     */
    public boolean wouldCycle(String departmentId, String parentId) {
        if (departmentId == null || departmentId.isBlank() || parentId == null || parentId.isBlank()) {
            return false;
        }
        if (departmentId.equals(parentId)) {
            return true;
        }
        String current = parentId;
        Set<String> seen = new HashSet<>();
        while (current != null && seen.add(current)) {
            if (departmentId.equals(current)) {
                return true;
            }
            IamDepartmentEntity row = byId.get(current);
            if (row == null || row.getParentId() == null) {
                return false;
            }
            current = IamIds.text(row.getParentId().longValueExact());
        }
        return false;
    }

    /**
     * 按当前实体重算路径与同级索引，供本文件内后续行使用。
     */
    public void refresh() {
        List<IamDepartmentEntity> rows = new ArrayList<>(byId.values());
        byId.clear();
        idsByPath.clear();
        pathById.clear();
        childrenByParent.clear();
        for (IamDepartmentEntity row : rows) {
            index(row);
        }
    }

    /**
     * 按所选部门及其可见下级做树前序；空选择表示全部可见部门。
     *
     * @param selectedIds 勾选的部门 ID
     * @return 导出行顺序
     */
    public List<IamDepartmentEntity> exportRows(List<String> selectedIds) {
        Set<String> included = new LinkedHashSet<>();
        if (selectedIds == null || selectedIds.isEmpty()) {
            included.addAll(byId.keySet());
        } else {
            for (String id : selectedIds) {
                if (byId.containsKey(id)) {
                    collectDescendants(id, included);
                }
            }
        }
        List<IamDepartmentEntity> ordered = new ArrayList<>();
        for (IamDepartmentEntity root : roots()) {
            walk(root, included, ordered);
        }
        return List.copyOf(ordered);
    }

    private void index(IamDepartmentEntity row) {
        String id = IamIds.text(row.getId().longValueExact());
        byId.put(id, row);
        String parentId = row.getParentId() == null ? null : IamIds.text(row.getParentId().longValueExact());
        childrenByParent.computeIfAbsent(parentKey(parentId), key -> new ArrayList<>());
        List<String> siblings = childrenByParent.get(parentKey(parentId));
        if (!siblings.contains(id)) {
            siblings.add(id);
        }
        String path = pathOf(row);
        pathById.put(id, path);
        idsByPath.computeIfAbsent(path, key -> new ArrayList<>());
        List<String> ids = idsByPath.get(path);
        if (!ids.contains(id)) {
            ids.add(id);
        }
    }

    private String pathOf(IamDepartmentEntity row) {
        List<String> names = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        IamDepartmentEntity current = row;
        while (current != null) {
            String id = IamIds.text(current.getId().longValueExact());
            if (!seen.add(id)) {
                break;
            }
            names.addFirst(current.getName() == null ? "" : current.getName().trim());
            if (current.getParentId() == null) {
                break;
            }
            current = byId.get(IamIds.text(current.getParentId().longValueExact()));
        }
        return String.join(DepartmentWorkbookColumns.PATH_SEPARATOR, names);
    }

    private void collectDescendants(String id, Set<String> included) {
        if (!included.add(id)) {
            return;
        }
        for (String childId : childrenByParent.getOrDefault(id, List.of())) {
            collectDescendants(childId, included);
        }
    }

    private List<IamDepartmentEntity> roots() {
        List<IamDepartmentEntity> roots = new ArrayList<>();
        for (IamDepartmentEntity row : byId.values()) {
            if (row.getParentId() == null || !byId.containsKey(IamIds.text(row.getParentId().longValueExact()))) {
                roots.add(row);
            }
        }
        roots.sort(order());
        return roots;
    }

    private void walk(IamDepartmentEntity row, Set<String> included, List<IamDepartmentEntity> ordered) {
        String id = IamIds.text(row.getId().longValueExact());
        if (included.contains(id)) {
            ordered.add(row);
        }
        List<IamDepartmentEntity> children = new ArrayList<>();
        for (String childId : childrenByParent.getOrDefault(id, List.of())) {
            IamDepartmentEntity child = byId.get(childId);
            if (child != null) {
                children.add(child);
            }
        }
        children.sort(order());
        for (IamDepartmentEntity child : children) {
            walk(child, included, ordered);
        }
    }

    private static Comparator<IamDepartmentEntity> order() {
        return Comparator
                .comparing((IamDepartmentEntity row) -> row.getSortOrder() == null ? 0 : row.getSortOrder())
                .thenComparing(row -> row.getId().longValueExact());
    }

    private static String parentKey(String parentId) {
        return parentId == null || parentId.isBlank() ? ROOT_KEY : parentId;
    }
}
