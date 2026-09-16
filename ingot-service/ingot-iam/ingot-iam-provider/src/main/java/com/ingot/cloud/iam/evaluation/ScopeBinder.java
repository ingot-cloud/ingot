package com.ingot.cloud.iam.evaluation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.ActionScopeCeiling;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeBindingKind;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.ScopeKind;

/**
 * <p>把角色合成后的范围表达式与分配/委派参数绑定为可执行条款，委派上限按交集收缩。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class ScopeBinder {
    private ScopeBinder() {
    }

    /**
     * 绑定一条授权的范围；空表达式表示有操作无对象。
     *
     * @param grant 已合成的操作授权
     * @param bindings 分配上的命名参数
     * @return 并集条款；无法绑定的管理/对象参数不扩大范围
     */
    public static List<ScopeClause> bind(ActionGrant grant, Map<String, ScopeBinding> bindings) {
        if (grant == null || grant.scopes() == null || grant.scopes().isEmpty()) {
            return List.of();
        }
        List<ScopeClause> clauses = new ArrayList<>();
        for (ScopeExpression expression : grant.scopes()) {
            ScopeClause clause = bind(expression, bindings);
            if (clause != null && !clause.empty()) {
                clauses.add(clause);
            }
        }
        return List.copyOf(clauses);
    }

    /**
     * 将授权条款与委派上限求交，上限缺省时原样返回。
     *
     * @param grantClauses 授权条款
     * @param ceiling 委派上限；{@code null} 表示无委派
     * @return 收缩后的条款；与上限不相交时为空
     */
    public static List<ScopeClause> constrain(List<ScopeClause> grantClauses, ActionScopeCeiling ceiling) {
        if (ceiling == null) {
            return grantClauses == null ? List.of() : List.copyOf(grantClauses);
        }
        return intersect(grantClauses, bind(ceiling));
    }

    /**
     * 绑定一条委派上限自身的范围条款。
     *
     * @param ceiling 委派逐操作上限
     * @return 并集条款；无范围表达式时为空
     */
    public static List<ScopeClause> bind(ActionScopeCeiling ceiling) {
        if (ceiling == null) {
            return List.of();
        }
        return bind(new ActionGrant(ceiling.actionId(), ceiling.scopes() == null ? List.of() : ceiling.scopes()),
                ceiling.scopeBindings() == null ? Map.of() : ceiling.scopeBindings());
    }

    /**
     * 判断委派上限是否覆盖请求的范围，用于写入时拒绝超限分配。
     *
     * <p>按字面比较：显式部门与对象 ID 必须逐个出现在同一条上限条款中，连带下级只能由同样连带下级的上限覆盖，
     * 不在写入路径展开部门树。无法证明被覆盖时按失败关闭判为超限。</p>
     *
     * @param ceilingClauses 委派上限条款
     * @param requestedClauses 分配请求的条款
     * @return 完全被覆盖时为 true；请求无对象时视为覆盖
     */
    public static boolean covers(List<ScopeClause> ceilingClauses, List<ScopeClause> requestedClauses) {
        if (requestedClauses == null || requestedClauses.isEmpty()) {
            return true;
        }
        if (ceilingClauses == null || ceilingClauses.isEmpty()) {
            return false;
        }
        return requestedClauses.stream()
                .allMatch(requested -> ceilingClauses.stream().anyMatch(ceiling -> covers(ceiling, requested)));
    }

    private static boolean covers(ScopeClause ceiling, ScopeClause requested) {
        if (ceiling.all()) {
            return true;
        }
        if (requested.all()) {
            return false;
        }
        if (requested.self() && !ceiling.self()) {
            return false;
        }
        if (requested.memberDepartments() && (!ceiling.memberDepartments()
                || (requested.memberDepartmentDescendants() && !ceiling.memberDepartmentDescendants()))) {
            return false;
        }
        if (!requested.departmentIds().isEmpty()
                && (!ceiling.departmentIds().containsAll(requested.departmentIds())
                || (requested.departmentDescendants() && !ceiling.departmentDescendants()))) {
            return false;
        }
        return requested.objectIds().isEmpty() || ceiling.objectIds().containsAll(requested.objectIds());
    }

    /**
     * 两组并集条款按分配律求交。
     *
     * @param left 左并集
     * @param right 右并集
     * @return 相交后仍覆盖对象的条款
     */
    public static List<ScopeClause> intersect(List<ScopeClause> left, List<ScopeClause> right) {
        if (left == null || left.isEmpty() || right == null || right.isEmpty()) {
            return List.of();
        }
        List<ScopeClause> result = new ArrayList<>();
        for (ScopeClause first : left) {
            for (ScopeClause second : right) {
                ScopeClause merged = first.intersect(second);
                if (!merged.empty()) {
                    result.add(merged);
                }
            }
        }
        return List.copyOf(result);
    }

    private static ScopeClause bind(ScopeExpression expression, Map<String, ScopeBinding> bindings) {
        if (expression == null || expression.kind() == null) {
            return null;
        }
        return switch (expression.kind()) {
            case ALL -> ScopeClause.universe();
            case SELF -> new ScopeClause(false, true, false, false, List.of(), false, List.of());
            case MEMBER_DEPARTMENTS -> new ScopeClause(false, false, true,
                    Boolean.TRUE.equals(expression.includeDescendants()), List.of(), false, List.of());
            case MANAGED_DEPARTMENTS -> departments(expression.parameterKey(), bindings,
                    Boolean.TRUE.equals(expression.includeDescendants()));
            case OBJECT_SET -> objects(expression.parameterKey(), bindings);
        };
    }

    private static ScopeClause departments(String parameterKey, Map<String, ScopeBinding> bindings,
                                           boolean descendants) {
        ScopeBinding binding = binding(parameterKey, bindings, ScopeBindingKind.DEPARTMENTS);
        if (binding == null || binding.ids() == null || binding.ids().isEmpty()) {
            return null;
        }
        return new ScopeClause(false, false, false, false, unique(binding.ids()), descendants, List.of());
    }

    private static ScopeClause objects(String parameterKey, Map<String, ScopeBinding> bindings) {
        ScopeBinding binding = binding(parameterKey, bindings, ScopeBindingKind.OBJECTS);
        if (binding == null || binding.ids() == null || binding.ids().isEmpty()) {
            return null;
        }
        return new ScopeClause(false, false, false, false, List.of(), false, unique(binding.ids()));
    }

    private static ScopeBinding binding(String parameterKey, Map<String, ScopeBinding> bindings,
                                        ScopeBindingKind expected) {
        if (parameterKey == null || parameterKey.isBlank() || bindings == null) {
            return null;
        }
        ScopeBinding binding = bindings.get(parameterKey);
        if (binding == null || binding.kind() != expected) {
            return null;
        }
        return binding;
    }

    private static List<String> unique(List<String> ids) {
        Set<String> values = new LinkedHashSet<>();
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                values.add(id);
            }
        }
        return List.copyOf(values);
    }
}
