package com.ingot.framework.security.credential.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.security.api.rpc.RemoteCredentialService;
import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.config.EventBusAutoConfiguration;
import com.ingot.framework.security.credential.internal.CredentialCacheCoordinator;
import com.ingot.framework.security.credential.internal.CredentialPolicyConfigServiceFactory;
import com.ingot.framework.security.credential.internal.LocalCompiledPolicyCache;
import com.ingot.framework.security.credential.internal.RedisCredentialPolicyConfigService;
import com.ingot.framework.security.credential.internal.RemoteCredentialPolicyConfigService;
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
     * L0 Remote delegate（仅在没有本地 delegate 时启用，典型场景：非 ingot-security 微服务）。
     */
    @Bean(name = CREDENTIAL_POLICY_CONFIG_DELEGATE)
    @ConditionalOnBean(RemoteCredentialService.class)
    @ConditionalOnMissingBean(name = CREDENTIAL_POLICY_CONFIG_DELEGATE)
    public CredentialPolicyConfigService credentialPolicyConfigDelegate(RemoteCredentialService remoteCredentialService) {
        log.info("[Credential] register remote delegate (RemoteCredentialPolicyConfigService)");
        return new RemoteCredentialPolicyConfigService(remoteCredentialService);
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
     * 编译后的策略列表本地缓存。
     */
    @Bean
    @ConditionalOnMissingBean(LocalCompiledPolicyCache.class)
    public LocalCompiledPolicyCache localCompiledPolicyCache() {
        return new LocalCompiledPolicyCache();
    }

    /**
     * 失效广播协调器：订阅 {@code CredentialInvalidationEvent}，回调时清 L1+L2 与编译策略。
     */
    @Bean
    @ConditionalOnBean(InvalidationBus.class)
    @ConditionalOnProperty(value = "ingot.security.credential.cache.invalidation-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(CredentialCacheCoordinator.class)
    public CredentialCacheCoordinator credentialCacheCoordinator(InvalidationBus bus,
                                                                 CredentialPolicyConfigService policyConfigService,
                                                                 LocalCompiledPolicyCache compiledPolicyCache) {
        return new CredentialCacheCoordinator(bus, policyConfigService, compiledPolicyCache);
    }

    @Bean
    @ConditionalOnMissingBean(CredentialPolicyLoader.class)
    @ConditionalOnProperty(name = "ingot.security.credential.policy.mode", havingValue = "local", matchIfMissing = true)
    public CredentialPolicyLoader localCredentialPolicyLoader(CredentialSecurityProperties properties,
                                                              LocalCompiledPolicyCache compiledPolicyCache,
                                                              PasswordEncoder passwordEncoder) {
        return new LocalCredentialPolicyLoader(properties, compiledPolicyCache, passwordEncoder);
    }

    @Bean
    @ConditionalOnMissingBean(CredentialPolicyLoader.class)
    @ConditionalOnProperty(name = "ingot.security.credential.policy.mode", havingValue = "remote")
    public CredentialPolicyLoader credentialPolicyLoader(CredentialPolicyConfigService policyConfigService,
                                                         LocalCompiledPolicyCache compiledPolicyCache,
                                                         PasswordEncoder passwordEncoder) {
        return new RemoteCredentialPolicyLoader(policyConfigService, compiledPolicyCache, passwordEncoder);
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
    public InitialPasswordService initialPasswordService(CredentialSecurityProperties properties) {
        return new DefaultInitialPasswordService(properties);
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
