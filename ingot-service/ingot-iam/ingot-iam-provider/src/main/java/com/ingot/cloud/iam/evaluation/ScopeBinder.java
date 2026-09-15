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
     * 将授权条款与委派上限求交；上限不含该操作时返回 {@code null} 表示整项无效。
     *
     * @param grantClauses 授权条款
     * @param ceiling 委派上限；空表示无委派
     * @return 收缩后的条款；操作不在上限内时为 {@code null}
     */
    public static List<ScopeClause> constrain(List<ScopeClause> grantClauses, ActionScopeCeiling ceiling) {
        if (ceiling == null) {
            return grantClauses == null ? List.of() : List.copyOf(grantClauses);
        }
        List<ScopeClause> ceilingClauses = bind(new ActionGrant(ceiling.actionId(),
                ceiling.scopes() == null ? List.of() : ceiling.scopes()),
                ceiling.scopeBindings() == null ? Map.of() : ceiling.scopeBindings());
        return intersect(grantClauses, ceilingClauses);
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
