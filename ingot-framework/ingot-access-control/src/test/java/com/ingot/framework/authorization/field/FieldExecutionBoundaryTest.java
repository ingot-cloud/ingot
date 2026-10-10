package com.ingot.framework.authorization.field;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.authorization.*;
import com.ingot.framework.commons.annotation.field.*;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.iam.extension.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>嵌套不可变对象、非文本限制与完整查询范围的公开执行边界。</p>
 * @author jy
 * @since 1.0.0
 */
class FieldExecutionBoundaryTest {
    private static final ResourceKey KEY = new ResourceKey(AuthorizationDomain.PLATFORM, "test", "member");
    private static final String READ = "test:member:read";
    private static final String PHONE = "phone";

    @Test
    void nestedCollectionAndRecordOmitHiddenValuesWithoutMutatingOriginal() {
        var mapper = new ObjectMapper();
        var registry = new FieldBindingRegistry(mapper);
        registry.register(Envelope.class, KEY, READ, FieldUse.READ);
        var engine = new FieldProjectionEngine(mapper, registry, new DefaultMaskStrategy());
        var raw = new Envelope(List.of(new Detail("1", "13812345678")), 1);
        var hidden = new FieldReadSnapshot(Map.of(), Map.of());
        assertFalse(engine.project(raw, KEY, hidden).get("items").get(0).has(PHONE));
        assertFalse(mapper.valueToTree(engine.projectRecord(raw, KEY, hidden)).get("items").get(0).has(PHONE));
        assertEquals("13812345678", raw.items().getFirst().phone());
        var masked = new FieldReadSnapshot(Map.of(KEY, Map.of(PHONE, new FieldAccess(FieldVisibility.MASKED, true))),
                Map.of(KEY, Map.of(PHONE, MaskSpec.PHONE)));
        assertEquals("138****5678", engine.project(raw, KEY, masked).get("items").get(0).get(PHONE).textValue());
        assertEquals(1, registry.manifest(KEY).bindings().size());
    }

    @Test
    void primitiveAndCrossDtoMaskTypeConflictsFailAtRegistration() {
        var registry = new FieldBindingRegistry(new ObjectMapper());
        assertThrows(IllegalArgumentException.class, () -> registry.register(Primitive.class, KEY, READ, FieldUse.READ));
        registry.register(Detail.class, KEY, READ, FieldUse.READ);
        assertThrows(IllegalArgumentException.class, () -> registry.register(NonText.class, KEY, READ, FieldUse.READ));
    }

    @Test
    void fullSelfSourceCannotAuthorizeFilteringAnAllQueryFromAnotherRole() {
        var full = new FieldAccess(FieldVisibility.FULL, false);
        var all = new ScopeCondition(true, List.of(), null, List.of());
        var self = new ScopeCondition(false, List.of(), "viewer", List.of());
        var policy = new FieldPolicyDecision(Map.of(PHONE, new FieldAccess(FieldVisibility.HIDDEN, false)), Map.of(PHONE, full),
                List.of(new ResolvedFieldRule(PHONE, List.of(self), full)), Map.of(PHONE, new FieldOperations(false, true)),
                Map.of(), FieldMergeMode.GRANTS);
        FieldPolicyProcessor.requireOriginalLookup(policy, PHONE, List.of(self));
        assertThrows(SdkAuthorizationException.class, () -> FieldPolicyProcessor.requireOriginalLookup(policy, PHONE, List.of(all)));
        assertThrows(SdkAuthorizationException.class, () -> FieldFilterExecutor.require(Map.of(PHONE, "original"), Map.of()));
    }

    /** 验证实际 DTO 的分类、绑定或执行边界。 */
    private record Envelope(@PublicField List<Detail> items, @PublicField Integer total) { }
    /** 验证实际 DTO 的分类、绑定或执行边界。 */
    private record Detail(@PublicField String id, @FieldBinding(key = PHONE) String phone) { }
    /** 验证实际 DTO 的分类、绑定或执行边界。 */
    private record Primitive(@FieldBinding(key = PHONE) int phone) { }
    /** 验证实际 DTO 的分类、绑定或执行边界。 */
    private record NonText(@FieldBinding(key = PHONE) Integer phone) { }
}
