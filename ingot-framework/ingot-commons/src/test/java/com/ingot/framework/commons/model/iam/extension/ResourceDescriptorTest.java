package com.ingot.framework.commons.model.iam.extension;

import java.util.List;
import java.util.Map;

import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.FieldCapability;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.ScopeKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>验证无字段资源的空配置绑定不会启动失败，有字段资源仍必须显式声明安全默认值。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class ResourceDescriptorTest {
    private static final ResourceKey KEY = new ResourceKey(AuthorizationDomain.PLATFORM, "example", "incident");
    private static final String READ = "example:incident:read";
    private static final List<ActionDescriptor> ACTIONS = List.of(new ActionDescriptor(READ, ExecutionMode.READ_ONLY));

    @Test
    void zeroFieldResourceAcceptsAnOmittedEmptyDefaultMap() {
        var descriptor = new ResourceDescriptor(KEY, ACTIONS, List.of(ScopeKind.ALL), List.of(), null, READ, false, false);
        assertEquals(Map.of(), descriptor.defaults());
    }

    @Test
    void declaredFieldsStillRequireExplicitDefaults() {
        var fields = List.of(new FieldCapability("title", "标题", List.of(FieldVisibility.FULL), false, false, null));
        assertThrows(IllegalArgumentException.class, () -> new ResourceDescriptor(KEY, ACTIONS,
            List.of(ScopeKind.ALL), fields, Map.of(), READ, false, false));
        assertThrows(NullPointerException.class, () -> new ResourceDescriptor(KEY, ACTIONS,
            List.of(ScopeKind.ALL), fields, null, READ, false, false));
    }
}
