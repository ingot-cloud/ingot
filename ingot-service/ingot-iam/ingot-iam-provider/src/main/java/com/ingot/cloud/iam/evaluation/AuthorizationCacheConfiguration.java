package com.ingot.cloud.iam.evaluation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.cache.config.LayeredCacheBuilder;
import com.ingot.framework.cache.config.LayeredCacheSettings;
import com.ingot.framework.cache.coordinator.LayeredCacheCoordinator;
import com.ingot.framework.cache.registry.LayeredCacheRegistry;
import com.ingot.framework.cache.source.CacheSourceHolder;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationInvalidationEvent;
import com.ingot.framework.eventbus.InvalidationBus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * <p>装配授权热路径分层缓存：L1/L2 最长 30 秒，关闭 Resilient/LKG 与地板放行。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IamAuthorizationCacheProperties.class)
public class AuthorizationCacheConfiguration {
    /**
     * 授权视图缓存 bean 名。
     */
    public static final String CACHE_BEAN_NAME = "iamAuthorizationViewCache";
    /**
     * 来源持有者 bean 名，避免与其它模块的同类型 bean 冲突。
     */
    public static final String SOURCE_HOLDER_BEAN_NAME = "iamAuthorizationSourceHolder";
    private static final String CACHE_NAME = "iam-authorization";
    private static final String ALL_DOMAIN = "all";
    private static final TypeReference<JdbcAuthorizationEvaluator.AuthorizationView> VIEW_TYPE = new TypeReference<>() {
    };

    /**
     * 登记本缓存的来源持有者。
     *
     * @return 独立命名的来源持有者
     */
    @Bean(SOURCE_HOLDER_BEAN_NAME)
    public CacheSourceHolder iamAuthorizationSourceHolder() {
        return new CacheSourceHolder();
    }

    /**
     * 组装授权视图分层缓存。Redis 缺失时仅保留 L1。
     *
     * @param properties 模块配置
     * @param evaluator 数据库求值器
     * @param redisProvider 可选 Redis
     * @param objectMapperProvider JSON 序列化
     * @param registryProvider 缓存注册表
     * @param sourceHolder 本缓存来源
     * @return 分层缓存
     */
    @Bean(CACHE_BEAN_NAME)
    @ConditionalOnBean(JdbcAuthorizationEvaluator.class)
    public LayeredCache<String, JdbcAuthorizationEvaluator.AuthorizationView> iamAuthorizationViewCache(
            IamAuthorizationCacheProperties properties,
            JdbcAuthorizationEvaluator evaluator,
            ObjectProvider<StringRedisTemplate> redisProvider,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            ObjectProvider<LayeredCacheRegistry> registryProvider,
            @Qualifier(SOURCE_HOLDER_BEAN_NAME) CacheSourceHolder sourceHolder) {
        LayeredCacheSettings settings = LayeredCacheSettings.builder()
                .l1Enabled(properties.isL1Enabled())
                .l1Ttl(properties.boundedL1Ttl())
                .l1MaximumSize(properties.getL1MaximumSize())
                .l2Enabled(properties.isL2Enabled())
                .l2Ttl(properties.boundedL2Ttl())
                .resilienceEnabled(false)
                .localFloorEnabled(false)
                .build();
        return LayeredCacheBuilder.<String, JdbcAuthorizationEvaluator.AuthorizationView>named(CACHE_NAME)
                .loader(evaluator::evaluateRaw)
                .settings(settings)
                .cacheable(view -> view != null)
                .sourceHolder(sourceHolder)
                .l2MultiKey(redisProvider.getIfAvailable(), objectMapperProvider.getIfAvailable(), VIEW_TYPE,
                        properties.getRedisKeyPrefix())
                .registry(registryProvider.getIfAvailable())
                .build();
    }

    /**
     * 订阅跨节点授权失效并清理本缓存；发布方仍须先清本地。
     *
     * @param bus 失效总线
     * @param cache 授权视图缓存
     * @return 协调器
     */
    @Bean
    @ConditionalOnBean({InvalidationBus.class, JdbcAuthorizationEvaluator.class})
    public LayeredCacheCoordinator<AuthorizationInvalidationEvent, String> iamAuthorizationCacheCoordinator(
            InvalidationBus bus,
            @Qualifier(CACHE_BEAN_NAME) LayeredCache<String, JdbcAuthorizationEvaluator.AuthorizationView> cache) {
        LayeredCacheCoordinator<AuthorizationInvalidationEvent, String> coordinator =
                new LayeredCacheCoordinator<>(bus, AuthorizationInvalidationEvent.class,
                        event -> ALL_DOMAIN, ALL_DOMAIN);
        coordinator.register(ALL_DOMAIN, cache::evictAll);
        return coordinator;
    }
}
