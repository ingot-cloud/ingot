package com.ingot.cloud.iam.extension;

import java.util.List;
import com.ingot.cloud.iam.persistence.mapper.AuthorizationCandidateMapper;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.framework.commons.model.iam.extension.ResourceKey;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * <p>目录内置注册不再从安全操作的历史前缀猜测应用。</p>
 * @author jy
 * @since 1.0.0
 */
class SecurityResourceRegistryTest {
    @Test void securityNamespaceMatchesCatalogAndKeepsExactAction() {
        var registry=new BuiltinResourceProviders(mock(AuthorizationCandidateMapper.class)).registry(List.of());
        for (var descriptor:SecurityResources.descriptors()) {
            assertEquals(descriptor,registry.require(descriptor.key()).descriptor());
            assertNull(registry.find(new ResourceKey(AuthorizationDomain.PLATFORM,"iam-platform",descriptor.key().resourceCode())));
        }
    }
}
