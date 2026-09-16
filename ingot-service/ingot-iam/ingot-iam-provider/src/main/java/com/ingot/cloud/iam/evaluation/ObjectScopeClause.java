package com.ingot.cloud.iam.evaluation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>一条已展开的对象范围合取条件，ID 集合与任职部门集合同时成立才匹配。</p>
 *
 * @param requiredIds 目标对象 ID；空表示不限制 ID
 * @param departmentSets 每组部门各需命中至少一条任职；空表示不限制任职
 * @author jy
 * @since 1.0.0
 */
public record ObjectScopeClause(Set<BigInteger> requiredIds, List<Set<BigInteger>> departmentSets) {
    /**
     * 复制集合，避免编译结果被调用方修改。
     */
    public ObjectScopeClause {
        requiredIds = copyIds(requiredIds);
        departmentSets = copySets(departmentSets);
    }

    private static Set<BigInteger> copyIds(Set<BigInteger> ids) {
        return ids == null || ids.isEmpty() ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(ids));
    }

    private static List<Set<BigInteger>> copySets(List<Set<BigInteger>> sets) {
        if (sets == null || sets.isEmpty()) {
            return List.of();
        }
        List<Set<BigInteger>> copied = new ArrayList<>(sets.size());
        for (Set<BigInteger> set : sets) {
            copied.add(copyIds(set));
        }
        return Collections.unmodifiableList(copied);
    }
}
