package com.ingot.framework.security.account.adapter.config;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.cloud.security.api.rpc.RemoteAccountLockoutPolicyService;
import com.ingot.framework.cache.config.LayeredCacheBuilder;
import com.ingot.framework.cache.config.LayeredCacheSettings;
import com.ingot.framework.cache.registry.LayeredCacheRegistry;
import com.ingot.framework.cache.source.CacheSourceHolder;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.commons.constants.RedisKeyConstants;
import com.ingot.framework.commons.model.security.PolicySourceMode;
import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.config.EventBusAutoConfiguration;
import com.ingot.framework.security.account.adapter.actuate.AccountLockoutPolicyEndpoint;
import com.ingot.framework.security.account.adapter.policy.AccountLockoutPolicyCacheCoordinator;
import com.ingot.framework.security.account.adapter.policy.CachedAccountLockoutPolicyLoader;
import com.ingot.framework.security.account.adapter.policy.LocalAccountLockoutFloorSupplier;
import com.ingot.framework.security.account.adapter.policy.RemoteAccountLockoutPolicyLoader;
import com.ingot.framework.security.account.domain.config.AccountDomainAutoConfiguration;
import com.ingot.framework.security.account.domain.config.AccountDomainProperties;
import com.ingot.framework.security.account.domain.model.LockoutPolicy;
import com.ingot.framework.security.account.domain.service.AccountLockoutPolicyLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * <p>{@code mode=remote} 时装配账号锁定策略分层缓存链与失效协调器。</p>
 *
 * <p>本类必须排在 {@link EventBusAutoConfiguration} 之后、{@link AccountDomainAutoConfiguration}
 * 之前，以便 {@code @ConditionalOnBean(InvalidationBus)} 可见总线，且 remote loader 覆盖 local 缺省。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(EventBusAutoConfiguration.class)
@AutoConfigureBefore(AccountDomainAutoConfiguration.class)
@ConditionalOnClass(RemoteAccountLockoutPolicyService.class)
@ConditionalOnProperty(name = "ingot.security.account.mode", havingValue = PolicySourceMode.VALUE_REMOTE)
@EnableConfigurationProperties(AccountDomainProperties.class)
public class AccountLockoutPolicyRemoteAutoConfiguration {

    /**
     * remote 模式策略缓存的 Bean 名称，供协调器按名称做条件判定。
     */
    public static final String CACHE_BEAN_NAME = "accountLockoutPolicyCache";

    /**
     * 账号锁定策略来源持有者的 Bean 名称，避免与其它模块的 {@link CacheSourceHolder} 混用。
     */
    public static final String SOURCE_HOLDER_BEAN = "accountLockoutPolicySourceHolder";

    private static final String CACHE_NAME = "account-lockout-policy";
    private static final TypeReference<List<LockoutPolicy>> POLICY_LIST_TYPE = new TypeReference<>() {
    };

    /**
     * 具名来源持有者，供 Actuator 与 Resilient 层共用。
     *
     * @return 新的持有者
     */
    @Bean(SOURCE_HOLDER_BEAN)
    @ConditionalOnMissingBean(name = SOURCE_HOLDER_BEAN)
    public CacheSourceHolder accountLockoutPolicySourceHolder() {
        return new CacheSourceHolder();
    }

    /**
     * 降级来源可观测端点。
     *
     * @param sourceHolder 与缓存链共用的持有者
     * @return Actuator 端点
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    @ConditionalOnBean(name = SOURCE_HOLDER_BEAN)
    @ConditionalOnMissingBean
    public AccountLockoutPolicyEndpoint accountLockoutPolicyEndpoint(
            @Qualifier(SOURCE_HOLDER_BEAN) CacheSourceHolder sourceHolder) {
        return new AccountLockoutPolicyEndpoint(sourceHolder);
    }

    /**
     * Nacos 地板供给器。
     *
     * @param properties 账号域配置
     * @return 非空单元素地板
     */
    @Bean
    @ConditionalOnMissingBean(LocalAccountLockoutFloorSupplier.class)
    public LocalAccountLockoutFloorSupplier localAccountLockoutFloorSupplier(
            AccountDomainProperties properties) {
        return new LocalAccountLockoutFloorSupplier(properties);
    }

