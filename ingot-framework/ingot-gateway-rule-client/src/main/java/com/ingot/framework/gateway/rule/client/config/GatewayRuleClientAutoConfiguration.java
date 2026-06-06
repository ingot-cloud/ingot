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
 * <p>装配条件：{@code ingot.security.policy.client.enabled=true}（默认开启）。</p>
 *
 * <p>本类装配跨域共用的协调器与远端快照拉取器；各域（限流 / 黑白名单 / 挑战策略）
 * 的 Service 实现分别由子包内的 AutoConfiguration 负责，通过
 * {@code ingot.security.<domain>.policy.mode} 控制 local / remote 装配。</p>
 *
 * <h3>典型配置</h3>
 *
 * <pre>{@code
 * ingot:
 *   security:
 *     policy:
 *       client:
 *         enabled: true                 # SDK 总开关（默认 true）
 *         invalidation-enabled: true    # 跨节点失效订阅（生产建议开）
 *     ratelimit:
 *       enabled: true
 *       policy:
 *         mode: remote                  # 各域独立配置 local / remote
 *     blacklist:
 *       enabled: true
 *       policy:
 *         mode: remote
 *     challenge:
 *       enabled: true
 *       policy:
 *         mode: remote
 * }</pre>
 *
 * <p>remote 模式依赖本类装配的 {@link RemoteSnapshotFetcher}，需引入
 * {@code ingot-security-api} Feign 客户端并启用 {@code @EnableFeignClients}。</p>
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
     * 安全策略缓存协调器。
     * <p>激活条件：{@code invalidation-enabled=true}（默认）且 classpath 存在
     * {@link com.ingot.framework.eventbus.InvalidationBus} Bean。</p>
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
     * 共享的远端安全策略快照拉取器。
     * <p>激活条件：classpath 存在 {@link RemoteSecurityPolicyService} 且已注册为 Spring Bean
     *（需引入 ingot-security-api 并启用 {@code @EnableFeignClients}）。</p>
     * <p>各域 {@code policy.mode=remote} 时注入本 Bean 拉取全量快照。</p>
     */
    @Bean
    @ConditionalOnClass(RemoteSecurityPolicyService.class)
    @ConditionalOnBean(RemoteSecurityPolicyService.class)
    @ConditionalOnMissingBean
    public RemoteSnapshotFetcher remoteSnapshotFetcher(RemoteSecurityPolicyService remoteService) {
        return new RemoteSnapshotFetcher(remoteService);
    }
}
