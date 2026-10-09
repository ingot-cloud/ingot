package com.ingot.framework.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * <p>独立资源服务按显式密钥配置开放对象查询端点。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration(after = AuthorizationSdkConfiguration.class)
@EnableConfigurationProperties(ResourceRpcProperties.class)
public class ResourceRpcConfiguration {

    /**
     * 装配服务端。
     * @param registry 本服务注册
     * @param mapper JSON
     * @param properties 可信配置
     * @return 端点
     */
    @Bean
    @ConditionalOnProperty(prefix = "ingot.iam.resource-rpc", name = "secret")
    public ResourceObjectEndpoint resourceObjectEndpoint(ResourceRegistry registry, ObjectMapper mapper,
            ResourceRpcProperties properties) {
        ResourceRpcSigner.sign("", properties.getSecret());
        return new ResourceObjectEndpoint(registry, mapper, properties.getSecret());
    }

}
