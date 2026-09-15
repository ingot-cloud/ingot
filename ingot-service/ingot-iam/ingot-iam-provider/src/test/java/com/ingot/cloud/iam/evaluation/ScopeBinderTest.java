package com.ingot.cloud.iam.evaluation;

import java.util.List;
import java.util.Map;

import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.ActionScopeCeiling;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeBindingKind;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.ScopeKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>验证范围绑定、空对象范围以及委派上限求交。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class ScopeBinderTest {
    @Test
    void emptyGrantScopesYieldNoObjects() {
        assertTrue(ScopeBinder.bind(new ActionGrant("1", List.of()), Map.of()).isEmpty());
    }

    @Test
    void allIsUniverse() {
        List<ScopeClause> clauses = ScopeBinder.bind(
                new ActionGrant("1", List.of(new ScopeExpression(ScopeKind.ALL, null, null))), Map.of());
        assertEquals(1, clauses.size());
        assertTrue(clauses.getFirst().all());
    }

    @Test
    void missingManagedBindingDoesNotWiden() {
        List<ScopeClause> clauses = ScopeBinder.bind(new ActionGrant("1",
                        List.of(new ScopeExpression(ScopeKind.MANAGED_DEPARTMENTS, "depts", true))),
                Map.of());
        assertTrue(clauses.isEmpty());
    }

    @Test
    void delegationCeilingIntersectsManagedDepartments() {
        ActionGrant grant = new ActionGrant("1",
                List.of(new ScopeExpression(ScopeKind.MANAGED_DEPARTMENTS, "depts", false)));
        Map<String, ScopeBinding> assignment = Map.of("depts",
                new ScopeBinding(ScopeBindingKind.DEPARTMENTS, List.of("11", "12")));
        ActionScopeCeiling ceiling = new ActionScopeCeiling("1",
                List.of(new ScopeExpression(ScopeKind.MANAGED_DEPARTMENTS, "limit", false)),
                Map.of("limit", new ScopeBinding(ScopeBindingKind.DEPARTMENTS, List.of("12", "13"))));
        List<ScopeClause> result = ScopeBinder.constrain(ScopeBinder.bind(grant, assignment), ceiling);
        assertEquals(1, result.size());
        assertEquals(List.of("12"), result.getFirst().departmentIds());
        assertFalse(result.getFirst().all());
    }
}
