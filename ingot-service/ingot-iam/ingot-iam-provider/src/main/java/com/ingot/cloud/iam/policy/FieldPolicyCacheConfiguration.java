package com.ingot.cloud.iam.policy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.iam.evaluation.IamAuthorizationCacheProperties;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangedSpringEvent;
import com.ingot.framework.cache.config.*;
import com.ingot.framework.cache.coordinator.LayeredCacheCoordinator;
import com.ingot.framework.cache.registry.LayeredCacheRegistry;
import com.ingot.framework.cache.source.CacheSourceHolder;
import com.ingot.framework.cache.spi.LayeredCache;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationInvalidationEvent;
import com.ingot.framework.eventbus.InvalidationBus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.event.*;
import lombok.RequiredArgsConstructor;

/**
 * <p>租户字段配置接入统一分层缓存；写事务绕过缓存，无 LKG 或地板放行。</p>
 * @author jy
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
public class FieldPolicyCacheConfiguration {
    private static final String CACHE_NAME = "iam-field-policy";
    private static final String PREFIX = "fields-v3:policy:";
    private static final String ALL = "all";
    /** 公共默认策略当前引用键。 */
    public static final String LATEST = "latest";

    /** 本缓存独立的来源持有者。 */
    @Bean
    public CacheSourceHolder iamFieldPolicySourceHolder() { return new CacheSourceHolder(); }

    /** 装配最多 30 秒的配置缓存，身份与对象关系不存入此缓存。 */
    @Bean
    public LayeredCache<String, FieldPolicySnapshot> iamFieldPolicyCache(IamAuthorizationCacheProperties properties,
            FieldAccessEvaluator fields, ObjectMapper mapper, ObjectProvider<StringRedisTemplate> redis,
            ObjectProvider<LayeredCacheRegistry> registries,
            @Qualifier("iamFieldPolicySourceHolder") CacheSourceHolder source) {
        return LayeredCacheBuilder.<String, FieldPolicySnapshot>named(CACHE_NAME)
                .settings(LayeredCacheSettings.builder().l1Enabled(properties.isL1Enabled())
                        .l1Ttl(properties.boundedL1Ttl()).l1MaximumSize(properties.getL1MaximumSize())
                        .l2Enabled(properties.isL2Enabled()).l2Ttl(properties.boundedL2Ttl())
                        .resilienceEnabled(false).localFloorEnabled(false).build())
                .loader(key -> {
                    try {
                        var query = mapper.readValue(key, Query.class);
                        return fields.snapshotCachedConfiguration(query.tenantId(), query.scenario());
                    }
                    catch (java.io.IOException exception) { throw new IllegalArgumentException("非法字段缓存键", exception); }
                }).cacheable(value -> value != null).sourceHolder(source)
                .l2MultiKey(redis.getIfAvailable(), mapper, new TypeReference<FieldPolicySnapshot>() { },
                        properties.getRedisKeyPrefix() + PREFIX).registry(registries.getIfAvailable()).build();
    }

    /** 默认策略当前版本与不可变版本引用缓存，写路径始终直接读取持久化。 */
    @Bean
    public LayeredCache<String, FieldDefaultReference> iamFieldDefaultCache(
            IamAuthorizationCacheProperties properties, PolicyWriteRepository policies, ObjectMapper mapper,
            ObjectProvider<StringRedisTemplate> redis, ObjectProvider<LayeredCacheRegistry> registries) {
        return LayeredCacheBuilder.<String, FieldDefaultReference>named("iam-field-default")
                .settings(LayeredCacheSettings.builder().l1Enabled(properties.isL1Enabled()).l1Ttl(properties.boundedL1Ttl())
                        .l1MaximumSize(properties.getL1MaximumSize()).l2Enabled(properties.isL2Enabled()).l2Ttl(properties.boundedL2Ttl())
                        .resilienceEnabled(false).localFloorEnabled(false).build())
                .loader(key -> FieldDefaultReference.from(key.equals(LATEST) ? policies.latestRevision(com.ingot.framework.commons.model.iam.DefaultPolicyKind.FIELD)
                        : policies.findRevision(Long.parseLong(key), com.ingot.framework.commons.model.iam.DefaultPolicyKind.FIELD)))
                .cacheable(value -> value != null).sourceHolder(new CacheSourceHolder())
                .l2MultiKey(redis.getIfAvailable(), mapper, new TypeReference<FieldDefaultReference>() { },
                        properties.getRedisKeyPrefix() + PREFIX + "default:").registry(registries.getIfAvailable()).build();
    }

    /** 接收跨节点失效。 */
    @Bean @ConditionalOnBean(InvalidationBus.class)
    public LayeredCacheCoordinator<AuthorizationInvalidationEvent, String> iamFieldPolicyCoordinator(InvalidationBus bus,
            LayeredCache<String, FieldPolicySnapshot> cache,
            LayeredCache<String, FieldDefaultReference> defaults) {
        var coordinator = new LayeredCacheCoordinator<>(bus, AuthorizationInvalidationEvent.class, event -> ALL, ALL);
        coordinator.register(ALL, () -> clear(cache, defaults));
        return coordinator;
    }

    /** 发布方在广播之前清理自己的配置缓存。 */
    @Bean
    public OriginInvalidator iamFieldPolicyOriginInvalidator(LayeredCache<String, FieldPolicySnapshot> cache,
            LayeredCache<String, FieldDefaultReference> defaults) {
        return new OriginInvalidator(cache, defaults);
    }

    private static void clear(LayeredCache<?, ?>... caches) {
        for (var cache : caches) {
            try { cache.evictAll(); }
            catch (RuntimeException failure) { org.slf4j.LoggerFactory.getLogger(FieldPolicyCacheConfiguration.class).warn("字段策略失效异常: {}", cache.name(), failure); }
        }
    }

    /**
     * <p>提交后本地失效，排序在既有广播监听器之前。</p>
     * @author jy
     * @since 1.0.0
     */
    @RequiredArgsConstructor
    public static final class OriginInvalidator {
        private final LayeredCache<String, FieldPolicySnapshot> cache;
        private final LayeredCache<String, FieldDefaultReference> defaults;
        /** 提交后清理本地和共享热缓存，回滚不触发。 */
        @Order(Ordered.HIGHEST_PRECEDENCE)
        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
        public void changed(AuthorizationChangedSpringEvent event) { clear(cache, defaults); }
    }

    /**
     * <p>仅配置所需的稳定缓存身份，不包含查看者或目标原值。</p>
     * @param tenantId 策略归属租户
     * @param scenario 后台或通讯录
     * @author jy
     * @since 1.0.0
     */
    public record Query(long tenantId, com.ingot.framework.commons.model.iam.PolicyScenario scenario) { }
}
