package com.ingot.framework.gateway.rule.client.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.security.api.model.vo.policy.SecurityPolicySnapshotVO;
import com.ingot.cloud.security.api.rpc.RemoteSecurityPolicyService;
import com.ingot.framework.cache.config.LayeredCacheBuilder;
import com.ingot.framework.cache.config.LayeredCacheSettings;
import com.ingot.framework.cache.coordinator.CacheRefreshPublisher;
import com.ingot.framework.cache.registry.LayeredCacheRegistry;
import com.ingot.framework.cache.source.CacheSourceHolder;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.config.EventBusAutoConfiguration;
import com.ingot.framework.gateway.rule.client.actuate.SecurityPolicyEndpoint;
import com.ingot.framework.gateway.rule.client.blacklist.config.BlacklistProperties;
import com.ingot.framework.gateway.rule.client.challenge.config.ChallengeProperties;
import com.ingot.framework.gateway.rule.client.internal.FeignPolicySnapshotFetcher;
import com.ingot.framework.gateway.rule.client.internal.LocalPolicyEnvironmentRefreshListener;
import com.ingot.framework.gateway.rule.client.internal.LocalPolicyFloorSupplier;
import com.ingot.framework.gateway.rule.client.internal.RemoteSnapshotFetcher;
import com.ingot.framework.gateway.rule.client.internal.SecurityPolicyCacheCoordinator;
import com.ingot.framework.gateway.rule.client.ratelimit.config.RateLimitProperties;
import com.ingot.framework.gateway.rule.client.violation.config.ViolationEscalationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * <p>安全策略客户端 SDK 顶层自动配置，装配四域共享的快照缓存链与跨节点失效协调器。</p>
 *
 * <p>本类提供的是<b>能力</b>而非功能，因此没有总开关：缓存链仅在 Feign 客户端
 * {@link RemoteSecurityPolicyService} 已注册时装配，装配后不主动发请求（各域按需 lazy fetch），
 * 关闭它没有收益却会让域 {@code mode=remote} 无法注入依赖。配置分层如下，
 * 各层<b>互不级联</b>——任一层的状态不影响其他层对应的功能：</p>
 * <ul>
 *     <li><b>能力层</b>（无开关）：{@link FeignPolicySnapshotFetcher}、{@link LocalPolicyFloorSupplier}、
 *         共享快照缓存、{@link RemoteSnapshotFetcher}、{@link CacheSourceHolder}、
 *         {@link SecurityPolicyEndpoint}。</li>
 *     <li><b>基础设施调参</b>：{@code ingot.security.policy.client.*}，见 {@link GatewayRuleClientProperties}。</li>
 *     <li><b>功能层</b>：{@code ingot.security.<domain>.enabled} + {@code policy.mode}，
 *         由各域自己的 AutoConfiguration 门控。</li>
 * </ul>
 *
 * <p>本类<b>不</b>绑定各域 {@code *Properties}——那些由各域 AutoConfiguration 自行
 * {@code @EnableConfigurationProperties}。{@link LocalPolicyFloorSupplier} 经
 * {@link ObjectProvider} 延迟解析，因此域关闭时其地板片段自然为空，地板内容与域开关始终一致。</p>
 *
 * <p>必须排在 {@link EventBusAutoConfiguration} 之后：{@code securityPolicyCacheCoordinator}
 * 带有 {@code @ConditionalOnBean(InvalidationBus.class)}，若本类先于 event-bus 配置执行，
 * 条件不成立会导致协调器被<b>永久跳过</b>，表现为 Platform 改规则后网关侧不热更新。</p>
 *
 * @author jy
 * @since 2026/5/26
 * @see GatewayRuleClientProperties
 * @see GatewayRuleClientWiringReporter
 * @apiNote 启动期 {@link GatewayRuleClientWiringReporter} 会汇总打印各域装配结果与地板贡献来源，
 *          用于快速确认配置是否按预期生效。
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(EventBusAutoConfiguration.class)
@EnableConfigurationProperties(GatewayRuleClientProperties.class)
public class GatewayRuleClientAutoConfiguration {

    private static final String CACHE_NAME = "security-policy-snapshot";
    private static final TypeReference<SecurityPolicySnapshotVO> SNAPSHOT_TYPE = new TypeReference<>() {
    };

