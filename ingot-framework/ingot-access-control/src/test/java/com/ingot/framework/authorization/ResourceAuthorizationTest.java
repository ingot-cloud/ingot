package com.ingot.framework.authorization;

import java.util.List;
import java.util.Map;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.ScopeKind;
import com.ingot.framework.commons.model.iam.extension.ActionDescriptor;
import com.ingot.framework.commons.model.iam.extension.ExecutionMode;
import com.ingot.framework.commons.model.iam.extension.FieldPolicyDecision;
import com.ingot.framework.commons.model.iam.extension.ResolvedFieldRule;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import com.ingot.framework.commons.model.iam.extension.ResourceObjectQuery;
import com.ingot.framework.commons.model.iam.extension.ScopeCondition;
import com.ingot.framework.commons.model.iam.extension.ScopeTarget;
import com.ingot.framework.commons.model.iam.extension.SignedResourceObjectRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证独立资源范围、字段处理与服务调用证明的拒绝边界。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
class ResourceAuthorizationTest {

    @Test
    void conjunctionRetainsEveryDimensionAndDisjunctionDoesNotExpandEmptyScopes() {
        var scope = new ScopeCondition(false, List.of("uuid-a"), "member-a",
                List.of(List.of("dept-a", "dept-b"), List.of("dept-b")));
        assertTrue(ScopeRules.matches(List.of(scope),
                new ScopeTarget("uuid-a", "member-a", "tenant-a", List.of("dept-b"))));
        assertFalse(ScopeRules.matches(List.of(scope),
                new ScopeTarget("uuid-a", "member-b", "tenant-a", List.of("dept-b"))));
        assertFalse(ScopeRules.matches(List.of(scope),
                new ScopeTarget("uuid-a", "member-a", "tenant-a", List.of("dept-a"))));
        assertFalse(ScopeRules.matches(List.of(), new ScopeTarget("uuid-a", "member-a", null, List.of())));
        assertFalse(ScopeRules.matches(List.of(new ScopeCondition(false, List.of(), null, List.of())),
                new ScopeTarget("uuid-a", "member-a", null, List.of())));
    }

    @Test
    void matchingRulesReplaceDefaultsThenRestrictEachOtherAndTheCapabilityCeiling() {
        var full = new FieldAccess(FieldVisibility.FULL, true);
        var masked = new FieldAccess(FieldVisibility.MASKED, false);
        var all = List.of(new ScopeCondition(true, List.of(), null, List.of()));
        var policy = new FieldPolicyDecision(Map.of("secret", masked), Map.of("secret", full),
                List.of(new ResolvedFieldRule("secret", all, full)));
        var target = new ScopeTarget("a", "viewer", null, List.of());
        assertEquals(full, FieldPolicyProcessor.access(policy, target).get("secret"));
        var restricted = new FieldPolicyDecision(policy.defaults(), Map.of("secret", masked), policy.rules());
        assertEquals(masked, FieldPolicyProcessor.access(restricted, target).get("secret"));
        assertThrows(BizException.class, () -> FieldPolicyProcessor.requireOriginalLookup(restricted, "secret"));
    }

    @Test
    void projectionOmitsHiddenValuesAndWritesRejectUnknownNullAndMaskedPlaceholders() {
        var fields = Map.of("phone", new FieldAccess(FieldVisibility.MASKED, false), "hidden",
                new FieldAccess(FieldVisibility.HIDDEN, false), "name", new FieldAccess(FieldVisibility.FULL, true));
        var output = FieldPolicyProcessor.project(
                Map.of("phone", "123456", "hidden", "private", "name", "显示名", "unregistered", "raw"), fields, Map.of());
        assertEquals(Map.of("phone", "***", "name", "显示名"), output);
        assertThrows(BizException.class,
                () -> FieldPolicyProcessor.requireWritable(Map.of("phone", "changed"), fields));
        FieldPolicyProcessor.requireWritable(Map.of("name", "***"), fields);
        Map<String, Object> clear = new java.util.HashMap<>();
        clear.put("hidden", null);
        assertThrows(BizException.class, () -> FieldPolicyProcessor.requireWritable(clear, fields));
        FieldPolicyProcessor.requireWritable(Map.of("name", "new"), fields);
    }

    @Test
    void registrySeparatesSameResourceCodeAcrossApplicationsAndRejectsDuplicateKeys() {
        var a = provider("iam-ops");
        var b = provider("iam-reports");
        var registry = new ResourceRegistry(List.of(a, b));
        assertSame(a, registry.require(a.descriptor().key()));
        assertSame(b, registry.require(b.descriptor().key()));
        assertThrows(IllegalArgumentException.class, () -> new ResourceRegistry(List.of(a, provider("iam-ops"))));
        assertThrows(BizException.class,
                () -> registry.require(new ResourceKey(AuthorizationDomain.PLATFORM, "unknown", "incident")));
    }

    @Test
    void serviceSignatureRejectsPayloadTamperingWrongKeyAndExpiredRequests() {
        var secret = "test-only-resource-service-secret-32bytes";
        var signed = ResourceRpcSigner.sign("{\"context\":\"current\"}", secret);
        ResourceRpcSigner.verify(signed, secret);
        assertThrows(BizException.class, () -> ResourceRpcSigner
            .verify(new SignedResourceObjectRequest("replacement", signed.timestamp(), signed.signature()), secret));
        assertThrows(BizException.class,
                () -> ResourceRpcSigner.verify(signed, "different-service-test-secret-32bytes"));
        assertThrows(BizException.class,
                () -> ResourceRpcSigner.verify(
                        new SignedResourceObjectRequest(signed.payload(), signed.timestamp() - 60, signed.signature()),
                        secret));
    }

    @Test
    void rawLookupRequiresBothFullVisibilityAndRegisteredCapabilities() {
        var full = new FieldAccess(FieldVisibility.FULL, true);
        var policy = new FieldPolicyDecision(Map.of("title", full), Map.of("title", full), List.of(),
                Map.of("title", new com.ingot.framework.commons.model.iam.FieldOperations(true, true)), Map.of(), com.ingot.framework.commons.model.iam.FieldMergeMode.RESTRICTIONS);
        FieldPolicyProcessor.requireOriginalLookup(policy, "title");
        var hidden = new FieldPolicyDecision(policy.defaults(), policy.ceilings(),
                List.of(new ResolvedFieldRule("title",
                        List.of(new ScopeCondition(false, List.of("x"), null, List.of())),
                        new FieldAccess(FieldVisibility.HIDDEN, false))),
                policy.operations(), policy.masks(), policy.mergeMode());
        assertThrows(BizException.class, () -> FieldPolicyProcessor.requireOriginalLookup(hidden, "title"));
    }

    private static ResourceObjectProvider provider(String app) {
        var key = new ResourceKey(AuthorizationDomain.PLATFORM, app, "incident");
        var descriptor = new ResourceDescriptor(key,
                List.of(new ActionDescriptor(app + ":incident:read", ExecutionMode.READ_ONLY)),
                List.of(ScopeKind.ALL, ScopeKind.OBJECT_SET), List.of(), Map.of(), app + ":incident:read", false);
        return new ResourceObjectProvider() {
            public ResourceDescriptor descriptor() {
                return descriptor;
            }

            public AuthorizationCandidatePage candidates(ResourceObjectQuery q) {
                return new AuthorizationCandidatePage(List.of(), 0, q.page(), q.pageSize(), true, null);
            }

            public boolean objectsExist(AuthorizationContext actor, List<String> ids) {
                return true;
            }
        };
    }

}
