package com.ingot.cloud.iam.extension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangedSpringEvent;
import com.ingot.cloud.iam.evaluation.IamAuthorizationCacheProperties;
import com.ingot.framework.cache.config.*;
import com.ingot.framework.cache.coordinator.LayeredCacheCoordinator;
import com.ingot.framework.cache.registry.LayeredCacheRegistry;
import com.ingot.framework.cache.source.CacheSourceHolder;
import com.ingot.framework.cache.spi.*;
import com.ingot.framework.commons.model.iam.extension.*;
import com.ingot.framework.data.mybatis.scope.authorization.AuthorizationInvalidationEvent;
import com.ingot.framework.eventbus.InvalidationBus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.event.*;

/**
 * <p>清单、目录上限和不可变角色字段版本的统一缓存，禁止授权故障降级放行。</p>
 * @author jy
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false) @RequiredArgsConstructor
public class FieldMetadataCacheConfiguration {
    private static final String MANIFEST_SOURCE = "iamFieldManifestSource";
    private static final String METADATA_SOURCE = "iamFieldMetadataSource";
    private static final String VERSION_SOURCE = "iamFieldVersionSource";
    private static final String PREFIX = "fields-v3:";
    private static final String ALL = "all";
    private final IamAuthorizationCacheProperties properties;
    private final ObjectMapper mapper;
    private final ObjectProvider<StringRedisTemplate> redis;
    private final ObjectProvider<LayeredCacheRegistry> registries;

    /** 清单独立来源。 */
    @Bean(MANIFEST_SOURCE) public CacheSourceHolder manifestSource() { return new CacheSourceHolder(); }
    /** 目录能力独立来源。 */
    @Bean(METADATA_SOURCE) public CacheSourceHolder metadataSource() { return new CacheSourceHolder(); }
    /** 固定版本独立来源。 */
    @Bean(VERSION_SOURCE) public CacheSourceHolder versionSource() { return new CacheSourceHolder(); }

    /** 签名远程清单缓存，无用户身份依赖。 */
    @Bean
    public LayeredCache<String, FieldBindingManifest> iamFieldManifestCache(FieldManifestService service,
            @Qualifier(MANIFEST_SOURCE) CacheSourceHolder source) {
        return builder("iam-field-manifest", source, new TypeReference<FieldBindingManifest>() { })
                .loader(key -> {
                    try { return service.load(mapper.readValue(key, ResourceKey.class)); }
                    catch (java.io.IOException failure) { throw new RemoteUnavailableException("字段清单键无效", failure); }
                }).cacheable(value -> value != null && !value.bindings().isEmpty()).build();
    }

    /** 当前目录能力缓存，写事务使用 ResourceFieldMetadata.requireFresh。 */
    @Bean
    public LayeredCache<String, ResourceFieldMetadata.Entry> iamFieldMetadataCache(ResourceFieldMetadata service,
            @Qualifier(METADATA_SOURCE) CacheSourceHolder source) {
        return builder("iam-field-metadata", source, new TypeReference<ResourceFieldMetadata.Entry>() { })
                .loader(key -> {
                    try {
                        var lookup = mapper.readValue(key, ResourceFieldMetadata.Lookup.class);
                        return service.requireFresh(lookup.resource(), lookup.administrator());
                    }
                    catch (java.io.IOException failure) { throw new RemoteUnavailableException("字段目录键无效", failure); }
                }).cacheable(value -> value != null).build();
    }

    /** 已发布不可变版本的批量解析产物，当前分配与身份事实仍 fresh。 */
    @Bean
    public LayeredCache<String, RoleFieldPermissionService.FrozenVersions> iamFieldVersionCache(RoleFieldPermissionService service,
            @Qualifier(VERSION_SOURCE) CacheSourceHolder source) {
        return builder("iam-field-versions", source, new TypeReference<RoleFieldPermissionService.FrozenVersions>() { })
                .loader(service::loadVersions).cacheable(value -> value != null && !value.values().isEmpty()).build();
    }

    private <T> LayeredCacheBuilder<String, T> builder(String name, CacheSourceHolder source, TypeReference<T> type) {
        return LayeredCacheBuilder.<String, T>named(name).sourceHolder(source)
                .settings(LayeredCacheSettings.builder().l1Enabled(properties.isL1Enabled())
                        .l1Ttl(properties.boundedL1Ttl()).l1MaximumSize(properties.getL1MaximumSize())
                        .l2Enabled(properties.isL2Enabled()).l2Ttl(properties.boundedL2Ttl())
                        .resilienceEnabled(false).localFloorEnabled(false).build())
                .l2MultiKey(redis.getIfAvailable(), mapper, type, properties.getRedisKeyPrefix() + PREFIX + name + ":")
                .registry(registries.getIfAvailable());
    }

    /** 跨节点授权或目录变化同时清理三个独立缓存。 */
    @Bean @ConditionalOnBean(InvalidationBus.class)
    public LayeredCacheCoordinator<AuthorizationInvalidationEvent, String> iamFieldMetadataCoordinator(InvalidationBus bus,
            LayeredCache<String, FieldBindingManifest> manifests, LayeredCache<String, ResourceFieldMetadata.Entry> metadata,
            LayeredCache<String, RoleFieldPermissionService.FrozenVersions> versions) {
        var coordinator = new LayeredCacheCoordinator<>(bus, AuthorizationInvalidationEvent.class, event -> ALL, ALL);
        coordinator.register(ALL, () -> clear(manifests, metadata, versions));
        return coordinator;
    }

    /** 发布方提交后先清本地，再由已有监听器广播。 */
    @Bean
    public OriginInvalidator iamFieldMetadataOriginInvalidator(LayeredCache<String, FieldBindingManifest> manifests,
            LayeredCache<String, ResourceFieldMetadata.Entry> metadata, LayeredCache<String, RoleFieldPermissionService.FrozenVersions> versions) {
        return new OriginInvalidator(manifests, metadata, versions);
    }

    private static void clear(LayeredCache<?, ?>... caches) {
        for (var cache : caches) {
            try { cache.evictAll(); }
            catch (RuntimeException failure) {
                org.slf4j.LoggerFactory.getLogger(FieldMetadataCacheConfiguration.class).warn("字段缓存失效异常: {}", cache.name(), failure);
            }
        }
    }

    /**
     * <p>提交后本地失效，失败不阻止其他缓存清理和广播。</p>
     * @author jy
     * @since 1.0.0
     */
    @RequiredArgsConstructor
    public static final class OriginInvalidator {
        private final LayeredCache<String, FieldBindingManifest> manifests;
        private final LayeredCache<String, ResourceFieldMetadata.Entry> metadata;
        private final LayeredCache<String, RoleFieldPermissionService.FrozenVersions> versions;
        /** 回滚不会清理授权缓存。 */
        @Order(Ordered.HIGHEST_PRECEDENCE)
        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
        public void changed(AuthorizationChangedSpringEvent event) { clear(manifests, metadata, versions); }
    }
}
