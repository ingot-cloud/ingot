package com.ingot.framework.authorization.field;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.authorization.*;
import com.ingot.framework.commons.annotation.field.*;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证别名、多 DTO、不可变输出和原始提交键的拒绝边界。</p>
 * @author jy
 * @since 1.0.0
 */
class FieldBindingTest {
    private static final String PHONE = MemberFieldKey.VALUE_PHONE;
    private static final ResourceKey RESOURCE = new ResourceKey(AuthorizationDomain.PLATFORM, "test", "members");
    private static final String READ = "test:members:read";
    private static final String WRITE = "test:members:update";
    private final ObjectMapper mapper = new ObjectMapper();
    private final FieldBindingRegistry registry = new FieldBindingRegistry(mapper);

    @Test
    void mapsJsonAliasAndMultipleDtoTypesWithoutMutatingOriginal() {
        registry.register(Detail.class, RESOURCE, READ, FieldUse.READ);
        registry.register(ListRow.class, RESOURCE, READ, FieldUse.READ);
        var engine = new FieldProjectionEngine(mapper, registry, new DefaultMaskStrategy());
        var original = new Detail("id", "13812345678");
        var snapshot = new FieldReadSnapshot(Map.of(RESOURCE, Map.of(PHONE,
                new FieldAccess(FieldVisibility.MASKED, true))), Map.of(RESOURCE, Map.of(PHONE, MaskSpec.PHONE)));
        var projected = engine.project(original, RESOURCE, snapshot);
        assertEquals("138****5678", projected.get("contactPhone").textValue());
        assertEquals("13812345678", original.aphone());
        assertEquals(2, registry.manifest(RESOURCE).bindings().size());
        var hidden = engine.project(original, RESOURCE, new FieldReadSnapshot(Map.of(), Map.of()));
        assertTrue(hidden.has("id"));
        assertFalse(hidden.has("contactPhone"));
    }

    @Test
    void explicitNullIsSubmittedMissingIsUntouchedAndUnknownOrReadonlyIsRejected() throws Exception {
        var plan = registry.register(Patch.class, RESOURCE, WRITE, FieldUse.WRITE);
        assertTrue(FieldInputCollector.collect(mapper.readTree("{\"expectedVersion\":\"1\"}"), plan).isEmpty());
        var clear = FieldInputCollector.collect(mapper.readTree("{\"contactPhone\":null}"), plan);
        assertTrue(clear.get(RESOURCE).get(PHONE).isNull());
        assertThrows(SdkAuthorizationException.class,
                () -> FieldInputCollector.collect(mapper.readTree("{\"aphone\":\"secret\"}"), plan));
        assertThrows(SdkAuthorizationException.class,
                () -> FieldInputCollector.collect(mapper.readTree("{\"joinedAt\":null}"), plan));
    }

    @Test
    void nestedWriteUsesSameActualKeyValidationAndCannotIgnoreUnknownChild() throws Exception {
        var plan = registry.register(Command.class, RESOURCE, WRITE, FieldUse.WRITE);
        var clear = FieldInputCollector.collect(mapper.readTree("{\"profile\":{\"contactPhone\":null}}"), plan);
        assertTrue(clear.get(RESOURCE).get(PHONE).isNull());
        assertThrows(SdkAuthorizationException.class, () -> FieldInputCollector.collect(mapper.readTree("{\"profile\":{\"unknown\":null}}"), plan));
        assertThrows(SdkAuthorizationException.class, () -> FieldInputCollector.collect(mapper.readTree("{\"profile\":null}"), plan));
    }

    @Test
    void startupRejectsUnclassifiedConflictingAndDuplicateWriteMappings() {
        assertThrows(IllegalArgumentException.class, () -> registry.register(Unclassified.class, RESOURCE, READ, FieldUse.READ));
        assertThrows(IllegalArgumentException.class, () -> registry.register(Conflict.class, RESOURCE, READ, FieldUse.READ));
        assertThrows(IllegalArgumentException.class, () -> registry.register(Duplicate.class, RESOURCE, WRITE, FieldUse.WRITE));
    }

    @Test
    void maskedEditableMayWriteAndClearButHiddenCannot() {
        var masked = Map.of(PHONE, new FieldAccess(FieldVisibility.MASKED, true));
        FieldPolicyProcessor.requireWritable(Map.of(PHONE, "new-value"), masked);
        var clear = new java.util.HashMap<String, Object>();
        clear.put(PHONE, null);
        FieldPolicyProcessor.requireWritable(clear, masked);
        assertThrows(SdkAuthorizationException.class, () -> FieldPolicyProcessor.requireWritable(clear,
                Map.of(PHONE, new FieldAccess(FieldVisibility.HIDDEN, false))));
    }

    /** 单个嵌套修改对象；批量命令需要逐对象执行事务门禁。 */
    private record Command(@PublicField Patch profile) { }
    /** 验证实际 DTO 的分类、绑定或执行边界。 */
    private record Detail(@PublicField String id, @FieldBinding(key = PHONE) @JsonProperty("contactPhone") String aphone) { }
    /** 验证实际 DTO 的分类、绑定或执行边界。 */
    private record ListRow(@PublicField String id, @FieldBinding(key = PHONE) String phone) { }
    /** 验证实际 DTO 的分类、绑定或执行边界。 */
    private record Patch(@PublicField String expectedVersion,
            @FieldBinding(key = PHONE, uses = FieldUse.WRITE) @JsonProperty("contactPhone") String aphone,
            @FieldBinding(key = "joinedAt") String joinedAt) { }
    /** 验证实际 DTO 的分类、绑定或执行边界。 */
    private record Unclassified(String phone) { }
    /** 验证实际 DTO 的分类、绑定或执行边界。 */
    private record Conflict(@PublicField @FieldBinding(key = PHONE) String phone) { }
    /** 验证实际 DTO 的分类、绑定或执行边界。 */
    private record Duplicate(@FieldBinding(key = PHONE, uses = FieldUse.WRITE) String a,
            @FieldBinding(key = PHONE, uses = FieldUse.WRITE) String b) { }
}
