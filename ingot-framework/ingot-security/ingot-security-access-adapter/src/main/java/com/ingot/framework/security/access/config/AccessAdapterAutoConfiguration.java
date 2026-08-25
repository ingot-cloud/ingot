package com.ingot.framework.security.access.config;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.security.api.event.SecurityPolicyDomain;
import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;
import com.ingot.cloud.security.api.rpc.RemoteLoginFailurePolicyService;
import com.ingot.framework.cache.config.LayeredCacheBuilder;
import com.ingot.framework.cache.config.LayeredCacheSettings;
import com.ingot.framework.cache.registry.LayeredCacheRegistry;
import com.ingot.framework.cache.source.CacheSourceHolder;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.commons.constants.RedisKeyConstants;
import com.ingot.framework.commons.model.security.PolicySourceMode;
import com.ingot.framework.eventbus.InvalidationBus;
import com.ingot.framework.eventbus.config.EventBusAutoConfiguration;
import com.ingot.framework.security.access.actuate.LoginFailurePolicyEndpoint;
import com.ingot.framework.security.access.internal.LocalLoginFailureFloorSupplier;
import com.ingot.framework.security.access.internal.LoginFailurePolicyCacheCoordinator;
import com.ingot.framework.security.access.listener.LoginFailureAccessListener;
import com.ingot.framework.security.access.model.LoginFailurePolicy;
import com.ingot.framework.security.access.redis.RedisLoginFailureCounter;
import com.ingot.framework.security.access.redis.RedisTempBlockWriter;
import com.ingot.framework.security.access.service.LoginFailureCounter;
import com.ingot.framework.security.access.service.LoginFailurePolicyLoader;
import com.ingot.framework.security.access.service.LoginFailureProtectionService;
import com.ingot.framework.security.access.service.TempBlockWriter;
import com.ingot.framework.security.access.service.impl.CachedLoginFailurePolicyLoader;
import com.ingot.framework.security.access.service.impl.LocalLoginFailurePolicyLoader;
import com.ingot.framework.security.access.service.impl.RemoteLoginFailurePolicyLoader;
import com.ingot.framework.security.recording.transport.feign.SecurityEventReportPublisher;
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
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * <p>访问防护适配器自动配置：装配登录失败策略加载链、计数/封禁 Redis 实现与失效协调器。</p>
 *
 * <p>{@code mode=local}（默认）读 Nacos 四维配置；{@code mode=remote} 走
 * {@code L1 → L2 → remote → LKG → Nacos 地板}。本类必须排在 {@link EventBusAutoConfiguration}
 * 之后，否则 {@code @ConditionalOnBean(InvalidationBus.class)} 会因总线尚未注册而永久跳过协调器。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(EventBusAutoConfiguration.class)
@EnableConfigurationProperties(AccessProtectionProperties.class)
public class AccessAdapterAutoConfiguration {

    /**
     * remote 模式策略缓存的 Bean 名称，供协调器按名称做条件判定。
     */
    public static final String CACHE_BEAN_NAME = "loginFailurePolicyCache";

    /**
     * 登录失败策略来源持有者的 Bean 名称，避免与其它模块的 {@link CacheSourceHolder} 混用。
     */
    public static final String SOURCE_HOLDER_BEAN = "loginFailurePolicySourceHolder";

    private static final String CACHE_NAME = "login-failure-policy";
    private static final TypeReference<List<LoginFailurePolicy>> POLICY_LIST_TYPE = new TypeReference<>() {
    };

