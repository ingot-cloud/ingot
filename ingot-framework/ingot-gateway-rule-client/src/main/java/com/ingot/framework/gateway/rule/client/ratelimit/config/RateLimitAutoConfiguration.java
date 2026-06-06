package com.ingot.framework.gateway.rule.client.ratelimit.config;

import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.framework.gateway.rule.client.internal.RemoteSnapshotFetcher;
import com.ingot.framework.gateway.rule.client.internal.SecurityPolicyCacheCoordinator;
import com.ingot.framework.gateway.rule.client.ratelimit.RateLimitRuleService;
import com.ingot.framework.gateway.rule.client.ratelimit.internal.LocalRateLimitRuleService;
import com.ingot.framework.gateway.rule.client.ratelimit.internal.RemoteRateLimitRuleService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 限流域 SDK 自动配置。
 *
 * <p>装配条件：{@code ingot.security.ratelimit.enabled=true}。</p>
 *
 * <ul>
 *     <li>{@code policy.mode=local}（默认）— 装配 {@link LocalRateLimitRuleService}，
 *         规则来自 {@link RateLimitProperties.Policy#getRules()} / yaml。</li>
 *     <li>{@code policy.mode=remote} — 装配 {@link RemoteRateLimitRuleService}，
 *         通过 {@code RemoteSnapshotFetcher} 调 ingot-service-security 的
 *         {@code /inner/security/policy/snapshot} 拉取。</li>
 * </ul>
 *
 * <p>同时把 {@link RateLimitRuleService#evictAll()} 注册到
 * {@link SecurityPolicyCacheCoordinator}（{@code RATE_LIMIT_RULE} 与
 * {@code ENDPOINT_GROUP} 域），保证 SDK 自身的 L1 缓存能在跨节点失效时清空。
 * 网关侧的 Sentinel 规则 reload 由
 * {@code SentinelGatewayConfiguration} 单独再注册一次回调（同域 List 串行，
 * 互不覆盖）。</p>
 *
 * <p>yaml 配置示例见 {@link RateLimitProperties}。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(prefix = "ingot.security.ratelimit",
        name = "enabled", havingValue = "true")
public class RateLimitAutoConfiguration {

    /**
     * local 模式限流规则服务。
     * <p>激活条件：{@code policy.mode=local}（默认）且容器中尚无 {@link RateLimitRuleService} Bean。</p>
     */
    @Bean
    @ConditionalOnMissingBean(RateLimitRuleService.class)
    @ConditionalOnProperty(prefix = "ingot.security.ratelimit.policy",
            name = "mode", havingValue = "local", matchIfMissing = true)
    public RateLimitRuleService localRateLimitRuleService(RateLimitProperties properties) {
        log.info("[RateLimit] using local rule service");
        return new LocalRateLimitRuleService(properties);
    }

    /**
     * remote 模式限流规则服务。
     * <p>激活条件：{@code policy.mode=remote} 且存在 {@link RemoteSnapshotFetcher} Bean。</p>
     */
    @Bean
    @ConditionalOnMissingBean(RateLimitRuleService.class)
    @ConditionalOnProperty(prefix = "ingot.security.ratelimit.policy",
            name = "mode", havingValue = "remote")
    public RateLimitRuleService remoteRateLimitRuleService(RemoteSnapshotFetcher fetcher) {
        log.info("[RateLimit] using remote rule service");
        return new RemoteRateLimitRuleService(fetcher);
    }

    /**
     * 注册到 Coordinator：收到 RATE_LIMIT_RULE / ENDPOINT_GROUP / ALL 失效事件时
     * 清空 ratelimit Service 的本地缓存。
     */
    @Bean
    static CoordinatorRegistrar rateLimitCoordinatorRegistrar() {
        return new CoordinatorRegistrar();
    }

    /**
     * 限流域缓存失效注册器：在 {@link jakarta.annotation.PostConstruct} 阶段把
     * {@link RateLimitRuleService#evictAll()} 挂到 Coordinator 的
     * {@code RATE_LIMIT_RULE} 与 {@code ENDPOINT_GROUP} 域。
     */
    @RequiredArgsConstructor
    static class CoordinatorRegistrar {

        @Autowired(required = false)
        private SecurityPolicyCacheCoordinator coordinator;

        @Autowired(required = false)
        private RateLimitRuleService rateLimitRuleService;

        @PostConstruct
        public void register() {
            if (coordinator == null || rateLimitRuleService == null) {
                return;
            }
            Runnable evict = rateLimitRuleService::evictAll;
            coordinator.register(SecurityPolicyDomain.RATE_LIMIT_RULE, evict);
            coordinator.register(SecurityPolicyDomain.ENDPOINT_GROUP, evict);
        }
    }
}
