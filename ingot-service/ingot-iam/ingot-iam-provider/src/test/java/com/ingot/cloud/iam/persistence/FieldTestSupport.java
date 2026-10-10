package com.ingot.cloud.iam.persistence;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.iam.extension.*;
import com.ingot.cloud.iam.persistence.mapper.AuthorizationCandidateMapper;
import com.ingot.framework.authorization.field.*;
import com.ingot.framework.commons.annotation.field.FieldUse;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.iam.extension.*;
import org.springframework.beans.factory.ObjectProvider;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/**
 * <p>测试共用启动绑定；模拟对象仅代替无关缓存及目录持久化。</p>
 * @author jy
 * @since 1.0.0
 */
public final class FieldTestSupport {
    private FieldTestSupport() { }
    /** 无配置缓存，与生产缺省同义。 */
    @SuppressWarnings("unchecked")
    public static <T> ObjectProvider<T> noCache() { return mock(ObjectProvider.class); }
    /** 编译首轮 DTO 的真实投影器。 */
    public static FieldProjectionEngine projection() {
        var mapper = new ObjectMapper().findAndRegisterModules();
        mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        var registry = new FieldBindingRegistry(mapper);
        for (var resource : List.of(MemberResources.PLATFORM_MEMBER, MemberResources.TENANT_MEMBER, MemberResources.TENANT_DIRECTORY)) {
            String prefix = resource.applicationCode() + ":" + resource.resourceCode() + ":";
            registry.register(MemberRecord.class, resource, prefix + "read", FieldUse.READ);
            registry.register(MemberProfileInput.class, resource, prefix + "update", FieldUse.WRITE);
            registry.register(MemberCreateInput.class, resource, prefix + "create", FieldUse.WRITE);
        }
        return new FieldProjectionEngine(mapper, registry, new DefaultMaskStrategy());
    }
    /** 精确操作清单，覆盖多个业务测试资源。 */
    public static FieldManifestService manifests() {
        var service = mock(FieldManifestService.class);
        when(service.require(any())).thenAnswer(invocation -> {
            ResourceKey resource = invocation.getArgument(0);
            String prefix = resource.applicationCode() + ":" + resource.resourceCode() + ":";
            List<FieldBindingManifest.Binding> bindings = new ArrayList<>();
            for (var field : MemberFieldKey.values()) {
                bindings.add(new FieldBindingManifest.Binding(prefix + "read", FieldUse.READ, "Read", field.getValue(), field.getValue(), true));
                bindings.add(new FieldBindingManifest.Binding(prefix + "update", FieldUse.WRITE, "Patch", field.getValue(), field.getValue(), true));
                bindings.add(new FieldBindingManifest.Binding(prefix + "create", FieldUse.WRITE, "Create", field.getValue(), field.getValue(), true));
                bindings.add(new FieldBindingManifest.Binding(prefix + "read", FieldUse.FILTER, "Query", field.getValue(), field.getValue(), true));
            }
            return new FieldBindingManifest(resource, "test-version", bindings);
        });
        return service;
    }
    /** 单元策略测试中的当前平台上限。 */
    public static ResourceFieldMetadata metadata() {
        var registry = new BuiltinResourceProviders(mock(AuthorizationCandidateMapper.class)).registry(List.of());
        var service = mock(ResourceFieldMetadata.class);
        when(service.requireFresh(any(), anyBoolean())).thenAnswer(invocation ->
                new ResourceFieldMetadata.Entry("10", registry.require(invocation.getArgument(0)).descriptor()));
        return service;
    }
}