    @Bean
    @ConditionalOnBean(InvalidationBus.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "ingot.security.policy.client",
            name = "invalidation-enabled", havingValue = "true", matchIfMissing = true)
    public SecurityPolicyCacheCoordinator securityPolicyCacheCoordinator(InvalidationBus bus) {
        log.info("[SecurityPolicy] cache coordinator enabled");
        return new SecurityPolicyCacheCoordinator(bus);
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheSourceHolder securityPolicySourceHolder() {
        return new CacheSourceHolder();
    }

    /**
     * 共享快照的刷新事件发布器，供 Sentinel 等无缓存读者的组件订阅版本变化。
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheRefreshPublisher<SecurityPolicySnapshotVO> securityPolicyRefreshPublisher() {
        return new CacheRefreshPublisher<>(CACHE_NAME);
    }

    @Bean
    @ConditionalOnClass(RemoteSecurityPolicyService.class)
    @ConditionalOnBean(RemoteSecurityPolicyService.class)
    @ConditionalOnMissingBean
    public FeignPolicySnapshotFetcher feignPolicySnapshotFetcher(RemoteSecurityPolicyService remoteService) {
        return new FeignPolicySnapshotFetcher(remoteService);
    }

    /**
     * Nacos 地板供给器。四个域的 {@code *Properties} 均以 {@link ObjectProvider} 延迟解析，
     * 域未启用（Properties Bean 不存在）时该域地板片段为空，无需感知域开关。
     */
    @Bean
    @ConditionalOnBean(FeignPolicySnapshotFetcher.class)
    @ConditionalOnMissingBean
    public LocalPolicyFloorSupplier localPolicyFloorSupplier(
            ObjectProvider<RateLimitProperties> rateLimitProvider,
            ObjectProvider<BlacklistProperties> blacklistProvider,
            ObjectProvider<ChallengeProperties> challengeProvider,
            ObjectProvider<ViolationEscalationProperties> violationEscalationProvider) {
        return new LocalPolicyFloorSupplier(rateLimitProvider, blacklistProvider,
                challengeProvider, violationEscalationProvider);
    }

    /**
     * 四域共享的快照分层缓存。
     *
     * <p>缓存放在共享层而非各域内部，是把冷启动与全量失效后的远端调用从四次压到一次的关键。
     * 关闭弹性只是移除降级阶梯，L1/L2 仍然生效，且 {@link RemoteSnapshotFetcher} 始终存在，
     * 各域 {@code mode=remote} 不会因此注入失败。</p>
     */
    @Bean
    @ConditionalOnBean({FeignPolicySnapshotFetcher.class, LocalPolicyFloorSupplier.class})
    @ConditionalOnMissingBean(name = "securityPolicySnapshotCache")
    public LayeredCache<String, SecurityPolicySnapshotVO> securityPolicySnapshotCache(
            FeignPolicySnapshotFetcher loader,
            LocalPolicyFloorSupplier floorSupplier,
            CacheSourceHolder sourceHolder,
            CacheRefreshPublisher<SecurityPolicySnapshotVO> refreshPublisher,
            GatewayRuleClientProperties properties,
            ObjectProvider<StringRedisTemplate> redisProvider,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            ObjectProvider<LayeredCacheRegistry> registryProvider) {
        GatewayRuleClientProperties.Cache cacheProps = properties.getCache();
        LayeredCacheSettings settings = LayeredCacheSettings.builder()
                .l1Enabled(cacheProps.isL1Enabled())
                .l1Ttl(cacheProps.getL1Ttl())
                .l1MaximumSize(cacheProps.getL1MaximumSize())
                .l2Enabled(cacheProps.isL2Enabled())
                .l2Ttl(cacheProps.getL2Ttl())
                .resilienceEnabled(properties.isResilienceEnabled())
                .localFloorEnabled(properties.isLocalFloorEnabled())
                .build();

        StringRedisTemplate redisTemplate = redisProvider.getIfAvailable();
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();

        return LayeredCacheBuilder.<String, SecurityPolicySnapshotVO>named(CACHE_NAME)
                .loader(loader)
                .settings(settings)
                .emptyValue(SecurityPolicySnapshotVO::new)
                .sourceHolder(sourceHolder)
                .resilientSingleKey(redisTemplate, objectMapper, SNAPSHOT_TYPE,
                        properties.getLkgRedisKey(), floorSupplier)
                .l2SingleKey(redisTemplate, objectMapper, SNAPSHOT_TYPE, cacheProps.getL2RedisKey())
                .refreshPublisher(refreshPublisher)
                .registry(registryProvider.getIfAvailable())
                .build();
    }

    @Bean
    @ConditionalOnBean(name = "securityPolicySnapshotCache")
    @ConditionalOnMissingBean
    public RemoteSnapshotFetcher remoteSnapshotFetcher(
            LayeredCache<String, SecurityPolicySnapshotVO> securityPolicySnapshotCache,
            CacheSourceHolder sourceHolder) {
        return new RemoteSnapshotFetcher(securityPolicySnapshotCache, sourceHolder);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    @ConditionalOnBean(CacheSourceHolder.class)
    @ConditionalOnMissingBean
    public SecurityPolicyEndpoint securityPolicyEndpoint(CacheSourceHolder sourceHolder) {
        return new SecurityPolicyEndpoint(sourceHolder);
    }

    @Bean
    static GatewayRuleClientWiringReporter gatewayRuleClientWiringReporter() {
        return new GatewayRuleClientWiringReporter();
    }

    /**
     * Nacos / Spring Cloud 配置热更新监听；local 模式各域向此注册 {@code evictAll} 回调。
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.cloud.context.environment.EnvironmentChangeEvent")
    @ConditionalOnMissingBean
    public LocalPolicyEnvironmentRefreshListener localPolicyEnvironmentRefreshListener() {
        return new LocalPolicyEnvironmentRefreshListener();
    }
}
