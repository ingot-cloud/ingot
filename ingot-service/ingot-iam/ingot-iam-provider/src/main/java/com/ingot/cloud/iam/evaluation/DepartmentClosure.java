package com.ingot.cloud.iam.evaluation;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * <p>在租户部门树上展开下级，供管理部门与任职范围生成 SQL IN 集合，不在内存中过滤查询结果。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class DepartmentClosure {
    private DepartmentClosure() {
    }

    /**
     * 展开部门 ID；不包含下级时原样返回。
     *
     * @param jdbc IAM 目标库
     * @param tenantId 当前租户
     * @param roots 起始部门
     * @param includeDescendants 是否并入全部下级
     * @return 去重后的部门 ID
     */
    public static Set<Long> expand(NamedParameterJdbcTemplate jdbc, long tenantId, Collection<Long> roots,
                                   boolean includeDescendants) {
        Set<Long> result = new LinkedHashSet<>();
        if (roots == null) {
            return result;
        }
        for (Long root : roots) {
            if (root != null) {
                result.add(root);
            }
        }
        if (!includeDescendants || result.isEmpty()) {
            return result;
        }
        Map<Long, List<Long>> children = new HashMap<>();
        jdbc.query("SELECT id,parent_id FROM iam_department WHERE tenant_id=:tenantId", Map.of("tenantId", tenantId),
                (row, index) -> {
                    long id = row.getLong("id");
                    Long parent = row.getObject("parent_id") == null ? null : row.getLong("parent_id");
                    if (parent != null) {
                        children.computeIfAbsent(parent, key -> new java.util.ArrayList<>()).add(id);
                    }
                    return id;
                });
        ArrayDeque<Long> pending = new ArrayDeque<>(result);
        Set<Long> visited = new HashSet<>(result);
        while (!pending.isEmpty()) {
            Long current = pending.removeFirst();
            for (Long child : children.getOrDefault(current, List.of())) {
                if (visited.add(child)) {
                    result.add(child);
                    pending.addLast(child);
                }
            }
        }
        return result;
    }
}
