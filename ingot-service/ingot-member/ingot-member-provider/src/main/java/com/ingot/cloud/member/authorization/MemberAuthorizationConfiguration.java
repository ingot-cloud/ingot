package com.ingot.cloud.member.authorization;

import java.util.List;

import com.ingot.framework.authorization.ResourceObjectProvider;
import com.ingot.framework.authorization.ResourceRegistry;
import com.ingot.framework.commons.model.iam.AuthorizationCandidatePage;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.MemberAdminResources;
import com.ingot.framework.commons.model.iam.extension.ResourceDescriptor;
import com.ingot.framework.commons.model.iam.extension.ResourceObjectQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>注册 Member 服务执行的三个全局平台资源。</p>
 * @author jy
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
public class MemberAuthorizationConfiguration {
    /** 将可信资源描述接入在线授权 SDK。 */
    @Bean
    public ResourceRegistry resourceRegistry() {
        List<ResourceObjectProvider> providers = MemberAdminResources.descriptors().stream()
                .<ResourceObjectProvider>map(GlobalProvider::new).toList();
        return new ResourceRegistry(providers);
    }

    /**
     * <p>全局资源不支持对象选择或对象绑定。</p>
     * @author jy
     * @since 1.0.0
     */
    private record GlobalProvider(ResourceDescriptor descriptor) implements ResourceObjectProvider {
        /** 返回固定的可信描述。 */
        @Override public ResourceDescriptor descriptor() { return descriptor; }

        /** 返回对象范围不受支持的结果。 */
        @Override public AuthorizationCandidatePage candidates(ResourceObjectQuery query) {
            return new AuthorizationCandidatePage(List.of(), 0, query.page(), query.pageSize(), false,
                    "App 用户管理资源仅支持全局范围");
        }

        /** 全局资源不接受对象绑定。 */
        @Override public boolean objectsExist(AuthorizationContext context, List<String> ids) { return false; }
    }
}
