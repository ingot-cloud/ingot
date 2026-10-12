package com.ingot.cloud.security.authorization;

import java.util.List;
import com.ingot.framework.authorization.ResourceObjectProvider;
import com.ingot.framework.authorization.ResourceRegistry;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.SecurityResources;
import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import com.ingot.framework.commons.model.iam.extension.ResourceObjectQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>只装配本服务实际执行的全局安全资源。</p>
 * @author jy
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
public class SecurityAuthorizationConfiguration {
    /** 提供与 IAM 目录完全一致的安全资源注册表。 */
    @Bean
    public ResourceRegistry resourceRegistry() {
        return new ResourceRegistry(SecurityResources.descriptors().stream()
                .map(GlobalProvider::new).map(ResourceObjectProvider.class::cast).toList());
    }

    /**
     * <p>全局配置无对象候选，不能用于 OBJECT_SET 授权。</p>
     * @author jy
     * @since 1.0.0
     */
    private record GlobalProvider(ResourceDescriptor descriptor) implements ResourceObjectProvider {
        /** 返回固定的可信描述。 */
        @Override public ResourceDescriptor descriptor() { return descriptor; }
        /** 明确拒绝未实现的对象能力。 */
        @Override public AuthorizationCandidatePage candidates(ResourceObjectQuery query) {
            return new AuthorizationCandidatePage(List.of(), 0, query.page(), query.pageSize(), false, "全局安全资源不支持对象范围");
        }
        /** 全局资源没有可绑定对象。 */
        @Override public boolean objectsExist(AuthorizationContext context, List<String> ids) { return false; }
    }
}
