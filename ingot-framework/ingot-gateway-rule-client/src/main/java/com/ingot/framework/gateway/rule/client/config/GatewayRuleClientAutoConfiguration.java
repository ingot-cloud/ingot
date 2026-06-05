package com.ingot.framework.gateway.rule.client.config;

import com.ingot.cloud.security.api.rpc.RemoteSecurityPolicyService;
import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.gateway.rule.client.internal.RemoteSnapshotFetcher;
import com.ingot.framework.gateway.rule.client.internal.SecurityPolicyCacheCoordinator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 安全策略客户端 SDK 顶层自动配置。
 *
 * <p>本类装配跨域共用的协调器；各域（限流 / 黑白名单 / 挑战策略）的 Loader
 * 与装饰链分别由子包内的 AutoConfiguration 负责，通过
 * {@code ingot.security.<domain>.policy.mode} 控制装配。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@AutoConfiguration
@EnableConfigurationProperties(GatewayRuleClientProperties.class)
@ConditionalOnProperty(prefix = "ingot.security.policy.client",
        name = "enabled", havingValue = "true", matchIfMissing = true)
public class GatewayRuleClientAutoConfiguration {

    /**
     * 仅在 InvalidationBus 可用且 invalidation 开关打开时装配协调器。
     */
    @Bean
    @ConditionalOnBean(InvalidationBus.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "ingot.security.policy.client",
            name = "invalidation-enabled", havingValue = "true", matchIfMissing = true)
    public SecurityPolicyCacheCoordinator securityPolicyCacheCoordinator(InvalidationBus bus) {
        return new SecurityPolicyCacheCoordinator(bus);
    }

    /**
     * 共享的远端快照拉取器；仅当存在 {@link RemoteSecurityPolicyService} Bean
     * （即引入了 Feign 客户端、且打开了 {@code @EnableFeignClients}）时装配。
     */
    @Bean
    @ConditionalOnClass(RemoteSecurityPolicyService.class)
    @ConditionalOnBean(RemoteSecurityPolicyService.class)
    @ConditionalOnMissingBean
    public RemoteSnapshotFetcher remoteSnapshotFetcher(RemoteSecurityPolicyService remoteService) {
        return new RemoteSnapshotFetcher(remoteService);
    }
}
