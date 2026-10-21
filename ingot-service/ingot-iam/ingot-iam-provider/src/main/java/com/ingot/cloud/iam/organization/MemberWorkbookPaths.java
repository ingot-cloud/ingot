package com.ingot.cloud.iam.organization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.support.IamIds;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberDepartmentBinding;
import com.ingot.framework.commons.model.iam.MemberDepartmentView;
import com.ingot.framework.commons.model.iam.MemberWorkbookColumns;
import com.ingot.framework.commons.model.iam.MemberWorkbookReasons;

/**
 * <p>按当前可见部门树建立从根部门名开始的路径索引，解析导入单元格并格式化导出。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class MemberWorkbookPaths {
    private final Map<String, List<String>> idsByPath;
    private final Map<String, String> pathById;

    /**
     * 用可见部门行建立路径索引；同名路径全部保留以便判歧义。
     *
     * @param rows 当前可见部门
     */
    public MemberWorkbookPaths(List<IamDepartmentEntity> rows) {
        Map<String, IamDepartmentEntity> byId = new LinkedHashMap<>();
        for (IamDepartmentEntity row : rows) {
            byId.put(IamIds.text(row.getId().longValueExact()), row);
        }
        Map<String, List<String>> ids = new LinkedHashMap<>();
        Map<String, String> paths = new LinkedHashMap<>();
        for (IamDepartmentEntity row : rows) {
            String id = IamIds.text(row.getId().longValueExact());
            String path = pathOf(row, byId);
            paths.put(id, path);
            ids.computeIfAbsent(path, key -> new ArrayList<>()).add(id);
        }
        this.idsByPath = Map.copyOf(ids);
        this.pathById = Map.copyOf(paths);
    }

    /**
     * 格式化任职为路径单元格，第一项为主部门。
     *
     * @param departments 成员任职
     * @return 路径文本；没有任职时为空串
     */
    public String format(List<MemberDepartmentView> departments) {
        if (departments == null || departments.isEmpty()) {
            return "";
        }
        List<MemberDepartmentView> ordered = new ArrayList<>(departments);
        ordered.sort((left, right) -> Boolean.compare(right.primary(), left.primary()));
        List<String> paths = new ArrayList<>();
        for (MemberDepartmentView department : ordered) {
            String path = pathById.get(department.id());
            if (path != null && !path.isBlank()) {
                paths.add(path);
            }
        }
        return String.join(MemberWorkbookColumns.DEPARTMENT_SEPARATOR, paths);
    }

    /**
     * 解析路径单元格为任职绑定，第一项为主部门。
     *
     * @param cell 路径文本
     * @return 任职；空白表示清空
     */
    public List<MemberDepartmentBinding> parse(String cell) {
        if (cell == null || cell.isBlank()) {
            return List.of();
        }
        String[] parts = cell.split(MemberWorkbookColumns.DEPARTMENT_SEPARATOR, -1);
        List<MemberDepartmentBinding> bindings = new ArrayList<>();
        for (int index = 0; index < parts.length; index++) {
            String path = parts[index].trim();
            if (path.isEmpty()) {
                continue;
            }
            bindings.add(new MemberDepartmentBinding(requireId(path), index == 0));
        }
        return List.copyOf(bindings);
    }

    private String requireId(String path) {
        List<String> ids = idsByPath.get(path);
        if (ids == null || ids.isEmpty()) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(), MemberWorkbookReasons.DEPARTMENT_NOT_FOUND);
        }
        if (ids.size() > 1) {
            throw new BizException(IamReasonCode.INVALID_ARGUMENT.getCode(),
                    MemberWorkbookReasons.DEPARTMENT_AMBIGUOUS);
        }
        return ids.getFirst();
    }

    private static String pathOf(IamDepartmentEntity row, Map<String, IamDepartmentEntity> byId) {
        List<String> names = new ArrayList<>();
        IamDepartmentEntity current = row;
        while (current != null) {
            names.addFirst(current.getName() == null ? "" : current.getName().trim());
            if (current.getParentId() == null) {
                break;
            }
            current = byId.get(IamIds.text(current.getParentId().longValueExact()));
        }
        return String.join(MemberWorkbookColumns.PATH_SEPARATOR, names);
    }
}
