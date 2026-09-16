package com.ingot.cloud.iam.evaluation;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.persistence.mapper.IamDepartmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * <p>在租户部门树上展开下级，一次加载当前租户树后在内存中 BFS，供范围编译复用。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class DepartmentClosure {
    private final IamDepartmentMapper departments;

    /**
     * 读取当前租户的父子索引，只选择展开所需列。
     *
     * @param tenantId 已授权租户 ID
     * @return 父部门到直接子部门
     */
    public Map<BigInteger, List<BigInteger>> loadChildren(BigInteger tenantId) {
        Map<BigInteger, List<BigInteger>> children = new HashMap<>();
        for (IamDepartmentEntity row : departments.selectList(Wrappers.<IamDepartmentEntity>lambdaQuery()
                .select(IamDepartmentEntity::getId, IamDepartmentEntity::getParentId)
                .eq(IamDepartmentEntity::getTenantId, tenantId))) {
            if (row.getParentId() != null) {
                children.computeIfAbsent(row.getParentId(), key -> new ArrayList<>()).add(row.getId());
            }
        }
        return children;
    }

    /**
     * 展开部门 ID；不包含下级时原样返回，不重复查询部门树。
     *
     * @param tenantId 已授权租户 ID
     * @param roots 起始部门
     * @param includeDescendants 是否并入全部下级
     * @return 去重后的部门 ID
     */
    public Set<BigInteger> expand(BigInteger tenantId, Collection<BigInteger> roots, boolean includeDescendants) {
        if (!includeDescendants) {
            return copy(roots);
        }
        return expand(loadChildren(tenantId), roots, true);
    }

    /**
     * 使用已加载的父子索引展开部门 ID。
     *
     * @param children 当前租户父子索引
     * @param roots 起始部门
     * @param includeDescendants 是否并入全部下级
     * @return 去重后的部门 ID
     */
    public Set<BigInteger> expand(Map<BigInteger, List<BigInteger>> children, Collection<BigInteger> roots,
                                  boolean includeDescendants) {
        Set<BigInteger> result = copy(roots);
        if (!includeDescendants || result.isEmpty() || children == null || children.isEmpty()) {
            return result;
        }
        ArrayDeque<BigInteger> pending = new ArrayDeque<>(result);
        Set<BigInteger> visited = new HashSet<>(result);
        while (!pending.isEmpty()) {
            BigInteger current = pending.removeFirst();
            for (BigInteger child : children.getOrDefault(current, List.of())) {
                if (visited.add(child)) {
                    result.add(child);
                    pending.addLast(child);
                }
            }
        }
        return result;
    }

    private static Set<BigInteger> copy(Collection<BigInteger> roots) {
        Set<BigInteger> result = new LinkedHashSet<>();
        if (roots == null) {
            return result;
        }
        for (BigInteger root : roots) {
            if (root != null) {
                result.add(root);
            }
        }
        return result;
    }
}
