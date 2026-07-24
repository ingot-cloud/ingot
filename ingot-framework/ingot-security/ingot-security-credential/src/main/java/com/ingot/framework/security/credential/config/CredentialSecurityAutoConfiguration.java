package com.ingot.framework.security.credential.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.security.api.rpc.RemoteCredentialService;
import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.config.EventBusAutoConfiguration;
import com.ingot.framework.security.credential.actuate.CredentialPolicyEndpoint;
import com.ingot.framework.security.credential.internal.CredentialCacheCoordinator;
import com.ingot.framework.security.credential.internal.CredentialPolicyConfigServiceFactory;
import com.ingot.framework.security.credential.internal.CredentialPolicySourceHolder;
import com.ingot.framework.security.credential.internal.LastKnownGoodStore;
import com.ingot.framework.security.credential.internal.LocalFloorSupplier;
import com.ingot.framework.security.credential.internal.RedisCredentialPolicyConfigService;
import com.ingot.framework.security.credential.internal.RemoteCredentialPolicyConfigService;
import com.ingot.framework.security.credential.internal.ResilientCredentialPolicyConfigService;
import com.ingot.framework.security.credential.service.CredentialPolicyConfigService;
import com.ingot.framework.security.credential.service.CredentialPolicyLoader;
import com.ingot.framework.security.credential.service.CredentialSecurityService;
import com.ingot.framework.security.credential.service.InitialPasswordService;
import com.ingot.framework.security.credential.service.PasswordExpirationService;
import com.ingot.framework.security.credential.service.PasswordHistoryService;
import com.ingot.framework.security.credential.service.impl.DefaultCredentialSecurityService;
import com.ingot.framework.security.credential.service.impl.DefaultInitialPasswordService;
import com.ingot.framework.security.credential.service.impl.LocalCredentialPolicyLoader;
import com.ingot.framework.security.credential.service.impl.NoOpPasswordExpirationService;
import com.ingot.framework.security.credential.service.impl.NoOpPasswordHistoryService;
import com.ingot.framework.security.credential.service.impl.RemoteCredentialPolicyLoader;
import com.ingot.framework.security.credential.validator.DefaultPasswordValidator;
import com.ingot.framework.security.credential.validator.PasswordValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 凭证安全自动配置。
 * <p>
 * 装配顺序：
 * <ol>
 *     <li>L0 delegate：默认基于 {@link RemoteCredentialService} 的 Feign 实现；
 *         {@code ingot-security-provider} 进程内通过同名 bean 覆盖为本地 Mapper 直查。</li>
 *     <li>L2 Redis 共享缓存：当 {@link StringRedisTemplate} 存在且
 *         {@code ingot.security.credential.cache.l2-enabled=true} 时启用。</li>
 *     <li>L1 Caffeine：最外层，{@code ingot.security.credential.cache.l1-enabled=true} 时启用，
 *         作为 {@code @Primary} 暴露。</li>
 *     <li>跨节点失效：{@link InvalidationBus} 存在且 {@code invalidation-enabled=true} 时
 *         注册 {@link CredentialCacheCoordinator}。</li>
 * </ol>
 * <p>
 * 本类必须排在 {@link EventBusAutoConfiguration} 之后：{@code credentialCacheCoordinator} bean
 * 带 {@code @ConditionalOnBean(InvalidationBus.class)}；若早于 event-bus 配置类执行，
 * 会因总线尚未注册而导致协调器永远跳过。
 * </p>
 *
 * @author jymot
 * @since 2026-01-21
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(EventBusAutoConfiguration.class)
@EnableConfigurationProperties({CredentialSecurityProperties.class, CredentialCacheProperties.class})
public class CredentialSecurityAutoConfiguration {
    public static final String CREDENTIAL_POLICY_CONFIG_DELEGATE = "credentialPolicyConfigDelegate";

    /**
     * LKG 快照独立 Redis key（与 L1/L2 热缓存命名空间区分，长存 / 不过期）。
     */
    static final String LKG_REDIS_KEY = "in:credential:policy:lkg";