    /**
     * remote 模式分层缓存：L1 → L2 → Feign → LKG → 地板。
     *
     * @param remoteService         安全中心 Feign
     * @param floorSupplier         Nacos 地板
     * @param sourceHolder          来源持有者
     * @param properties            账号域配置
     * @param redisProvider         Redis，缺省则跳过 L2/LKG
     * @param objectMapperProvider  JSON
     * @param registryProvider      Actuator 注册表
     * @return 分层缓存
     */
    @Bean(CACHE_BEAN_NAME)
    @ConditionalOnMissingBean(name = CACHE_BEAN_NAME)
    public LayeredCache<String, List<LockoutPolicy>> accountLockoutPolicyCache(
            RemoteAccountLockoutPolicyService remoteService,
            LocalAccountLockoutFloorSupplier floorSupplier,
            @Qualifier(SOURCE_HOLDER_BEAN) CacheSourceHolder sourceHolder,
            AccountDomainProperties properties,
            ObjectProvider<StringRedisTemplate> redisProvider,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            ObjectProvider<LayeredCacheRegistry> registryProvider) {
        AccountDomainProperties.Cache cacheProps = properties.getPolicy().getCache();
        LayeredCacheSettings settings = LayeredCacheSettings.builder()
                .l1Enabled(cacheProps.isL1Enabled())
                .l1Ttl(cacheProps.getL1Ttl())
                .l1MaximumSize(cacheProps.getL1MaximumSize())
                .l2Enabled(cacheProps.isL2Enabled())
                .l2Ttl(cacheProps.getL2Ttl())
                .resilienceEnabled(true)
                .localFloorEnabled(properties.getPolicy().getFallback().isLocalFloorEnabled())
                .build();

        StringRedisTemplate redisTemplate = redisProvider.getIfAvailable();
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();

        log.info("[AccountLockout] register layered cache (L1 -> L2 -> remote -> LKG -> local-floor), localFloorEnabled={}",
                settings.isLocalFloorEnabled());
        return LayeredCacheBuilder.<String, List<LockoutPolicy>>named(CACHE_NAME)
                .loader(new RemoteAccountLockoutPolicyLoader(remoteService))
                .settings(settings)
                .cacheable(v -> v != null && !v.isEmpty())
                .emptyValue(List::of)
                .sourceHolder(sourceHolder)
                .resilientSingleKey(redisTemplate, objectMapper, POLICY_LIST_TYPE,
                        RedisKeyConstants.AccountLockoutPolicy.LKG, floorSupplier)
                .l2SingleKey(redisTemplate, objectMapper, POLICY_LIST_TYPE, cacheProps.getL2RedisKey())
                .registry(registryProvider.getIfAvailable())
                .build();
    }

    /**
     * 消费侧 seam 的 remote 实现。
     *
     * @param accountLockoutPolicyCache 分层缓存
     * @return 按 userType 取行的 loader
     */
    @Bean
    @ConditionalOnBean(name = CACHE_BEAN_NAME)
    @ConditionalOnMissingBean(AccountLockoutPolicyLoader.class)
    public AccountLockoutPolicyLoader cachedAccountLockoutPolicyLoader(
            LayeredCache<String, List<LockoutPolicy>> accountLockoutPolicyCache) {
        return new CachedAccountLockoutPolicyLoader(accountLockoutPolicyCache);
    }

    /**
     * 订阅 {@code ACCOUNT_LOCKOUT} 失效并清 L1/L2。
     *
     * @param bus          失效总线
     * @param policyLoader 策略加载器
     * @return 协调器
     */
    @Bean
    @ConditionalOnBean(InvalidationBus.class)
    @ConditionalOnMissingBean(AccountLockoutPolicyCacheCoordinator.class)
    public AccountLockoutPolicyCacheCoordinator accountLockoutPolicyCacheCoordinator(
            InvalidationBus bus,
            AccountLockoutPolicyLoader policyLoader) {
        AccountLockoutPolicyCacheCoordinator coordinator = new AccountLockoutPolicyCacheCoordinator(bus);
        coordinator.register(SecurityPolicyDomain.ACCOUNT_LOCKOUT, policyLoader::evictAll);
        return coordinator;
    }
}
