package com.ingot.framework.security.credential.config;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.security.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.cloud.security.api.rpc.RemoteCredentialService;
import com.ingot.framework.cache.config.LayeredCacheBuilder;
import com.ingot.framework.cache.config.LayeredCacheSettings;
import com.ingot.framework.cache.registry.LayeredCacheRegistry;
import com.ingot.framework.cache.source.CacheSourceHolder;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.commons.model.security.PolicySourceMode;
import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.config.EventBusAutoConfiguration;
import com.ingot.framework.security.credential.actuate.CredentialPolicyEndpoint;
import com.ingot.framework.security.credential.internal.CredentialCacheCoordinator;
import com.ingot.framework.security.credential.internal.LayeredCredentialPolicyConfigService;
import com.ingot.framework.security.credential.internal.LocalFloorSupplier;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * <p>凭证安全自动配置：装配策略配置分层缓存、密码校验与初始密码服务。</p>
 *
 * <p>装配顺序：</p>
 * <ol>
 *     <li>L0 delegate：默认基于 {@link RemoteCredentialService} 的 Feign 实现；
 *         {@code ingot-security-provider} 进程内通过同名 bean 覆盖为本地 Mapper 直查，不包 Resilient。</li>
 *     <li>对外 {@link CredentialPolicyConfigService}：在 delegate 之上叠加 L1/L2；
 *         仅当 delegate 是远端 Feign 时再套 remote → LKG → 地板。</li>
 *     <li>跨节点失效：{@link InvalidationBus} 存在且 {@code invalidation-enabled=true} 时
 *         注册 {@link CredentialCacheCoordinator}。</li>
 * </ol>
 *
 * <p>本类必须排在 {@link EventBusAutoConfiguration} 之后：协调器带
 * {@code @ConditionalOnBean(InvalidationBus.class)}；若早于 event-bus 配置类执行，
 * 会因总线尚未注册而导致协调器永远跳过。</p>
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
     * 凭证策略来源持有者的 Bean 名称，避免与其它模块的 {@link CacheSourceHolder} 混用。
     */
    public static final String SOURCE_HOLDER_BEAN = "credentialPolicySourceHolder";

    /**
     * 策略缓存的 Bean 名称。
     */
    public static final String CACHE_BEAN_NAME = "credentialPolicyConfigCache";

    /**
     * LKG 快照独立 Redis key（与 L1/L2 热缓存命名空间区分，长存 / 不过期）。
     */
    static final String LKG_REDIS_KEY = "in:credential:policy:lkg";

    private static final String CACHE_NAME = "credential";
    private static final TypeReference<List<CredentialPolicyConfigVO>> POLICY_LIST_TYPE = new TypeReference<>() {
    };

    @Bean(SOURCE_HOLDER_BEAN)
    @ConditionalOnMissingBean(name = SOURCE_HOLDER_BEAN)
    public CacheSourceHolder credentialPolicySourceHolder() {
        return new CacheSourceHolder();
    }

    @Bean
    @ConditionalOnMissingBean(LocalFloorSupplier.class)
    public LocalFloorSupplier credentialLocalFloorSupplier(CredentialSecurityProperties properties) {
        return new LocalFloorSupplier(properties);
    }

    /**
     * L0 Remote delegate（仅在没有本地 delegate 时启用，典型场景：非 ingot-security 微服务）。
     * <p>{@code ingot-security-provider} 以本地 Mapper delegate 覆盖本 bean，不经弹性兜底
     * （本地无远程失败语义）。L1/L2 与 Resilient 由 {@link #credentialPolicyConfigService} 叠加。</p>
     */
    @Bean(name = CREDENTIAL_POLICY_CONFIG_DELEGATE)
    @ConditionalOnBean(RemoteCredentialService.class)
    @ConditionalOnMissingBean(name = CREDENTIAL_POLICY_CONFIG_DELEGATE)
    public CredentialPolicyConfigService credentialPolicyConfigDelegate(RemoteCredentialService remoteCredentialService) {
        log.info("[Credential] register remote delegate (RemoteCredentialPolicyConfigService)");
        return new RemoteCredentialPolicyConfigService(remoteCredentialService);
    }

    /**
     * 策略配置分层缓存。远端 delegate 套完整降级阶梯；provider 本地 delegate 只叠 L1/L2。
     */
    @Bean(CACHE_BEAN_NAME)
    @ConditionalOnBean(name = CREDENTIAL_POLICY_CONFIG_DELEGATE)
    @ConditionalOnMissingBean(name = CACHE_BEAN_NAME)
    public LayeredCache<String, List<CredentialPolicyConfigVO>> credentialPolicyConfigCache(
            @Qualifier(CREDENTIAL_POLICY_CONFIG_DELEGATE) CredentialPolicyConfigService delegate,
            LocalFloorSupplier localFloorSupplier,
            @Qualifier(SOURCE_HOLDER_BEAN) CacheSourceHolder sourceHolder,
            CredentialCacheProperties cacheProperties,
            CredentialSecurityProperties securityProperties,
            ObjectProvider<StringRedisTemplate> redisProvider,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            ObjectProvider<LayeredCacheRegistry> registryProvider) {
        boolean remoteDelegate = delegate instanceof RemoteCredentialPolicyConfigService;
        LayeredCacheSettings settings = LayeredCacheSettings.builder()
                .l1Enabled(cacheProperties.isL1Enabled())
                .l1Ttl(cacheProperties.getL1Ttl())
                .l1MaximumSize(cacheProperties.getL1MaximumSize())
                .l2Enabled(cacheProperties.isL2Enabled())
                .l2Ttl(cacheProperties.getL2Ttl())
                .resilienceEnabled(remoteDelegate)
                .localFloorEnabled(securityProperties.getPolicy().getFallback().isLocalFloorEnabled())
                .build();

        StringRedisTemplate redisTemplate = redisProvider.getIfAvailable();
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();
        String l2Key = cacheProperties.getL2KeyPrefix() + LayeredCredentialPolicyConfigService.CACHE_KEY;

        LayeredCacheBuilder<String, List<CredentialPolicyConfigVO>> builder =
                LayeredCacheBuilder.<String, List<CredentialPolicyConfigVO>>named(CACHE_NAME)
                        .loader(key -> delegate.getAll())
                        .settings(settings)
                        .cacheable(v -> v != null && !v.isEmpty())
                        .emptyValue(List::of)
                        .sourceHolder(sourceHolder)
                        .l2SingleKey(redisTemplate, objectMapper, POLICY_LIST_TYPE, l2Key)
                        .registry(registryProvider.getIfAvailable());
        if (remoteDelegate) {
            builder.resilientSingleKey(redisTemplate, objectMapper, POLICY_LIST_TYPE,
                    LKG_REDIS_KEY, localFloorSupplier);
        }
        log.info("[Credential] layered cache assembled (l1={}, l2={}, resilient={})",
                cacheProperties.isL1Enabled(), cacheProperties.isL2Enabled(), remoteDelegate);
        return builder.build();
    }

    /**
     * 对外暴露的 {@link CredentialPolicyConfigService}：{@code @Primary}，供校验与 loader 注入。
     */
    @Bean
    @Primary
    @ConditionalOnBean(name = CACHE_BEAN_NAME)
    public CredentialPolicyConfigService credentialPolicyConfigService(
            @Qualifier(CACHE_BEAN_NAME) LayeredCache<String, List<CredentialPolicyConfigVO>> credentialPolicyConfigCache) {
        return new LayeredCredentialPolicyConfigService(credentialPolicyConfigCache);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    @ConditionalOnMissingBean(CredentialPolicyEndpoint.class)
    public CredentialPolicyEndpoint credentialPolicyEndpoint(
            @Qualifier(SOURCE_HOLDER_BEAN) CacheSourceHolder sourceHolder) {
        return new CredentialPolicyEndpoint(sourceHolder);
    }

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
    @ConditionalOnProperty(name = "ingot.security.credential.policy.mode", havingValue = PolicySourceMode.VALUE_LOCAL, matchIfMissing = true)
    public CredentialPolicyLoader localCredentialPolicyLoader(CredentialSecurityProperties properties,
                                                              PasswordEncoder passwordEncoder) {
        return new LocalCredentialPolicyLoader(properties, passwordEncoder);
    }

    @Bean
    @ConditionalOnMissingBean(CredentialPolicyLoader.class)
    @ConditionalOnProperty(name = "ingot.security.credential.policy.mode", havingValue = PolicySourceMode.VALUE_REMOTE)
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