    /**
     * 凭证策略生效来源与降级计数持有者（降级可观测）。
     */
    @Bean
    @ConditionalOnMissingBean(CredentialPolicySourceHolder.class)
    public CredentialPolicySourceHolder credentialPolicySourceHolder() {
        return new CredentialPolicySourceHolder();
    }

    /**
     * Nacos 本地地板供给器：远程不可用且无 LKG 时的最终兜底来源。
     */
    @Bean
    @ConditionalOnMissingBean(LocalFloorSupplier.class)
    public LocalFloorSupplier credentialLocalFloorSupplier(CredentialSecurityProperties properties) {
        return new LocalFloorSupplier(properties);
    }

    /**
     * 最近成功快照（LKG）存储：Redis 独立 key 长存 / 不过期，为唯一 LKG 源（不持进程内副本）；
     * Redis 不可用时 LKG 不可用，交由 Nacos 地板兜底，保证多节点降级来源一致。
     */
    @Bean
    @ConditionalOnMissingBean(LastKnownGoodStore.class)
    public LastKnownGoodStore credentialLastKnownGoodStore(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                                           ObjectProvider<ObjectMapper> objectMapperProvider) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        ObjectMapper mapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new LastKnownGoodStore(redisTemplate, mapper, LKG_REDIS_KEY, null);
    }

    /**
     * L0 Remote delegate（仅在没有本地 delegate 时启用，典型场景：非 ingot-security 微服务）。
     * <p>原始 Feign delegate 外包裹 {@link ResilientCredentialPolicyConfigService}，成为热缓存链最内层，
     * 提供 remote → LKG → Nacos 地板 的降级阶梯。{@code ingot-security-provider} 以本地 Mapper delegate
     * 覆盖本 bean，不经弹性兜底（本地无远程失败语义）。</p>
     */
    @Bean(name = CREDENTIAL_POLICY_CONFIG_DELEGATE)
    @ConditionalOnBean(RemoteCredentialService.class)
    @ConditionalOnMissingBean(name = CREDENTIAL_POLICY_CONFIG_DELEGATE)
    public CredentialPolicyConfigService credentialPolicyConfigDelegate(RemoteCredentialService remoteCredentialService,
                                                                        LastKnownGoodStore lkgStore,
                                                                        LocalFloorSupplier localFloorSupplier,
                                                                        CredentialPolicySourceHolder sourceHolder,
                                                                        CredentialSecurityProperties properties) {
        RemoteCredentialPolicyConfigService raw = new RemoteCredentialPolicyConfigService(remoteCredentialService);
        boolean localFloorEnabled = properties.getPolicy().getFallback().isLocalFloorEnabled();
        log.info("[Credential] register resilient remote delegate (remote -> LKG -> local-floor), localFloorEnabled={}",
                localFloorEnabled);
        return new ResilientCredentialPolicyConfigService(raw, lkgStore, localFloorSupplier, localFloorEnabled, sourceHolder);
    }

    /**
     * L2 Redis 共享缓存层。delegate 必须存在；Redis 不可用时跳过。
     */
    @Bean
    @ConditionalOnBean({StringRedisTemplate.class})
    @ConditionalOnProperty(value = "ingot.security.credential.cache.l2-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(RedisCredentialPolicyConfigService.class)
    public RedisCredentialPolicyConfigService credentialPolicyConfigRedisLayer(
            @Qualifier(CREDENTIAL_POLICY_CONFIG_DELEGATE) CredentialPolicyConfigService delegate,
            StringRedisTemplate redisTemplate,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            CredentialCacheProperties properties) {
        ObjectMapper mapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        RedisCredentialPolicyConfigService layer = CredentialPolicyConfigServiceFactory
                .composeRedisLayer(delegate, properties, redisTemplate, mapper);
        if (layer != null) {
            log.info("[Credential] L2 Redis layer enabled, ttl={}, keyPrefix={}",
                    properties.getL2Ttl(), properties.getL2KeyPrefix());
        }
        return layer;
    }

    /**
     * 对外暴露的 {@link CredentialPolicyConfigService}：在 delegate 之上叠加 L2、L1。
     */
    @Bean
    @Primary
    @ConditionalOnBean(name = CREDENTIAL_POLICY_CONFIG_DELEGATE)
    public CredentialPolicyConfigService credentialPolicyConfigService(
            @Qualifier(CREDENTIAL_POLICY_CONFIG_DELEGATE) CredentialPolicyConfigService delegate,
            ObjectProvider<RedisCredentialPolicyConfigService> redisLayerProvider,
            CredentialCacheProperties properties) {
        CredentialPolicyConfigService inner = redisLayerProvider.getIfAvailable();
        if (inner == null) {
            inner = delegate;
        }
        CredentialPolicyConfigService composed = CredentialPolicyConfigServiceFactory
                .composeCaffeineLayer(inner, properties);
        log.info("[Credential] CredentialPolicyConfigService composed (l1={}, l2={})",
                properties.isL1Enabled(), properties.isL2Enabled());
        return composed;
    }

    /**
     * 降级可观测 actuator 端点：仅当类路径存在 Spring Boot Actuator 时装配。
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    @ConditionalOnMissingBean(CredentialPolicyEndpoint.class)
    public CredentialPolicyEndpoint credentialPolicyEndpoint(CredentialPolicySourceHolder sourceHolder) {
        return new CredentialPolicyEndpoint(sourceHolder);
    }

    /**
     * 失效广播协调器：订阅 {@code CredentialInvalidationEvent}，回调时清 L1+L2 缓存。
     */
    @Bean
    @ConditionalOnBean(InvalidationBus.class)
    @ConditionalOnProperty(value = "ingot.security.credential.cache.invalidation-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(CredentialCacheCoordinator.class)
    public CredentialCacheCoordinator credentialCacheCoordinator(InvalidationBus bus,
                                                                 CredentialPolicyConfigService policyConfigService) {
        return new CredentialCacheCoordinator(bus, policyConfigService);
    }

    @Bean
    @ConditionalOnMissingBean(CredentialPolicyLoader.class)
    @ConditionalOnProperty(name = "ingot.security.credential.policy.mode", havingValue = "local", matchIfMissing = true)
    public CredentialPolicyLoader localCredentialPolicyLoader(CredentialSecurityProperties properties,
                                                              PasswordEncoder passwordEncoder) {
        return new LocalCredentialPolicyLoader(properties, passwordEncoder);
    }

    @Bean
    @ConditionalOnMissingBean(CredentialPolicyLoader.class)
    @ConditionalOnProperty(name = "ingot.security.credential.policy.mode", havingValue = "remote")
    public CredentialPolicyLoader credentialPolicyLoader(CredentialPolicyConfigService policyConfigService,
                                                         PasswordEncoder passwordEncoder) {
        return new RemoteCredentialPolicyLoader(policyConfigService, passwordEncoder);
    }

    @Bean
    @ConditionalOnMissingBean(PasswordValidator.class)
    public PasswordValidator passwordValidator(CredentialPolicyLoader policyLoader) {
        return new DefaultPasswordValidator(policyLoader);
    }

    @Bean
    @ConditionalOnMissingBean(PasswordHistoryService.class)
    public PasswordHistoryService passwordHistoryService() {
        return new NoOpPasswordHistoryService();
    }

    @Bean
    @ConditionalOnMissingBean(PasswordExpirationService.class)
    public PasswordExpirationService passwordExpirationService() {
        return new NoOpPasswordExpirationService();
    }

    @Bean
    @ConditionalOnMissingBean(InitialPasswordService.class)
    public InitialPasswordService initialPasswordService(CredentialPolicyLoader credentialPolicyLoader) {
        return new DefaultInitialPasswordService(credentialPolicyLoader);
    }

    @Bean
    @ConditionalOnMissingBean(CredentialSecurityService.class)
    public CredentialSecurityService credentialSecurityService(
            PasswordValidator passwordValidator,
            PasswordHistoryService passwordHistoryService,
            PasswordExpirationService passwordExpirationService,
            CredentialSecurityProperties properties,
            CredentialPolicyLoader credentialPolicyLoader) {
        return new DefaultCredentialSecurityService(
                passwordValidator,
                passwordHistoryService,
                passwordExpirationService,
                properties,
                credentialPolicyLoader
        );
    }
}
