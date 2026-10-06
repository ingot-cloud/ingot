package com.ingot.cloud.iam.extension;

import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.ingot.cloud.iam.persistence.mapper.AuthorizationCandidateMapper;
import com.ingot.framework.authorization.AuthorizationAccess;
import com.ingot.framework.authorization.AuthorizationClient;
import com.ingot.framework.authorization.IamActionAspect;
import com.ingot.framework.authorization.RemoteResourceObjectProvider;
import com.ingot.framework.authorization.ResourceObjectProvider;
import com.ingot.framework.authorization.ResourceObjectRpc;
import com.ingot.framework.authorization.ResourceRegistry;
import com.ingot.framework.authorization.ResourceRpcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>显式装配可信provider，注册业务Bean即可接入新应用。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ResourceRpcProperties.class)
public class ResourceExtensionConfiguration {

    /**
     * 装配完整命名空间注册表。
     * @param mapper 内置查询
     * @param extensions 业务provider
     * @return 注册表
     */
    @Bean
    public ResourceRegistry resourceRegistry(AuthorizationCandidateMapper mapper,
            List<ResourceObjectProvider> extensions, ResourceRpcProperties properties, ApplicationContext context,
            ObjectMapper json) {
        var providers = new ArrayList<>(extensions);
        for (var registration : properties.getRegistrations()) {
            if (registration.getDescriptor() == null || registration.getServiceName() == null
                    || !registration.getServiceName().matches("[a-zA-Z][a-zA-Z0-9-]*")) {
                throw new IllegalArgumentException("远程资源必须指定完整描述与服务发现名称");
            }
            var remote = new FeignClientBuilder(context).forType(ResourceObjectRpc.class, registration.getServiceName())
                .build();
            providers.add(new RemoteResourceObjectProvider(registration.getDescriptor(), remote,
                    registration.getSecret(), json));
        }
        return new BuiltinResourceProviders(mapper).registry(providers);
    }

    /**
     * 通用准入门禁。
     * @param client 本地引擎
     * @return 门禁
     */
    @Bean
    public AuthorizationAccess authorizationAccess(AuthorizationClient client) {
        return new AuthorizationAccess(client);
    }

    /**
     * 精确注解入口。
     * @param access 通用门禁
     * @return 切面
     */
    @Bean
    public IamActionAspect iamActionAspect(AuthorizationAccess access) {
        return new IamActionAspect(access);
    }

}