    @Bean(SOURCE_HOLDER_BEAN)
    @ConditionalOnMissingBean(name = SOURCE_HOLDER_BEAN)
    public CacheSourceHolder loginFailurePolicySourceHolder() {
        return new CacheSourceHolder();
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    @ConditionalOnBean(name = SOURCE_HOLDER_BEAN)
    @ConditionalOnMissingBean
    public LoginFailurePolicyEndpoint loginFailurePolicyEndpoint(
            @Qualifier(SOURCE_HOLDER_BEAN) CacheSourceHolder sourceHolder) {
        return new LoginFailurePolicyEndpoint(sourceHolder);
    }

    @Bean
    @ConditionalOnMissingBean(LocalLoginFailureFloorSupplier.class)
    public LocalLoginFailureFloorSupplier localLoginFailureFloorSupplier(AccessProtectionProperties properties) {
        return new LocalLoginFailureFloorSupplier(properties);
    }

    @Bean
    @ConditionalOnMissingBean(LoginFailurePolicyLoader.class)
    @ConditionalOnProperty(name = "ingot.security.access.mode", havingValue = PolicySourceMode.VALUE_LOCAL, matchIfMissing = true)
    public LoginFailurePolicyLoader localLoginFailurePolicyLoader(AccessProtectionProperties properties) {
        return new LocalLoginFailurePolicyLoader(properties);
    }

    /**
     * remote 模式的分层缓存：首次补齐 L1+L2，使失效广播能真实清缓存。
     */
    @Bean(CACHE_BEAN_NAME)
    @ConditionalOnClass(RemoteLoginFailurePolicyService.class)
    @ConditionalOnMissingBean(name = CACHE_BEAN_NAME)
    @ConditionalOnProperty(name = "ingot.security.access.mode", havingValue = PolicySourceMode.VALUE_REMOTE)
    public LayeredCache<String, List<LoginFailurePolicy>> loginFailurePolicyCache(
            RemoteLoginFailurePolicyService remoteService,
            LocalLoginFailureFloorSupplier floorSupplier,
            @Qualifier(SOURCE_HOLDER_BEAN) CacheSourceHolder sourceHolder,
            AccessProtectionProperties properties,
            ObjectProvider<StringRedisTemplate> redisProvider,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            ObjectProvider<LayeredCacheRegistry> registryProvider) {
        AccessProtectionProperties.Cache cacheProps = properties.getPolicy().getCache();
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

        log.info("[LoginFailure] register layered cache (L1 -> L2 -> remote -> LKG -> local-floor), localFloorEnabled={}",
                settings.isLocalFloorEnabled());
        return LayeredCacheBuilder.<String, List<LoginFailurePolicy>>named(CACHE_NAME)
                .loader(new RemoteLoginFailurePolicyLoader(remoteService))
                .settings(settings)
                .cacheable(v -> v != null && !v.isEmpty())
                .emptyValue(List::of)
                .sourceHolder(sourceHolder)
                .resilientSingleKey(redisTemplate, objectMapper, POLICY_LIST_TYPE,
                        RedisKeyConstants.LoginFailure.POLICY_LKG, floorSupplier)
                .l2SingleKey(redisTemplate, objectMapper, POLICY_LIST_TYPE, cacheProps.getL2RedisKey())
                .registry(registryProvider.getIfAvailable())
                .build();
    }

    @Bean
    @ConditionalOnBean(name = CACHE_BEAN_NAME)
    @ConditionalOnMissingBean(LoginFailurePolicyLoader.class)
    @ConditionalOnProperty(name = "ingot.security.access.mode", havingValue = PolicySourceMode.VALUE_REMOTE)
    public LoginFailurePolicyLoader cachedLoginFailurePolicyLoader(
            LayeredCache<String, List<LoginFailurePolicy>> loginFailurePolicyCache) {
        return new CachedLoginFailurePolicyLoader(loginFailurePolicyCache);
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(LoginFailureCounter.class)
    public LoginFailureCounter redisLoginFailureCounter(StringRedisTemplate redisTemplate) {
        return new RedisLoginFailureCounter(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(LoginFailureCounter.class)
    public LoginFailureCounter noopLoginFailureCounter() {
        return new LoginFailureCounter() {
            @Override
            public long increment(String counterKey, java.time.Duration window) {
                return 0L;
            }

            @Override
            public void reset(String counterKey) {
                // no-op
            }
        };
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(TempBlockWriter.class)
    public TempBlockWriter redisTempBlockWriter(StringRedisTemplate redisTemplate) {
        return new RedisTempBlockWriter(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(TempBlockWriter.class)
    public TempBlockWriter noopTempBlockWriter() {
        return (keyType, keyValue, ttl) -> {
            // no-op
        };
    }

    @Bean
    @ConditionalOnMissingBean(LoginFailureProtectionService.class)
    public LoginFailureProtectionService loginFailureProtectionService(
            LoginFailurePolicyLoader policyLoader,
            LoginFailureCounter counter,
            TempBlockWriter tempBlockWriter,
            LoginFailureEventSink eventSink) {
        return new LoginFailureProtectionService(
                policyLoader,
                counter,
                tempBlockWriter,
                eventSink::offer);
    }

    @Bean
    @ConditionalOnBean(LoginFailureProtectionService.class)
    @ConditionalOnMissingBean(LoginFailureAccessListener.class)
    public LoginFailureAccessListener loginFailureAccessListener(
            LoginFailureProtectionService loginFailureProtectionService) {
        log.info("[LoginFailure] register LoginFailureAccessListener");
        return new LoginFailureAccessListener(loginFailureProtectionService);
    }

    @Bean
    @ConditionalOnBean(InvalidationBus.class)
    @ConditionalOnMissingBean(LoginFailurePolicyCacheCoordinator.class)
    public LoginFailurePolicyCacheCoordinator loginFailurePolicyCacheCoordinator(
            InvalidationBus bus,
            LoginFailurePolicyLoader policyLoader) {
        LoginFailurePolicyCacheCoordinator coordinator = new LoginFailurePolicyCacheCoordinator(bus);
        coordinator.register(SecurityPolicyDomain.LOGIN_FAILURE_PROTECTION, policyLoader::evictAll);
        return coordinator;
    }

    @Bean
    @ConditionalOnBean(SecurityEventReportPublisher.class)
    @ConditionalOnMissingBean(LoginFailureEventSink.class)
    public LoginFailureEventSink recordingLoginFailureEventSink(SecurityEventReportPublisher reportPublisher) {
        return reportPublisher::publish;
    }

    @Bean
    @ConditionalOnMissingBean(LoginFailureEventSink.class)
    public LoginFailureEventSink noopLoginFailureEventSink() {
        return dto -> {
            // recording 未装配时不阻塞登录防护
        };
    }

    /**
     * <p>登录失败事件投递口，隔离 recording 是否装配。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    @FunctionalInterface
    interface LoginFailureEventSink {
        /**
         * 投递一条登录失败相关安全事件；实现必须 fail-open，不得阻塞登录主路径。
         *
         * @param dto 事件内容
         */
        void offer(SecurityEventReportDTO dto);
    }
}
