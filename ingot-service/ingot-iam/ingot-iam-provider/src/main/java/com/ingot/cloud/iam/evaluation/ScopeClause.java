package com.ingot.cloud.iam.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * <p>一条求值后的范围合取条件，同一授权内多项范围先并集，委派上限再求交。</p>
 *
 * @param all 覆盖当前域全部对象
 * @param self 仅覆盖操作者自身
 * @param memberDepartments 覆盖操作者任职部门中的对象
 * @param memberDepartmentDescendants 任职部门是否含下级
 * @param departmentIds 显式管理部门 ID
 * @param departmentDescendants 管理部门是否含下级
 * @param objectIds 显式对象 ID
 * @author jy
 * @since 1.0.0
 */
public record ScopeClause(boolean all, boolean self, boolean memberDepartments, boolean memberDepartmentDescendants,
                          List<String> departmentIds, boolean departmentDescendants, List<String> objectIds) {
    /**
     * 复制 ID 集合，避免缓存值被调用方修改。
     */
    public ScopeClause {
        departmentIds = copy(departmentIds);
        objectIds = copy(objectIds);
    }

    /**
     * 构造无约束的全域条款。
     *
     * @return 全域条款
     */
    public static ScopeClause universe() {
        return new ScopeClause(true, false, false, false, List.of(), false, List.of());
    }

    /**
     * 判断条款是否不覆盖任何对象。
     *
     * @return 无对象时为 true
     */
    public boolean empty() {
        return !all && !self && !memberDepartments && departmentIds.isEmpty() && objectIds.isEmpty();
    }

    /**
     * 与另一条款求交；全域与任意条款的交集为该条款。
     *
     * @param other 另一条款
     * @return 交集；不相交时为空条款
     */
    public ScopeClause intersect(ScopeClause other) {
        if (other == null || other.empty() || empty()) {
            return emptyClause();
        }
        if (all) {
            return other;
        }
        if (other.all) {
            return this;
        }
        boolean nextSelf = self || other.self;
        boolean nextMember = memberDepartments || other.memberDepartments;
        boolean nextMemberDescendants = descendantFlag(memberDepartments, memberDepartmentDescendants,
                other.memberDepartments, other.memberDepartmentDescendants);
        List<String> nextDepartments = intersectIds(departmentIds, other.departmentIds);
        boolean nextDeptDescendants = descendantFlag(!departmentIds.isEmpty(), departmentDescendants,
                !other.departmentIds.isEmpty(), other.departmentDescendants);
        List<String> nextObjects = intersectIds(objectIds, other.objectIds);
        ScopeClause result = new ScopeClause(false, nextSelf, nextMember, nextMemberDescendants, nextDepartments,
                nextDeptDescendants, nextObjects);
        if ((hasIds(departmentIds) && hasIds(other.departmentIds) && nextDepartments.isEmpty())
                || (hasIds(objectIds) && hasIds(other.objectIds) && nextObjects.isEmpty())) {
            return emptyClause();
        }
        return result.empty() ? emptyClause() : result;
    }

    private static ScopeClause emptyClause() {
        return new ScopeClause(false, false, false, false, List.of(), false, List.of());
    }

    private static boolean descendantFlag(boolean leftPresent, boolean leftDescendants, boolean rightPresent,
                                          boolean rightDescendants) {
        if (leftPresent && rightPresent) {
            return leftDescendants && rightDescendants;
        }
        if (leftPresent) {
            return leftDescendants;
        }
        return rightPresent && rightDescendants;
    }

    private static List<String> intersectIds(List<String> left, List<String> right) {
        if (!hasIds(left)) {
            return copy(right);
        }
        if (!hasIds(right)) {
            return copy(left);
        }
        LinkedHashSet<String> keep = new LinkedHashSet<>(left);
        keep.retainAll(right);
        return List.copyOf(keep);
    }

    private static boolean hasIds(List<String> ids) {
        return ids != null && !ids.isEmpty();
    }

    private static List<String> copy(List<String> ids) {
        return ids == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(ids));
    }
}
