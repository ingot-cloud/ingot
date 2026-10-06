package com.ingot.framework.authorization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldMergeMode;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.extension.ActionDecision;
import com.ingot.framework.commons.model.iam.extension.AuthorizationDecision;
import com.ingot.framework.commons.model.iam.extension.FieldPolicyDecision;
import com.ingot.framework.commons.model.iam.extension.ResolvedFieldRule;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import com.ingot.framework.commons.model.iam.extension.ScopeCondition;
import com.ingot.framework.commons.model.iam.extension.ScopeTarget;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>
 * 验证字段授权与操作、目标范围不可拼接，以及新契约缺失时拒绝。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
class RoleFieldAuthorizationTest {

    private static final String PHONE = "phone";

    private static final String READ = "iam-ops:incident:read";

    private static final String WRITE = "iam-ops:incident:update";

    private static final FieldAccess FULL = new FieldAccess(FieldVisibility.FULL, true);

    private static final FieldAccess MASKED = new FieldAccess(FieldVisibility.MASKED, false);

    private static final FieldAccess HIDDEN = new FieldAccess(FieldVisibility.HIDDEN, false);

    private static final ScopeCondition ALL = new ScopeCondition(true, List.of(), null, List.of());

    @Test
    void positiveUnionIsIndependentOfRuleOrder() {
        for (var rules : List.of(List.of(rule(ALL, HIDDEN), rule(ALL, FULL), rule(ALL, MASKED)),
                List.of(rule(ALL, FULL), rule(ALL, MASKED), rule(ALL, HIDDEN)))) {
            assertEquals(FULL, access(policy(rules), "A"));
        }
        assertEquals(MASKED, access(policy(List.of(rule(ALL, HIDDEN), rule(ALL, MASKED))), "A"));
    }

    @Test
    void fullNarrowScopeCannotExpandToBroaderMaskedScope() {
        var narrow = new ScopeCondition(false, List.of("A"), null, List.of());
        var policy = policy(List.of(rule(narrow, FULL), rule(ALL, MASKED)));
        assertEquals(FULL, access(policy, "A"));
        assertEquals(MASKED, access(policy, "B"));
        assertThrows(SdkAuthorizationException.class, () -> FieldPolicyProcessor.requireOriginalLookup(policy, PHONE));
        var fullAll = policy(List.of(rule(narrow, MASKED), rule(ALL, FULL)));
        FieldPolicyProcessor.requireOriginalLookup(fullAll, PHONE);
    }

    @Test
    void noMatchMissingNewFieldAndDirectoryCeilingFailClosed() {
        var narrow = new ScopeCondition(false, List.of("A"), null, List.of());
        assertEquals(HIDDEN, access(policy(List.of(rule(narrow, FULL))), "B"));
        var lowered = new FieldPolicyDecision(Map.of(PHONE, HIDDEN), Map.of(PHONE, MASKED), List.of(rule(ALL, FULL)),
                Set.of(), Set.of(), FieldMergeMode.GRANTS);
        assertEquals(MASKED, access(lowered, "A"));
        var empty = policy(List.of());
        assertEquals(HIDDEN, access(empty, "A"));
        assertThrows(SdkAuthorizationException.class, () -> FieldPolicyProcessor.requireOriginalSort(empty, PHONE));
    }

    @Test
    void readFullCannotSupplementMaskedWriteAndNullIsChecked() {
        var key = new ResourceKey(AuthorizationDomain.PLATFORM, "iam-ops", "incident");
        var view = new AuthorizationDecision(key,
                new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "2"),
                Map.of(READ, new ActionDecision(true, false, List.of(ALL), policy(List.of(rule(ALL, FULL)))), WRITE,
                        new ActionDecision(true, false, List.of(ALL), policy(List.of(rule(ALL, MASKED))))),
                "1", java.time.Instant.now().plusSeconds(60));
        assertEquals(FULL, access(FieldPolicyProcessor.forAction(view, READ), "A"));
        var submitted = new HashMap<String, Object>();
        submitted.put(PHONE, null);
        assertThrows(SdkAuthorizationException.class, () -> FieldPolicyProcessor.requireWritable(submitted,
                FieldPolicyProcessor.access(FieldPolicyProcessor.forAction(view, WRITE), target("A"))));
        var missing = new AuthorizationDecision(key, view.context(),
                Map.of(READ, new ActionDecision(true, false, List.of(ALL), null)), "1", view.expiresAt());
        assertThrows(SdkAuthorizationException.class, () -> FieldPolicyProcessor.forAction(missing, READ));
    }

    @Test
    void tenantRestrictionsStillPreferHidden() {
        var legacy = new FieldPolicyDecision(Map.of(PHONE, FULL), Map.of(PHONE, FULL),
                List.of(rule(ALL, FULL), rule(ALL, HIDDEN)));
        assertEquals(HIDDEN, access(legacy, "A"));
    }

    private static FieldPolicyDecision policy(List<ResolvedFieldRule> rules) {
        return new FieldPolicyDecision(Map.of(PHONE, HIDDEN), Map.of(PHONE, FULL), rules, Set.of(PHONE), Set.of(PHONE),
                FieldMergeMode.GRANTS);
    }

    private static ResolvedFieldRule rule(ScopeCondition scope, FieldAccess value) {
        return new ResolvedFieldRule(PHONE, List.of(scope), value);
    }

    private static ScopeTarget target(String id) {
        return new ScopeTarget(id, id, null, List.of());
    }

    private static FieldAccess access(FieldPolicyDecision policy, String id) {
        return FieldPolicyProcessor.access(policy, target(id)).get(PHONE);
    }

}
